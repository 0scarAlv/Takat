package com.takat.finanzas.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.model.DailyExpense
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.monthLabel
import com.takat.finanzas.util.monthRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

data class StatsUiState(
    val monthLabel: String = monthLabel(YearMonth.now()),
    val totalExpenseCents: Long = 0,
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val dailyExpenses: List<DailyExpense> = emptyList(),
    val fromMillis: Long = 0,
    val toMillis: Long = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(repository: FinanceRepository) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<StatsUiState> = selectedMonth
        .flatMapLatest { month ->
            val (start, end) = monthRange(month)
            combine(
                repository.expensesByCategory(start, end),
                repository.expensesByDay(month)
            ) { categoryExpenses, dailyExpenses ->
                StatsUiState(
                    monthLabel = monthLabel(month),
                    totalExpenseCents = categoryExpenses.sumOf { it.totalCents },
                    categoryExpenses = categoryExpenses,
                    dailyExpenses = dailyExpenses,
                    fromMillis = start,
                    toMillis = end
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }
}
