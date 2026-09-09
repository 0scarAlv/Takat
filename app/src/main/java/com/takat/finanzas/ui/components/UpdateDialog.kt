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
import androidx.compose.runtime.collectAsState
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
import com.takat.finanzas.backup.BackupOutcome
import com.takat.finanzas.backup.runBackupNow
import com.takat.finanzas.network.UpdateInfo
import com.takat.finanzas.network.UpdateInstaller
import com.takat.finanzas.network.UpdateState
import com.takat.finanzas.ui.theme.AmberAccent
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.launch

/**
 * Checks GitHub once per app open (after [WhatsNewGate] has settled, so the two dialogs never
 * stack) and offers [UpdateAvailableDialog] if a newer signed release exists. Silent no-op
 * otherwise — see UpdateChecker for why this never surfaces an error to the user. The result is
 * shared via [UpdateState], which also drives the persistent "Actualizar" button in HomeScreen.
 */
@Composable
fun UpdateCheckGate() {
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UpdateState.checkOnce(BuildConfig.VERSION_NAME)
    }

    val info = UpdateState.available.value
    if (info != null && !dismissed) {
        UpdateAvailableDialog(info = info, onDismiss = { dismissed = true })
    }
}

/** Also used from Ajustes for the manual "Buscar actualizaciones" button. */
@Composable
fun UpdateAvailableDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = rememberRepository()
    val scope = rememberCoroutineScope()
    val appSettings by repository.appSettings().collectAsState(initial = null)
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var backupWarning by remember { mutableStateOf<String?>(null) }
    var runningBackup by remember { mutableStateOf(false) }
    var showBackupPrompt by remember { mutableStateOf(false) }
    var showBackupLocationInfo by remember { mutableStateOf(false) }

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

    /** Backs up first if auto-backup is on; otherwise asks whether to set it up before continuing. */
    fun startUpdateFlow() {
        if (appSettings?.backupFolderUri != null) {
            scope.launch {
                runningBackup = true
                val outcome = runBackupNow(context, repository)
                runningBackup = false
                if (outcome !is BackupOutcome.Success) {
                    backupWarning = "No se pudo hacer el respaldo automático antes de actualizar, pero la actualización va a seguir."
                }
                startDownload()
            }
        } else {
            showBackupPrompt = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!downloading && !runningBackup) onDismiss() },
        title = { Text("Actualización disponible") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text("Versión ${info.versionName} lista para descargar.", style = MaterialTheme.typography.bodyMedium)
                info.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(12.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (runningBackup) {
                    Spacer(Modifier.height(16.dp))
                    Text("Haciendo respaldo antes de actualizar…", style = MaterialTheme.typography.bodySmall)
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("$progress%", style = MaterialTheme.typography.bodySmall)
                }
                backupWarning?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AmberAccent)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading && !runningBackup,
                onClick = {
                    if (UpdateInstaller.canRequestInstallPackages(context)) {
                        startUpdateFlow()
                    } else {
                        Toast.makeText(
                            context,
                            "Activá \"Instalar apps desconocidas\" para Takat y volvé a tocar Descargar",
                            Toast.LENGTH_LONG
                        ).show()
                        installPermissionLauncher.launch(UpdateInstaller.installPermissionSettingsIntent(context))
                    }
                }
            ) { Text(if (downloading) "Descargando…" else if (runningBackup) "Un momento…" else "Descargar") }
        },
        dismissButton = {
            TextButton(enabled = !downloading && !runningBackup, onClick = onDismiss) { Text("Ahora no") }
        }
    )

    if (showBackupPrompt) {
        AlertDialog(
            onDismissRequest = { showBackupPrompt = false },
            title = { Text("¿Activar respaldo automático antes de actualizar?") },
            text = {
                Text(
                    "Todavía no tenés un respaldo automático configurado. Si algo sale mal con la actualización, no vas a tener una copia reciente de tus datos a mano."
                )
            },
            confirmButton = {
                TextButton(onClick = { showBackupPrompt = false; showBackupLocationInfo = true }) {
                    Text("Sí, quiero activarlo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPrompt = false; startDownload() }) {
                    Text("No, continuar igual")
                }
            }
        )
    }

    if (showBackupLocationInfo) {
        AlertDialog(
            onDismissRequest = { showBackupLocationInfo = false; onDismiss() },
            title = { Text("Dónde activarlo") },
            text = {
                Text("Andá a Ajustes → Respaldo automático → \"Elegir carpeta de respaldo\". Después volvé a tocar Actualizar.")
            },
            confirmButton = {
                TextButton(onClick = { showBackupLocationInfo = false; onDismiss() }) { Text("Entendido") }
            }
        )
    }
}
