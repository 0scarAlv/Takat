package com.takat.finanzas.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.ui.components.AccountColorDot
import com.takat.finanzas.ui.components.AddCategoryDialog
import com.takat.finanzas.ui.components.CategoryPicker
import com.takat.finanzas.ui.theme.AmberAccent
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.parseAmountToCents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: AddTransferViewModel = viewModel(
        factory = LambdaViewModelFactory { AddTransferViewModel(repository) }
    )
    val uiState by viewModel.uiState.collectAsState()
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.saved) { if (uiState.saved) onDone() }

    val fromAccount = uiState.accounts.find { it.account.id == uiState.fromAccountId }
    val toAccount = uiState.accounts.find { it.account.id == uiState.toAccountId }
    val amountCents = uiState.amountText.parseAmountToCents()
    val overBalanceWarning = fromAccount?.let { account ->
        if (!account.account.isDebt && amountCents != null && amountCents > account.balanceCents) {
            "Esa cuenta solo tiene ${account.balanceCents.centsToDisplay()} disponibles"
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva transferencia") },
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
            ExposedDropdownMenuBox(
                expanded = fromMenuExpanded,
                onExpandedChange = { fromMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = fromAccount?.account?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Desde") },
                    leadingIcon = {
                        fromAccount?.let { AccountColorDot(it.account.colorArgb) }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = fromMenuExpanded,
                    onDismissRequest = { fromMenuExpanded = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            leadingIcon = { AccountColorDot(account.account.colorArgb) },
                            text = { Text(account.account.name) },
                            onClick = {
                                viewModel.onFromAccountChange(account.account.id)
                                fromMenuExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = toMenuExpanded,
                onExpandedChange = { toMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = toAccount?.account?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hacia") },
                    leadingIcon = {
                        toAccount?.let { AccountColorDot(it.account.colorArgb) }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = toMenuExpanded,
                    onDismissRequest = { toMenuExpanded = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            leadingIcon = { AccountColorDot(account.account.colorArgb) },
                            text = { Text(account.account.name) },
                            onClick = {
                                viewModel.onToAccountChange(account.account.id)
                                toMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Column {
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (overBalanceWarning != null) {
                    Text(
                        overBalanceWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberAccent,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Column {
                Text("Motivo (opcional)", style = MaterialTheme.typography.labelLarge)
                CategoryPicker(
                    categories = uiState.categories,
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

            if (uiState.error != null) {
                Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Transferir")
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
