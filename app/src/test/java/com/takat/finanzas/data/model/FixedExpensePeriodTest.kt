package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.FixedExpenseFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FixedExpensePeriodTest {

    @Test
    fun `mensual reminder stages fire within the same month for a mid-month day`() {
        val dayOfMonth = 15

        assertEquals(
            "2026-07" to ReminderStage.PRE_DUE,
            FixedExpensePeriod.mensualReminderStage(dayOfMonth, LocalDate.of(2026, 7, 14))
        )
        assertEquals(
            "2026-07" to ReminderStage.DUE,
            FixedExpensePeriod.mensualReminderStage(dayOfMonth, LocalDate.of(2026, 7, 15))
        )
        assertEquals(
            "2026-07" to ReminderStage.FOLLOW_UP,
            FixedExpensePeriod.mensualReminderStage(dayOfMonth, LocalDate.of(2026, 7, 17))
        )
        assertNull(FixedExpensePeriod.mensualReminderStage(dayOfMonth, LocalDate.of(2026, 7, 20)))
    }

    @Test
    fun `pre-due reminder for a day-1 rule lands on the previous month's last day but resolves to next month's period`() {
        val result = FixedExpensePeriod.mensualReminderStage(1, LocalDate.of(2026, 7, 31))
        assertEquals("2026-08" to ReminderStage.PRE_DUE, result)
    }

    @Test
    fun `follow-up for a month-end rule lands in the next month but resolves to the short month's period`() {
        // dayOfMonth=31 clamps to day 30 in a 30-day month; +2 days rolls into the next month.
        val result = FixedExpensePeriod.mensualReminderStage(31, LocalDate.of(2026, 10, 2))
        assertEquals("2026-09" to ReminderStage.FOLLOW_UP, result)
    }

    @Test
    fun `period has started on day 1 for a first-half due date`() {
        assertEquals(true, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 10, quincenaOnly = true, LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun `period has not started before day 16 for a second-half due date when quincenaOnly is on`() {
        assertEquals(false, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = true, LocalDate.of(2026, 7, 15)))
        assertEquals(true, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = true, LocalDate.of(2026, 7, 16)))
    }

    @Test
    fun `period is always started for a second-half due date when quincenaOnly is off`() {
        assertEquals(true, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = false, LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun `quincenal periods are always started regardless of quincenaOnly`() {
        assertEquals(true, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.QUINCENAL, 20, quincenaOnly = true, LocalDate.of(2026, 7, 2)))
        assertEquals(true, FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.QUINCENAL, 20, quincenaOnly = false, LocalDate.of(2026, 7, 2)))
    }
}
