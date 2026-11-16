package com.takat.finanzas.ui.transaction

import com.takat.finanzas.ui.tutorial.TutorialAnchors
import com.takat.finanzas.ui.tutorial.tutorialAnchor
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.ui.components.AccountColorDot
import com.takat.finanzas.ui.components.BalanceAfterText
import com.takat.finanzas.ui.components.AddCategoryDialog
import com.takat.finanzas.ui.components.CategoryField
import com.takat.finanzas.ui.components.FixedExpensePicker
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.util.toDisplayDate
import kotlinx.coroutines.launch
import java.io.File
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.ui.components.FitLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    preselectedAccountId: Long?,
    preselectedFixedExpenseId: Long? = null,
    editTransactionId: Long? = null,
    initialShareUris: List<Uri> = emptyList(),
    onShareUrisConsumed: () -> Unit = {},
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: AddTransactionViewModel = viewModel(
        factory = LambdaViewModelFactory {
            AddTransactionViewModel(repository, preselectedAccountId, preselectedFixedExpenseId, editTransactionId)
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var fixedExpanded by rememberSaveable { mutableStateOf(preselectedFixedExpenseId != null) }
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

    LaunchedEffect(initialShareUris) {
        if (initialShareUris.isNotEmpty()) {
            initialShareUris.forEach { uri ->
                val bytes = repository.readFromUri(context.contentResolver, uri)
                val mime = context.contentResolver.getType(uri)
                val type = when {
                    mime?.startsWith("image/") == true -> AttachmentType.IMAGE
                    mime == "application/pdf" -> AttachmentType.PDF
                    else -> AttachmentType.JSON
                }
                val name = queryFileName(context, uri) ?: when (type) {
                    AttachmentType.IMAGE -> "Imagen.jpg"
                    AttachmentType.PDF -> "Comprobante.pdf"
                    AttachmentType.JSON -> "Comprobante.json"
                }
                viewModel.onAttachmentPicked(PendingAttachment(type, bytes, name))
            }
            onShareUrisConsumed()
        }
    }

    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCaptureFile
        pendingCaptureFile = null
        if (success && file != null) {
            val bytes = file.readBytes()
            file.delete()
            viewModel.onAttachmentPicked(PendingAttachment(AttachmentType.IMAGE, bytes, "Foto"))
        } else {
            file?.delete()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val (file, uri) = repository.createCaptureFile()
            pendingCaptureFile = file
            cameraLauncher.launch(uri)
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    val bytes = repository.readFromUri(context.contentResolver, uri)
                    val mime = context.contentResolver.getType(uri)
                    val type = if (mime == "application/pdf") AttachmentType.PDF else AttachmentType.JSON
                    val name = queryFileName(context, uri) ?: if (type == AttachmentType.PDF) "Comprobante.pdf" else "Comprobante.json"
                    viewModel.onAttachmentPicked(PendingAttachment(type, bytes, name))
                }
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                uris.forEach { uri ->
                    val bytes = repository.readFromUri(context.contentResolver, uri)
                    val name = queryFileName(context, uri) ?: "Imagen.jpg"
                    viewModel.onAttachmentPicked(PendingAttachment(AttachmentType.IMAGE, bytes, name))
                }
            }
        }
    }
    val onTakePhotoClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val (file, uri) = repository.createCaptureFile()
            pendingCaptureFile = file
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val selectedAccount = uiState.accounts.find { it.account.id == uiState.accountId }
    val relevantKind = if (uiState.isExpense) CategoryKind.EXPENSE else CategoryKind.INCOME
    val filteredCategories = uiState.categories.filter { it.kind == relevantKind || it.kind == CategoryKind.BOTH }
    val amountCents = uiState.amountText.parseAmountToCents()
    // When editing, the stored balance already includes the original transaction: back it out first.
    val editAdjustment = if (uiState.isEditing && uiState.originalAccountId == uiState.accountId) -uiState.originalAmountCents else 0L
    val baseBalance = selectedAccount?.let { it.balanceCents + editAdjustment }
    val balanceAfter = if (baseBalance != null && amountCents != null && amountCents != 0L) {
        baseBalance + if (uiState.isExpense) -amountCents else amountCents
    } else null
    val overBalanceWarning = selectedAccount?.let { account ->
        val creditAvailable = account.creditAvailableCents?.let { it + editAdjustment }
        val available = account.balanceCents + editAdjustment
        when {
            !uiState.isExpense || amountCents == null -> null
            !account.account.isDebt && amountCents > available ->
                "Esa cuenta solo tiene ${available.centsToDisplay()} disponibles"
            account.account.isDebt && creditAvailable != null && amountCents > creditAvailable ->
                if (creditAvailable > 0) "Con esto pasás el límite de la tarjeta: te quedan ${creditAvailable.centsToDisplay()}"
                else "Esta tarjeta ya pasó su límite"
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar movimiento" else "Nuevo movimiento") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.TX_TYPE)) {
                SegmentedButton(
                    selected = uiState.isExpense,
                    onClick = { viewModel.onTypeChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {}
                ) { FitLabel("Gasto") }
                SegmentedButton(
                    selected = !uiState.isExpense,
                    onClick = { viewModel.onTypeChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {}
                ) { FitLabel("Ingreso") }
            }

            // Monto: el dato principal, grande y con el color del tipo de movimiento.
            val amountColor = if (uiState.isExpense) MaterialTheme.financeColors.negative else MaterialTheme.financeColors.positive
            Column {
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Monto") },
                    prefix = { Text(if (uiState.isExpense) "−" else "+", color = amountColor) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = amountColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.TX_AMOUNT),
                    singleLine = true
                )
                if (balanceAfter != null) {
                    BalanceAfterText(balanceAfter, prefix = "Quedará en ${selectedAccount?.account?.name}")
                }
                if (overBalanceWarning != null) {
                    Text(
                        overBalanceWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financeColors.warning,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // Cuenta y fecha comparten fila.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it },
                    modifier = Modifier.weight(1.2f).tutorialAnchor(TutorialAnchors.TX_ACCOUNT)
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.account?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta") },
                        leadingIcon = {
                            selectedAccount?.let { AccountColorDot(it.account.colorArgb) }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                        supportingText = {
                            selectedAccount?.let { Text("Saldo ${it.balanceCents.centsToDisplay()}", maxLines = 1) }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        uiState.accounts.forEach { account ->
                            DropdownMenuItem(
                                leadingIcon = { AccountColorDot(account.account.colorArgb) },
                                text = { Text(account.account.name) },
                                trailingIcon = {
                                    Text(
                                        account.balanceCents.centsToDisplay(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    viewModel.onAccountChange(account.account.id)
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.dateMillis.toDisplayDate(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // El campo es de solo lectura: una capa encima abre el calendario al tocar cualquier parte.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClickLabel = "Elegir fecha") { showDatePicker = true }
                    )
                }
            }

            CategoryField(
                categories = filteredCategories,
                selectedId = uiState.categoryId,
                onSelect = viewModel::onCategoryChange,
                onAddNew = { showAddCategory = true },
                modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.TX_CATEGORY)
            )

            if (!uiState.isEditing && uiState.pendingFixedExpenses.isNotEmpty()) {
                val selectedFixed = uiState.pendingFixedExpenses.find { it.fixedExpense.id == uiState.selectedFixedExpenseId }
                val pendingCount = uiState.pendingFixedExpenses.size
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fixedExpanded = !fixedExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿Es un gasto fijo?", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        if (!fixedExpanded) {
                            Text(
                                selectedFixed?.fixedExpense?.name
                                    ?: if (pendingCount == 1) "1 pendiente" else "$pendingCount pendientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedFixed != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Icon(
                            if (fixedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (fixedExpanded) "Ocultar gastos fijos" else "Mostrar gastos fijos"
                        )
                    }
                    AnimatedVisibility(visible = fixedExpanded) {
                        FixedExpensePicker(
                            pending = uiState.pendingFixedExpenses,
                            selectedId = uiState.selectedFixedExpenseId,
                            onToggle = viewModel::onFixedExpenseToggle,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Nota (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(noteBringIntoViewRequester)
                    .onFocusEvent {
                        if (it.isFocused) {
                            coroutineScope.launch { noteBringIntoViewRequester.bringIntoView() }
                        }
                    },
                singleLine = true
            )

            if (!uiState.isEditing) Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Comprobante (opcional)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onTakePhotoClick) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Tomar foto")
                    }
                    IconButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Elegir de la galería")
                    }
                    IconButton(onClick = { fileLauncher.launch(arrayOf("application/pdf", "application/json")) }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Adjuntar archivo")
                    }
                }
                if (uiState.pendingAttachments.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.pendingAttachments.forEach { attachment ->
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(
                                        if (attachment.type == AttachmentType.IMAGE) Icons.Filled.PhotoCamera else Icons.Filled.Description,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(attachment.label) },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Quitar adjunto")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.TX_SAVE)) {
                Text("Guardar")
            }
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            showSalaryOption = !uiState.isExpense,
            onDismiss = { showAddCategory = false },
            onConfirm = { name, emoji, isSalary ->
                viewModel.addCategory(name, emoji, isSalary)
                showAddCategory = false
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
