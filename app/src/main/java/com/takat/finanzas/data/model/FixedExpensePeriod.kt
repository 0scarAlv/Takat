package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseFrequency
import java.time.LocalDate
import java.time.YearMonth

enum class ReminderStage { PRE_DUE, DUE, FOLLOW_UP }

/** Pure date math for fixed-expense periods: no Android/Room dependency, easy to reason about in isolation. */
object FixedExpensePeriod {

    /**
     * MENSUAL -> "yyyy-MM". QUINCENAL -> "yyyy-MM-Q1" (days 1-15) or "yyyy-MM-Q2" (16-end of month).
     */
    fun currentPeriodKey(frequency: FixedExpenseFrequency, today: LocalDate = LocalDate.now()): String {
        val yearMonth = "%04d-%02d".format(today.year, today.monthValue)
        return when (frequency) {
            FixedExpenseFrequency.MENSUAL -> yearMonth
            FixedExpenseFrequency.QUINCENAL -> if (today.dayOfMonth <= 15) "$yearMonth-Q1" else "$yearMonth-Q2"
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
     * Whether [today] has reached the point where a fixed expense becomes relevant to the current
     * quincena. QUINCENAL periods are always started (they're already split into Q1/Q2 by
     * [currentPeriodKey]). MENSUAL rules with [quincenaOnly] disabled are always started too (some
     * users think in whole months, not quincenas). Otherwise, MENSUAL rules due in the first half
     * start on day 1; rules due in the second half only start on day 16, so a bill due on day 20
     * doesn't count as pending during a quincena the user can't act on yet.
     */
    fun hasPeriodStarted(
        frequency: FixedExpenseFrequency,
        dayOfMonth: Int,
        quincenaOnly: Boolean,
        today: LocalDate = LocalDate.now()
    ): Boolean =
        when {
            frequency == FixedExpenseFrequency.QUINCENAL -> true
            !quincenaOnly -> true
            else -> {
                val clamped = dayOfMonth.coerceIn(1, YearMonth.from(today).lengthOfMonth())
                val startDay = if (clamped <= 15) 1 else 16
                today.dayOfMonth >= startDay
            }
        }
}
