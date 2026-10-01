package com.takat.finanzas.ui.fixedexpense

import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CreditPurchaseTest {

    private fun plan(totalPaidCuota: Long = 11406) = FixedExpenseEntity(
        id = 7,
        name = "Refri",
        amountCents = totalPaidCuota,
        accountId = 2,
        categoryId = null,
        frequency = FixedExpenseFrequency.MENSUAL,
        dayOfMonth = 5,
        totalDebtCents = totalPaidCuota * 12,
        installmentsCount = 12,
        creditAccountId = 2,
        financedPrincipalCents = 120_000,
        interestRateBps = 2500
    )

    @Test
    fun frenchCuota_monthlyAndQuincenal() {
        assertEquals(11406, frenchCuotaCents(120_000, 12, 25.0, FixedExpenseFrequency.MENSUAL))
        assertEquals(5677, frenchCuotaCents(120_000, 24, 25.0, FixedExpenseFrequency.QUINCENAL))
    }

    @Test
    fun frenchCuota_zeroRateSplitsEvenlyRoundingUp() {
        assertEquals(10_000, frenchCuotaCents(120_000, 12, 0.0, FixedExpenseFrequency.MENSUAL))
        assertEquals(3334, frenchCuotaCents(10_000, 3, 0.0, FixedExpenseFrequency.MENSUAL))
    }

    @Test
    fun parseRate_acceptsBlankCommaAndDot() {
        assertEquals(0.0, parseRatePercent(" ")!!, 0.0)
        assertEquals(25.5, parseRatePercent("25,5")!!, 0.0)
        assertEquals(null, parseRatePercent("abc"))
    }

    @Test
    fun financedRemaining_releasesPrincipalProportionally() {
        val p = plan()
        assertEquals(120_000, p.financedRemainingCents(0))
        assertEquals(110_000, p.financedRemainingCents(11406))
        assertEquals(0, p.financedRemainingCents(11406 * 12))
        // Overpaying never goes negative.
        assertEquals(0, p.financedRemainingCents(11406 * 20))
    }

    @Test
    fun financedRemaining_zeroForRegularDebtPlans() {
        val regularDebt = plan().copy(creditAccountId = null, financedPrincipalCents = null)
        assertEquals(0, regularDebt.financedRemainingCents(0))
    }

    @Test
    fun backup_roundTripsCreditPlanAndPaymentLink() {
        val card = AccountEntity(id = 2, name = "Visa", initialBalanceCents = 0, colorArgb = 0, isDebt = true)
        val payment = TransactionEntity(
            id = 1, accountId = 2, categoryId = null, amountCents = -11406, note = "Pago de Refri",
            date = 1_760_000_000_000, fixedExpenseId = 7, fixedExpensePeriodKey = "2026-09"
        )
        val csv = BackupCsv.encode(listOf(card), emptyList(), listOf(payment), emptyList(), fixedExpenses = listOf(plan()))
        val parsed = BackupCsv.decode(csv)

        val fe = parsed.fixedExpenses.single()
        assertEquals("Visa", fe.creditAccountName)
        assertEquals(120_000L, fe.financedPrincipalCents)
        assertEquals(2500, fe.interestRateBps)
        val tx = parsed.transactions.single()
        assertEquals("Refri", tx.fixedExpenseName)
        assertEquals("2026-09", tx.fixedExpensePeriodKey)
    }
}
