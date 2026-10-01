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

enum class FixedExpenseKind { NORMAL, DEUDA, CREDITO }

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
    val kind: FixedExpenseKind = FixedExpenseKind.NORMAL,
    val totalDebtText: String = "",
    val installmentsText: String = "",
    val creditAccountId: Long? = null,
    val principalText: String = "",
    val interestRateText: String = "",
    val cuotaText: String = "",
    /** Once the user types their own cuota (e.g. the one on the bank statement), stop overwriting it with the suggestion. */
    val cuotaEdited: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
) {
    val isDebt: Boolean get() = kind == FixedExpenseKind.DEUDA

    /** French-amortization cuota for the credit fields, or null while they're incomplete. */
    val suggestedCuotaCents: Long?
        get() {
            val principal = principalText.parseAmountToCents() ?: return null
            val installments = installmentsText.toIntOrNull() ?: return null
            if (principal <= 0 || installments <= 0) return null
            val annualRate = parseRatePercent(interestRateText) ?: return null
            return frenchCuotaCents(principal, installments, annualRate, frequency)
        }

    /** Cuota x installments: what the credit purchase will cost in the end. */
    val creditTotalToPayCents: Long?
        get() {
            val cuota = cuotaText.parseAmountToCents() ?: return null
            val installments = installmentsText.toIntOrNull() ?: return null
            if (cuota <= 0 || installments <= 0) return null
            return cuota * installments
        }

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

/** Blank means 0% (e.g. "tasa cero" store plans). Accepts "25", "25.5" or "25,5". */
internal fun parseRatePercent(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0.0
    return trimmed.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it < 1000 }
}

/**
 * Fixed cuota (sistema frances) for [principalCents] over [installments] periods at [annualRatePercent],
 * with the rate split evenly across the periods of a year (12 monthly, 24 quincenal). Rounded up to the cent.
 */
internal fun frenchCuotaCents(principalCents: Long, installments: Int, annualRatePercent: Double, frequency: FixedExpenseFrequency): Long {
    val periodsPerYear = if (frequency == FixedExpenseFrequency.QUINCENAL) 24 else 12
    val i = annualRatePercent / 100.0 / periodsPerYear
    if (i == 0.0) return ceilDiv(principalCents, installments)
    val cuota = principalCents * i / (1 - Math.pow(1 + i, -installments.toDouble()))
    return kotlin.math.ceil(cuota - 1e-6).toLong()
}

/** 2550 -> "25.5", 2500 -> "25". */
private fun formatRateBps(bps: Int): String =
    if (bps % 100 == 0) (bps / 100).toString() else (bps / 100.0).toString()

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
                        kind = when {
                            existing.isCreditPurchase -> FixedExpenseKind.CREDITO
                            existing.totalDebtCents != null -> FixedExpenseKind.DEUDA
                            else -> FixedExpenseKind.NORMAL
                        },
                        totalDebtText = existing.totalDebtCents?.toEditableAmountString() ?: "",
                        installmentsText = existing.installmentsCount?.toString() ?: "",
                        creditAccountId = existing.creditAccountId,
                        principalText = existing.financedPrincipalCents?.toEditableAmountString() ?: "",
                        interestRateText = existing.interestRateBps?.let { formatRateBps(it) } ?: "",
                        cuotaText = if (existing.isCreditPurchase) existing.amountCents.toEditableAmountString() else "",
                        // Never re-suggest over a saved plan's cuota: it's what the bank actually charges.
                        cuotaEdited = existing.isCreditPurchase
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, error = null) }
    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id) }
    fun onCategoryChange(id: Long) = _uiState.update { it.copy(categoryId = id) }
    fun onFrequencyChange(frequency: FixedExpenseFrequency) = updateCredit { it.copy(frequency = frequency) }
    fun onDayOfMonthChange(day: Int) = _uiState.update { it.copy(dayOfMonth = day) }
    fun onQuincenaOnlyChange(value: Boolean) = _uiState.update { it.copy(quincenaOnly = value) }
    fun onNotifyEnabledChange(value: Boolean) = _uiState.update { it.copy(notifyEnabled = value) }
    fun onEnabledChange(value: Boolean) = _uiState.update { it.copy(enabled = value) }
    fun onKindChange(kind: FixedExpenseKind) = updateCredit { it.copy(kind = kind, error = null) }
    fun onTotalDebtChange(value: String) = _uiState.update { it.copy(totalDebtText = value, error = null) }
    fun onInstallmentsChange(value: String) = updateCredit { it.copy(installmentsText = value, error = null) }
    fun onPrincipalChange(value: String) = updateCredit { it.copy(principalText = value, error = null) }
    fun onInterestRateChange(value: String) = updateCredit { it.copy(interestRateText = value, error = null) }
    fun onCuotaChange(value: String) = _uiState.update { it.copy(cuotaText = value, cuotaEdited = value.isNotBlank(), error = null) }

    /** The cuota is usually charged to the card itself, so picking the card also preselects it as the paying account. */
    fun onCreditAccountChange(id: Long) = _uiState.update { it.copy(creditAccountId = id, accountId = id, error = null) }

    /** Applies [transform] and refreshes the suggested cuota, unless the user already typed their own. */
    private fun updateCredit(transform: (FixedExpenseFormUiState) -> FixedExpenseFormUiState) = _uiState.update { state ->
        val next = transform(state)
        if (next.kind != FixedExpenseKind.CREDITO || next.cuotaEdited) next
        else next.copy(cuotaText = next.suggestedCuotaCents?.toEditableAmountString() ?: "")
    }

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
        var creditAccountId: Long? = null
        var principalCents: Long? = null
        var interestRateBps: Int? = null
        if (state.kind == FixedExpenseKind.CREDITO) {
            creditAccountId = state.creditAccountId
            if (creditAccountId == null) {
                _uiState.update { it.copy(error = "Elegí la cuenta de crédito") }
                return
            }
            principalCents = state.principalText.parseAmountToCents()
            if (principalCents == null || principalCents <= 0L) {
                _uiState.update { it.copy(error = "Ingresá el monto de la compra") }
                return
            }
            installments = state.installmentsText.toIntOrNull()
            if (installments == null || installments <= 0) {
                _uiState.update { it.copy(error = "Ingresá el número de cuotas") }
                return
            }
            val rate = parseRatePercent(state.interestRateText)
            if (rate == null) {
                _uiState.update { it.copy(error = "Ingresá una tasa válida (ej. 25 o 0)") }
                return
            }
            interestRateBps = Math.round(rate * 100).toInt()
            val cuota = state.cuotaText.parseAmountToCents()
            if (cuota == null || cuota <= 0L) {
                _uiState.update { it.copy(error = "Ingresá la cuota") }
                return
            }
            if (cuota * installments < principalCents) {
                _uiState.update { it.copy(error = "Con esa cuota no se llega a pagar el monto de la compra") }
                return
            }
            cents = cuota
            totalDebtCents = cuota * installments
        } else if (state.isDebt) {
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
                installmentsCount = installments,
                creditAccountId = creditAccountId,
                financedPrincipalCents = principalCents,
                interestRateBps = interestRateBps
            )
            if (fixedExpenseId == null) repository.addFixedExpense(entity) else repository.updateFixedExpense(entity)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
