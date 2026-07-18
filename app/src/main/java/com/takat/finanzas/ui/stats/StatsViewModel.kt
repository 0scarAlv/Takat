package com.takat.finanzas.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.currentMonthLabel
import com.takat.finanzas.util.currentMonthRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val monthLabel: String = currentMonthLabel(),
    val totalExpenseCents: Long = 0,
    val categoryExpenses: List<CategoryExpense> = emptyList()
)

class StatsViewModel(repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = run {
        val (start, end) = currentMonthRange()
        repository.expensesByCategory(start, end)
            .map { list -> StatsUiState(currentMonthLabel(), list.sumOf { it.totalCents }, list) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
