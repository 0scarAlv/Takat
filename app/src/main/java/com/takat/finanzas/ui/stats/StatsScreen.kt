package com.takat.finanzas.ui.stats

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.CornerRadius
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.model.DailyExpense
import com.takat.finanzas.ui.components.CategoryLabel
import com.takat.finanzas.ui.components.MonthSelector
import com.takat.finanzas.ui.theme.CategoricalOtherGray
import com.takat.finanzas.ui.theme.CategoricalPaletteDark
import com.takat.finanzas.ui.theme.CategoricalPaletteLight
import com.takat.finanzas.ui.theme.EmeraldPrimary
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun StatsScreen(
    onCategoryClick: (categoryId: Long?, from: Long, to: Long) -> Unit,
    onDayClick: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = rememberRepository()
    val viewModel: StatsViewModel = viewModel(factory = LambdaViewModelFactory { StatsViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    "Resumen del mes",
                    style = MaterialTheme.typography.titleMedium
                )
                MonthSelector(
                    label = uiState.monthLabel,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    uiState.totalExpenseCents.centsToDisplay(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uiState.dailyExpenses.any { it.totalCents > 0 }) {
            item {
                Text(
                    "Gasto por día",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item { DailyExpenseBarChart(uiState.dailyExpenses, onDayClick) }
        }

        if (uiState.categoryExpenses.isEmpty()) {
            item {
                Text(
                    "Sin gastos este mes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            item {
                Text(
                    "Gastos por categoría",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { CategoryDonutChart(uiState.categoryExpenses, uiState.totalExpenseCents) }

            val maxCents = uiState.categoryExpenses.maxOf { it.totalCents }.coerceAtLeast(1)
            items(uiState.categoryExpenses, key = { it.category?.id ?: -1L }) { expense ->
                CategoryExpenseRow(
                    expense = expense,
                    totalCents = uiState.totalExpenseCents,
                    maxCents = maxCents,
                    onClick = { onCategoryClick(expense.category?.id, uiState.fromMillis, uiState.toMillis) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private data class DonutSlice(val iconValue: String?, val name: String, val cents: Long, val color: Color)

private const val DONUT_MAX_SLICES = 7
private const val DONUT_HOLE_RATIO = 0.6f
private const val DONUT_GAP_DEGREES = 3f
private const val DONUT_START_ANGLE = -90f

@Composable
private fun CategoryDonutChart(expenses: List<CategoryExpense>, totalCents: Long) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = if (isDark) CategoricalPaletteDark else CategoricalPaletteLight

    val shown = expenses.take(DONUT_MAX_SLICES)
    val overflow = expenses.drop(DONUT_MAX_SLICES)
    val slices = shown.mapIndexed { index, expense ->
        DonutSlice(
            iconValue = expense.category?.emoji,
            name = expense.category?.name ?: "Sin categoría",
            cents = expense.totalCents,
            color = palette[index % palette.size]
        )
    } + if (overflow.isNotEmpty()) {
        listOf(DonutSlice("Category", "Otros", overflow.sumOf { it.totalCents }, CategoricalOtherGray))
    } else {
        emptyList()
    }

    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val expansions = slices.indices.map { index ->
                animateFloatAsState(
                    targetValue = if (index == selectedIndex) 1f else 0f,
                    animationSpec = tween(400),
                    label = "sliceExpansion"
                ).value
            }

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .blur(28.dp)
                        .background(EmeraldPrimary.copy(alpha = 0.16f), CircleShape)
                )
                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .pointerInput(slices, totalCents) {
                            detectTapGestures { offset ->
                                selectedIndex = hitTestSlice(offset, size.width.toFloat(), size.height.toFloat(), slices, totalCents)
                            }
                        }
                ) {
                    val outerRadius = size.minDimension / 2
                    val ringWidth = outerRadius * (1 - DONUT_HOLE_RATIO)
                    val baseInset = ringWidth / 2
                    val expansionPx = 6.dp.toPx()

                    var startAngle = DONUT_START_ANGLE
                    slices.forEachIndexed { index, slice ->
                        val rawSweep = if (totalCents > 0) 360f * slice.cents / totalCents else 0f
                        val drawSweep = (rawSweep - DONUT_GAP_DEGREES).coerceAtLeast(0f)
                        val inset = baseInset - expansions[index] * expansionPx
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = drawSweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            style = Stroke(width = ringWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += rawSweep
                    }
                }
                Crossfade(targetState = selectedIndex, animationSpec = tween(300), label = "donutCenter") { selected ->
                    val slice = selected?.let { slices.getOrNull(it) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (slice != null) {
                            val percent = if (totalCents > 0) (slice.cents * 100 / totalCents) else 0
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                "$percent%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                totalCents.centsToDisplay(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.forEachIndexed { index, slice ->
                    val percent = if (totalCents > 0) (slice.cents * 100 / totalCents) else 0
                    CategoryLabel(
                        value = slice.iconValue,
                        name = "${slice.name} · $percent%",
                        iconTint = slice.color,
                        iconSize = 16.dp,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.clickable {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }
            }
        }
    }
}

/** Returns the tapped slice's index, or null if the tap missed the ring band or landed in the gap. */
private fun hitTestSlice(offset: Offset, widthPx: Float, heightPx: Float, slices: List<DonutSlice>, totalCents: Long): Int? {
    val center = Offset(widthPx / 2, heightPx / 2)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = hypot(dx, dy)
    val outerRadius = minOf(widthPx, heightPx) / 2
    val holeRadius = outerRadius * DONUT_HOLE_RATIO
    if (distance < holeRadius - 8f || distance > outerRadius + 8f) return null

    var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angleDeg < 0) angleDeg += 360f
    val shifted = (angleDeg - DONUT_START_ANGLE).mod(360f)

    var cumStart = 0f
    slices.forEachIndexed { index, slice ->
        val rawSweep = if (totalCents > 0) 360f * slice.cents / totalCents else 0f
        if (shifted >= cumStart && shifted < cumStart + rawSweep) return index
        cumStart += rawSweep
    }
    return null
}

private const val DAY_CHART_HEIGHT_DP = 120

private enum class DayChartRange { MONTH, WEEK }

/** Splits a month's days (already in ascending date order) into real calendar weeks (Mon–Sun), first/last possibly partial. */
private fun chunkIntoWeeks(days: List<DailyExpense>): List<List<DailyExpense>> {
    val weeks = mutableListOf<MutableList<DailyExpense>>()
    days.forEach { day ->
        if (weeks.isEmpty() || day.date.dayOfWeek == DayOfWeek.MONDAY) weeks.add(mutableListOf())
        weeks.last().add(day)
    }
    return weeks
}

/**
 * One bar per calendar day; tapping a bar inspects that day, defaulting to the shown range's
 * peak-spending day. "Mes" shows the whole month; "Semana" narrows it to one calendar week at a
 * time (less crowded when you want to look at something specific), navigable with the arrows.
 */
@Composable
private fun DailyExpenseBarChart(dailyExpenses: List<DailyExpense>, onDayClick: (date: LocalDate) -> Unit) {
    var range by remember(dailyExpenses) { mutableStateOf(DayChartRange.MONTH) }
    val weeks = remember(dailyExpenses) { chunkIntoWeeks(dailyExpenses) }
    val defaultWeekIndex = remember(dailyExpenses, weeks) {
        val peakDate = dailyExpenses.maxBy { it.totalCents }.date
        weeks.indexOfFirst { week -> week.any { it.date == peakDate } }.coerceAtLeast(0)
    }
    var weekIndex by remember(dailyExpenses) { mutableStateOf(defaultWeekIndex) }

    val shownDays = if (range == DayChartRange.MONTH) dailyExpenses else weeks.getOrElse(weekIndex) { dailyExpenses }
    val maxCents = shownDays.maxOf { it.totalCents }.coerceAtLeast(1)
    val peakIndex = remember(shownDays) { shownDays.indices.maxBy { shownDays[it].totalCents } }
    var selectedIndex by remember(shownDays) { mutableStateOf(peakIndex) }
    val selected = shownDays.getOrNull(selectedIndex)
    val barColor = EmeraldPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (selectedIndex == peakIndex) "Día con más gasto" else "Ese día gastaste",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (selected != null && selected.totalCents > 0) {
                            "${selected.date.toDisplayDate()} · ${selected.totalCents.centsToDisplay()}"
                        } else {
                            "Sin gastos ese día"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = range == DayChartRange.MONTH,
                        onClick = { range = DayChartRange.MONTH },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Mes") }
                    SegmentedButton(
                        selected = range == DayChartRange.WEEK,
                        onClick = { range = DayChartRange.WEEK; weekIndex = defaultWeekIndex },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Semana") }
                }
            }

            if (range == DayChartRange.WEEK && weeks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { weekIndex = (weekIndex - 1).coerceAtLeast(0) }, enabled = weekIndex > 0) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Semana anterior")
                    }
                    val week = weeks.getOrElse(weekIndex) { weeks.last() }
                    Text(
                        "${week.first().date.toDisplayDate()} – ${week.last().date.toDisplayDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { weekIndex = (weekIndex + 1).coerceAtMost(weeks.lastIndex) },
                        enabled = weekIndex < weeks.lastIndex
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Semana siguiente")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DAY_CHART_HEIGHT_DP.dp)
                    .pointerInput(shownDays) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width.toFloat() / shownDays.size
                            val tappedIndex = (offset.x / slotWidth).toInt().coerceIn(0, shownDays.lastIndex)
                            selectedIndex = tappedIndex
                            val tappedDay = shownDays[tappedIndex]
                            if (tappedDay.totalCents > 0) onDayClick(tappedDay.date)
                        }
                    }
            ) {
                val slotWidth = size.width / shownDays.size
                val gap = (slotWidth * 0.2f).coerceAtMost(4.dp.toPx())
                val barWidth = (slotWidth - gap).coerceAtLeast(1f)
                val minBarHeight = 2.dp.toPx()
                val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

                shownDays.forEachIndexed { index, day ->
                    val fraction = day.totalCents.toFloat() / maxCents.toFloat()
                    val barHeight = (size.height * fraction).coerceAtLeast(minBarHeight)
                    val color = if (index == selectedIndex) barColor else barColor.copy(alpha = 0.35f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(index * slotWidth + gap / 2, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                shownDays.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val showLabel = range == DayChartRange.WEEK || day.date.dayOfMonth == 1 || day.date.dayOfMonth % 5 == 0
                        if (showLabel) {
                            Text(
                                day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseRow(expense: CategoryExpense, totalCents: Long, maxCents: Long, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryLabel(
                    value = expense.category?.emoji,
                    name = expense.category?.name ?: "Sin categoría",
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(expense.totalCents.centsToDisplay(), fontWeight = FontWeight.SemiBold)
                    val percent = if (totalCents > 0) (expense.totalCents * 100 / totalCents) else 0
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            ) {
                val fraction = (expense.totalCents.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(EmeraldPrimary, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
