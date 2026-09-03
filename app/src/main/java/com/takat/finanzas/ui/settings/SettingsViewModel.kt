package com.takat.finanzas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.csv.BackupZip
import com.takat.finanzas.data.csv.ParsedBackup
import com.takat.finanzas.data.entity.AppSettingsEntity
import com.takat.finanzas.data.entity.ThemeMode
import com.takat.finanzas.data.entity.TrustedDeviceEntity
import com.takat.finanzas.data.model.ImportResult
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.network.PairingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

data class SettingsUiState(
    val pendingImport: ParsedBackup? = null,
    val pendingAttachmentFiles: Map<String, ByteArray> = emptyMap(),
    val importError: String? = null,
    val importResult: ImportResult? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backupFolderUri: String? = null,
    val lastBackupEpochMillis: Long? = null,
    val lastBackupError: String? = null,
    val sarcasticMessagesEnabled: Boolean = true,
    val pairedDevices: List<TrustedDeviceEntity> = emptyList(),
    val pcAccessNickname: String? = null
)

class SettingsViewModel(
    private val repository: FinanceRepository,
    private val pairingManager: PairingManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        themeMode = settings?.themeMode ?: ThemeMode.SYSTEM,
                        backupFolderUri = settings?.backupFolderUri,
                        lastBackupEpochMillis = settings?.lastBackupEpochMillis,
                        lastBackupError = settings?.lastBackupError,
                        sarcasticMessagesEnabled = settings?.sarcasticMessagesEnabled ?: true,
                        pcAccessNickname = settings?.pcAccessNickname
                    )
                }
            }
        }
        viewModelScope.launch {
            pairingManager.devices().collect { devices ->
                _uiState.update { it.copy(pairedDevices = devices) }
            }
        }
    }

    fun revokeDevice(deviceToken: String) {
        viewModelScope.launch { pairingManager.revoke(deviceToken) }
    }

    fun onPcAccessNicknameChange(nickname: String) {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(pcAccessNickname = nickname.ifBlank { null }))
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(themeMode = mode))
        }
    }

    fun onBackupFolderPicked(uri: String) {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(backupFolderUri = uri, lastBackupError = null))
        }
    }

    fun clearBackupFolder() {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(backupFolderUri = null, lastBackupError = null))
        }
    }

    fun onSarcasticMessagesChange(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.appSettings().first() ?: AppSettingsEntity()
            repository.updateAppSettings(current.copy(sarcasticMessagesEnabled = enabled))
        }
    }

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
