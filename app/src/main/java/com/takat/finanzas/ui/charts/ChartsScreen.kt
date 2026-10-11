package com.takat.finanzas.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.model.DailyFlow
import com.takat.finanzas.ui.components.FitLabel
import com.takat.finanzas.ui.components.MonthSelector
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(modifier: Modifier = Modifier) {
    val repository = rememberRepository()
    val viewModel: ChartsViewModel = viewModel(factory = LambdaViewModelFactory { ChartsViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val budgetViewModel: DailyBudgetViewModel = viewModel(factory = LambdaViewModelFactory { DailyBudgetViewModel(repository) })
    val budgetState by budgetViewModel.uiState.collectAsState()

    val budgetSection: @Composable () -> Unit = {
        DailyBudgetSection(
            state = budgetState,
            onEnabledChange = budgetViewModel::onEnabledChange,
            onPeriodTypeChange = budgetViewModel::onPeriodTypeChange,
            onDayOfMonthChange = budgetViewModel::onDayOfMonthChange,
            onBasisChange = budgetViewModel::onBasisChange
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (budgetState.enabled) item { budgetSection() }

        item {
            Column {
                Text("Ingresos vs gastos", style = MaterialTheme.typography.titleMedium)
                MonthSelector(
                    label = uiState.monthLabel,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item { BalanceHero(uiState) }

        item { FlowChartCard(uiState) }

        item { InsightTiles(uiState) }

        // Off, the daily budget is just a switch at the bottom; on, it's what you check most, so it leads.
        if (!budgetState.enabled) item { budgetSection() }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Month balance on the same gradient card as the home hero, with what share of the income was kept. */
@Composable
private fun BalanceHero(state: ChartsUiState) {
    val gradient = if (state.balanceCents < 0) MaterialTheme.financeColors.heroNegative
    else MaterialTheme.financeColors.heroPositive
    val muted = Color.White.copy(alpha = 0.82f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.verticalGradient(gradient))
            .padding(20.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column {
                Text("Balance del mes", style = MaterialTheme.typography.labelLarge, color = muted)
                Text(
                    state.balanceCents.centsToDisplay(showSign = true),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(savingsSentence(state), style = MaterialTheme.typography.bodyMedium, color = muted)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroFigure("Ingresos", state.incomeCents, muted, Modifier.weight(1f))
                    HeroFigure("Gastos", state.expenseCents, muted, Modifier.weight(1f))
                }
            }
        }
    }
}

private fun savingsSentence(state: ChartsUiState): String = when {
    state.incomeCents == 0L && state.expenseCents == 0L -> "Sin movimientos este mes."
    state.incomeCents == 0L -> "Todavía no hay ingresos este mes."
    state.balanceCents >= 0 -> "Te quedaste con el ${percent(state.balanceCents, state.incomeCents)} de lo que ingresaste."
    else -> "Gastaste ${percent(-state.balanceCents, state.incomeCents)} más de lo que ingresaste."
}

@Composable
private fun HeroFigure(label: String, cents: Long, muted: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted)
        ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) {
            FitLabel(cents.centsToDisplay(), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FlowChartCard(state: ChartsUiState) {
    TakatCard {
        Column(Modifier.padding(16.dp)) {
            Text("Cómo se movió el mes", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ingresos y gastos acumulados día a día. Tocá la gráfica para ver un día.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            val chartDays = state.chartDays
            if (chartDays.none { it.incomeCents != 0L || it.expenseCents != 0L }) {
                Text(
                    if (state.elapsedDays == 0) "Este mes todavía no empieza." else "Todavía no hay movimientos este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                CumulativeFlowChart(chartDays, monthLength = state.days.size)
            }
        }
    }
}

/**
 * Two running totals (income and spending) across the month, x spread over the whole month so the
 * current one visibly stops at today. Tap anywhere to read the totals up to that day.
 */
@Composable
private fun CumulativeFlowChart(days: List<DailyFlow>, monthLength: Int) {
    val colors = MaterialTheme.financeColors
    val incomeColor = colors.positive
    val expenseColor = colors.negative
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    val incomeTotals = remember(days) { days.runningFold(0L) { acc, d -> acc + d.incomeCents }.drop(1) }
    val expenseTotals = remember(days) { days.runningFold(0L) { acc, d -> acc + d.expenseCents }.drop(1) }
    var selected by remember(days) { mutableIntStateOf(days.lastIndex) }

    SelectedDayHeader(
        day = days[selected],
        incomeTotal = incomeTotals[selected],
        expenseTotal = expenseTotals[selected]
    )
    Spacer(Modifier.height(10.dp))

    val maxCents = niceCeiling(maxOf(incomeTotals.last(), expenseTotals.last(), 1))
    val span = (monthLength - 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(days) {
                detectTapGestures { offset ->
                    selected = (offset.x / size.width * span).roundToInt().coerceIn(0, days.lastIndex)
                }
            }
    ) {
        val top = 14.dp.toPx()
        val bottom = size.height - 2.dp.toPx()
        fun x(i: Int) = i.toFloat() / span * size.width
        fun y(cents: Long) = bottom - (bottom - top) * (cents.toFloat() / maxCents)

        // Gridlines at 0, ½ and the top of the scale, each with its amount just above the line.
        listOf(0L, maxCents / 2, maxCents).forEach { level ->
            drawLine(
                gridColor, Offset(0f, y(level)), Offset(size.width, y(level)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (level == 0L) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            if (level > 0) {
                drawText(textMeasurer, compactCents(level), Offset(0f, y(level) - 14.dp.toPx()), labelStyle)
            }
        }

        fun series(totals: List<Long>, color: Color) {
            val line = Path().apply {
                moveTo(x(0), y(totals[0]))
                for (i in 1..totals.lastIndex) lineTo(x(i), y(totals[i]))
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(totals.lastIndex), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0f)), top, bottom))
            drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        series(expenseTotals, expenseColor)
        series(incomeTotals, incomeColor)

        val sx = x(selected)
        drawLine(labelColor.copy(alpha = 0.35f), Offset(sx, top), Offset(sx, bottom), strokeWidth = 1.dp.toPx())
        listOf(incomeTotals[selected] to incomeColor, expenseTotals[selected] to expenseColor).forEach { (cents, color) ->
            drawCircle(color, radius = 5.dp.toPx(), center = Offset(sx, y(cents)))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(sx, y(cents)))
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(1, (monthLength + 1) / 2, monthLength).forEach { day ->
            Text("$day", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot("Ingresos", incomeColor)
        LegendDot("Gastos", expenseColor)
    }
}

@Composable
private fun SelectedDayHeader(day: DailyFlow, incomeTotal: Long, expenseTotal: Long) {
    val colors = MaterialTheme.financeColors
    Text(
        "Hasta el ${day.date.toDisplayDate()}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Ingresos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(incomeTotal.centsToDisplay(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.positive)
        }
        Column(Modifier.weight(1f)) {
            Text("Gastos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(expenseTotal.centsToDisplay(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.negative)
        }
    }
    val dayParts = listOfNotNull(
        day.incomeCents.takeIf { it != 0L }?.let { "ingresó ${it.centsToDisplay()}" },
        day.expenseCents.takeIf { it != 0L }?.let { "gastó ${it.centsToDisplay()}" }
    )
    Text(
        if (dayParts.isEmpty()) "Ese día no hubo movimientos." else "Ese día ${dayParts.joinToString(" y ")}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
}

/** 2×2 grid of quick reads on the month's spending pace. */
@Composable
private fun InsightTiles(state: ChartsUiState) {
    val colors = MaterialTheme.financeColors
    val elapsed = state.elapsedDays
    if (elapsed == 0) return
    val lived = state.days.take(elapsed)
    val average = state.expenseCents / elapsed

    val paceTile: Triple<String, String, String> = if (state.isCurrentMonth) {
        Triple("Proyección del mes", (average * state.days.size).centsToDisplay(), "si seguís a este ritmo")
    } else {
        val peak = state.days.maxBy { it.expenseCents }
        if (peak.expenseCents == 0L) Triple("Día de más gasto", "—", "no hubo gastos")
        else Triple("Día de más gasto", peak.expenseCents.centsToDisplay(), peak.date.toDisplayDate())
    }

    val previous = state.previousExpenseCents
    val compareCurrent = if (state.isCurrentMonth) {
        // Compare like with like: this month so far against the previous month's daily pace over the same days.
        previous * elapsed / state.month.minusMonths(1).lengthOfMonth()
    } else previous
    val changeValue: String
    val changeColor: Color
    when {
        previous == 0L -> { changeValue = "—"; changeColor = Color.Unspecified }
        else -> {
            val diff = state.expenseCents - compareCurrent
            val pct = if (compareCurrent == 0L) 0 else (diff * 100 / compareCurrent).toInt()
            changeValue = "${if (pct > 0) "+" else ""}$pct%"
            changeColor = if (diff > 0) colors.negative else colors.positive
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightTile("Gasto promedio", average.centsToDisplay(), "por día", Modifier.weight(1f))
            InsightTile(paceTile.first, paceTile.second, paceTile.third, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightTile(
                "Días sin gastos",
                "${lived.count { it.expenseCents == 0L }}",
                "de $elapsed ${if (elapsed == 1) "día" else "días"}",
                Modifier.weight(1f)
            )
            InsightTile(
                "Vs. mes anterior",
                changeValue,
                if (previous == 0L) "sin gastos el mes anterior"
                else if (state.isCurrentMonth) "en gastos, a esta altura"
                else "en gastos (antes ${previous.centsToDisplay()})",
                Modifier.weight(1f),
                valueColor = changeColor
            )
        }
    }
}

@Composable
private fun InsightTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier,
    valueColor: Color = Color.Unspecified
) {
    TakatCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = valueColor)) {
                FitLabel(value, modifier = Modifier.fillMaxWidth())
            }
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun percent(part: Long, whole: Long): String = "${(part * 100 / whole)}%"

/**
 * Rounds up to a round step of a power of ten (in whole units), so the chart's top gridline reads cleanly
 * without leaving most of the chart empty: $235 tops out at $300, not $500.
 */
private fun niceCeiling(cents: Long): Long {
    var magnitude = 100L
    while (magnitude * 10 <= cents) magnitude *= 10
    return listOf(10L, 15L, 20L, 25L, 30L, 40L, 50L, 60L, 80L, 100L).map { it * magnitude / 10 }.first { it >= cents }
}

/** Short axis label: $850, $1.5k, $2M. */
private fun compactCents(cents: Long): String {
    val units = abs(cents) / 100.0
    fun trim(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
    return when {
        units >= 1_000_000 -> "$${trim(units / 1_000_000)}M"
        units >= 1_000 -> "$${trim(units / 1_000)}k"
        else -> "$${trim(units)}"
    }
}
