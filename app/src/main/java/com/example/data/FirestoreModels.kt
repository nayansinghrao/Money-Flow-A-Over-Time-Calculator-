package com.example.data

import androidx.annotation.Keep
import com.example.domain.DayOverride
import com.example.domain.DayType
import com.example.domain.ExitTime
import com.example.domain.IndiaDate
import com.example.domain.PayrollEngine
import com.example.domain.WorkStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val trackingStartCycle: String = "",
    val timezone: String = "Asia/Kolkata",
    val schemaVersion: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DayEntryDoc(
    @DocumentId val dateKey: String = "",
    val userId: String = "",
    val date: String = "",
    val dayType: String = DayType.OFFICE_DAY.name,
    val workStatus: String = WorkStatus.FULL.name,
    val exitTime: String? = null,
    val overtimeEnabled: Boolean = false,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainOverrideOrNull(): DayOverride? {
        val rawDate = date.ifBlank { dateKey }
        val parsedDate = IndiaDate.parseOrNull(rawDate) ?: return null
        val fallbackDayType = PayrollEngine.defaultDayType(parsedDate)
        val parsedDayType = DayType.fromWire(dayType, fallbackDayType)
        val fallbackStatus = WorkStatus.defaultStatusFor(parsedDayType)
        val parsedStatus = WorkStatus.fromWire(workStatus, fallbackStatus)
            .takeIf { it in WorkStatus.validStatusesFor(parsedDayType) }
            ?: fallbackStatus
        return DayOverride(
            date = parsedDate,
            dayType = parsedDayType,
            workStatus = parsedStatus,
            exitTime = ExitTime.parseOrNull(exitTime),
            overtimeEnabled = overtimeEnabled,
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}
