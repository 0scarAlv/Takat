package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseFrequency
import java.time.LocalDate
import java.time.YearMonth

enum class ReminderStage { PRE_DUE, DUE, FOLLOW_UP }

/** Pure date math for fixed-expense periods: no Android/Room dependency, easy to reason about in isolation. */
object FixedExpensePeriod {

    private fun quincenaKey(date: LocalDate): String {
        val yearMonth = "%04d-%02d".format(date.year, date.monthValue)
        return if (date.dayOfMonth <= 15) "$yearMonth-Q1" else "$yearMonth-Q2"
    }

    /**
     * The last day of the quincena a salary logged on [salaryDate] pays for — i.e. when the *next*
     * salary is expected. Used by [com.takat.finanzas.util.nextPaymentDate].
     */
    fun salaryAnchorBoundary(salaryDate: LocalDate): LocalDate =
        if (salaryDate.dayOfMonth <= 15) {
            YearMonth.from(salaryDate).atEndOfMonth()
        } else {
            YearMonth.from(salaryDate).plusMonths(1).atDay(15)
        }

    /**
     * The date fixed-expense periods are resolved against: [today], or the first day of the quincena
     * [lastSalaryDate] pays for when that quincena hasn't started on the calendar yet (a salary was logged
     * before the calendar caught up to day 16/1). Salary logged on Sep 30 funds Oct-Q1, so periods resolve
     * as if it were already Oct 1 — monthly bills roll over to October right away, same as quincenal ones.
     * A stale anchor (its quincena already started) never moves the date backwards.
     */
    fun effectiveDate(today: LocalDate = LocalDate.now(), lastSalaryDate: LocalDate? = null): LocalDate {
        val salaryHalfStart = lastSalaryDate?.let {
            if (it.dayOfMonth <= 15) it.withDayOfMonth(16) else YearMonth.from(it).plusMonths(1).atDay(1)
        }
        return if (salaryHalfStart != null && salaryHalfStart > today) salaryHalfStart else today
    }

    /**
     * MENSUAL -> "yyyy-MM". QUINCENAL -> "yyyy-MM-Q1" (days 1-15) or "yyyy-MM-Q2" (16-end of month),
     * both resolved against [effectiveDate], so a salary logged ahead of the calendar moves either one forward.
     */
    fun currentPeriodKey(
        frequency: FixedExpenseFrequency,
        today: LocalDate = LocalDate.now(),
        lastSalaryDate: LocalDate? = null
    ): String {
        val date = effectiveDate(today, lastSalaryDate)
        return when (frequency) {
            FixedExpenseFrequency.MENSUAL -> "%04d-%02d".format(date.year, date.monthValue)
            FixedExpenseFrequency.QUINCENAL -> quincenaKey(date)
        }
    }

    /** QUINCENAL notifications fire on day 1 (first quincena) and day 16 (second quincena) of every month. */
    fun isQuincenalNotifyDay(today: LocalDate = LocalDate.now()): Boolean =
        today.dayOfMonth == 1 || today.dayOfMonth == 16

    /**
     * MENSUAL only: scans the previous, current, and next month's due date (each independently
     * clamped to that month's own length) and returns the reminder stage that matches [today], with
     * the "yyyy-MM" period key of whichever month it belongs to. Scanning three months instead of
     * just "today's" month is what resolves month-boundary edge cases correctly: a PRE_DUE check for
     * a `dayOfMonth=1` rule lands on the last day of the *previous* month, and a FOLLOW_UP check for
     * a rule near month-end can land in the *next* month — in both cases the right period is the
     * candidate month's, not today's.
     */
    fun mensualReminderStage(dayOfMonth: Int, today: LocalDate = LocalDate.now()): Pair<String, ReminderStage>? {
        for (offset in -1L..1L) {
            val month = YearMonth.from(today).plusMonths(offset)
            val dueDate = month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))
            val stage = when (today) {
                dueDate.minusDays(1) -> ReminderStage.PRE_DUE
                dueDate -> ReminderStage.DUE
                dueDate.plusDays(2) -> ReminderStage.FOLLOW_UP
                else -> null
            }
            if (stage != null) return "%04d-%02d".format(month.year, month.monthValue) to stage
        }
        return null
    }

    /**
     * Whether the current period of a fixed expense has become relevant yet, evaluated at [effectiveDate].
     * QUINCENAL periods are always started (they're already split into Q1/Q2 by [currentPeriodKey]).
     * MENSUAL rules with [quincenaOnly] disabled are always started too (some users think in whole months,
     * not quincenas). Otherwise, MENSUAL rules due in the first half start on day 1; rules due in the second
     * half only start on day 16, so a bill due on day 20 doesn't count as pending during a quincena the user
     * can't act on yet. A salary logged ahead of the calendar counts as having reached its quincena: one on
     * day 14 starts a day-20 bill, while one on day 30 starts next month's day-6 bill but not its day-20 one.
     */
    fun hasPeriodStarted(
        frequency: FixedExpenseFrequency,
        dayOfMonth: Int,
        quincenaOnly: Boolean,
        today: LocalDate = LocalDate.now(),
        lastSalaryDate: LocalDate? = null
    ): Boolean =
        when {
            frequency == FixedExpenseFrequency.QUINCENAL -> true
            !quincenaOnly -> true
            else -> {
                val date = effectiveDate(today, lastSalaryDate)
                val clamped = dayOfMonth.coerceIn(1, YearMonth.from(date).lengthOfMonth())
                val startDay = if (clamped <= 15) 1 else 16
                date.dayOfMonth >= startDay
            }
        }
}
