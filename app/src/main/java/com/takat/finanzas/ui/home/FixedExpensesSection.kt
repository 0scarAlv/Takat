package com.takat.finanzas.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.ui.theme.financeColors
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import com.takat.finanzas.ui.components.IconBadge
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.data.entity.UiStyle
import com.takat.finanzas.ui.classic.ClassicFixedExpensesSection
import com.takat.finanzas.ui.theme.LocalUiStyle

@Composable
fun FixedExpensesSection(
    onManageClick: () -> Unit,
    onPayClick: (fixedExpenseId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalUiStyle.current == UiStyle.CLASSIC) {
        ClassicFixedExpensesSection(onManageClick, onPayClick, modifier)
        return
    }
    val repository = rememberRepository()
    val viewModel: FixedExpensesSectionViewModel =
        viewModel(factory = LambdaViewModelFactory { FixedExpensesSectionViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.rows.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    val pendingRows = uiState.rows.filter { it.active && !it.isFullyPaid }
    val pendingCents = pendingRows.sumOf { it.remainingCents }

    TakatCard(modifier = modifier) {
        Column(modifier = Modifier.animateContentSize()) {
            // Collapsed by default: the header alone already answers "how much is still pending".
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    containerColor = MaterialTheme.financeColors.warning.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.financeColors.warning
                ) {
                    Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text("Gastos fijos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (pendingRows.isEmpty()) {
                            // Inactive rows aren't paid, just skipped this period: only claim "todo pagado" when that's literally true.
                            if (uiState.rows.all { it.isFullyPaid }) "Todo pagado este período" else "Nada pendiente este período"
                        }
                        else "${pendingRows.size} pendiente${if (pendingRows.size == 1) "" else "s"} · ${pendingCents.centsToDisplay()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                uiState.rows.forEach { row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FixedExpenseRow(
                        row = row,
                        onPayClick = { onPayClick(row.fixedExpenseId) },
                        onActiveChange = { viewModel.onActiveChange(row.fixedExpenseId, row.periodKey, it) }
                    )
                }
            }
        }
    }
}

/**
 * Paying is an explicit button rather than a tap anywhere on the row: with the whole row clickable, a near
 * miss on the switch opened the payment form instead of toggling it.
 */
@Composable
private fun FixedExpenseRow(
    row: FixedExpenseRowUiState,
    onPayClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    if (row.frequency == FixedExpenseFrequency.QUINCENAL) "Quincenal" else "Mensual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            val partiallyPaid = row.paidCents > 0 && !row.isFullyPaid
            Text(
                if (partiallyPaid) {
                    "${row.paidCents.centsToDisplay()} de ${row.amountCents.centsToDisplay()} · faltan ${row.remainingCents.centsToDisplay()}"
                } else {
                    row.amountCents.centsToDisplay()
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (partiallyPaid && row.amountCents > 0) {
                LinearProgressIndicator(
                    progress = { (row.paidCents.toFloat() / row.amountCents).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = MaterialTheme.financeColors.positive,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {}
                )
            }
        }
        if (row.isFullyPaid) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.financeColors.positive,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Pagado", color = MaterialTheme.financeColors.positive, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            FilledTonalButton(
                onClick = onPayClick,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) { Text("Pagar") }
            Switch(
                checked = row.active,
                onCheckedChange = onActiveChange,
                modifier = Modifier.semantics { contentDescription = "Cuenta en este período: ${row.name}" }
            )
        }
    }
}
