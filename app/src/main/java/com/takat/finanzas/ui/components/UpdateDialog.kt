package com.takat.finanzas.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.network.UpdateChecker
import com.takat.finanzas.network.UpdateInfo
import com.takat.finanzas.network.UpdateInstaller
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.launch

/**
 * Checks GitHub once per app open (after [WhatsNewGate] has settled, so the two dialogs never
 * stack) and offers [UpdateAvailableDialog] if a newer signed release exists. Silent no-op
 * otherwise — see UpdateChecker for why this never surfaces an error to the user.
 */
@Composable
fun UpdateCheckGate() {
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
    }

    val info = updateInfo
    if (info != null && !dismissed) {
        UpdateAvailableDialog(info = info, onDismiss = { dismissed = true })
    }
}

/** Also used from Ajustes for the manual "Buscar actualizaciones" button. */
@Composable
fun UpdateAvailableDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Comes back from system Settings after the user (hopefully) allowed the source; they
        // just need to tap "Descargar" again — re-checking here would need another round trip.
    }

    fun startDownload() {
        downloading = true
        error = null
        scope.launch {
            try {
                val file = UpdateInstaller.downloadApk(context, info.downloadUrl, info.assetName) { percent ->
                    progress = percent
                }
                UpdateInstaller.installApk(context, file)
                onDismiss()
            } catch (e: Exception) {
                DebugLog.log("UpdateAvailableDialog: download failed: ${e.message}")
                error = "No se pudo descargar la actualización. Probá de nuevo más tarde."
                downloading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("Actualización disponible") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("Versión ${info.versionName} lista para descargar.", style = MaterialTheme.typography.bodyMedium)
                info.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(12.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("$progress%", style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    if (UpdateInstaller.canRequestInstallPackages(context)) {
                        startDownload()
                    } else {
                        Toast.makeText(
                            context,
                            "Activá \"Instalar apps desconocidas\" para Takat y volvé a tocar Descargar",
                            Toast.LENGTH_LONG
                        ).show()
                        installPermissionLauncher.launch(UpdateInstaller.installPermissionSettingsIntent(context))
                    }
                }
            ) { Text(if (downloading) "Descargando…" else "Descargar") }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = onDismiss) { Text("Ahora no") }
        }
    )
}
