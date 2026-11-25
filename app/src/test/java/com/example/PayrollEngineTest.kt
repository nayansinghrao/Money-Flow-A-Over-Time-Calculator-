package com.example

import com.example.domain.DayOverride
import com.example.domain.DayType
import com.example.domain.ExitTime
import com.example.domain.IndiaDate
import com.example.domain.IndiaDayOfWeek
import com.example.domain.PayrollEngine
import com.example.domain.RationalRupees
import com.example.domain.WorkStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure Kotlin unit tests verifying MoneyFlow payroll calculations, exact rational OT arithmetic,
 * Sunday/holiday premiums, month/year rollovers, non-30-day provisional cycle handling,
 * reversible edits, and future-day exclusion from earned totals.
 */
class PayrollEngineTest {

    private val workDay = IndiaDate(2026, 11, 25) // Wednesday
    private val saturday = IndiaDate(2026, 11, 28) // Saturday
    private val sunday = IndiaDate(2026, 11, 29) // Sunday

    @Test
    fun officeExitAt1830_noOt_exact500() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(18, 30),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(0, result.overtimeBlocks)
        assertEquals(RationalRupees.RUPEES_500, result.regularPay)
        assertEquals(RationalRupees.ZERO, result.holidayPremium)
        assertEquals(RationalRupees.ZERO, result.overtimePay)
        assertEquals(500_00L, result.totalPay.toPaise())
        assertEquals("₹500", result.totalPay.formatInr())
    }

    @Test
    fun officeExitAt1842WithOt_exact527_78() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(18, 42),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(1, result.overtimeBlocks)
        assertEquals("18:42", result.exitTime?.toWireString())
        assertEquals(RationalRupees.of(500, 18), result.overtimePay)
        assertEquals(527_78L, result.totalPay.toPaise())
        assertEquals("₹527.78", result.totalPay.formatInr())
    }

    @Test
    fun officeExitAt1900WithOt_boundaryStaysAt1Block_exact527_78() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(19, 0),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(1, result.overtimeBlocks)
        assertEquals(527_78L, result.totalPay.toPaise())
        assertEquals("₹527.78", result.totalPay.formatInr())
    }

    @Test
    fun officeExitAt1901WithOt_startsSecondBlock_exact555_56() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(19, 1),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(2, result.overtimeBlocks)
        assertEquals("19:01", result.exitTime?.toWireString())
        assertEquals(RationalRupees.of(1000, 18), result.overtimePay)
        assertEquals(555_56L, result.totalPay.toPaise())
        assertEquals("₹555.56", result.totalPay.formatInr())
    }

    @Test
    fun officeExitAt2000WithOt_3Blocks_exact583_33() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(20, 0),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(3, result.overtimeBlocks)
        assertEquals(RationalRupees.of(1500, 18), result.overtimePay)
        assertEquals(583_33L, result.totalPay.toPaise())
        assertEquals("₹583.33", result.totalPay.formatInr())
    }

    @Test
    fun officeExitAfter1830_withOtSwitchOff_paysZeroOt() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(20, 0),
                overtimeEnabled = false
            ),
            today = workDay
        )
        assertEquals(0, result.overtimeBlocks)
        assertEquals(RationalRupees.ZERO, result.overtimePay)
        assertEquals(500_00L, result.totalPay.toPaise())
    }

    @Test
    fun paidSundayHalfDayWorked_exact750() {
        assertEquals(IndiaDayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(DayType.SUNDAY, PayrollEngine.defaultDayType(sunday))

        val result = PayrollEngine.calculateDay(
            date = sunday,
            override = DayOverride(
                date = sunday,
                dayType = DayType.SUNDAY,
                workStatus = WorkStatus.HALF,
                exitTime = null,
                overtimeEnabled = false
            ),
            today = sunday
        )
        assertEquals(RationalRupees.RUPEES_500, result.regularPay)
        assertEquals(RationalRupees.RUPEES_250, result.holidayPremium)
        assertEquals(RationalRupees.ZERO, result.overtimePay)
        assertEquals(750_00L, result.totalPay.toPaise())
        assertEquals("₹750", result.totalPay.formatInr())
    }

    @Test
    fun paidSundayFullDayWorkedWithOtAt1900_exact1027_78() {
        val result = PayrollEngine.calculateDay(
            date = sunday,
            override = DayOverride(
                date = sunday,
                dayType = DayType.SUNDAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(19, 0),
                overtimeEnabled = true
            ),
            today = sunday
        )
        assertEquals(RationalRupees.RUPEES_500, result.regularPay)
        assertEquals(RationalRupees.RUPEES_500, result.holidayPremium)
        assertEquals(1, result.overtimeBlocks)
        assertEquals(RationalRupees.of(500, 18), result.overtimePay)
        assertEquals(1027_78L, result.totalPay.toPaise())
        assertEquals("₹1,027.78", result.totalPay.formatInr())
    }

    @Test
    fun officeAbsenceLeave_exactZero_noOtEvenIfFlagSet() {
        val result = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.LEAVE,
                exitTime = ExitTime(20, 0),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(RationalRupees.ZERO, result.regularPay)
        assertEquals(RationalRupees.ZERO, result.holidayPremium)
        assertEquals(RationalRupees.ZERO, result.overtimePay)
        assertEquals(0, result.overtimeBlocks)
        assertEquals(0L, result.totalPay.toPaise())
        assertEquals("₹0", result.totalPay.formatInr())
    }

    @Test
    fun unworkedHolidayAndSaturdayDefaults() {
        // Saturday defaults to Office Day (₹500)
        assertEquals(IndiaDayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(DayType.OFFICE_DAY, PayrollEngine.defaultDayType(saturday))
        val satDefault = PayrollEngine.calculateDay(saturday, null, saturday)
        assertEquals(DayType.OFFICE_DAY, satDefault.dayType)
        assertEquals(WorkStatus.FULL, satDefault.workStatus)
        assertEquals(500_00L, satDefault.totalPay.toPaise())

        // Unworked company holiday pays ₹500 and NEVER counts OT
        val unworkedHoliday = PayrollEngine.calculateDay(
            date = workDay,
            override = DayOverride(
                date = workDay,
                dayType = DayType.HOLIDAY,
                workStatus = WorkStatus.PAID_OFF,
                exitTime = ExitTime(20, 0),
                overtimeEnabled = true
            ),
            today = workDay
        )
        assertEquals(RationalRupees.RUPEES_500, unworkedHoliday.regularPay)
        assertEquals(RationalRupees.ZERO, unworkedHoliday.holidayPremium)
        assertEquals(RationalRupees.ZERO, unworkedHoliday.overtimePay)
        assertEquals(500_00L, unworkedHoliday.totalPay.toPaise())
    }

    @Test
    fun monthAndYearRollover_cycleBoundariesPaydayAndProvisionalFlag() {
        // Nov 24, 2026 – Dec 23, 2026 (30 days: Nov has 30 days -> 7 days Nov + 23 days Dec = 30)
        // Payday rolls over into Jan 1, 2027!
        val novDecCycle = PayrollEngine.cycleForDate(IndiaDate(2026, 11, 25))
        assertEquals(IndiaDate(2026, 11, 24), novDecCycle.startDate)
        assertEquals(IndiaDate(2026, 12, 23), novDecCycle.endDate)
        assertEquals(IndiaDate(2027, 1, 1), novDecCycle.paydayDate)
        assertEquals(30, novDecCycle.totalDays)
        assertFalse(novDecCycle.isNonStandardLength)

        // Dec 24, 2026 – Jan 23, 2027 (year rollover inside cycle, 31 days: Dec has 31 -> 8 + 23 = 31)
        // Payday = Feb 1, 2027
        val decJanFromDec = PayrollEngine.cycleForDate(IndiaDate(2026, 12, 24))
        val decJanFromJan = PayrollEngine.cycleForDate(IndiaDate(2027, 1, 23))
        assertEquals(decJanFromDec, decJanFromJan)
        assertEquals(IndiaDate(2026, 12, 24), decJanFromDec.startDate)
        assertEquals(IndiaDate(2027, 1, 23), decJanFromDec.endDate)
        assertEquals(IndiaDate(2027, 2, 1), decJanFromDec.paydayDate)
        assertEquals(31, decJanFromDec.totalDays)
        assertTrue(decJanFromDec.isNonStandardLength)

        // Feb non-leap cycle: 24 Feb 2026 – 23 Mar 2026 (5 + 23 = 28 days -> provisional)
        val febNonLeap = PayrollEngine.cycleForDate(IndiaDate(2026, 3, 1))
        assertEquals(IndiaDate(2026, 2, 24), febNonLeap.startDate)
        assertEquals(IndiaDate(2026, 3, 23), febNonLeap.endDate)
        assertEquals(IndiaDate(2026, 4, 1), febNonLeap.paydayDate)
        assertEquals(28, febNonLeap.totalDays)
        assertTrue(febNonLeap.isNonStandardLength)

        // Feb leap cycle: 24 Feb 2024 – 23 Mar 2024 (6 + 23 = 29 days -> provisional)
        val febLeap = PayrollEngine.cycleForDate(IndiaDate(2024, 2, 29))
        assertEquals(29, febLeap.totalDays)
        assertTrue(febLeap.isNonStandardLength)
    }

    @Test
    fun futureDaysExcludedFromEarned_includedInForecastWithoutFutureOtOrHolidayWork() {
        val today = IndiaDate(2026, 11, 25) // Day 2 of Nov 24 – Dec 23 cycle (30 days total)
        val cycle = PayrollEngine.cycleForDate(today)

        // Add OT on today (elapsed) AND a speculative future OT entry on Nov 26 (future)
        val overrides = mapOf(
            IndiaDate(2026, 11, 25) to DayOverride(
                date = IndiaDate(2026, 11, 25),
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(19, 0),
                overtimeEnabled = true
            ),
            IndiaDate(2026, 11, 26) to DayOverride(
                date = IndiaDate(2026, 11, 26),
                dayType = DayType.HOLIDAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(20, 0),
                overtimeEnabled = true
            )
        )

        val summary = PayrollEngine.calculateCycle(cycle = cycle, overrides = overrides, today = today)

        // Elapsed: Nov 24 (auto default ₹500) + Nov 25 (₹500 + ₹27.78 OT) = ₹1,027.78
        assertEquals(2, summary.elapsedDaysCount)
        assertEquals(28, summary.remainingDaysCount)
        assertEquals(1027_78L, summary.earnedTotal.toPaise())
        assertEquals("₹1,027.78", summary.earnedTotal.formatInr())

        // Forecast: earned (₹1,027.78) + 28 remaining days * ₹500 (₹14,000) = ₹15,027.78
        // Future Nov 26 override MUST NOT leak into earned or speculative forecast!
        assertEquals(14000_00L, summary.remainingNormalPay.toPaise())
        assertEquals(15027_78L, summary.forecastTotal.toPaise())
        assertEquals("₹15,027.78", summary.forecastTotal.formatInr())

        // Peak elapsed day must be Nov 25 (₹527.78), not Nov 26 (future)
        assertEquals(listOf(IndiaDate(2026, 11, 25)), summary.peakElapsedDays.map { it.date })
    }

    @Test
    fun exactRationalAggregationAcrossMultipleOtDays_avoidsRoundingDrift() {
        val today = IndiaDate(2026, 11, 26)
        val cycle = PayrollEngine.cycleForDate(today)
        // Three elapsed days (Nov 24, 25, 26), each with 1 OT block (500/18 = 27.7777...)
        // Exact sum of OT = 3 * 500/18 = 1500/18 = 83.3333... -> ₹83.33 (NOT 3 * 27.78 = ₹83.34!)
        val overrides = listOf(
            IndiaDate(2026, 11, 24),
            IndiaDate(2026, 11, 25),
            IndiaDate(2026, 11, 26)
        ).associateWith { d ->
            DayOverride(
                date = d,
                dayType = DayType.OFFICE_DAY,
                workStatus = WorkStatus.FULL,
                exitTime = ExitTime(19, 0),
                overtimeEnabled = true
            )
        }

        val summary = PayrollEngine.calculateCycle(cycle, overrides, today)
        assertEquals(3, summary.earnedOvertimeBlocks)
        assertEquals(83_33L, summary.earnedOvertimePay.toPaise())
        assertEquals(1583_33L, summary.earnedTotal.toPaise())
        assertEquals("₹1,583.33", summary.earnedTotal.formatInr())
    }

    @Test
    fun reversibleEditsRecalculateWithoutDoubleCounting() {
        val today = IndiaDate(2026, 11, 25)
        val cycle = PayrollEngine.cycleForDate(today)

        val edited = PayrollEngine.calculateCycle(
            cycle = cycle,
            overrides = mapOf(
                today to DayOverride(
                    date = today,
                    dayType = DayType.HOLIDAY,
                    workStatus = WorkStatus.FULL,
                    exitTime = ExitTime(19, 0),
                    overtimeEnabled = true
                )
            ),
            today = today
        )
        assertEquals(1527_78L, edited.earnedTotal.toPaise()) // Nov 24 (500) + Nov 25 (1027.78)

        // Revert by removing override -> cleanly recalculates back to ₹1,000
        val reverted = PayrollEngine.calculateCycle(
            cycle = cycle,
            overrides = emptyMap(),
            today = today
        )
        assertEquals(1000_00L, reverted.earnedTotal.toPaise())
    }

    @Test
    fun indianRupeeFormatting_lakhsAndThousandsGrouping() {
        assertEquals("₹1,25,450", RationalRupees.of(125450).formatInr())
        assertEquals("₹1,25,450.00", RationalRupees.of(125450).formatInr(alwaysShowPaise = true))
        assertEquals("₹15,527.78", (RationalRupees.of(15500) + RationalRupees.OT_BLOCK_RATE).formatInr())
    }
}
