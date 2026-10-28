package com.takat.finanzas.ui.settings

import com.takat.finanzas.ui.tutorial.TutorialAnchors
import com.takat.finanzas.ui.tutorial.tutorialAnchor
import com.takat.finanzas.ui.tutorial.LocalTutorialController
import com.takat.finanzas.ui.tutorial.TutorialCatalog
import kotlinx.coroutines.flow.map
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.backup.DailyBackupWorker
import com.takat.finanzas.data.csv.ImportMode
import com.takat.finanzas.data.entity.ThemeMode
import com.takat.finanzas.data.entity.UiStyle
import com.takat.finanzas.network.LocalApiServer
import com.takat.finanzas.network.LocalIpAddress
import com.takat.finanzas.network.MdnsAdvertiser
import com.takat.finanzas.network.PcAccessForegroundService
import com.takat.finanzas.network.UpdateChecker
import com.takat.finanzas.network.UpdateInfo
import com.takat.finanzas.network.UpdateState
import com.takat.finanzas.notifications.FixedExpenseReminderWorker
import com.takat.finanzas.ui.components.IconBadge
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.ui.components.UpdateAvailableDialog
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberPairingManager
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import com.takat.finanzas.ui.components.FitLabel

/**
 * Settings are a short main list of grouped one-line rows; the three heavy areas (backup, PC access,
 * debugging) open as sub-pages inside this same screen instead of being stacked into one long scroll.
 */
private enum class SettingsPage(val title: String) {
    MAIN("Ajustes"),
    BACKUP("Respaldo y restauración"),
    PC_ACCESS("Acceso desde PC"),
    GUIDES("Guías"),
    DEBUG("Depuración")
}

