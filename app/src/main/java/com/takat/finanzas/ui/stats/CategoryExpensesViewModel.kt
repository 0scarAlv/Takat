package com.takat.finanzas.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CategoryExpensesUiState(
    val movements: List<Movement.TransactionMovement> = emptyList(),
    val totalCents: Long = 0
) {
    val categoryLabel: String
        get() = movements.firstOrNull()?.category?.name ?: "Sin categoría"
}

class CategoryExpensesViewModel(
    repository: FinanceRepository,
    categoryId: Long?,
    fromMillis: Long,
    toMillis: Long
) : ViewModel() {
    val uiState: StateFlow<CategoryExpensesUiState> = repository
        .expenseTransactionsForCategory(categoryId, fromMillis, toMillis)
        .map { movements -> CategoryExpensesUiState(movements, movements.sumOf { -it.transaction.amountCents }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryExpensesUiState())
}
