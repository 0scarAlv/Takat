package com.takat.finanzas.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.data.entity.ThemeMode
import com.takat.finanzas.notifications.FixedExpenseReminderWorker
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenFixedExpenses: () -> Unit) {
    val repository = rememberRepository()
    val viewModel: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.use { viewModel.exportBackup(it) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) viewModel.parseForPreview(bytes)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            Text(
                "Elegí el tema de la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.onThemeModeChange(ThemeMode.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(0, 3)
                ) { Text("Claro") }
                SegmentedButton(
                    selected = uiState.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.onThemeModeChange(ThemeMode.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(1, 3)
                ) { Text("Oscuro") }
                SegmentedButton(
                    selected = uiState.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.onThemeModeChange(ThemeMode.SYSTEM) },
                    shape = SegmentedButtonDefaults.itemShape(2, 3)
                ) { Text("Sistema") }
            }

            Spacer(Modifier.height(32.dp))
            Text("Gastos fijos", style = MaterialTheme.typography.titleMedium)
            Text(
                "Gestioná tus gastos recurrentes: alquiler, servicios, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onOpenFixedExpenses, modifier = Modifier.fillMaxWidth()) {
                Text("Gestionar gastos fijos")
            }

            Spacer(Modifier.height(32.dp))
            Text("Datos", style = MaterialTheme.typography.titleMedium)
            Text(
                "Exportá tus cuentas, movimientos y comprobantes adjuntos en un solo archivo .zip, para revisarlos o pasarlos a otro teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { exportLauncher.launch("takat_backup_${LocalDate.now()}.zip") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Exportar datos") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/zip", "text/csv", "text/comma-separated-values", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar datos") }

            Spacer(Modifier.height(32.dp))
            Text("Acerca de", style = MaterialTheme.typography.titleMedium)
            var secretTapCount by remember { mutableIntStateOf(0) }
            var lastTapAt by remember { mutableLongStateOf(0L) }
            Text(
                "Versión ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    val now = System.currentTimeMillis()
                    secretTapCount = if (now - lastTapAt > 1500) 1 else secretTapCount + 1
                    lastTapAt = now
                    if (secretTapCount >= 7) {
                        secretTapCount = 0
                        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<FixedExpenseReminderWorker>().build())
                        Toast.makeText(context, "Revisando gastos fijos pendientes de avisar…", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Text(
                "Derechos reservados Oscar Alvarado 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    uiState.importError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("No se pudo leer el archivo") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("Cerrar") } }
        )
    }

    uiState.pendingImport?.let { parsed ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPreview,
            title = { Text("¿Importar estos datos?") },
            text = {
                val attachmentCount = parsed.transactions.sumOf { it.attachmentEntries.size }
                Text(
                    "Se van a agregar ${parsed.accounts.size} cuentas, ${parsed.categories.size} categorías, " +
                        "${parsed.transactions.size} movimientos y ${parsed.transfers.size} transferencias" +
                        (if (attachmentCount > 0) " (con $attachmentCount comprobantes adjuntos)" else "") + ".\n\n" +
                        "Las cuentas y categorías que ya existan con el mismo nombre se reutilizan, pero los " +
                        "movimientos siempre se agregan como nuevos — si ya importaste este archivo antes, se " +
                        "van a duplicar."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPreview) { Text("Cancelar") }
            }
        )
    }

    uiState.importResult?.let { result ->
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("Importación completa") },
            text = {
                Text(
                    "Se agregaron ${result.accountsAdded} cuentas, ${result.categoriesAdded} categorías, " +
                        "${result.transactionsAdded} movimientos, ${result.transfersAdded} transferencias y " +
                        "${result.attachmentsAdded} comprobantes." +
                        if (result.skipped > 0) {
                            "\n\n${result.skipped} filas se saltearon por referenciar una cuenta que no se pudo resolver."
                        } else {
                            ""
                        }
                )
            },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text("Cerrar") } }
        )
    }
}
