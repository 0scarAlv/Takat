package com.takat.finanzas.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountDetailUiState(
    val accountWithBalance: AccountWithBalance? = null,
    val movements: List<Movement> = emptyList()
)

class AccountDetailViewModel(repository: FinanceRepository, accountId: Long) : ViewModel() {
    val uiState: StateFlow<AccountDetailUiState> = combine(
        repository.accountWithBalance(accountId),
        repository.movementsForAccount(accountId)
    ) { accountWithBalance, movements -> AccountDetailUiState(accountWithBalance, movements) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())
}
