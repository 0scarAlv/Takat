package com.takat.finanzas.ui.charts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetPeriodType
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBudgetSection(
    state: DailyBudgetUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPeriodTypeChange: (BudgetPeriodType) -> Unit,
    onDayOfMonthChange: (Int) -> Unit,
    onBasisChange: (BudgetBasis) -> Unit
) {
    var configExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Presupuesto diario",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
            }

            if (state.enabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Disponible por día",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            state.dailyBudgetCents.centsToDisplay(showSign = true),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.dailyBudgetCents < 0) NegativeRed else PositiveGreen
                        )
                    }
                    state.nextPaymentDate?.let { date ->
                        Text(
                            "Faltan ${state.daysRemaining} días\npara el ${date.toDisplayDate()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { configExpanded = !configExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Configuración", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = if (configExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (configExpanded) "Ocultar configuración" else "Mostrar configuración",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (configExpanded) {
                    Spacer(Modifier.height(8.dp))
                    Text("Período de pago", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.periodType == BudgetPeriodType.QUINCENA,
                            onClick = { onPeriodTypeChange(BudgetPeriodType.QUINCENA) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Quincena") }
                        SegmentedButton(
                            selected = state.periodType == BudgetPeriodType.MES,
                            onClick = { onPeriodTypeChange(BudgetPeriodType.MES) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Mes") }
                    }

                    if (state.periodType == BudgetPeriodType.MES) {
                        Spacer(Modifier.height(10.dp))
                        var dayText by remember { mutableStateOf(state.dayOfMonth.toString()) }
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { text ->
                                dayText = text
                                val day = text.toIntOrNull()
                                if (day != null && day in 1..31) onDayOfMonthChange(day)
                            },
                            label = { Text("Día de pago (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Base de cálculo", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.basis == BudgetBasis.DISPONIBLE,
                            onClick = { onBasisChange(BudgetBasis.DISPONIBLE) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Disponible") }
                        SegmentedButton(
                            selected = state.basis == BudgetBasis.CAPITAL,
                            onClick = { onBasisChange(BudgetBasis.CAPITAL) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Capital total") }
                    }
                }
            }
        }
    }
}
