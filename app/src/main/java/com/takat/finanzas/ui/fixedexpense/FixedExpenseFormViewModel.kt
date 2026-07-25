package com.takat.finanzas.ui.fixedexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.util.toEditableAmountString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FixedExpenseFormUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isEditing: Boolean = false,
    val name: String = "",
    val amountText: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val frequency: FixedExpenseFrequency = FixedExpenseFrequency.MENSUAL,
    val dayOfMonth: Int = 1,
    val notifyEnabled: Boolean = false,
    val enabled: Boolean = true,
    val error: String? = null,
    val saved: Boolean = false
)

class FixedExpenseFormViewModel(
    private val repository: FinanceRepository,
    private val fixedExpenseId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(FixedExpenseFormUiState(isEditing = fixedExpenseId != null))
    val uiState: StateFlow<FixedExpenseFormUiState> = _uiState.asStateFlow()

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
        if (fixedExpenseId != null) {
            viewModelScope.launch {
                val existing = repository.fixedExpenses().first().find { it.id == fixedExpenseId } ?: return@launch
                _uiState.update {
                    it.copy(
                        name = existing.name,
                        amountText = existing.amountCents.toEditableAmountString(),
                        accountId = existing.accountId,
                        categoryId = existing.categoryId,
                        frequency = existing.frequency,
                        dayOfMonth = existing.dayOfMonth,
                        notifyEnabled = existing.notifyEnabled,
                        enabled = existing.enabled
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id) }
    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun onFrequencyChange(frequency: FixedExpenseFrequency) = _uiState.update { it.copy(frequency = frequency) }
    fun onDayOfMonthChange(day: Int) = _uiState.update { it.copy(dayOfMonth = day) }
    fun onNotifyEnabledChange(value: Boolean) = _uiState.update { it.copy(notifyEnabled = value) }
    fun onEnabledChange(value: Boolean) = _uiState.update { it.copy(enabled = value) }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val id = repository.addCategory(CategoryEntity(name = name, emoji = emoji, kind = CategoryKind.EXPENSE))
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
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Ingresá un nombre") }
            return
        }
        val cents = state.amountText.parseAmountToCents()
        if (cents == null || cents == 0L) {
            _uiState.update { it.copy(error = "Ingresá un monto válido") }
            return
        }
        viewModelScope.launch {
            val entity = FixedExpenseEntity(
                id = fixedExpenseId ?: 0,
                name = state.name.trim(),
                amountCents = cents,
                accountId = accountId,
                categoryId = state.categoryId,
                frequency = state.frequency,
                dayOfMonth = state.dayOfMonth,
                notifyEnabled = state.notifyEnabled,
                enabled = state.enabled
            )
            if (fixedExpenseId == null) repository.addFixedExpense(entity) else repository.updateFixedExpense(entity)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
