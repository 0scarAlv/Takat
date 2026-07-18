package com.takat.finanzas.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.key
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: AccountDetailViewModel = viewModel(
        factory = LambdaViewModelFactory { AccountDetailViewModel(repository, accountId) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val accountWithBalance = uiState.accountWithBalance
    val scope = rememberCoroutineScope()
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(accountWithBalance?.account?.name ?: "Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar cuenta")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo movimiento")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Saldo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    (accountWithBalance?.balanceCents ?: 0).centsToDisplay(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if ((accountWithBalance?.balanceCents ?: 0) < 0) NegativeRed else MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.movements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin movimientos todavía.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(uiState.movements, key = { it.key }) { movement ->
                        MovementRow(
                            movement = movement,
                            currentAccountId = accountId,
                            onClick = { selectedMovement = movement }
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }

    selectedMovement?.let { movement ->
        MovementDetailDialog(
            movement = movement,
            onDismiss = { selectedMovement = null },
            onDelete = {
                scope.launch {
                    when (movement) {
                        is Movement.TransactionMovement -> repository.deleteTransaction(movement.transaction)
                        is Movement.TransferMovement -> repository.deleteTransfer(movement.transfer)
                    }
                    selectedMovement = null
                }
            }
        )
    }
}
