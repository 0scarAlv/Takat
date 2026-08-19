package com.takat.finanzas.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetPeriodType
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.computeLiveBudget
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
    /** "Valor diario": live value, recalculated with every movement. */
    val dailyBudgetCents: Long = 0,
    /** "Presupuesto diario": static value frozen at the last day rollover. */
    val frozenBudgetCents: Long = 0,
    /** "Gastado hoy": sum of today's expenses. */
    val spentTodayCents: Long = 0,
    /** frozenBudgetCents - spentTodayCents. Negative means overspent for the day. */
    val remainingTodayCents: Long = 0,
    val sarcasticMessagesEnabled: Boolean = true
)

class DailyBudgetViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<DailyBudgetUiState> =
        combine(
            repository.budgetSettings(),
            repository.accountTotals(),
            repository.spentTodayCents(),
            repository.appSettings(),
            repository.lastSalaryDate()
        ) { settings, totals, spentTodayCents, appSettings, lastSalaryDate ->
            val today = LocalDate.now(ZoneId.systemDefault())
            val enabled = settings?.enabled ?: false
            val live = computeLiveBudget(settings, totals, today, lastSalaryDate)

            if (enabled) {
                viewModelScope.launch { repository.ensureDailyBudgetFrozen(today) }
            }

            DailyBudgetUiState(
                enabled = enabled,
                periodType = settings?.periodType ?: BudgetPeriodType.QUINCENA,
                dayOfMonth = settings?.dayOfMonth ?: 1,
                basis = settings?.basis ?: BudgetBasis.DISPONIBLE,
                nextPaymentDate = live.nextPaymentDate,
                daysRemaining = live.daysRemaining,
                dailyBudgetCents = live.liveValueCents,
                frozenBudgetCents = if (enabled) settings?.frozenBudgetCents ?: 0 else 0,
                spentTodayCents = spentTodayCents,
                remainingTodayCents = if (enabled) (settings?.frozenBudgetCents ?: 0) - spentTodayCents else 0,
                sarcasticMessagesEnabled = appSettings?.sarcasticMessagesEnabled ?: true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyBudgetUiState())

    fun onEnabledChange(enabled: Boolean) = persist { it.copy(enabled = enabled) }
    fun onPeriodTypeChange(type: BudgetPeriodType) = persist { it.copy(periodType = type) }
    fun onBasisChange(basis: BudgetBasis) = persist { it.copy(basis = basis) }
    fun onDayOfMonthChange(day: Int) = persist { it.copy(dayOfMonth = day) }

    /**
     * Resets [BudgetSettingsEntity.frozenBudgetEpochDay] to the sentinel so the change takes effect on
     * "Presupuesto diario" right away instead of waiting for the next day rollover — otherwise switching
     * basis/período/día after it already froze once today leaves the old frozen value on screen even
     * though "Valor diario" (computed live) already reflects the new settings. See [computeBudgetFreeze].
     */
    private fun persist(mutate: (BudgetSettingsEntity) -> BudgetSettingsEntity) {
        viewModelScope.launch {
            val current = repository.budgetSettings().first() ?: BudgetSettingsEntity()
            repository.updateBudgetSettings(mutate(current).copy(frozenBudgetEpochDay = 0))
        }
    }
}
