package com.example.domain

import java.util.Calendar
import java.util.TimeZone

enum class IndiaDayOfWeek(val shortLabel: String) {
    MONDAY("Mon"),
    TUESDAY("Tue"),
    WEDNESDAY("Wed"),
    THURSDAY("Thu"),
    FRIDAY("Fri"),
    SATURDAY("Sat"),
    SUNDAY("Sun")
}

/**
 * Pure Kotlin proleptic Gregorian local date representing an India Standard Time (Asia/Kolkata)
 * calendar date stored as canonical YYYY-MM-DD. Works identically on Android API 24+ and JVM tests
 * without requiring core library desugaring.
 */
data class IndiaDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<IndiaDate> {

    init {
        require(year in 1970..2199) { "Year out of supported range: $year" }
        require(month in 1..12) { "Month out of range: $month" }
        require(day in 1..lengthOfMonth(year, month)) {
            "Day $day out of range for $year-$month"
        }
    }

    val epochDay: Long
        get() = daysFromCivil(year, month, day)

    val dayOfWeek: IndiaDayOfWeek
        get() {
            // 1970-01-01 (epochDay 0) was Thursday (index 3 in Mon=0..Sun=6)
            val idx = Math.floorMod(epochDay + 3L, 7L).toInt()
            return IndiaDayOfWeek.entries[idx]
        }

    fun isSunday(): Boolean = dayOfWeek == IndiaDayOfWeek.SUNDAY

    fun plusDays(daysToAdd: Long): IndiaDate = fromEpochDay(epochDay + daysToAdd)

    fun minusDays(daysToSubtract: Long): IndiaDate = fromEpochDay(epochDay - daysToSubtract)

    fun daysUntil(other: IndiaDate): Long = other.epochDay - epochDay

    fun toIsoString(): String =
        "%04d-%02d-%02d".format(year, month, day)

    fun shortDisplay(): String =
        "$day ${MONTH_SHORT[month - 1]}"

    fun shortDisplayWithYear(): String =
        "$day ${MONTH_SHORT[month - 1]} $year"

    fun fullDisplay(): String =
        "${dayOfWeek.shortLabel}, $day ${MONTH_SHORT[month - 1]} $year"

    fun monthShort(): String = MONTH_SHORT[month - 1]

    fun monthYearShort(): String = "${MONTH_SHORT[month - 1]} $year"

    override fun compareTo(other: IndiaDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        private val INDIA_TZ: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        private val ISO_REGEX = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
        private val MONTH_SHORT = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        fun isLeapYear(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> throw IllegalArgumentException("Invalid month: $month")
        }

        fun parse(iso: String): IndiaDate {
            val match = ISO_REGEX.matchEntire(iso.trim())
                ?: throw IllegalArgumentException("Invalid YYYY-MM-DD date: '$iso'")
            val y = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            val d = match.groupValues[3].toInt()
            return IndiaDate(y, m, d)
        }

        fun parseOrNull(iso: String?): IndiaDate? =
            if (iso.isNullOrBlank()) null else runCatching { parse(iso) }.getOrNull()

        fun todayInIndia(nowMillis: Long = System.currentTimeMillis()): IndiaDate {
            val cal = Calendar.getInstance(INDIA_TZ)
            cal.timeInMillis = nowMillis
            return IndiaDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun currentTimeInIndia(nowMillis: Long = System.currentTimeMillis()): ExitTime {
            val cal = Calendar.getInstance(INDIA_TZ)
            cal.timeInMillis = nowMillis
            return ExitTime(
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE)
            )
        }

        // Howard Hinnant's exact civil-from-days / days-from-civil algorithms
        fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = (year - if (month <= 2) 1 else 0).toLong()
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (month + if (month > 2) -3 else 9) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097 + doe - 719468
        }

        fun fromEpochDay(epochDay: Long): IndiaDate {
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val m = mp + if (mp < 10) 3 else -9
            val year = (y + if (m <= 2) 1 else 0).toInt()
            return IndiaDate(year, m.toInt(), d.toInt())
        }
    }
}
