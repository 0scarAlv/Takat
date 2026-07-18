package com.takat.finanzas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.csv.ParsedBackup
import com.takat.finanzas.data.model.ImportResult
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val pendingImport: ParsedBackup? = null,
    val importError: String? = null,
    val importResult: ImportResult? = null
)

class SettingsViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    suspend fun exportCsv(): String = repository.exportCsv()

    fun parseForPreview(csv: String) {
        val parsed = try {
            BackupCsv.decode(csv)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(importError = e.message ?: "Formato de CSV no reconocido") }
            return
        }
        _uiState.update { it.copy(pendingImport = parsed, importError = null) }
    }

    fun confirmImport() {
        val parsed = _uiState.value.pendingImport ?: return
        viewModelScope.launch {
            val result = repository.commitImport(parsed)
            _uiState.update { it.copy(pendingImport = null, importResult = result) }
        }
    }

    fun dismissPreview() = _uiState.update { it.copy(pendingImport = null) }
    fun dismissError() = _uiState.update { it.copy(importError = null) }
    fun dismissResult() = _uiState.update { it.copy(importResult = null) }
}
