package com.takat.finanzas.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DayExpensesUiState(
    val movements: List<Movement.TransactionMovement> = emptyList(),
    val totalCents: Long = 0
)

class DayExpensesViewModel(repository: FinanceRepository, date: LocalDate) : ViewModel() {
    val uiState: StateFlow<DayExpensesUiState> = repository
        .expenseTransactionsForDay(date)
        .map { movements -> DayExpensesUiState(movements, movements.sumOf { -it.transaction.amountCents }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayExpensesUiState())
}
