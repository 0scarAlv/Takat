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
    val accountName: String
) {
    val frequencyLabel: String
        get() = when (entity.frequency) {
            FixedExpenseFrequency.MENSUAL -> "Mensual · día ${entity.dayOfMonth}"
            FixedExpenseFrequency.QUINCENAL -> "Quincenal"
        }
}

data class FixedExpensesUiState(
    val rows: List<FixedExpenseListRow> = emptyList(),
    val history: List<FixedExpensePaymentRecord> = emptyList()
)

class FixedExpensesViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<FixedExpensesUiState> =
        combine(repository.fixedExpenses(), repository.accountsWithBalance(), repository.paidHistory()) { rules, accounts, history ->
            val accountNameById = accounts.associate { it.account.id to it.account.name }
            FixedExpensesUiState(
                rows = rules.map { FixedExpenseListRow(it, accountNameById[it.accountId] ?: "?") },
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
