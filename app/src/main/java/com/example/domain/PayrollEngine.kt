package com.example.domain

/**
 * Pure deterministic payroll calculation engine for MoneyFlow.
 *
 * Rules:
 * - India Standard Time local dates (YYYY-MM-DD).
 * - Salary cycle: 24th of month M through 23rd of month M+1 inclusive.
 * - Payday: 1st of the month following the cycle end (both salary and OT).
 * - Default day type: Sundays are SUNDAY; Mon-Sat (including Saturday) default to OFFICE_DAY.
 * - Office Day: Full = ₹500, Half = ₹250, Leave = ₹0.
 * - Sunday / Company Holiday: Paid Off = ₹500, Half Worked = ₹500 + ₹250 (= ₹750),
 *   Full Worked = ₹500 + ₹500 (= ₹1,000). Holiday/Sunday extras are separate from OT.
 * - OT: explicit toggle, worked dates only (no OT for leave or unworked holidays/Sundays).
 *   After 18:30, each started 30-min block counts; exact boundary stays at that count.
 *   OT = ceil(max(0, minutesAfter1830) / 30) * (500 / 18) rupees in exact rational arithmetic.
 * - Earned so far: elapsed dates (date <= today) within the cycle; every elapsed date
 *   automatically contributes its default pay even without opening an override.
 * - Forecast: earnedSoFar + (remainingDates * ₹500), zero future OT or speculative holiday work.
 * - Non-30-day cycles: daily rate totals shown with provisional projection flag.
 */
object PayrollEngine {

    fun defaultDayType(date: IndiaDate): DayType =
        if (date.isSunday()) DayType.SUNDAY else DayType.OFFICE_DAY

    fun cycleForDate(date: IndiaDate): SalaryCycle {
        val startDate = if (date.day >= 24) {
            IndiaDate(date.year, date.month, 24)
        } else {
            val prevYear = if (date.month == 1) date.year - 1 else date.year
            val prevMonth = if (date.month == 1) 12 else date.month - 1
            IndiaDate(prevYear, prevMonth, 24)
        }
        return cycleFromStart(startDate)
    }

    fun cycleFromStart(startDate: IndiaDate): SalaryCycle {
        require(startDate.day == 24) { "Salary cycle must start on the 24th, got $startDate" }
        val endYear = if (startDate.month == 12) startDate.year + 1 else startDate.year
        val endMonth = if (startDate.month == 12) 1 else startDate.month + 1
        val endDate = IndiaDate(endYear, endMonth, 23)

        val payYear = if (endMonth == 12) endYear + 1 else endYear
        val payMonth = if (endMonth == 12) 1 else endMonth + 1
        val paydayDate = IndiaDate(payYear, payMonth, 1)

        return SalaryCycle(
            startDate = startDate,
            endDate = endDate,
            paydayDate = paydayDate
        )
    }

    fun previousCycle(cycle: SalaryCycle): SalaryCycle {
        val prevYear = if (cycle.startDate.month == 1) cycle.startDate.year - 1 else cycle.startDate.year
        val prevMonth = if (cycle.startDate.month == 1) 12 else cycle.startDate.month - 1
        return cycleFromStart(IndiaDate(prevYear, prevMonth, 24))
    }

    fun nextCycle(cycle: SalaryCycle): SalaryCycle {
        val nextYear = if (cycle.startDate.month == 12) cycle.startDate.year + 1 else cycle.startDate.year
        val nextMonth = if (cycle.startDate.month == 12) 1 else cycle.startDate.month + 1
        return cycleFromStart(IndiaDate(nextYear, nextMonth, 24))
    }

    /**
     * Returns all tracked cycles in chronological order from the effective start cycle
     * (the earlier of [trackingStartCycleStart] and any real persisted override's cycle)
     * through the current cycle containing [today].
     */
    fun trackedCycles(
        trackingStartCycleStart: IndiaDate,
        today: IndiaDate,
        overrides: Map<IndiaDate, DayOverride> = emptyMap()
    ): List<SalaryCycle> {
        val currentCycle = cycleForDate(today)
        val earliestOverrideCycleStart = overrides.keys.minOrNull()?.let { cycleForDate(it).startDate }
        val normalizedTrackingStart = cycleForDate(trackingStartCycleStart).startDate
        var cursorStart = listOfNotNull(
            normalizedTrackingStart,
            earliestOverrideCycleStart
        ).minOrNull() ?: currentCycle.startDate

        if (cursorStart > currentCycle.startDate) {
            cursorStart = currentCycle.startDate
        }

        val result = mutableListOf<SalaryCycle>()
        var cursor = cycleFromStart(cursorStart)
        var safety = 0
        while (cursor.startDate <= currentCycle.startDate && safety < 120) {
            result.add(cursor)
            cursor = nextCycle(cursor)
            safety++
        }
        return result
    }

