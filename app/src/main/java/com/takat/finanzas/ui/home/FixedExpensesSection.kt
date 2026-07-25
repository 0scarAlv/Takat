package com.takat.finanzas.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.ui.theme.AmberAccent
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay

@Composable
fun FixedExpensesSection(
    onManageClick: () -> Unit,
    onPayClick: (fixedExpenseId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = rememberRepository()
    val viewModel: FixedExpensesSectionViewModel =
        viewModel(factory = LambdaViewModelFactory { FixedExpensesSectionViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.rows.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gastos fijos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (uiState.pendingTotalCents > 0) "${uiState.pendingTotalCents.centsToDisplay()} pendiente este período" else "Todo pagado este período",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.pendingTotalCents > 0) AmberAccent else PositiveGreen
                    )
                }
                IconButton(onClick = onManageClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Gestionar gastos fijos")
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Ocultar gastos fijos" else "Mostrar gastos fijos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                uiState.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .let { base -> if (row.paidMovement == null) base.clickable { onPayClick(row.fixedExpenseId) } else base },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(row.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                row.amountCents.centsToDisplay(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (row.paidMovement != null) {
                            Text("Pagado ✓", color = PositiveGreen, fontWeight = FontWeight.SemiBold)
                        } else {
                            Switch(
                                checked = row.active,
                                onCheckedChange = { viewModel.onActiveChange(row.fixedExpenseId, row.periodKey, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
