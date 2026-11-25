package com.example.domain

enum class DayType(val displayLabel: String) {
    OFFICE_DAY("Office Day"),
    SUNDAY("Sunday"),
    HOLIDAY("Holiday");

    companion object {
        fun fromWire(raw: String?, fallback: DayType): DayType =
            entries.firstOrNull { it.name == raw } ?: fallback
    }
}

enum class WorkStatus(val displayLabel: String) {
    FULL("Full Day"),
    HALF("Half Day"),
    LEAVE("Leave"),
    PAID_OFF("Paid Off");

    fun isWorked(): Boolean = this == FULL || this == HALF

    companion object {
        fun fromWire(raw: String?, fallback: WorkStatus): WorkStatus =
            entries.firstOrNull { it.name == raw } ?: fallback

        fun validStatusesFor(dayType: DayType): List<WorkStatus> = when (dayType) {
            DayType.OFFICE_DAY -> listOf(FULL, HALF, LEAVE)
            DayType.SUNDAY, DayType.HOLIDAY -> listOf(PAID_OFF, HALF, FULL)
        }

        fun defaultStatusFor(dayType: DayType): WorkStatus = when (dayType) {
            DayType.OFFICE_DAY -> FULL
            DayType.SUNDAY, DayType.HOLIDAY -> PAID_OFF
        }
    }
}

/**
 * Exact 24-hour local exit time stored verbatim to the minute ("HH:mm").
 * Threshold for OT is 18:30 (1110 minutes from midnight).
 */
data class ExitTime(
    val hour: Int,
    val minute: Int
) : Comparable<ExitTime> {
    init {
        require(hour in 0..23) { "Hour must be 0..23, got $hour" }
        require(minute in 0..59) { "Minute must be 0..59, got $minute" }
    }

    val totalMinutes: Int
        get() = hour * 60 + minute

    val minutesAfter1830: Int
        get() = (totalMinutes - OT_THRESHOLD_MINUTES).coerceAtLeast(0)

    /**
     * After 18:30, each started 30-minute block counts; exact boundary stays at that block count.
     * E.g. 18:30 -> 0, 18:42 -> 1, 19:00 -> 1, 19:01 -> 2, 20:00 -> 3.
     */
    val overtimeHalfHourBlocks: Int
        get() {
            val mins = minutesAfter1830
            return if (mins <= 0) 0 else (mins + 29) / 30
        }

    fun toWireString(): String = "%02d:%02d".format(hour, minute)

    fun display24h(): String = "%02d:%02d".format(hour, minute)

    fun display12h(): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(h12, minute, period)
    }

    fun plusMinutes(delta: Int): ExitTime {
        val clamped = (totalMinutes + delta).coerceIn(0, 23 * 60 + 59)
        return ExitTime(clamped / 60, clamped % 60)
    }

    override fun compareTo(other: ExitTime): Int = totalMinutes.compareTo(other.totalMinutes)

    companion object {
        const val OT_THRESHOLD_MINUTES = 18 * 60 + 30
        private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

        fun parseOrNull(raw: String?): ExitTime? {
            if (raw.isNullOrBlank()) return null
            val m = TIME_REGEX.matchEntire(raw.trim()) ?: return null
            return ExitTime(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
    }
}

data class DayOverride(
    val date: IndiaDate,
    val dayType: DayType,
    val workStatus: WorkStatus,
    val exitTime: ExitTime? = null,
    val overtimeEnabled: Boolean = false,
    val note: String? = null
)

data class SalaryCycle(
    val startDate: IndiaDate,
    val endDate: IndiaDate,
    val paydayDate: IndiaDate
) {
    val id: String = startDate.toIsoString()

    val totalDays: Int = (startDate.daysUntil(endDate) + 1L).toInt()

    val isNonStandardLength: Boolean = totalDays != 30

    val rangeLabel: String
        get() = if (startDate.year == endDate.year) {
            "${startDate.shortDisplay()} – ${endDate.shortDisplayWithYear()}"
        } else {
            "${startDate.shortDisplayWithYear()} – ${endDate.shortDisplayWithYear()}"
        }

    val shortLabel: String
        get() = if (startDate.year == endDate.year) {
            "${startDate.monthShort()}–${endDate.monthShort()}"
        } else {
            "${startDate.monthShort()}–${endDate.monthShort()} '${(endDate.year % 100).toString().padStart(2, '0')}"
        }

    val paydayLabel: String = paydayDate.shortDisplayWithYear()

    fun contains(date: IndiaDate): Boolean = date >= startDate && date <= endDate

    fun allDates(): List<IndiaDate> =
        List(totalDays) { offset -> startDate.plusDays(offset.toLong()) }
}

data class DayEarnings(
    val date: IndiaDate,
    val dayType: DayType,
    val workStatus: WorkStatus,
    val exitTime: ExitTime?,
    val overtimeEnabled: Boolean,
    val overtimeBlocks: Int,
    val regularPay: RationalRupees,
    val holidayPremium: RationalRupees,
    val overtimePay: RationalRupees,
    val totalPay: RationalRupees,
    val isElapsed: Boolean,
    val isToday: Boolean,
    val hasOverride: Boolean,
    val note: String?
)

data class CycleSummary(
    val cycle: SalaryCycle,
    val days: List<DayEarnings>,
    val elapsedDaysCount: Int,
    val remainingDaysCount: Int,
    val isOngoing: Boolean,
    val isCompleted: Boolean,
    val earnedRegularPay: RationalRupees,
    val earnedHolidayPremium: RationalRupees,
    val earnedOvertimePay: RationalRupees,
    val earnedTotal: RationalRupees,
    val earnedOvertimeBlocks: Int,
    val remainingNormalPay: RationalRupees,
    val forecastTotal: RationalRupees,
    val isForecastProvisional: Boolean,
    val peakElapsedDays: List<DayEarnings>
)