    fun calculateDay(
        date: IndiaDate,
        override: DayOverride?,
        today: IndiaDate
    ): DayEarnings {
        val isElapsed = date <= today
        val isToday = date == today
        val dayType = override?.dayType ?: defaultDayType(date)

        // Sanitize workStatus against dayType in case of legacy or drifted data
        val validStatuses = WorkStatus.validStatusesFor(dayType)
        val workStatus = override?.workStatus?.takeIf { it in validStatuses }
            ?: WorkStatus.defaultStatusFor(dayType)

        val exitTime = override?.exitTime
        val overtimeEnabled = override?.overtimeEnabled ?: false

        // Base regular pay and holiday/Sunday work premium
        val regularPay: RationalRupees
        val holidayPremium: RationalRupees
        when (dayType) {
            DayType.OFFICE_DAY -> {
                regularPay = when (workStatus) {
                    WorkStatus.FULL -> RationalRupees.RUPEES_500
                    WorkStatus.HALF -> RationalRupees.RUPEES_250
                    WorkStatus.LEAVE -> RationalRupees.ZERO
                    WorkStatus.PAID_OFF -> RationalRupees.RUPEES_500
                }
                holidayPremium = RationalRupees.ZERO
            }
            DayType.SUNDAY, DayType.HOLIDAY -> {
                regularPay = RationalRupees.RUPEES_500
                holidayPremium = when (workStatus) {
                    WorkStatus.PAID_OFF -> RationalRupees.ZERO
                    WorkStatus.HALF -> RationalRupees.RUPEES_250
                    WorkStatus.FULL -> RationalRupees.RUPEES_500
                    WorkStatus.LEAVE -> RationalRupees.ZERO
                }
            }
        }

        // OT applies ONLY when explicitly enabled AND on a worked date (not leave / unworked holiday)
        val otEligible = overtimeEnabled && workStatus.isWorked() && exitTime != null
        val otBlocks = if (otEligible) exitTime!!.overtimeHalfHourBlocks else 0
        val overtimePay = if (otBlocks > 0) {
            RationalRupees.OT_BLOCK_RATE * otBlocks
        } else {
            RationalRupees.ZERO
        }

        val totalPay = regularPay + holidayPremium + overtimePay

        return DayEarnings(
            date = date,
            dayType = dayType,
            workStatus = workStatus,
            exitTime = exitTime,
            overtimeEnabled = overtimeEnabled,
            overtimeBlocks = otBlocks,
            regularPay = regularPay,
            holidayPremium = holidayPremium,
            overtimePay = overtimePay,
            totalPay = totalPay,
            isElapsed = isElapsed,
            isToday = isToday,
            hasOverride = override != null,
            note = override?.note?.takeIf { it.isNotBlank() }
        )
    }

    fun calculateCycle(
        cycle: SalaryCycle,
        overrides: Map<IndiaDate, DayOverride>,
        today: IndiaDate
    ): CycleSummary {
        val allDates = cycle.allDates()
        val dayBreakdowns = allDates.map { date ->
            calculateDay(date = date, override = overrides[date], today = today)
        }

        val elapsedDays = dayBreakdowns.filter { it.isElapsed }
        val remainingDaysCount = dayBreakdowns.size - elapsedDays.size

        val earnedRegular = elapsedDays.map { it.regularPay }.sumRupees()
        val earnedHoliday = elapsedDays.map { it.holidayPremium }.sumRupees()
        val earnedOvertime = elapsedDays.map { it.overtimePay }.sumRupees()
        val earnedTotal = earnedRegular + earnedHoliday + earnedOvertime
        val earnedOtBlocks = elapsedDays.sumOf { it.overtimeBlocks }

        // Forecast = earned so far + normal ₹500/day for remaining future dates (zero future OT/holiday work)
        val remainingNormalPay = RationalRupees.RUPEES_500 * remainingDaysCount
        val forecastTotal = earnedTotal + remainingNormalPay

        val maxElapsedPay = elapsedDays.maxOfOrNull { it.totalPay }
        val peakElapsedDays = if (maxElapsedPay != null && maxElapsedPay.isPositive) {
            elapsedDays.filter { it.totalPay == maxElapsedPay }
        } else {
            emptyList()
        }

        val isOngoing = today >= cycle.startDate && today <= cycle.endDate
        val isCompleted = today > cycle.endDate

        return CycleSummary(
            cycle = cycle,
            days = dayBreakdowns,
            elapsedDaysCount = elapsedDays.size,
            remainingDaysCount = remainingDaysCount,
            isOngoing = isOngoing,
            isCompleted = isCompleted,
            earnedRegularPay = earnedRegular,
            earnedHolidayPremium = earnedHoliday,
            earnedOvertimePay = earnedOvertime,
            earnedTotal = earnedTotal,
            earnedOvertimeBlocks = earnedOtBlocks,
            remainingNormalPay = remainingNormalPay,
            forecastTotal = forecastTotal,
            isForecastProvisional = cycle.isNonStandardLength,
            peakElapsedDays = peakElapsedDays
        )
    }
}
