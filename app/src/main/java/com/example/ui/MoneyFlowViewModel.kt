package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DayEntryDoc
import com.example.data.MoneyFlowRepository
import com.example.data.UserProfileDoc
import com.example.data.extractFriendlyFirestoreError
import com.example.domain.CycleSummary
import com.example.domain.DayOverride
import com.example.domain.IndiaDate
import com.example.domain.MonthlySummary
import com.example.domain.PayrollEngine
import com.example.domain.SalaryCycle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SyncStatus {
    data object Connected : SyncStatus
    data object Syncing : SyncStatus
    data object Offline : SyncStatus
    data class Error(val message: String) : SyncStatus
}

data class MoneyFlowUiState(
    val today: IndiaDate,
    val currentCycle: SalaryCycle,
    val trackingStartCycle: IndiaDate,
    val cycleSummaries: List<CycleSummary>,
    val selectedCycleSummary: CycleSummary,
    val currentMonthSummary: MonthlySummary,
    val selectedDate: IndiaDate,
    val todayEntry: com.example.domain.DayEarnings,
    val overrides: Map<IndiaDate, DayOverride>,
    val existingEntryKeys: Set<String>,
    val syncStatus: SyncStatus,
    val isSaving: Boolean,
    val editingDate: IndiaDate?,
    val showAccountSheet: Boolean,
    val showDatePickerDialog: Boolean,
    val bannerMessage: String?,
    val isBannerError: Boolean,
    val userEmail: String?,
    val userDisplayName: String?,
    val firebaseProjectId: String,
    val firestoreDatabaseId: String
)

private sealed interface RemoteDataState {
    data object Loading : RemoteDataState
    data class Loaded(
        val profile: UserProfileDoc?,
        val entries: List<DayEntryDoc>,
        val isFromCache: Boolean,
        val hasPendingWrites: Boolean
    ) : RemoteDataState
    data class Failed(val message: String) : RemoteDataState
}

