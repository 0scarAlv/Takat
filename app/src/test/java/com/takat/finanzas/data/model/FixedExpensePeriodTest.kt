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

    @Test
    fun `a salary logged before day 15 jumps the quincenal period key to the second half immediately`() {
        val today = LocalDate.of(2026, 8, 14)
        assertEquals("2026-08-Q1", FixedExpensePeriod.currentPeriodKey(FixedExpenseFrequency.QUINCENAL, today))
        assertEquals(
            "2026-08-Q2",
            FixedExpensePeriod.currentPeriodKey(FixedExpenseFrequency.QUINCENAL, today, lastSalaryDate = today)
        )
    }

    @Test
    fun `a salary logged after day 15 jumps the quincenal period key to next month's first half`() {
        val today = LocalDate.of(2026, 8, 29)
        assertEquals("2026-08-Q2", FixedExpensePeriod.currentPeriodKey(FixedExpenseFrequency.QUINCENAL, today))
        assertEquals(
            "2026-09-Q1",
            FixedExpensePeriod.currentPeriodKey(FixedExpenseFrequency.QUINCENAL, today, lastSalaryDate = today)
        )
    }

    @Test
    fun `a stale salary anchor from an already-passed quincena is ignored`() {
        val staleSalary = LocalDate.of(2026, 7, 14)
        val today = LocalDate.of(2026, 8, 20)
        assertEquals(
            "2026-08-Q2",
            FixedExpensePeriod.currentPeriodKey(FixedExpenseFrequency.QUINCENAL, today, lastSalaryDate = staleSalary)
        )
    }

    @Test
    fun `a same-day salary starts a second-half MENSUAL bill before day 16`() {
        val today = LocalDate.of(2026, 8, 14)
        assertEquals(
            false,
            FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = true, today)
        )
        assertEquals(
            true,
            FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = true, today, lastSalaryDate = today)
        )
    }

    @Test
    fun `a salary funding the first half does not start a second-half MENSUAL bill early`() {
        // Salary logged Jul 31 targets Aug-Q1 (day > 15 funds next month's first half); a second-half
        // bill needs Aug-Q2, a later half than that, so it still waits for day 16.
        val today = LocalDate.of(2026, 8, 3)
        val salaryJul31 = LocalDate.of(2026, 7, 31)
        assertEquals(
            false,
            FixedExpensePeriod.hasPeriodStarted(FixedExpenseFrequency.MENSUAL, 20, quincenaOnly = true, today, lastSalaryDate = salaryJul31)
        )
    }

    @Test
    fun `salary anchor boundary is end of month for a pre-15 salary and day 15 of next month otherwise`() {
        assertEquals(LocalDate.of(2026, 8, 31), FixedExpensePeriod.salaryAnchorBoundary(LocalDate.of(2026, 8, 14)))
        assertEquals(LocalDate.of(2026, 9, 15), FixedExpensePeriod.salaryAnchorBoundary(LocalDate.of(2026, 8, 29)))
    }
}
