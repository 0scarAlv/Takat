package com.takat.finanzas.ui.transfer

import com.takat.finanzas.ui.tutorial.TutorialEvents
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.capitalizeFirst
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.util.toEditableAmountString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddTransferUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    /** Name of the recurring transfer this transfer pays, when opened from "Pagar". */
    val fixedExpenseName: String? = null,
    val error: String? = null,
    val saved: Boolean = false
)

class AddTransferViewModel(
    private val repository: FinanceRepository,
    preselectedFromAccountId: Long? = null,
    preselectedFixedExpenseId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransferUiState(fromAccountId = preselectedFromAccountId))
    val uiState: StateFlow<AddTransferUiState> = _uiState.asStateFlow()

    // The recurring-transfer period this transfer pays, if any.
    private var fixedExpenseId: Long? = null
    private var fixedExpensePeriodKey: String? = null

    init {
        if (preselectedFixedExpenseId != null) {
            viewModelScope.launch {
                val pending = repository.pendingFixedExpenses().first()
                    .find { it.fixedExpense.id == preselectedFixedExpenseId && it.fixedExpense.isTransfer }
                    ?: return@launch
                fixedExpenseId = pending.fixedExpense.id
                fixedExpensePeriodKey = pending.periodKey
                _uiState.update {
                    it.copy(
                        fromAccountId = pending.fixedExpense.accountId,
                        toAccountId = pending.fixedExpense.toAccountId,
                        categoryId = pending.fixedExpense.categoryId,
                        amountText = pending.remainingCents.takeIf { c -> c > 0 }?.toEditableAmountString() ?: it.amountText,
                        note = "Pago de ${pending.fixedExpense.name}",
                        fixedExpenseName = pending.fixedExpense.name
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.accountsWithBalance().collect { accounts ->
                _uiState.update { state ->
                    val fromId = state.fromAccountId ?: accounts.firstOrNull()?.account?.id
                    state.copy(
                        accounts = accounts,
                        fromAccountId = fromId,
                        // Default destination is the first account that isn't the origin (which may be preselected).
                        toAccountId = state.toAccountId ?: accounts.firstOrNull { it.account.id != fromId }?.account?.id
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
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value.capitalizeFirst()) }

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
                    date = state.dateMillis,
                    fixedExpenseId = fixedExpenseId,
                    fixedExpensePeriodKey = fixedExpensePeriodKey
                )
            )
            // Before flagging saved: the guide must advance before the screen pops back to Home.
            TutorialEvents.emit(TutorialEvents.TRANSFER_CREATED)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
