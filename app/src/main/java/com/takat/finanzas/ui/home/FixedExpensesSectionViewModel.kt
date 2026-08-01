package com.takat.finanzas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FixedExpenseRowUiState(
    val fixedExpenseId: Long,
    val periodKey: String,
    val name: String,
    val amountCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val active: Boolean,
    val lastPayment: Movement.TransactionMovement?
) {
    val isFullyPaid: Boolean get() = remainingCents <= 0
}

data class FixedExpensesSectionUiState(
    val rows: List<FixedExpenseRowUiState> = emptyList(),
    val pendingTotalCents: Long = 0
)

class FixedExpensesSectionViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<FixedExpensesSectionUiState> =
        combine(repository.pendingFixedExpenses(), repository.allMovements()) { pending, movements ->
            val movementByTransactionId = movements
                .filterIsInstance<Movement.TransactionMovement>()
                .associateBy { it.transaction.id }
            val rows = pending.map {
                // Shown as a monthly-equivalent figure (see "Gastos fijos (gastos mensuales)" legend):
                // a QUINCENAL rule only bills its amount once per half-month, so its monthly figure is x2.
                val monthlyMultiplier = if (it.fixedExpense.frequency == FixedExpenseFrequency.QUINCENAL) 2 else 1
                FixedExpenseRowUiState(
                    fixedExpenseId = it.fixedExpense.id,
                    periodKey = it.periodKey,
                    name = it.fixedExpense.name,
                    amountCents = it.fixedExpense.amountCents * monthlyMultiplier,
                    paidCents = it.paidCents * monthlyMultiplier,
                    remainingCents = it.remainingCents * monthlyMultiplier,
                    active = it.active,
                    lastPayment = it.lastPaymentTransactionId?.let { txId -> movementByTransactionId[txId] }
                )
            }
            FixedExpensesSectionUiState(
                rows = rows,
                pendingTotalCents = rows.filter { it.active && !it.isFullyPaid }.sumOf { it.remainingCents }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixedExpensesSectionUiState())

    fun onActiveChange(fixedExpenseId: Long, periodKey: String, active: Boolean) {
        viewModelScope.launch { repository.setFixedExpensePeriodActive(fixedExpenseId, periodKey, active) }
    }
}
