package com.takat.finanzas.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repository = rememberRepository()
    val viewModel: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.exportCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
                if (text != null) viewModel.parseForPreview(text)
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
            Text("Datos", style = MaterialTheme.typography.titleMedium)
            Text(
                "Exportá tus cuentas y movimientos a un CSV para revisarlos en Excel o para pasarlos a otro teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { exportLauncher.launch("takat_backup_${LocalDate.now()}.csv") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Exportar datos") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar datos") }

            Spacer(Modifier.height(32.dp))
            Text("Acerca de", style = MaterialTheme.typography.titleMedium)
            Text(
                "Versión ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
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
                Text(
                    "Se van a agregar ${parsed.accounts.size} cuentas, ${parsed.categories.size} categorías, " +
                        "${parsed.transactions.size} movimientos y ${parsed.transfers.size} transferencias.\n\n" +
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
                        "${result.transactionsAdded} movimientos y ${result.transfersAdded} transferencias." +
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
