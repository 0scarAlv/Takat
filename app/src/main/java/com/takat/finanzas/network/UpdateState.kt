package com.takat.finanzas.network

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * Process-wide holder for the once-per-launch GitHub Releases check, so the automatic dialog
 * ([com.takat.finanzas.ui.components.UpdateCheckGate], mounted once in MainActivity) and the
 * persistent green "Actualizar" button next to the Takat title in HomeScreen can both react to the
 * same result without hitting the network twice.
 */
object UpdateState {
    private val _available = mutableStateOf<UpdateInfo?>(null)
    val available: State<UpdateInfo?> = _available
    private var checked = false

    suspend fun checkOnce(currentVersionName: String) {
        if (checked) return
        checked = true
        _available.value = UpdateChecker.checkForUpdate(currentVersionName)
    }

    /** Used by the manual "Buscar actualizaciones" button in Ajustes, which runs its own check on demand. */
    fun record(info: UpdateInfo?) {
        checked = true
        _available.value = info
    }
}
