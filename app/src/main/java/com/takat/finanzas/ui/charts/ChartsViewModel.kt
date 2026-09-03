package com.takat.finanzas.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.monthLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

data class ChartsUiState(
    val monthLabel: String = monthLabel(YearMonth.now()),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChartsViewModel(repository: FinanceRepository) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ChartsUiState> = selectedMonth
        .flatMapLatest { month ->
            repository.incomeExpenseSummary(month).map { summary ->
                ChartsUiState(monthLabel(month), summary.incomeCents, summary.expenseCents)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    fun previousMonth() = selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = selectedMonth.update { it.plusMonths(1) }
}
