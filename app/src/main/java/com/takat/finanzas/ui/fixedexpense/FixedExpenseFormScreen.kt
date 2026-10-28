package com.takat.finanzas.ui.fixedexpense

import com.takat.finanzas.ui.tutorial.TutorialAnchors
import com.takat.finanzas.ui.tutorial.tutorialAnchor
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
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.ui.components.AddCategoryDialog
import com.takat.finanzas.ui.components.CategoryPicker
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.parseAmountToCents
import com.takat.finanzas.ui.components.FitLabel

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
    var showAddCategory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

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
                modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.FIXED_NAME),
                singleLine = true
            )

            Column(modifier = Modifier.tutorialAnchor(TutorialAnchors.FIXED_KIND)) {
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    val kinds = listOf(
                        FixedExpenseKind.NORMAL to "Normal",
                        FixedExpenseKind.DEUDA to "Cuotas",
                        FixedExpenseKind.CREDITO to "Crédito"
                    )
                    kinds.forEachIndexed { index, (kind, label) ->
                        SegmentedButton(
                            selected = uiState.kind == kind,
                            onClick = { viewModel.onKindChange(kind) },
                            shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                            icon = {}
                        ) { FitLabel(label) }
                    }
                }
            }

            if (uiState.kind == FixedExpenseKind.CREDITO) {
                Text(
                    "El monto de la compra se suma a la deuda de la cuenta de crédito, pero no a tus gastos: " +
                        "cada cuota se registra como gasto del mes en su categoría cuando la pagás.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccountDropdown(
                    label = "Cuenta de la deuda",
                    supportingText = "La tarjeta o cuenta de crédito donde queda registrado todo lo que debés de esta compra.",
                    accounts = uiState.accounts.sortedByDescending { it.isDebt },
                    selectedId = uiState.creditAccountId,
                    onSelect = viewModel::onCreditAccountChange
                )
                OutlinedTextField(
                    value = uiState.principalText,
                    onValueChange = viewModel::onPrincipalChange,
                    label = { Text("Monto de la compra") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.installmentsText,
                    onValueChange = viewModel::onInstallmentsChange,
                    label = { Text("Número de cuotas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.interestRateText,
                    onValueChange = viewModel::onInterestRateChange,
                    label = { Text("Tasa de interés anual % (vacío = 0)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.cuotaText,
                    onValueChange = viewModel::onCuotaChange,
                    label = { Text("Cuota por período") },
                    supportingText = {
                        Text(
                            uiState.suggestedCuotaCents?.let { "Sugerida: ${it.centsToDisplay()}. Si tu banco cobra otra (seguro, IVA, comisiones), poné la del estado de cuenta." }
                                ?: "Se calcula sola con el monto, las cuotas y la tasa."
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                val principal = uiState.principalText.parseAmountToCents()
                uiState.creditTotalToPayCents?.let { total ->
                    val interest = principal?.let { (total - it).coerceAtLeast(0) }
                    Text(
                        "Total a pagar: ${total.centsToDisplay()}" +
                            (interest?.let { " · Intereses: ${it.centsToDisplay()}" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AccountDropdown(
                    label = "Cuenta de pago",
                    supportingText = "De esta cuenta sale el dinero de cada cuota. Si la cuota te llega en el estado " +
                        "de cuenta de la tarjeta, dejá la misma tarjeta; si la pagás directo (efectivo, débito), elegí esa cuenta.",
                    accounts = uiState.accounts,
                    selectedId = uiState.accountId,
                    onSelect = viewModel::onAccountChange
                )
            } else if (uiState.isDebt) {
                Text(
                    "Para algo sencillo: una deuda o préstamo que pagás en partes iguales, sin tasa de interés ni " +
                        "tarjeta. Solo lleva la cuenta de cuánto te falta pagar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.totalDebtText,
                    onValueChange = viewModel::onTotalDebtChange,
                    label = { Text("Monto total de la deuda") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.installmentsText,
                    onValueChange = viewModel::onInstallmentsChange,
                    label = { Text("Número de cuotas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                uiState.debtInstallmentCents?.let { cuota ->
                    Text(
                        "Cuota: ${cuota.centsToDisplay()} por período. Si pagás de más en un período, la cuota " +
                            "no cambia; solo se te va a avisar cuánto te queda de deuda total.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.FIXED_AMOUNT),
                    singleLine = true
                )
            }

            if (uiState.kind != FixedExpenseKind.CREDITO) {
                AccountDropdown(
                    label = "Cuenta",
                    accounts = uiState.accounts,
                    selectedId = uiState.accountId,
                    onSelect = viewModel::onAccountChange,
                    modifier = Modifier.tutorialAnchor(TutorialAnchors.FIXED_ACCOUNT)
                )
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

            Column(modifier = Modifier.tutorialAnchor(TutorialAnchors.FIXED_FREQUENCY)) {
                Text("Frecuencia", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    SegmentedButton(
                        selected = uiState.frequency == FixedExpenseFrequency.MENSUAL,
                        onClick = { viewModel.onFrequencyChange(FixedExpenseFrequency.MENSUAL) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = {}
                    ) { FitLabel("Mensual") }
                    SegmentedButton(
                        selected = uiState.frequency == FixedExpenseFrequency.QUINCENAL,
                        onClick = { viewModel.onFrequencyChange(FixedExpenseFrequency.QUINCENAL) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = {}
                    ) { FitLabel("Quincenal") }
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

            Row(modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.FIXED_NOTIFY)) {
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

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().tutorialAnchor(TutorialAnchors.FIXED_SAVE)) {
                Text("Guardar")
            }
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            showSalaryOption = false,
            onDismiss = { showAddCategory = false },
            onConfirm = { name, emoji, _ ->
                viewModel.addCategory(name, emoji)
                showAddCategory = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    supportingText: String? = null,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = accounts.find { it.id == selectedId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelect(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
