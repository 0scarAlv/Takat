package com.takat.finanzas.ui.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetPeriodType
import com.takat.finanzas.ui.components.FitLabel
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate

/**
 * Daily budget card. Enabled, it leads the Presupuesto page and answers "how much can I still spend
 * today?" first; disabled, it collapses to a single row with the switch to turn it on.
 */
@Composable
fun DailyBudgetSection(
    state: DailyBudgetUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPeriodTypeChange: (BudgetPeriodType) -> Unit,
    onDayOfMonthChange: (Int) -> Unit,
    onBasisChange: (BudgetBasis) -> Unit
) {
    var configExpanded by rememberSaveable { mutableStateOf(false) }

    TakatCard {
        Column(Modifier.padding(16.dp)) {
            if (!state.enabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Presupuesto diario", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Reparte tu saldo entre los días que faltan para tu próximo pago.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = false, onCheckedChange = onEnabledChange)
                }
                return@Column
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Presupuesto de hoy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { configExpanded = !configExpanded }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = if (configExpanded) "Ocultar configuración" else "Configurar presupuesto",
                        tint = if (configExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TodaySummary(state)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            PeriodSummary(state)

            AnimatedVisibility(visible = configExpanded) {
                BudgetSettings(state, onEnabledChange, onPeriodTypeChange, onDayOfMonthChange, onBasisChange)
            }
        }
    }
}

/** What's left for today in big type, and a bar of how much of today's budget is already spent. */
@Composable
private fun TodaySummary(state: DailyBudgetUiState) {
    val colors = MaterialTheme.financeColors
    val budget = state.frozenBudgetCents
    val spent = state.spentTodayCents
    val remaining = state.remainingTodayCents
    val fraction = if (budget > 0) spent.toFloat() / budget else if (spent > 0) 1f else 0f
    val barColor = when {
        remaining < 0 || budget <= 0 -> colors.negative
        fraction >= 0.75f -> colors.warning
        else -> colors.positive
    }
    val animatedFraction by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "spentToday")

    Text(
        if (remaining < 0) "Hoy te pasaste por" else "Todavía podés gastar",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ProvideTextStyle(
        MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            color = if (remaining < 0) colors.negative else Color.Unspecified
        )
    ) {
        FitLabel(
            if (remaining < 0) (-remaining).centsToDisplay() else remaining.centsToDisplay(),
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (remaining < 100 && state.sarcasticMessagesEnabled) {
        Text("Te deseo suerte.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .background(barColor)
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Gastaste ${spent.centsToDisplay()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "de ${budget.centsToDisplay()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The pay period at a glance: days left, the live "valor diario" recalculated with the current balance,
 * and how it compares with the budget fixed at the start of the day (spending less today raises it).
 */
@Composable
private fun PeriodSummary(state: DailyBudgetUiState) {
    val colors = MaterialTheme.financeColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Próximo pago", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) {
                FitLabel(
                    when (state.daysRemaining) {
                        0L -> "Hoy"
                        1L -> "Mañana"
                        else -> "En ${state.daysRemaining} días"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.nextPaymentDate?.let {
                Text(it.toDisplayDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Valor diario actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (state.dailyBudgetCents < 0) colors.negative else colors.positive
                )
            ) {
                FitLabel(state.dailyBudgetCents.centsToDisplay(), modifier = Modifier.fillMaxWidth())
            }
            Text("con tu saldo de ahora", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val diff = state.dailyBudgetCents - state.frozenBudgetCents
    if (diff != 0L) {
        Spacer(Modifier.height(10.dp))
        val good = diff > 0
        val tone = if (good) colors.positive else colors.warning
        Text(
            if (good) "Vas ${diff.centsToDisplay()} por día mejor que al empezar el día."
            else "Vas ${(-diff).centsToDisplay()} por día por debajo de como empezaste el día.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = tone,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(tone.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSettings(
    state: DailyBudgetUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPeriodTypeChange: (BudgetPeriodType) -> Unit,
    onDayOfMonthChange: (Int) -> Unit,
    onBasisChange: (BudgetBasis) -> Unit
) {
    Column {
        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Período de pago", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.periodType == BudgetPeriodType.QUINCENA,
                onClick = { onPeriodTypeChange(BudgetPeriodType.QUINCENA) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = {}
            ) { FitLabel("Quincena") }
            SegmentedButton(
                selected = state.periodType == BudgetPeriodType.MES,
                onClick = { onPeriodTypeChange(BudgetPeriodType.MES) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = {}
            ) { FitLabel("Mes") }
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
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = {}
            ) { FitLabel("Disponible") }
            SegmentedButton(
                selected = state.basis == BudgetBasis.CAPITAL,
                onClick = { onBasisChange(BudgetBasis.CAPITAL) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = {}
            ) { FitLabel("Capital total") }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Presupuesto diario activado", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = true, onCheckedChange = onEnabledChange)
        }
    }
}
