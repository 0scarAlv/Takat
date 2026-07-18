package com.takat.finanzas.ui.stats

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpensesScreen(
    categoryId: Long?,
    fromMillis: Long,
    toMillis: Long,
    onBack: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: CategoryExpensesViewModel = viewModel(
        factory = LambdaViewModelFactory { CategoryExpensesViewModel(repository, categoryId, fromMillis, toMillis) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedMovement by remember { mutableStateOf<Movement.TransactionMovement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.categoryLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total gastado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    uiState.totalCents.centsToDisplay(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.movements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin gastos en este período.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(uiState.movements, key = { it.transaction.id }) { movement ->
                        MovementRow(
                            movement = movement,
                            currentAccountId = null,
                            onClick = { selectedMovement = movement }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
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
                    repository.deleteTransaction(movement.transaction)
                    selectedMovement = null
                }
            }
        )
    }
}