private data class LocalUiInteractionState(
    val selectedCycleId: String? = null,
    val selectedDate: IndiaDate? = null,
    val editingDate: IndiaDate? = null,
    val showAccountSheet: Boolean = false,
    val showDatePickerDialog: Boolean = false,
    val isSaving: Boolean = false,
    val profileInitError: String? = null,
    val bannerMessage: String? = null,
    val isBannerError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyFlowViewModel(
    private val repository: MoneyFlowRepository,
    private val currentUserId: String,
    private val userEmail: String?,
    private val userDisplayName: String?,
    private val firebaseProjectId: String,
    private val firestoreDatabaseId: String,
    private val todayProvider: () -> IndiaDate = { IndiaDate.todayInIndia() }
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val localState = MutableStateFlow(LocalUiInteractionState())

    private val remoteState: StateFlow<RemoteDataState> = retryTrigger
        .flatMapLatest {
            combine(
                repository.observeProfile(currentUserId),
                repository.observeDayEntries(currentUserId)
            ) { profileSnap, entriesSnap ->
                RemoteDataState.Loaded(
                    profile = profileSnap.data,
                    entries = entriesSnap.data,
                    isFromCache = profileSnap.isFromCache || entriesSnap.isFromCache,
                    hasPendingWrites = profileSnap.hasPendingWrites || entriesSnap.hasPendingWrites
                ) as RemoteDataState
            }.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing MoneyFlow Firestore state", rootError)
                emit(
                    RemoteDataState.Failed(
                        extractFriendlyFirestoreError(
                            rootError,
                            "Cloud Firestore sync interrupted."
                        )
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RemoteDataState.Loading
        )

    val uiState: StateFlow<MoneyFlowUiState> = combine(remoteState, localState) { remote, local ->
        val today = todayProvider()
        val currentCycle = PayrollEngine.cycleForDate(today)

        val entriesList = when (remote) {
            is RemoteDataState.Loaded -> remote.entries
            else -> emptyList()
        }
        val overrides: Map<IndiaDate, DayOverride> = entriesList
            .mapNotNull { it.toDomainOverrideOrNull() }
            .associateBy { it.date }
        val existingKeys = entriesList.map { it.dateKey.ifBlank { it.date } }.toSet()

        val profileStart = (remote as? RemoteDataState.Loaded)
            ?.profile
            ?.trackingStartCycle
            ?.let { IndiaDate.parseOrNull(it) }
            ?.takeIf { it.day == 24 }
            ?: currentCycle.startDate

        val trackedCycles = PayrollEngine.trackedCycles(
            trackingStartCycleStart = profileStart,
            today = today,
            overrides = overrides
        )
        val summaries = trackedCycles.map { cycle ->
            PayrollEngine.calculateCycle(cycle = cycle, overrides = overrides, today = today)
        }

        val selectedSummary = summaries.firstOrNull { it.cycle.id == local.selectedCycleId }
            ?: summaries.lastOrNull()
            ?: PayrollEngine.calculateCycle(currentCycle, overrides, today)

        val defaultSelectedDate = when {
            selectedSummary.cycle.contains(today) -> today
            selectedSummary.peakElapsedDays.isNotEmpty() -> selectedSummary.peakElapsedDays.last().date
            else -> selectedSummary.cycle.endDate
        }
        val effectiveSelectedDate = local.selectedDate
            ?.takeIf { selectedSummary.cycle.contains(it) }
            ?: defaultSelectedDate

        val todayEntry = PayrollEngine.calculateDay(
            date = today,
            override = overrides[today],
            today = today
        )

        val syncStatus = when {
            remote is RemoteDataState.Failed -> SyncStatus.Error(remote.message)
            local.profileInitError != null -> SyncStatus.Error(local.profileInitError)
            local.isSaving -> SyncStatus.Syncing
            remote is RemoteDataState.Loading -> SyncStatus.Syncing
            remote is RemoteDataState.Loaded && remote.hasPendingWrites -> SyncStatus.Syncing
            remote is RemoteDataState.Loaded && remote.isFromCache -> SyncStatus.Offline
            else -> SyncStatus.Connected
        }

        val currentMonthSummary = PayrollEngine.calculateCurrentMonth(
            today = today,
            trackingStartCycleStart = profileStart,
            overrides = overrides
        )

        MoneyFlowUiState(
            today = today,
            currentCycle = currentCycle,
            trackingStartCycle = profileStart,
            cycleSummaries = summaries,
            selectedCycleSummary = selectedSummary,
            currentMonthSummary = currentMonthSummary,
            selectedDate = effectiveSelectedDate,
            todayEntry = todayEntry,
            overrides = overrides,
            existingEntryKeys = existingKeys,
            syncStatus = syncStatus,
            isSaving = local.isSaving,
            editingDate = local.editingDate,
            showAccountSheet = local.showAccountSheet,
            showDatePickerDialog = local.showDatePickerDialog,
            bannerMessage = local.bannerMessage,
            isBannerError = local.isBannerError,
            userEmail = userEmail,
            userDisplayName = userDisplayName,
            firebaseProjectId = firebaseProjectId,
            firestoreDatabaseId = firestoreDatabaseId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildInitialState()
    )

    init {
        initializeProfile()
    }

    private fun buildInitialState(): MoneyFlowUiState {
        val today = todayProvider()
        val currentCycle = PayrollEngine.cycleForDate(today)
        val summary = PayrollEngine.calculateCycle(currentCycle, emptyMap(), today)
        val monthSummary = PayrollEngine.calculateCurrentMonth(
            today = today,
            trackingStartCycleStart = currentCycle.startDate,
            overrides = emptyMap()
        )
        return MoneyFlowUiState(
            today = today,
            currentCycle = currentCycle,
            trackingStartCycle = currentCycle.startDate,
            cycleSummaries = listOf(summary),
            selectedCycleSummary = summary,
            currentMonthSummary = monthSummary,
            selectedDate = today,
            todayEntry = PayrollEngine.calculateDay(today, null, today),
            overrides = emptyMap(),
            existingEntryKeys = emptySet(),
            syncStatus = SyncStatus.Syncing,
            isSaving = false,
            editingDate = null,
            showAccountSheet = false,
            showDatePickerDialog = false,
            bannerMessage = null,
            isBannerError = false,
            userEmail = userEmail,
            userDisplayName = userDisplayName,
            firebaseProjectId = firebaseProjectId,
            firestoreDatabaseId = firestoreDatabaseId
        )
    }

    private fun initializeProfile() {
        val currentCycleStart = PayrollEngine.cycleForDate(todayProvider()).startDate.toIsoString()
        viewModelScope.launch {
            val result = repository.ensureProfileInitialized(
                userId = currentUserId,
                email = userEmail,
                displayName = userDisplayName,
                defaultTrackingStartCycle = currentCycleStart
            )
            result.onSuccess {
                localState.update { it.copy(profileInitError = null) }
            }.onFailure { err ->
                localState.update {
                    it.copy(
                        profileInitError = extractFriendlyFirestoreError(
                            err,
                            "Unable to initialize cloud profile."
                        )
                    )
                }
            }
        }
    }

    fun retrySync() {
        localState.update { it.copy(profileInitError = null, bannerMessage = null, isBannerError = false) }
        retryTrigger.update { it + 1 }
        initializeProfile()
    }

    fun selectCycle(cycleId: String) {
        localState.update { it.copy(selectedCycleId = cycleId, selectedDate = null) }
    }

    fun selectPreviousCycle() {
        val current = uiState.value
        val idx = current.cycleSummaries.indexOfFirst { it.cycle.id == current.selectedCycleSummary.cycle.id }
        if (idx > 0) {
            selectCycle(current.cycleSummaries[idx - 1].cycle.id)
        }
    }

    fun selectNextCycle() {
        val current = uiState.value
        val idx = current.cycleSummaries.indexOfFirst { it.cycle.id == current.selectedCycleSummary.cycle.id }
        if (idx >= 0 && idx < current.cycleSummaries.lastIndex) {
            selectCycle(current.cycleSummaries[idx + 1].cycle.id)
        }
    }

    fun selectDate(date: IndiaDate) {
        val cycle = PayrollEngine.cycleForDate(date)
        localState.update {
            it.copy(
                selectedCycleId = cycle.id,
                selectedDate = date
            )
        }
    }

    fun openTodayEditor() {
        openEditorForDate(todayProvider())
    }

    fun openEditorForDate(date: IndiaDate) {
        localState.update { it.copy(editingDate = date, showDatePickerDialog = false) }
    }

    fun closeEditor() {
        localState.update { it.copy(editingDate = null) }
    }

    fun setShowAccountSheet(show: Boolean) {
        localState.update { it.copy(showAccountSheet = show) }
    }

    fun setShowDatePickerDialog(show: Boolean) {
        localState.update { it.copy(showDatePickerDialog = show) }
    }

    fun dismissBanner() {
        localState.update { it.copy(bannerMessage = null, isBannerError = false) }
    }

    fun saveDayOverride(override: DayOverride) {
        val snapshot = uiState.value
        val docKey = override.date.toIsoString()
        val exists = docKey in snapshot.existingEntryKeys
        val entryCycleStart = PayrollEngine.cycleForDate(override.date).startDate
        val needsTrackingStartExpansion = entryCycleStart < snapshot.trackingStartCycle

        localState.update { it.copy(isSaving = true, bannerMessage = null, isBannerError = false) }
        viewModelScope.launch {
            val saveResult = repository.saveDayOverride(
                userId = currentUserId,
                override = override,
                documentExists = exists
            )
            saveResult.onSuccess {
                if (needsTrackingStartExpansion) {
                    repository.updateTrackingStartCycle(
                        userId = currentUserId,
                        trackingStartCycle = entryCycleStart.toIsoString()
                    )
                }
                localState.update {
                    it.copy(
                        isSaving = false,
                        editingDate = null,
                        selectedCycleId = entryCycleStart.toIsoString(),
                        selectedDate = override.date,
                        bannerMessage = "Saved ${override.date.shortDisplay()} to Cloud Firestore.",
                        isBannerError = false
                    )
                }
            }.onFailure { err ->
                localState.update {
                    it.copy(
                        isSaving = false,
                        bannerMessage = extractFriendlyFirestoreError(
                            err,
                            "Failed to save entry to Cloud Firestore."
                        ),
                        isBannerError = true
                    )
                }
            }
        }
    }

    fun resetDayToDefault(date: IndiaDate) {
        localState.update { it.copy(isSaving = true, bannerMessage = null, isBannerError = false) }
        viewModelScope.launch {
            val deleteResult = repository.deleteDayOverride(userId = currentUserId, date = date)
            deleteResult.onSuccess {
                localState.update {
                    it.copy(
                        isSaving = false,
                        editingDate = null,
                        selectedDate = date,
                        bannerMessage = "Reverted ${date.shortDisplay()} to automatic default pay.",
                        isBannerError = false
                    )
                }
            }.onFailure { err ->
                localState.update {
                    it.copy(
                        isSaving = false,
                        bannerMessage = extractFriendlyFirestoreError(
                            err,
                            "Failed to reset entry in Cloud Firestore."
                        ),
                        isBannerError = true
                    )
                }
            }
        }
    }

    fun updateTrackingStartCycle(newStartDate: IndiaDate) {
        val currentCycleStart = PayrollEngine.cycleForDate(todayProvider()).startDate
        val clampedStart = if (newStartDate > currentCycleStart) currentCycleStart else newStartDate
        localState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.updateTrackingStartCycle(
                userId = currentUserId,
                trackingStartCycle = clampedStart.toIsoString()
            )
            result.onSuccess {
                localState.update {
                    it.copy(
                        isSaving = false,
                        bannerMessage = "Tracking starts from ${PayrollEngine.cycleFromStart(clampedStart).rangeLabel}.",
                        isBannerError = false
                    )
                }
            }.onFailure { err ->
                localState.update {
                    it.copy(
                        isSaving = false,
                        bannerMessage = extractFriendlyFirestoreError(
                            err,
                            "Failed to update tracking start cycle."
                        ),
                        isBannerError = true
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "MoneyFlowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