private val dateTimeFormat: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPairScan: () -> Unit
) {
    val repository = rememberRepository()
    val pairingManager = rememberPairingManager()
    val viewModel: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(repository, pairingManager) })
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    var pcAccessEnabled by remember { mutableStateOf(PcAccessForegroundService.isRunning) }

    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }

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

    val backupFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onBackupFolderPicked(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.title) },
                navigationIcon = {
                    IconButton(onClick = { if (page == SettingsPage.MAIN) onBack() else page = SettingsPage.MAIN }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (page) {
                SettingsPage.MAIN -> MainPage(
                    uiState = uiState,
                    viewModel = viewModel,
                    pcAccessEnabled = pcAccessEnabled,
                    onOpenFixedExpenses = onOpenFixedExpenses,
                    onOpenCategories = onOpenCategories,
                    onOpenPage = { page = it }
                )

                SettingsPage.BACKUP -> {
                    SettingsGroup("Copia manual") {
                        SettingsRow(
                            icon = Icons.Filled.FileUpload,
                            title = "Exportar datos",
                            summary = "Un .zip con cuentas, movimientos y comprobantes",
                            onClick = { exportLauncher.launch("takat_backup_${LocalDate.now()}.zip") }
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Filled.FileDownload,
                            title = "Importar datos",
                            summary = "Restaurar desde un respaldo .zip",
                            onClick = { importLauncher.launch(arrayOf("application/zip", "text/csv", "text/comma-separated-values", "*/*")) }
                        )
                    }

                    SettingsGroup("Respaldo automático") {
                        val backupFolderUri = uiState.backupFolderUri
                        if (backupFolderUri != null) {
                            val folderName = remember(backupFolderUri) {
                                DocumentFile.fromTreeUri(context, Uri.parse(backupFolderUri))?.name
                            }
                            SettingsRow(
                                icon = Icons.Filled.Folder,
                                title = "Carpeta: ${folderName ?: "elegida"}",
                                summary = uiState.lastBackupEpochMillis?.let { "Último respaldo: ${dateTimeFormat.format(Date(it))}" }
                                    ?: "Todavía no se hizo ningún respaldo",
                                onClick = { backupFolderLauncher.launch(null) },
                                trailing = { Text("Cambiar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                            )
                            uiState.lastBackupError?.let { error ->
                                Text(
                                    "El último intento falló: $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp, bottom = 8.dp)
                                )
                            }
                            RowDivider()
                            SettingsRow(
                                icon = Icons.Filled.Save,
                                title = "Respaldar ahora",
                                onClick = {
                                    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<DailyBackupWorker>().build())
                                    Toast.makeText(context, "Generando respaldo…", Toast.LENGTH_SHORT).show()
                                }
                            )
                            RowDivider()
                            SettingsRow(
                                icon = Icons.Filled.FolderOff,
                                title = "Desactivar respaldo automático",
                                titleColor = MaterialTheme.colorScheme.error,
                                onClick = viewModel::clearBackupFolder
                            )
                        } else {
                            SettingsRow(
                                icon = Icons.Filled.Folder,
                                title = "Elegir carpeta de respaldo",
                                summary = "Apagado",
                                onClick = { backupFolderLauncher.launch(null) }
                            )
                        }
                    }
                    Footnote(
                        "El respaldo automático guarda una copia por día en la carpeta elegida (se conservan las últimas 30). " +
                            "Si la carpeta está solo en este teléfono, no te protege si perdés el teléfono: elegí una que se " +
                            "sincronice con la nube."
                    )
                }

                SettingsPage.PC_ACCESS -> {
                    SettingsGroup(null) {
                        SwitchRow(
                            icon = Icons.Filled.Computer,
                            title = "Permitir conexiones",
                            summary = if (pcAccessEnabled) "Activo en esta red wifi" else "Apagado",
                            checked = pcAccessEnabled,
                            onCheckedChange = { enabled ->
                                pcAccessEnabled = enabled
                                val serviceIntent = Intent(context, PcAccessForegroundService::class.java)
                                if (enabled) context.startForegroundService(serviceIntent) else context.stopService(serviceIntent)
                            }
                        )
                    }
                    if (pcAccessEnabled) {
                        SettingsGroup("Conexión") {
                            val ip = remember(pcAccessEnabled) { LocalIpAddress.current() }
                            if (ip != null) {
                                val url = "http://$ip:${LocalApiServer.DEFAULT_PORT}"
                                val clipboard = LocalClipboardManager.current
                                SettingsRow(
                                    icon = Icons.Filled.Wifi,
                                    title = url,
                                    summary = "Abrí esta dirección en la PC",
                                    onClick = {
                                        clipboard.setText(AnnotatedString(url))
                                        Toast.makeText(context, "Dirección copiada", Toast.LENGTH_SHORT).show()
                                    },
                                    trailing = { Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar dirección") }
                                )
                            } else {
                                SettingsRow(icon = Icons.Filled.Wifi, title = "Sin wifi", summary = "No se detectó una red wifi activa")
                            }
                            RowDivider()
                            SettingsRow(
                                icon = Icons.Filled.QrCodeScanner,
                                title = "Vincular nueva PC",
                                summary = "Escaneá el código que muestra la PC",
                                onClick = onOpenPairScan
                            )
                            OutlinedTextField(
                                value = uiState.pcAccessNickname ?: "",
                                onValueChange = viewModel::onPcAccessNicknameChange,
                                label = { Text("Apodo en la red") },
                                supportingText = { Text("Se anuncia como \"${MdnsAdvertiser.serviceNameFor(uiState.pcAccessNickname)}\"") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                    if (uiState.pairedDevices.isNotEmpty()) {
                        SettingsGroup("Dispositivos vinculados") {
                            uiState.pairedDevices.forEachIndexed { index, device ->
                                if (index > 0) RowDivider()
                                SettingsRow(
                                    icon = Icons.Filled.Computer,
                                    title = device.name,
                                    summary = "Último uso: ${dateTimeFormat.format(Date(device.lastUsedAt))}",
                                    trailing = {
                                        TextButton(onClick = { viewModel.revokeDevice(device.deviceToken) }) {
                                            Text("Revocar", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Footnote(
                        "La PC tiene que estar en la misma wifi. Los datos viajan cifrados de punta a punta, sin pasar por " +
                            "ningún servidor externo. La mayoría de navegadores en Windows necesitan la dirección numérica, " +
                            "no el nombre .local."
                    )
                }

                SettingsPage.GUIDES -> GuidesPage()

                SettingsPage.DEBUG -> {
                    var debugLoggingEnabled by remember { mutableStateOf(DebugLog.isEnabled) }
                    SettingsGroup(null) {
                        SwitchRow(
                            icon = Icons.Filled.BugReport,
                            title = "Grabar registro",
                            summary = if (debugLoggingEnabled) "Grabando" else "Apagado",
                            checked = debugLoggingEnabled,
                            onCheckedChange = {
                                debugLoggingEnabled = it
                                DebugLog.setEnabled(it)
                            }
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Filled.Share,
                            title = "Compartir registro",
                            onClick = {
                                val uri = DebugLog.prepareShareFile(context)
                                if (uri != null) {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir registro"))
                                } else {
                                    Toast.makeText(context, "Todavía no hay nada registrado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Filled.DeleteSweep,
                            title = "Borrar registro",
                            onClick = {
                                DebugLog.clear()
                                Toast.makeText(context, "Registro borrado", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Footnote(
                        "Si te pasa algo raro (por ejemplo, el widget que no se actualiza), activá el registro, esperá a que " +
                            "vuelva a pasar y compartilo. Se guarda solo en este teléfono hasta que lo compartís."
                    )
                }
            }
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
        var mode by remember { mutableStateOf(ImportMode.ONLY_MISSING) }
        AlertDialog(
            onDismissRequest = viewModel::dismissPreview,
            title = { Text("Restaurar copia de seguridad") },
            text = {
                val attachmentCount = parsed.transactions.sumOf { it.attachmentEntries.size }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "El archivo tiene ${parsed.accounts.size} cuentas, ${parsed.categories.size} categorías, " +
                            "${parsed.transactions.size} movimientos, ${parsed.transfers.size} transferencias y " +
                            "${parsed.fixedExpenses.size} gastos fijos" +
                            (if (attachmentCount > 0) " (con $attachmentCount comprobantes)" else "") + "."
                    )
                    Text("¿Qué hago con lo que ya tenés en la app?", style = MaterialTheme.typography.labelLarge)
                    ImportModeOption(
                        selected = mode == ImportMode.ONLY_MISSING,
                        title = "Solo agregar lo que falta (recomendado)",
                        description = "Cuentas, categorías y gastos fijos se reutilizan por nombre, y los movimientos " +
                            "idénticos (misma cuenta, fecha, monto, categoría y nota) no se vuelven a agregar.",
                        onClick = { mode = ImportMode.ONLY_MISSING }
                    )
                    ImportModeOption(
                        selected = mode == ImportMode.REPLACE_ALL,
                        title = "Reemplazar todo (en limpio)",
                        description = "Borra todas tus cuentas, categorías, movimientos, transferencias y gastos fijos, " +
                            "y deja solo lo que trae la copia. Si algo falla a mitad de camino, no se borra nada.",
                        onClick = { mode = ImportMode.REPLACE_ALL }
                    )
                    ImportModeOption(
                        selected = mode == ImportMode.DUPLICATE,
                        title = "Agregar todo aunque se duplique",
                        description = "Cuentas y categorías se reutilizan por nombre, pero todos los movimientos se " +
                            "agregan como nuevos aunque ya existan.",
                        onClick = { mode = ImportMode.DUPLICATE }
                    )
                    if (mode == ImportMode.REPLACE_ALL) {
                        Text(
                            "Ojo: lo que no esté en este archivo se pierde. Si no estás seguro, exportá una copia antes.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(mode) }) {
                    Text(
                        if (mode == ImportMode.REPLACE_ALL) "Borrar y restaurar" else "Restaurar",
                        color = if (mode == ImportMode.REPLACE_ALL) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
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
                        "${result.transactionsAdded} movimientos, ${result.transfersAdded} transferencias, " +
                        "${result.fixedExpensesAdded} pagos programados y ${result.attachmentsAdded} comprobantes." +
                        (if (result.duplicatesSkipped > 0) "\n\n${result.duplicatesSkipped} movimientos ya estaban y no se duplicaron." else "") +
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

@Composable
private fun MainPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    pcAccessEnabled: Boolean,
    onOpenFixedExpenses: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SettingsGroup("Apariencia") {
        ChoiceRow(
            icon = Icons.Filled.Palette,
            title = "Diseño",
            options = listOf("Nuevo", "Clásico"),
            selectedIndex = if (uiState.uiStyle == UiStyle.CLASSIC) 1 else 0,
            onSelect = { viewModel.onUiStyleChange(if (it == 1) UiStyle.CLASSIC else UiStyle.MODERN) }
        )
        RowDivider()
        ChoiceRow(
            icon = Icons.Filled.DarkMode,
            title = "Tema",
            options = listOf("Claro", "Oscuro", "Sistema"),
            selectedIndex = when (uiState.themeMode) {
                ThemeMode.LIGHT -> 0
                ThemeMode.DARK -> 1
                ThemeMode.SYSTEM -> 2
            },
            onSelect = { viewModel.onThemeModeChange(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[it]) }
        )
    }

    SettingsGroup("Finanzas") {
        SettingsRow(
            icon = Icons.Filled.EventRepeat,
            title = "Gastos fijos",
            summary = "Alquiler, servicios, cuotas y créditos",
            onClick = onOpenFixedExpenses,
            modifier = Modifier.tutorialAnchor(TutorialAnchors.SETTINGS_FIXED)
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Filled.Category,
            title = "Categorías",
            summary = "Nombres e íconos",
            onClick = onOpenCategories
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Filled.CreditCard,
            title = "Restar créditos del disponible",
            summary = if (uiState.includeFinancedInAvailable) "Se resta todo lo que falta pagar"
            else "Solo baja la cuota de cada período",
            checked = uiState.includeFinancedInAvailable,
            onCheckedChange = viewModel::onIncludeFinancedInAvailableChange
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Filled.Mood,
            title = "Mensajes sarcásticos",
            summary = "Comentarios cuando andás corto de plata",
            checked = uiState.sarcasticMessagesEnabled,
            onCheckedChange = viewModel::onSarcasticMessagesChange
        )
    }

    SettingsGroup("Datos") {
        SettingsRow(
            icon = Icons.Filled.Backup,
            title = "Respaldo y restauración",
            summary = when {
                uiState.backupFolderUri == null -> "Respaldo automático apagado"
                uiState.lastBackupError != null -> "El último respaldo falló"
                else -> uiState.lastBackupEpochMillis?.let { "Último respaldo: ${dateTimeFormat.format(Date(it))}" }
                    ?: "Respaldo automático activo"
            },
            summaryColor = if (uiState.lastBackupError != null && uiState.backupFolderUri != null) MaterialTheme.colorScheme.error else null,
            onClick = { onOpenPage(SettingsPage.BACKUP) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Filled.Computer,
            title = "Acceso desde PC",
            summary = buildString {
                append(if (pcAccessEnabled) "Activo" else "Apagado")
                val count = uiState.pairedDevices.size
                if (count > 0) append(" · $count ${if (count == 1) "PC vinculada" else "PCs vinculadas"}")
            },
            onClick = { onOpenPage(SettingsPage.PC_ACCESS) }
        )
    }

    SettingsGroup("Ayuda") {
        SettingsRow(
            icon = Icons.Filled.School,
            title = "Guías",
            summary = "Tutoriales paso a paso sobre la app",
            onClick = { onOpenPage(SettingsPage.GUIDES) }
        )
    }

    var checkingUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    SettingsGroup("Acerca de") {
        SettingsRow(
            icon = Icons.Filled.SystemUpdate,
            title = if (checkingUpdate) "Buscando…" else "Buscar actualizaciones",
            summary = "Se revisan contra los releases de GitHub",
            onClick = if (checkingUpdate) null else {
                {
                    checkingUpdate = true
                    scope.launch {
                        val info = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                        UpdateState.record(info)
                        checkingUpdate = false
                        if (info != null) {
                            manualUpdateInfo = info
                        } else {
                            Toast.makeText(context, "Ya tenés la última versión instalada.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Filled.BugReport,
            title = "Depuración",
            summary = "Registro para reportar problemas",
            onClick = { onOpenPage(SettingsPage.DEBUG) }
        )
    }
    manualUpdateInfo?.let { info ->
        UpdateAvailableDialog(info = info, onDismiss = { manualUpdateInfo = null })
    }

    // Seven quick taps on the version line force a fixed-expense reminder check (hidden testing aid).
    var secretTapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Takat ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
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

/** Every guide in [TutorialCatalog]; tapping one goes back to Home and runs it there. */
@Composable
private fun GuidesPage() {
    val controller = LocalTutorialController.current ?: return
    val progress by controller.progress.collectAsState()
    val repository = rememberRepository()
    val hasAccounts by remember { repository.accountsWithBalance().map { it.isNotEmpty() } }.collectAsState(initial = true)
    SettingsGroup(null) {
        TutorialCatalog.guides.forEachIndexed { index, guide ->
            if (index > 0) RowDivider()
            val done = progress[guide.id]?.completedAt != null
            val locked = guide.requiresAccount && !hasAccounts
            SettingsRow(
                icon = guide.icon,
                title = guide.title,
                summary = if (locked) "Primero creá una cuenta (guía \"Tus cuentas\")" else "${guide.summary} · ${guide.steps.size} pasos",
                onClick = if (locked) null else { { controller.start(guide) } },
                trailing = {
                    if (locked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Bloqueada", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (done) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Completada", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
    Footnote(
        "Las guías te acompañan sobre la app de verdad: lo que creás durante una guía (como una cuenta) queda " +
            "guardado. Podés saltarlas cuando quieras y repetirlas desde acá."
    )
}

/** A titled block of rows sharing one card. [title] null renders the card alone (sub-page lead group). */
@Composable
private fun SettingsGroup(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp, top = 8.dp)
                    .semantics { heading() }
            )
        }
        TakatCard(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun RowIcon(icon: ImageVector) {
    IconBadge(
        size = 36.dp,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/**
 * One line of settings: icon, title, optional one-line summary, and a trailing element (a chevron by default
 * when the row navigates somewhere).
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    titleColor: Color = Color.Unspecified,
    summaryColor: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = if (onClick != null) {
        { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            summary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = summaryColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

/** A row whose whole surface toggles the switch (not only the thumb), announced as one switch to TalkBack. */
@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Icon + title on one line, a full-width segmented picker under it. */
@Composable
private fun ChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {}
                ) { FitLabel(label) }
            }
        }
    }
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ImportModeOption(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
