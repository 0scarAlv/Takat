package com.takat.finanzas.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.model.AccountSnapshot
import com.takat.finanzas.data.model.BalancePoint
import com.takat.finanzas.data.model.BalanceSnapshot
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.ui.components.CategoryGlyph
import com.takat.finanzas.ui.components.FitLabel
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.ui.theme.CategoricalOtherGray
import com.takat.finanzas.ui.theme.CategoricalPaletteDark
import com.takat.finanzas.ui.theme.CategoricalPaletteLight
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.shortMonthLabel
import com.takat.finanzas.util.toDisplayDate

private const val CATEGORY_ROWS = 6
private const val TOTAL_SCOPE = -1

/**
 * "Saldo a la fecha": a statement-style page showing how the books stood right after one movement —
 * balance before/after, how the balance got there, and what was earned, spent and moved up to that point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSnapshotScreen(isTransfer: Boolean, movementId: Long, onBack: () -> Unit) {
    val repository = rememberRepository()
    // Nullable result inside a holder so "still loading" and "movement was deleted" can be told apart.
    val loaded by produceState<Pair<Boolean, BalanceSnapshot?>>(false to null, isTransfer, movementId) {
        value = true to repository.balanceSnapshotAt(isTransfer, movementId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saldo a la fecha") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        val (done, snapshot) = loaded
        when {
            !done -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            snapshot == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Este movimiento ya no existe.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> SnapshotContent(snapshot, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SnapshotContent(snapshot: BalanceSnapshot, modifier: Modifier) {
    // Scope = one of the touched accounts, or TOTAL_SCOPE for all accounts together.
    var scope by rememberSaveable { mutableStateOf(snapshot.accounts.firstOrNull()?.account?.id?.toInt() ?: TOTAL_SCOPE) }
    val account = snapshot.accounts.find { it.account.id.toInt() == scope }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MovementSummary(snapshot.movement) }

        item {
            val options = snapshot.accounts.map { it.account.id.toInt() to it.account.name } + (TOTAL_SCOPE to "Todas")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (id, label) ->
                    SegmentedButton(
                        selected = scope == id,
                        onClick = { scope = id },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {}
                    ) { FitLabel(label) }
                }
            }
        }

        item { BalanceHero(snapshot, account) }

        item {
            val series = account?.series ?: snapshot.capitalSeries
            TakatCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Cómo llegó a ese saldo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Saldo al final de cada día, desde el inicio hasta este movimiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    BalanceLineChart(series)
                }
            }
        }

        item {
            if (account != null) {
                FlowCard(
                    incomeCents = account.incomeCents,
                    expenseCents = account.expenseCents,
                    transferLines = listOf(
                        "Transferencias recibidas" to account.transferInCents,
                        "Transferencias enviadas" to account.transferOutCents
                    ),
                    transferNote = null
                )
            } else {
                FlowCard(
                    incomeCents = snapshot.incomeCents,
                    expenseCents = snapshot.expenseCents,
                    transferLines = listOf("Movido entre cuentas" to snapshot.transferCents),
                    transferNote = if (snapshot.transferCount > 0) {
                        "${snapshot.transferCount} ${if (snapshot.transferCount == 1) "transferencia" else "transferencias"}. " +
                            "No son ingreso ni gasto: solo cambian el dinero de cuenta."
                    } else null
                )
            }
        }

        item { MonthlyFlowCard(snapshot) }

        if (snapshot.expensesByCategory.isNotEmpty()) {
            item { CategoryCard(snapshot) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MovementSummary(movement: Movement) {
    val colors = MaterialTheme.financeColors
    val title: String
    val subtitle: String
    val amount: String
    val amountColor: Color
    when (movement) {
        is Movement.TransactionMovement -> {
            val tx = movement.transaction
            title = movement.category?.name ?: "Sin categoría"
            subtitle = listOfNotNull(movement.account?.name, tx.note?.takeIf { it.isNotBlank() }).joinToString(" · ")
            amount = tx.amountCents.centsToDisplay(showSign = true)
            amountColor = if (tx.amountCents >= 0) colors.positive else colors.negative
        }
        is Movement.TransferMovement -> {
            title = "Transferencia"
            subtitle = "${movement.fromAccount?.name ?: "Cuenta eliminada"} → ${movement.toAccount?.name ?: "Cuenta eliminada"}"
            amount = movement.transfer.amountCents.centsToDisplay()
            amountColor = Color.Unspecified
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                "Hasta el ${movement.date.toDisplayDate()}, incluido este movimiento",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

@Composable
private fun BalanceHero(snapshot: BalanceSnapshot, account: AccountSnapshot?) {
    val value = account?.balanceAfterCents ?: snapshot.capitalCents
    val gradient = if (value < 0 && account?.account?.isDebt != true) MaterialTheme.financeColors.heroNegative
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
                Text(
                    if (account != null) "Saldo de ${account.account.name} después" else "Capital total en ese momento",
                    style = MaterialTheme.typography.labelLarge,
                    color = muted
                )
                Text(value.centsToDisplay(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (account != null) {
                    HeroLine("Saldo antes", account.balanceBeforeCents.centsToDisplay(), muted)
                    HeroLine(
                        "Este movimiento",
                        (account.balanceAfterCents - account.balanceBeforeCents).centsToDisplay(showSign = true),
                        muted
                    )
                } else {
                    if (snapshot.debtCents != 0L) HeroLine("Deuda total", snapshot.debtCents.centsToDisplay(), muted)
                    HeroLine("Neto (capital − deuda)", (snapshot.capitalCents - snapshot.debtCents).centsToDisplay(), muted)
                }
            }
        }
    }
}

@Composable
private fun HeroLine(label: String, value: String, muted: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = muted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Line of end-of-day balances, x proportional to time. Tap to read a point; the last point is this movement. */
@Composable
private fun BalanceLineChart(series: List<BalancePoint>) {
    if (series.isEmpty()) {
        Text("Sin datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by remember(series) { mutableStateOf(series.lastIndex) }
    val point = series[selected]
    val lineColor = MaterialTheme.colorScheme.primary
    val zeroColor = MaterialTheme.colorScheme.outlineVariant

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (selected == series.lastIndex) "Este movimiento" else point.date.toDisplayDate(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(point.cents.centsToDisplay(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(8.dp))

    val firstDay = series.first().date.toEpochDay()
    val spanDays = (series.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
    val minCents = minOf(series.minOf { it.cents }, 0)
    val maxCents = series.maxOf { it.cents }.coerceAtLeast(minCents + 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .pointerInput(series) {
                detectTapGestures { offset ->
                    val tappedDay = firstDay + (offset.x / size.width * spanDays).toLong()
                    selected = series.indices.minBy { kotlin.math.abs(series[it].date.toEpochDay() - tappedDay) }
                }
            }
    ) {
        val pad = 6.dp.toPx()
        fun x(p: BalancePoint) =
            if (series.size == 1) size.width / 2 else (p.date.toEpochDay() - firstDay).toFloat() / spanDays * size.width
        fun y(cents: Long) = pad + (size.height - 2 * pad) * (1f - (cents - minCents).toFloat() / (maxCents - minCents))

        if (minCents < 0) {
            drawLine(
                zeroColor, Offset(0f, y(0)), Offset(size.width, y(0)),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        // Step line: a day's balance holds until the next recorded day, like a statement.
        val line = Path().apply {
            moveTo(x(series.first()), y(series.first().cents))
            series.drop(1).forEachIndexed { i, p ->
                lineTo(x(p), y(series[i].cents))
                lineTo(x(p), y(p.cents))
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(series.last()), size.height)
            lineTo(x(series.first()), size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))))
        drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val sel = series[selected]
        drawLine(lineColor.copy(alpha = 0.4f), Offset(x(sel), 0f), Offset(x(sel), size.height), strokeWidth = 1.dp.toPx())
        drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(x(sel), y(sel.cents)))
        drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(x(sel), y(sel.cents)))
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(series.first().date.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(series.last().date.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FlowCard(
    incomeCents: Long,
    expenseCents: Long,
    transferLines: List<Pair<String, Long>>,
    transferNote: String?
) {
    val colors = MaterialTheme.financeColors
    val max = maxOf(incomeCents, expenseCents, 1)
    TakatCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Acumulado desde el inicio", style = MaterialTheme.typography.titleMedium)
            ProportionBar("Ingresos", incomeCents, incomeCents.toFloat() / max, colors.positive)
            ProportionBar("Gastos", expenseCents, expenseCents.toFloat() / max, colors.negative)
            Text(
                if (incomeCents >= expenseCents) "Ingresaste ${(incomeCents - expenseCents).centsToDisplay()} más de lo que gastaste."
                else "Gastaste ${(expenseCents - incomeCents).centsToDisplay()} más de lo que ingresaste.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transferLines.any { it.second > 0 }) HorizontalDivider()
            transferLines.filter { it.second > 0 }.forEach { (label, cents) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(cents.centsToDisplay(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            if (transferNote != null) {
                Text(transferNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProportionBar(label: String, cents: Long, fraction: Float, color: Color, leading: (@Composable () -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(cents.centsToDisplay(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Income vs spending per month for the months leading up to the movement (all accounts in the total). */
@Composable
private fun MonthlyFlowCard(snapshot: BalanceSnapshot) {
    val colors = MaterialTheme.financeColors
    // Skip the months before the history starts, so a short history isn't mostly empty slots.
    val flows = snapshot.monthlyFlows.dropWhile { it.incomeCents == 0L && it.expenseCents == 0L }
    if (flows.isEmpty()) return
    val max = flows.maxOf { maxOf(it.incomeCents, it.expenseCents) }.coerceAtLeast(1)
    TakatCard {
        Column(Modifier.padding(16.dp)) {
            Text("Ingresos y gastos por mes", style = MaterialTheme.typography.titleMedium)
            Text(
                "Todas las cuentas, hasta este movimiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val slot = size.width / flows.size
                val barWidth = (slot * 0.3f).coerceAtMost(14.dp.toPx())
                val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                flows.forEachIndexed { i, flow ->
                    val center = slot * i + slot / 2
                    listOf(flow.incomeCents to colors.positive, flow.expenseCents to colors.negative)
                        .forEachIndexed { j, (cents, color) ->
                            val h = (size.height * cents / max).coerceAtLeast(if (cents > 0) 2.dp.toPx() else 0f)
                            val left = if (j == 0) center - barWidth - 1.dp.toPx() else center + 1.dp.toPx()
                            drawRoundRect(color, Offset(left, size.height - h), Size(barWidth, h), radius)
                        }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                flows.forEach { flow ->
                    Text(
                        shortMonthLabel(flow.month),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot("Ingresos", colors.positive)
                LegendDot("Gastos", colors.negative)
            }
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

@Composable
private fun CategoryCard(snapshot: BalanceSnapshot) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = if (isDark) CategoricalPaletteDark else CategoricalPaletteLight
    val shown = snapshot.expensesByCategory.take(CATEGORY_ROWS)
    val otherCents = snapshot.expensesByCategory.drop(CATEGORY_ROWS).sumOf { it.cents }
    val max = shown.first().cents.coerceAtLeast(otherCents).coerceAtLeast(1)

    TakatCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
            Text(
                "Todo lo gastado desde el inicio hasta este movimiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            shown.forEachIndexed { index, total ->
                val color = palette[index % palette.size]
                ProportionBar(
                    label = total.category?.name ?: "Sin categoría",
                    cents = total.cents,
                    fraction = total.cents.toFloat() / max,
                    color = color,
                    leading = { CategoryGlyph(total.category?.emoji, tint = color, iconSize = 18.dp) }
                )
            }
            if (otherCents > 0) {
                ProportionBar("Otras", otherCents, otherCents.toFloat() / max, CategoricalOtherGray)
            }
        }
    }
}
