package com.takat.finanzas.data.model

import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * "Saldo a la fecha": the state of the books right after one movement, replayed from each account's
 * initial balance through every movement up to and including it — like a bank statement's running balance.
 */
data class BalanceSnapshot(
    val movement: Movement,
    /** The account(s) the movement touched: one for a transaction, origin and destination for a transfer. */
    val accounts: List<AccountSnapshot>,
    /** Sum of non-debt accounts that count toward the total, after the movement. */
    val capitalCents: Long,
    /** What was owed on debt accounts that count toward the total, as a positive number. */
    val debtCents: Long,
    /** Cumulative income since the start, on accounts that count toward the total. */
    val incomeCents: Long,
    /** Cumulative spending since the start (positive number), on accounts that count toward the total. */
    val expenseCents: Long,
    /** Transfers between accounts so far: they move money without being income or spending. */
    val transferCount: Int,
    val transferCents: Long,
    /** End-of-day capital total from the first movement up to this one. */
    val capitalSeries: List<BalancePoint>,
    /** Cumulative spending per category up to this movement, largest first. */
    val expensesByCategory: List<CategoryTotal>,
    /** Income vs spending for the last months up to the movement's month, oldest first. */
    val monthlyFlows: List<MonthFlow>
)

data class AccountSnapshot(
    val account: AccountEntity,
    val balanceBeforeCents: Long,
    val balanceAfterCents: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val transferInCents: Long,
    val transferOutCents: Long,
    /** End-of-day balance of this account from its first movement up to this one. */
    val series: List<BalancePoint>
)

data class BalancePoint(val date: LocalDate, val cents: Long)

data class CategoryTotal(val category: CategoryEntity?, val cents: Long)

data class MonthFlow(val month: YearMonth, val incomeCents: Long, val expenseCents: Long)
