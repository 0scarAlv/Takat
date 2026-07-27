package com.takat.finanzas.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.model.PendingFixedExpense
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.util.toEditableAmountString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An attachment the user picked/captured that hasn't been persisted yet (no transactionId until save()). */
data class PendingAttachment(
    val type: AttachmentType,
    val bytes: ByteArray,
    val label: String
)

data class AddTransactionUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accountId: Long? = null,
    val isExpense: Boolean = true,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val pendingAttachment: PendingAttachment? = null,
    val pendingFixedExpenses: List<PendingFixedExpense> = emptyList(),
    val selectedFixedExpenseId: Long? = null,
    val error: String? = null,
    val saved: Boolean = false
)

class AddTransactionViewModel(
    private val repository: FinanceRepository,
    preselectedAccountId: Long?,
    private val preselectedFixedExpenseId: Long? = null
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
        viewModelScope.launch {
            repository.pendingFixedExpenses().collect { pending ->
                val stillSelectable = pending.filter { it.isPending }
                _uiState.update { it.copy(pendingFixedExpenses = stillSelectable) }
                if (preselectedFixedExpenseId != null && _uiState.value.selectedFixedExpenseId == null) {
                    stillSelectable.find { it.fixedExpense.id == preselectedFixedExpenseId }?.let { onFixedExpenseSelect(it) }
                }
            }
        }
    }

    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id) }
    fun onTypeChange(isExpense: Boolean) = _uiState.update { it.copy(isExpense = isExpense, categoryId = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }
    fun onAttachmentPicked(attachment: PendingAttachment) = _uiState.update { it.copy(pendingAttachment = attachment) }
    fun clearPendingAttachment() = _uiState.update { it.copy(pendingAttachment = null) }

    fun onFixedExpenseToggle(fixedExpenseId: Long) {
        if (_uiState.value.selectedFixedExpenseId == fixedExpenseId) {
            _uiState.update { it.copy(selectedFixedExpenseId = null) }
        } else {
            _uiState.value.pendingFixedExpenses.find { it.fixedExpense.id == fixedExpenseId }?.let { onFixedExpenseSelect(it) }
        }
    }

    private fun onFixedExpenseSelect(pending: PendingFixedExpense) {
        _uiState.update {
            it.copy(
                selectedFixedExpenseId = pending.fixedExpense.id,
                accountId = pending.fixedExpense.accountId,
                categoryId = pending.fixedExpense.categoryId,
                isExpense = true,
                amountText = pending.remainingCents.toEditableAmountString(),
                note = "Pago de ${pending.fixedExpense.name}"
            )
        }
    }

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
        val selectedFixedExpensePeriodKey = state.selectedFixedExpenseId?.let { id ->
            state.pendingFixedExpenses.find { it.fixedExpense.id == id }?.periodKey
        }
        viewModelScope.launch {
            val transactionId = repository.addTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = state.categoryId,
                    amountCents = if (state.isExpense) -cents else cents,
                    note = state.note.trim().ifBlank { null },
                    date = state.dateMillis,
                    // Tagging the transaction itself (rather than a separately tracked "paid" flag) means
                    // deleting it later automatically un-counts it — two partial payments (20 + 30) still add up.
                    fixedExpenseId = state.selectedFixedExpenseId,
                    fixedExpensePeriodKey = selectedFixedExpensePeriodKey
                )
            )
            state.pendingAttachment?.let { pending ->
                if (pending.type == AttachmentType.IMAGE) {
                    repository.addImageAttachment(transactionId, pending.bytes)
                } else {
                    repository.addDocumentAttachment(transactionId, pending.type, pending.bytes)
                }
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
