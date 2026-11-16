package com.takat.finanzas.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.model.DailyExpense
import com.takat.finanzas.data.model.MonthExpense
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

private const val MONTHLY_TREND_MONTHS = 6

data class StatsUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = monthLabel(YearMonth.now()),
    val totalExpenseCents: Long = 0,
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val dailyExpenses: List<DailyExpense> = emptyList(),
    val monthlyExpenses: List<MonthExpense> = emptyList(),
    val fromMillis: Long = 0,
    val toMillis: Long = 0
)

/**
 * Last month shown in the "Últimos meses" chart. It stays anchored on the current month while the
 * selected month is still inside the window, so picking an older month doesn't hide the newer ones
 * (and you can tap back to the present). It only slides back when the selection falls out of range.
 */
internal fun trendWindowEnd(selected: YearMonth, now: YearMonth = YearMonth.now()): YearMonth {
    val latestThatKeepsSelected = selected.plusMonths((MONTHLY_TREND_MONTHS - 1).toLong())
    val end = if (now.isBefore(latestThatKeepsSelected)) now else latestThatKeepsSelected
    return if (end.isBefore(selected)) selected else end
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(repository: FinanceRepository) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<StatsUiState> = selectedMonth
        .flatMapLatest { month ->
            val (start, end) = monthRange(month)
            combine(
                repository.expensesByCategory(start, end),
                repository.expensesByDay(month),
                repository.expensesByMonthRange(trendWindowEnd(month), MONTHLY_TREND_MONTHS)
            ) { categoryExpenses, dailyExpenses, monthlyExpenses ->
                StatsUiState(
                    selectedMonth = month,
                    monthLabel = monthLabel(month),
                    totalExpenseCents = categoryExpenses.sumOf { it.totalCents },
                    categoryExpenses = categoryExpenses,
                    dailyExpenses = dailyExpenses,
                    monthlyExpenses = monthlyExpenses,
                    fromMillis = start,
                    toMillis = end
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }
    fun selectMonth(month: YearMonth) = selectedMonth.update { month }
}
