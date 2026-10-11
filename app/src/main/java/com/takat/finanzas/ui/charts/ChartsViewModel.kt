package com.takat.finanzas.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.DailyFlow
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.monthLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class ChartsUiState(
    val month: YearMonth = YearMonth.now(),
    val monthLabel: String = monthLabel(YearMonth.now()),
    val days: List<DailyFlow> = emptyList(),
    val previousIncomeCents: Long = 0,
    val previousExpenseCents: Long = 0,
    val today: LocalDate = LocalDate.now()
) {
    val incomeCents: Long get() = days.sumOf { it.incomeCents }
    val expenseCents: Long get() = days.sumOf { it.expenseCents }
    val balanceCents: Long get() = incomeCents - expenseCents

    val isCurrentMonth: Boolean get() = month == YearMonth.from(today)

    /**
     * Days of the month drawn on the chart: the whole month once it's over, up to today for the current
     * one (or further, if something was logged with a future date), and none for a month yet to come.
     */
    val chartDays: List<DailyFlow>
        get() {
            val lastWithData = days.indexOfLast { it.incomeCents != 0L || it.expenseCents != 0L } + 1
            return days.take(maxOf(elapsedDays, lastWithData))
        }

    /** Days already lived in this month, the divisor for the daily average and the projection. */
    val elapsedDays: Int
        get() = when {
            month < YearMonth.from(today) -> days.size
            isCurrentMonth -> today.dayOfMonth
            else -> 0
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChartsViewModel(repository: FinanceRepository) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ChartsUiState> = selectedMonth
        .flatMapLatest { month ->
            combine(
                repository.incomeExpenseByDay(month),
                repository.incomeExpenseSummary(month.minusMonths(1))
            ) { days, previous ->
                ChartsUiState(
                    month = month,
                    monthLabel = monthLabel(month),
                    days = days,
                    previousIncomeCents = previous.incomeCents,
                    previousExpenseCents = previous.expenseCents,
                    today = LocalDate.now()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }
}
