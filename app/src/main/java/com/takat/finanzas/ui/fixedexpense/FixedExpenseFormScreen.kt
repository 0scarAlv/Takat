package com.takat.finanzas.ui.fixedexpense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.ui.components.AddCategoryDialog
import com.takat.finanzas.ui.components.CategoryPicker
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpenseFormScreen(
    fixedExpenseId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: FixedExpenseFormViewModel = viewModel(
        factory = LambdaViewModelFactory { FixedExpenseFormViewModel(repository, fixedExpenseId) }
    )
    val uiState by viewModel.uiState.collectAsState()
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

    val selectedAccount = uiState.accounts.find { it.id == uiState.accountId }
    val filteredCategories = uiState.categories.filter { it.kind == CategoryKind.EXPENSE || it.kind == CategoryKind.BOTH }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Editar gasto fijo" else "Nuevo gasto fijo") },
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
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            Column {
                Text("Categoría (opcional)", style = MaterialTheme.typography.labelLarge)
                CategoryPicker(
                    categories = filteredCategories,
                    selectedId = uiState.categoryId,
                    onSelect = viewModel::onCategoryChange,
                    onAddNew = { showAddCategory = true },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column {
                Text("Frecuencia", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SegmentedButton(
                        selected = uiState.frequency == FixedExpenseFrequency.MENSUAL,
                        onClick = { viewModel.onFrequencyChange(FixedExpenseFrequency.MENSUAL) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Mensual") }
                    SegmentedButton(
                        selected = uiState.frequency == FixedExpenseFrequency.QUINCENAL,
                        onClick = { viewModel.onFrequencyChange(FixedExpenseFrequency.QUINCENAL) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Quincenal") }
                }
            }

            if (uiState.frequency == FixedExpenseFrequency.MENSUAL) {
                var dayText by remember(uiState.dayOfMonth) { mutableStateOf(uiState.dayOfMonth.toString()) }
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { text ->
                        dayText = text
                        val day = text.toIntOrNull()
                        if (day != null && day in 1..31) viewModel.onDayOfMonthChange(day)
                    },
                    label = { Text("Día de pago (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Mostrar únicamente en la quincena correspondiente", modifier = Modifier.weight(1f))
                        Switch(checked = uiState.quincenaOnly, onCheckedChange = viewModel::onQuincenaOnlyChange)
                    }
                    Text(
                        "Si te pagan quincenal, activalo para que no aparezca como pendiente antes de la quincena " +
                            "en que se cobra (día 1 si el día de pago es 1-15, día 16 si es 16-31). Si te pagan " +
                            "mensual, desactivalo para verlo reflejado desde el día 1 del mes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Se avisa el día 1 (primera quincena) y el día 16 (segunda quincena).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Notificarme", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.notifyEnabled,
                    onCheckedChange = { checked ->
                        viewModel.onNotifyEnabledChange(checked)
                        val needsPermission = checked &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Activo", modifier = Modifier.weight(1f))
                Switch(checked = uiState.enabled, onCheckedChange = viewModel::onEnabledChange)
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
