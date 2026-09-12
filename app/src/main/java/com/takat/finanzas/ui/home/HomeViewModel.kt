package com.takat.finanzas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AppSettingsEntity
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totals: AccountTotals = AccountTotals(0, 0, 0),
    val movements: List<Movement> = emptyList(),
    val sarcasticMessagesEnabled: Boolean = true,
    val amountsHidden: Boolean = false
)

class HomeViewModel(private val repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.accountsWithBalance(),
        repository.accountTotals(),
        repository.allMovements(),
        repository.appSettings()
    ) { accounts, totals, movements, settings ->
        HomeUiState(
            accounts,
            totals,
            movements,
            settings?.sarcasticMessagesEnabled ?: true,
            settings?.amountsHidden ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleAmountsHidden() {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(amountsHidden = !current.amountsHidden))
        }
    }
}
