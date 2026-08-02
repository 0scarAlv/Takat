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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.ui.components.CategoryLabel
import com.takat.finanzas.ui.components.MonthSelector
import com.takat.finanzas.ui.theme.CategoricalOtherGray
import com.takat.finanzas.ui.theme.CategoricalPaletteDark
import com.takat.finanzas.ui.theme.CategoricalPaletteLight
import com.takat.finanzas.ui.theme.EmeraldPrimary
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun StatsScreen(
    onCategoryClick: (categoryId: Long?, from: Long, to: Long) -> Unit,
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
                    "Gastos por categoría",
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

        if (uiState.categoryExpenses.isEmpty()) {
            item {
                Text(
                    "Sin gastos este mes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
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
