package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseFrequency
import java.time.LocalDate
import java.time.YearMonth

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

    /** The day of month a notification should fire for [today]'s period, given the rule's frequency/dayOfMonth. */
    fun notifyDayOfMonth(frequency: FixedExpenseFrequency, dayOfMonth: Int, today: LocalDate = LocalDate.now()): Int =
        when (frequency) {
            FixedExpenseFrequency.MENSUAL -> dayOfMonth.coerceIn(1, YearMonth.from(today).lengthOfMonth())
            FixedExpenseFrequency.QUINCENAL -> if (today.dayOfMonth <= 15) 1 else 16
        }

    fun isNotifyDay(frequency: FixedExpenseFrequency, dayOfMonth: Int, today: LocalDate = LocalDate.now()): Boolean =
        today.dayOfMonth == notifyDayOfMonth(frequency, dayOfMonth, today)
}
