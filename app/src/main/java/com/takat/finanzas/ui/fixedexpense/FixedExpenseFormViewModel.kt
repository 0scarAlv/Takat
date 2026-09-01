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
    val quincenaOnly: Boolean = true,
    val notifyEnabled: Boolean = false,
    val enabled: Boolean = true,
    val isDebt: Boolean = false,
    val totalDebtText: String = "",
    val installmentsText: String = "",
    val error: String? = null,
    val saved: Boolean = false
) {
    /** Preview of the fixed cuota per period, computed live from [totalDebtText]/[installmentsText] while [isDebt] is on. */
    val debtInstallmentCents: Long?
        get() {
            val total = totalDebtText.parseAmountToCents() ?: return null
            val installments = installmentsText.toIntOrNull() ?: return null
            if (total <= 0 || installments <= 0) return null
            return ceilDiv(total, installments)
        }
}

/** Ceiling division so the first N-1 cuotas round up and the last one absorbs whatever's left over. */
private fun ceilDiv(total: Long, installments: Int): Long = (total + installments - 1) / installments

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
                        quincenaOnly = existing.quincenaOnly,
                        notifyEnabled = existing.notifyEnabled,
                        enabled = existing.enabled,
                        isDebt = existing.totalDebtCents != null,
                        totalDebtText = existing.totalDebtCents?.toEditableAmountString() ?: "",
                        installmentsText = existing.installmentsCount?.toString() ?: ""
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
    fun onQuincenaOnlyChange(value: Boolean) = _uiState.update { it.copy(quincenaOnly = value) }
    fun onNotifyEnabledChange(value: Boolean) = _uiState.update { it.copy(notifyEnabled = value) }
    fun onEnabledChange(value: Boolean) = _uiState.update { it.copy(enabled = value) }
    fun onIsDebtChange(value: Boolean) = _uiState.update { it.copy(isDebt = value, error = null) }
    fun onTotalDebtChange(value: String) = _uiState.update { it.copy(totalDebtText = value, error = null) }
    fun onInstallmentsChange(value: String) = _uiState.update { it.copy(installmentsText = value, error = null) }

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
        val totalDebtCents: Long?
        val installments: Int?
        val cents: Long
        if (state.isDebt) {
            totalDebtCents = state.totalDebtText.parseAmountToCents()
            if (totalDebtCents == null || totalDebtCents == 0L) {
                _uiState.update { it.copy(error = "Ingresá el monto total de la deuda") }
                return
            }
            installments = state.installmentsText.toIntOrNull()
            if (installments == null || installments <= 0) {
                _uiState.update { it.copy(error = "Ingresá el número de cuotas") }
                return
            }
            cents = ceilDiv(totalDebtCents, installments)
        } else {
            totalDebtCents = null
            installments = null
            val parsed = state.amountText.parseAmountToCents()
            if (parsed == null || parsed == 0L) {
                _uiState.update { it.copy(error = "Ingresá un monto válido") }
                return
            }
            cents = parsed
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
                quincenaOnly = state.quincenaOnly,
                notifyEnabled = state.notifyEnabled,
                enabled = state.enabled,
                totalDebtCents = totalDebtCents,
                installmentsCount = installments
            )
            if (fixedExpenseId == null) repository.addFixedExpense(entity) else repository.updateFixedExpense(entity)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
