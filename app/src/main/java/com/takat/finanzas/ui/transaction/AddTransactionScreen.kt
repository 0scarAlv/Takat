package com.takat.finanzas.ui.transaction

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.ui.components.AddCategoryDialog
import com.takat.finanzas.ui.components.CategoryPicker
import com.takat.finanzas.ui.components.FixedExpensePicker
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    preselectedAccountId: Long?,
    preselectedFixedExpenseId: Long? = null,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: AddTransactionViewModel = viewModel(
        factory = LambdaViewModelFactory { AddTransactionViewModel(repository, preselectedAccountId, preselectedFixedExpenseId) }
    )
    val uiState by viewModel.uiState.collectAsState()
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

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
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = repository.readFromUri(context.contentResolver, uri)
                val mime = context.contentResolver.getType(uri)
                val type = if (mime == "application/pdf") AttachmentType.PDF else AttachmentType.JSON
                val name = queryFileName(context, uri) ?: if (type == AttachmentType.PDF) "Comprobante.pdf" else "Comprobante.json"
                viewModel.onAttachmentPicked(PendingAttachment(type, bytes, name))
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = repository.readFromUri(context.contentResolver, uri)
                val name = queryFileName(context, uri) ?: "Imagen.jpg"
                viewModel.onAttachmentPicked(PendingAttachment(AttachmentType.IMAGE, bytes, name))
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

    val selectedAccount = uiState.accounts.find { it.id == uiState.accountId }
    val relevantKind = if (uiState.isExpense) CategoryKind.EXPENSE else CategoryKind.INCOME
    val filteredCategories = uiState.categories.filter { it.kind == relevantKind || it.kind == CategoryKind.BOTH }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo movimiento") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.isExpense,
                    onClick = { viewModel.onTypeChange(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Gasto") }
                SegmentedButton(
                    selected = !uiState.isExpense,
                    onClick = { viewModel.onTypeChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Ingreso") }
            }

            if (uiState.pendingFixedExpenses.isNotEmpty()) {
                Column {
                    Text("¿Es un gasto fijo?", style = MaterialTheme.typography.labelLarge)
                    FixedExpensePicker(
                        pending = uiState.pendingFixedExpenses,
                        selectedId = uiState.selectedFixedExpenseId,
                        onToggle = viewModel::onFixedExpenseToggle,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cuenta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
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
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onAccountChange(account.id)
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column {
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                CategoryPicker(
                    categories = filteredCategories,
                    selectedId = uiState.categoryId,
                    onSelect = viewModel::onCategoryChange,
                    onAddNew = { showAddCategory = true },
                    modifier = Modifier.padding(top = 8.dp)
                )
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

            Column {
                Text("Comprobante (opcional)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onTakePhotoClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Foto")
                    }
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Galería")
                    }
                    OutlinedButton(
                        onClick = { fileLauncher.launch(arrayOf("application/pdf", "application/json")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Archivo")
                    }
                }
                uiState.pendingAttachment?.let { attachment ->
                    AssistChip(
                        onClick = {},
                        modifier = Modifier.padding(top = 8.dp),
                        leadingIcon = {
                            Icon(
                                if (attachment.type == AttachmentType.IMAGE) Icons.Filled.PhotoCamera else Icons.Filled.Description,
                                contentDescription = null
                            )
                        },
                        label = { Text(attachment.label) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::clearPendingAttachment) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar adjunto")
                            }
                        }
                    )
                }
            }

            if (uiState.error != null) {
                Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onConfirm = { name, emoji ->
                viewModel.addCategory(name, emoji)
                showAddCategory = false
            }
        )
    }
}

private fun queryFileName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
