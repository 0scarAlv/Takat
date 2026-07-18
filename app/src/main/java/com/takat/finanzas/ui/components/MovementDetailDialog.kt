package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate

@Composable
fun MovementDetailDialog(
    movement: Movement,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("¿Eliminar este movimiento?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del movimiento") },
        text = {
            Column {
                when (movement) {
                    is Movement.TransactionMovement -> {
                        val tx = movement.transaction
                        DetailRow("Tipo", if (tx.amountCents >= 0) "Ingreso" else "Gasto")
                        DetailRow("Monto", tx.amountCents.centsToDisplay(showSign = true))
                        DetailRow("Cuenta", movement.account?.name ?: "Cuenta eliminada")
                        DetailRow("Categoría", movement.category?.let { "${it.emoji} ${it.name}" } ?: "Sin categoría")
                        DetailRow("Fecha", tx.date.toDisplayDate())
                        DetailRow("Nota", tx.note?.takeIf { it.isNotBlank() } ?: "—")
                    }

                    is Movement.TransferMovement -> {
                        val transfer = movement.transfer
                        DetailRow("Tipo", "Transferencia")
                        DetailRow("Monto", transfer.amountCents.centsToDisplay())
                        DetailRow("Desde", movement.fromAccount?.name ?: "Cuenta eliminada")
                        DetailRow("Hacia", movement.toAccount?.name ?: "Cuenta eliminada")
                        DetailRow("Motivo", movement.category?.let { "${it.emoji} ${it.name}" } ?: "Sin motivo")
                        DetailRow("Fecha", transfer.date.toDisplayDate())
                        DetailRow("Nota", transfer.note?.takeIf { it.isNotBlank() } ?: "—")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmingDelete = true }) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
    }
}
