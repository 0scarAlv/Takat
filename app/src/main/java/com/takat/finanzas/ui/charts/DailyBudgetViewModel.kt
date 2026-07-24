package com.takat.finanzas.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetPeriodType
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.daysRemaining
import com.takat.finanzas.util.dailyBudgetCents
import com.takat.finanzas.util.nextPaymentDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DailyBudgetUiState(
    val enabled: Boolean = false,
    val periodType: BudgetPeriodType = BudgetPeriodType.QUINCENA,
    val dayOfMonth: Int = 1,
    val basis: BudgetBasis = BudgetBasis.DISPONIBLE,
    val nextPaymentDate: LocalDate? = null,
    val daysRemaining: Long = 0,
    val dailyBudgetCents: Long = 0
)

class DailyBudgetViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<DailyBudgetUiState> =
        combine(repository.budgetSettings(), repository.accountTotals()) { settings, totals ->
            val today = LocalDate.now(ZoneId.systemDefault())
            val enabled = settings?.enabled ?: false
            val periodType = settings?.periodType ?: BudgetPeriodType.QUINCENA
            val dayOfMonth = settings?.dayOfMonth ?: 1
            val basis = settings?.basis ?: BudgetBasis.DISPONIBLE
            val nextDate = if (enabled) nextPaymentDate(today, periodType, dayOfMonth) else null
            val remaining = nextDate?.let { daysRemaining(today, it) } ?: 0
            val balance = if (basis == BudgetBasis.DISPONIBLE) totals.availableCents else totals.capitalCents
            DailyBudgetUiState(
                enabled = enabled,
                periodType = periodType,
                dayOfMonth = dayOfMonth,
                basis = basis,
                nextPaymentDate = nextDate,
                daysRemaining = remaining,
                dailyBudgetCents = if (enabled) dailyBudgetCents(balance, remaining) else 0
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyBudgetUiState())

    fun onEnabledChange(enabled: Boolean) = persist { it.copy(enabled = enabled) }
    fun onPeriodTypeChange(type: BudgetPeriodType) = persist { it.copy(periodType = type) }
    fun onBasisChange(basis: BudgetBasis) = persist { it.copy(basis = basis) }
    fun onDayOfMonthChange(day: Int) = persist { it.copy(dayOfMonth = day) }

    private fun persist(mutate: (BudgetSettingsEntity) -> BudgetSettingsEntity) {
        viewModelScope.launch {
            val current = repository.budgetSettings().first() ?: BudgetSettingsEntity()
            repository.updateBudgetSettings(mutate(current))
        }
    }
}
