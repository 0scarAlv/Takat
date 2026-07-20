package com.takat.finanzas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.csv.BackupZip
import com.takat.finanzas.data.csv.ParsedBackup
import com.takat.finanzas.data.model.ImportResult
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

data class SettingsUiState(
    val pendingImport: ParsedBackup? = null,
    val pendingAttachmentFiles: Map<String, ByteArray> = emptyMap(),
    val importError: String? = null,
    val importResult: ImportResult? = null
)

class SettingsViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    suspend fun exportBackup(output: OutputStream) = repository.exportBackup(output)

    fun parseForPreview(fileBytes: ByteArray) {
        val (csv, attachmentFiles) = BackupZip.read(fileBytes)
        val parsed = try {
            BackupCsv.decode(csv)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(importError = e.message ?: "Formato de respaldo no reconocido") }
            return
        }
        _uiState.update { it.copy(pendingImport = parsed, pendingAttachmentFiles = attachmentFiles, importError = null) }
    }

    fun confirmImport() {
        val parsed = _uiState.value.pendingImport ?: return
        val attachmentFiles = _uiState.value.pendingAttachmentFiles
        viewModelScope.launch {
            val result = repository.commitImport(parsed, attachmentFiles)
            _uiState.update { it.copy(pendingImport = null, pendingAttachmentFiles = emptyMap(), importResult = result) }
        }
    }

    fun dismissPreview() = _uiState.update { it.copy(pendingImport = null, pendingAttachmentFiles = emptyMap()) }
    fun dismissError() = _uiState.update { it.copy(importError = null) }
    fun dismissResult() = _uiState.update { it.copy(importResult = null) }
}
