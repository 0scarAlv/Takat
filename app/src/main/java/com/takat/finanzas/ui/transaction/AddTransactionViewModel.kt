package com.takat.finanzas.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.parseAmountToCents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransactionUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accountId: Long? = null,
    val isExpense: Boolean = true,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
    val saved: Boolean = false
)

class AddTransactionViewModel(
    private val repository: FinanceRepository,
    preselectedAccountId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState(accountId = preselectedAccountId))
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.accountsWithBalance().collect { list ->
                _uiState.update { state ->
                    val accounts = list.map { it.account }
                    state.copy(accounts = accounts, accountId = state.accountId ?: accounts.firstOrNull()?.id)
                }
            }
        }
        viewModelScope.launch {
            repository.categories.collect { cats -> _uiState.update { it.copy(categories = cats) } }
        }
    }

    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id) }
    fun onTypeChange(isExpense: Boolean) = _uiState.update { it.copy(isExpense = isExpense, categoryId = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val kind = if (_uiState.value.isExpense) CategoryKind.EXPENSE else CategoryKind.INCOME
            val id = repository.addCategory(CategoryEntity(name = name, emoji = emoji, kind = kind))
            _uiState.update { it.copy(categoryId = id) }
        }
    }

    fun save() {
        val state = _uiState.value
        val accountId = state.accountId
        if (accountId == null) {
            _uiState.update { it.copy(error = "Elegí una cuenta") }
            return
        }
        val cents = state.amountText.parseAmountToCents()
        if (cents == null || cents == 0L) {
            _uiState.update { it.copy(error = "Ingresá un monto válido") }
            return
        }
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = state.categoryId,
                    amountCents = if (state.isExpense) -cents else cents,
                    note = state.note.trim().ifBlank { null },
                    date = state.dateMillis
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
