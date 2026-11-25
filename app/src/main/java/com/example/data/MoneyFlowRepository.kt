package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.DayOverride
import com.example.domain.IndiaDate
import com.example.domain.WorkStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Single source of truth for MoneyFlow cloud persistence in Cloud Firestore.
 * Always connects to the provisioned named database ID (`R.string.firestore_database_id`),
 * isolates all documents under `/users/{uid}`, and listens in real time for remote edits.
 */
class MoneyFlowRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUserId(expectedUserId: String): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(currentUid == expectedUserId) {
            "Signed-in user changed; refusing cross-user Firestore operation."
        }
        require(currentUid.isNotBlank()) { "Authenticated UID must not be blank." }
        return currentUid
    }

    fun observeProfile(userId: String): Flow<UserProfileDoc?> = flow {
        val uid = requireAuthorizedUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeDayEntries(userId: String): Flow<List<DayEntryDoc>> = flow {
        val uid = requireAuthorizedUserId(userId)
        val path = "users/$uid/dayEntries"
        emitAll(
            db.collection("users").document(uid).collection("dayEntries")
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        DayEntryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Initializes the private user profile document `/users/{uid}` on first sign-in without
     * fabricating any day entries. If the profile already exists, preserves `trackingStartCycle`
     * and immutable `createdAt`.
     */
    suspend fun ensureProfileInitialized(
        userId: String,
        email: String?,
        displayName: String?,
        defaultTrackingStartCycle: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(CYCLE_START_REGEX.matches(defaultTrackingStartCycle)) {
                "Tracking start cycle must match YYYY-MM-24"
            }
            val cleanEmail = email?.trim()?.take(254)?.takeIf { it.isNotEmpty() }
            val cleanName = displayName?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "trackingStartCycle" to defaultTrackingStartCycle,
                    "timezone" to "Asia/Kolkata",
                    "schemaVersion" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail != null) createPayload["email"] = cleanEmail
                if (cleanName != null) createPayload["displayName"] = cleanName
                docRef.set(createPayload).await()
            } else {
                val existingEmail = existing.getString("email")
                val existingName = existing.getString("displayName")
                if (cleanEmail != existingEmail || cleanName != existingName) {
                    val updates = mutableMapOf<String, Any>(
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (cleanEmail != null) updates["email"] = cleanEmail
                    if (cleanName != null) updates["displayName"] = cleanName
                    if (updates.size > 1) {
                        docRef.update(updates).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTrackingStartCycle(
        userId: String,
        trackingStartCycle: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(CYCLE_START_REGEX.matches(trackingStartCycle)) {
                "Tracking start cycle must be on the 24th (YYYY-MM-24)."
            }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "trackingStartCycle" to trackingStartCycle,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Persists a single per-date override document at `/users/{uid}/dayEntries/{YYYY-MM-DD}`.
     * Uses `.set()` on creation and partial `.update()` on existing documents so immutable
     * `createdAt`, `userId`, and `date` fields are strictly preserved.
     */
    suspend fun saveDayOverride(
        userId: String,
        override: DayOverride,
        documentExists: Boolean
    ): Result<Unit> {
        val dateKey = override.date.toIsoString()
        val path = "users/$userId/dayEntries/$dateKey"
        return try {
            val uid = requireAuthorizedUserId(userId)
            require(override.workStatus in WorkStatus.validStatusesFor(override.dayType)) {
                "WorkStatus ${override.workStatus} is invalid for ${override.dayType}"
            }
            val cleanNote = override.note?.trim()?.take(200)?.takeIf { it.isNotEmpty() }
            val cleanExitTime = override.exitTime?.toWireString()
            val docRef = db.collection("users").document(uid)
                .collection("dayEntries").document(dateKey)

            val existsNow = if (documentExists) true else docRef.get().await().exists()
            if (existsNow) {
                val updates = mutableMapOf<String, Any>(
                    "dayType" to override.dayType.name,
                    "workStatus" to override.workStatus.name,
                    "overtimeEnabled" to override.overtimeEnabled,
                    "exitTime" to (cleanExitTime ?: FieldValue.delete()),
                    "note" to (cleanNote ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "date" to dateKey,
                    "dayType" to override.dayType.name,
                    "workStatus" to override.workStatus.name,
                    "overtimeEnabled" to override.overtimeEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanExitTime != null) payload["exitTime"] = cleanExitTime
                if (cleanNote != null) payload["note"] = cleanNote
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (documentExists) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDayOverride(
        userId: String,
        date: IndiaDate
    ): Result<Unit> {
        val dateKey = date.toIsoString()
        val path = "users/$userId/dayEntries/$dateKey"
        return try {
            val uid = requireAuthorizedUserId(userId)
            db.collection("users").document(uid)
                .collection("dayEntries").document(dateKey)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private companion object {
        val CYCLE_START_REGEX = Regex("^[0-9]{4}-[0-9]{2}-24$")
    }
}
