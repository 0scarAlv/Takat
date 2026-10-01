package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FixedExpenseFrequency { MENSUAL, QUINCENAL }

@Entity(
    tableName = "fixed_expenses",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("accountId"), Index("categoryId")]
)
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long?,
    val frequency: FixedExpenseFrequency,
    /** Day of month (1-31, clamped to the last day of shorter months) used only when frequency is MENSUAL. */
    val dayOfMonth: Int,
    /**
     * MENSUAL only: whether this expense counts as pending only from its own quincena onward
     * (day 1 if [dayOfMonth] <= 15, day 16 otherwise) instead of from day 1 of the month. Useful
     * for someone paid biweekly who can't act on a second-half bill during the first quincena;
     * someone paid monthly may prefer seeing it for the whole month instead.
     */
    val quincenaOnly: Boolean = true,
    val notifyEnabled: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Non-null marks this fixed expense as a debt payment plan: [amountCents] is the fixed cuota per
     * period (never re-amortized, even if a period is overpaid), but the rule stops counting as pending
     * once total lifetime payments reach [totalDebtCents] — see [com.takat.finanzas.data.model.PendingFixedExpense].
     */
    val totalDebtCents: Long? = null,
    /** Installments the debt was originally planned over. Informational only (used to prefill the edit form); not enforced. */
    val installmentsCount: Int? = null,
    /**
     * Non-null (together with [financedPrincipalCents]) marks this rule as a "compra a crédito": the purchase
     * principal is owed on this account (typically a credit card) and shows up in its balance, but it's never
     * a transaction — it's derived, and released proportionally as cuotas are paid. See [financedRemainingCents].
     * [totalDebtCents] holds the total to repay (cuota × installments, interest included) and [amountCents]
     * the cuota, so the regular debt-plan machinery handles pending/settled state unchanged.
     */
    val creditAccountId: Long? = null,
    /** Price of what was bought on credit, without interest. */
    val financedPrincipalCents: Long? = null,
    /** Annual interest rate in basis points (2500 = 25%). Informational: the cuota is stored as entered, never re-amortized. */
    val interestRateBps: Int? = null
) {
    val isCreditPurchase: Boolean get() = creditAccountId != null && financedPrincipalCents != null

    /**
     * Principal still owed on [creditAccountId] after [totalPaidCents] of cuotas, released in proportion to
     * the total to repay (so the interest share of each cuota never reduces the principal). Zero for rules
     * that aren't credit purchases.
     */
    fun financedRemainingCents(totalPaidCents: Long): Long {
        val principal = financedPrincipalCents ?: return 0
        if (creditAccountId == null) return 0
        val totalToPay = totalDebtCents?.takeIf { it > 0 } ?: return principal
        val paid = totalPaidCents.coerceIn(0, totalToPay)
        val released = (principal.toBigInteger() * paid.toBigInteger() / totalToPay.toBigInteger()).toLong()
        return (principal - released).coerceAtLeast(0)
    }
}
