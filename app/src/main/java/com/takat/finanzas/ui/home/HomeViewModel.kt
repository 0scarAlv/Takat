package com.takat.finanzas.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totals: AccountTotals = AccountTotals(0, 0, 0),
    val movements: List<Movement> = emptyList()
)

class HomeViewModel(repository: FinanceRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.accountsWithBalance(),
        repository.accountTotals(),
        repository.allMovements()
    ) { accounts, totals, movements -> HomeUiState(accounts, totals, movements) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
