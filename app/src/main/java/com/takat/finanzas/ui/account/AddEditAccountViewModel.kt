package com.takat.finanzas.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.ui.theme.AccountSwatches
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.util.toEditableAmountString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditAccountUiState(
    val name: String = "",
    val amountText: String = "",
    val isDebt: Boolean = false,
    val includeInTotal: Boolean = true,
    val colorArgb: Int = AccountSwatches.first(),
    val isEditing: Boolean = false,
    val existingAccount: AccountEntity? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class AddEditAccountViewModel(
    private val repository: FinanceRepository,
    private val accountId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditAccountUiState(isEditing = accountId != null))
    val uiState: StateFlow<AddEditAccountUiState> = _uiState.asStateFlow()

    init {
        if (accountId != null) {
            viewModelScope.launch {
                val existing = repository.accountWithBalance(accountId).first()?.account
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            name = existing.name,
                            amountText = existing.initialBalanceCents.toEditableAmountString(),
                            isDebt = existing.isDebt,
                            includeInTotal = existing.includeInTotal,
                            colorArgb = existing.colorArgb,
                            existingAccount = existing
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onIsDebtChange(value: Boolean) = _uiState.update { it.copy(isDebt = value) }
    fun onIncludeInTotalChange(value: Boolean) = _uiState.update { it.copy(includeInTotal = value) }
    fun onColorChange(color: Int) = _uiState.update { it.copy(colorArgb = color) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Ponele un nombre a la cuenta") }
            return
        }
        val cents = if (state.amountText.isBlank()) 0L else state.amountText.parseAmountToCents()
        if (cents == null) {
            _uiState.update { it.copy(error = "Monto inválido") }
            return
        }
        val signedCents = if (state.isDebt) -cents else cents

        viewModelScope.launch {
            val existing = state.existingAccount
            if (existing != null) {
                repository.updateAccount(
                    existing.copy(
                        name = state.name.trim(),
                        initialBalanceCents = signedCents,
                        colorArgb = state.colorArgb,
                        isDebt = state.isDebt,
                        includeInTotal = state.includeInTotal
                    )
                )
            } else {
                repository.addAccount(
                    AccountEntity(
                        name = state.name.trim(),
                        initialBalanceCents = signedCents,
                        colorArgb = state.colorArgb,
                        isDebt = state.isDebt,
                        includeInTotal = state.includeInTotal
                    )
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val existing = _uiState.value.existingAccount ?: return
        viewModelScope.launch {
            repository.deleteAccount(existing)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
