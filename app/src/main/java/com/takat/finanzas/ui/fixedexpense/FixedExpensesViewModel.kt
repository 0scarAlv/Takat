package com.takat.finanzas.ui.fixedexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.model.FixedExpensePaymentRecord
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FixedExpenseListRow(
    val entity: FixedExpenseEntity,
    val accountName: String,
    /** Sum of every payment ever made toward this rule, across all periods — only relevant when it's a debt. */
    val totalPaidCents: Long
) {
    val frequencyLabel: String
        get() = when (entity.frequency) {
            FixedExpenseFrequency.MENSUAL -> "Mensual · día ${entity.dayOfMonth}"
            FixedExpenseFrequency.QUINCENAL -> "Quincenal"
        }

    /** Null when this rule isn't a debt payment plan. */
    val debtRemainingCents: Long? get() = entity.totalDebtCents?.let { (it - totalPaidCents).coerceAtLeast(0) }
    val isDebtSettled: Boolean get() = debtRemainingCents == 0L
}

data class FixedExpensesUiState(
    val rows: List<FixedExpenseListRow> = emptyList(),
    val history: List<FixedExpensePaymentRecord> = emptyList()
)

class FixedExpensesViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<FixedExpensesUiState> =
        combine(repository.fixedExpenses(), repository.accountsWithBalance(), repository.paidHistory()) { rules, accounts, history ->
            val accountNameById = accounts.associate { it.account.id to it.account.name }
            val totalPaidByRule = history.groupBy { it.fixedExpense.id }.mapValues { (_, records) -> records.sumOf { it.paidCents } }
            FixedExpensesUiState(
                rows = rules.map { FixedExpenseListRow(it, accountNameById[it.accountId] ?: "?", totalPaidByRule[it.id] ?: 0) },
                history = history
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixedExpensesUiState())

    fun toggleEnabled(entity: FixedExpenseEntity) {
        viewModelScope.launch { repository.updateFixedExpense(entity.copy(enabled = !entity.enabled)) }
    }

    fun delete(entity: FixedExpenseEntity) {
        viewModelScope.launch { repository.deleteFixedExpense(entity) }
    }
}
