package com.takat.finanzas.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.parseAmountToCents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransferUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
    val saved: Boolean = false
)

class AddTransferViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransferUiState())
    val uiState: StateFlow<AddTransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.accountsWithBalance().collect { list ->
                _uiState.update { state ->
                    val accounts = list.map { it.account }
                    state.copy(
                        accounts = accounts,
                        fromAccountId = state.fromAccountId ?: accounts.firstOrNull()?.id,
                        toAccountId = state.toAccountId ?: accounts.getOrNull(1)?.id
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.categories.collect { cats -> _uiState.update { it.copy(categories = cats) } }
        }
    }

    fun onFromAccountChange(id: Long) = _uiState.update { it.copy(fromAccountId = id) }
    fun onToAccountChange(id: Long) = _uiState.update { it.copy(toAccountId = id) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val id = repository.addCategory(CategoryEntity(name = name, emoji = emoji, kind = CategoryKind.BOTH))
            _uiState.update { it.copy(categoryId = id) }
        }
    }

    fun save() {
        val state = _uiState.value
        val from = state.fromAccountId
        val to = state.toAccountId
        if (from == null || to == null) {
            _uiState.update { it.copy(error = "Elegí las dos cuentas") }
            return
        }
        if (from == to) {
            _uiState.update { it.copy(error = "Elegí dos cuentas distintas") }
            return
        }
        val cents = state.amountText.parseAmountToCents()
        if (cents == null || cents == 0L) {
            _uiState.update { it.copy(error = "Ingresá un monto válido") }
            return
        }
        viewModelScope.launch {
            repository.addTransfer(
                TransferEntity(
                    fromAccountId = from,
                    toAccountId = to,
                    categoryId = state.categoryId,
                    amountCents = cents,
                    note = state.note.trim().ifBlank { null },
                    date = state.dateMillis
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
