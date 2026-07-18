package com.takat.finanzas.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.ui.components.MonthSelector
import com.takat.finanzas.ui.theme.CategoricalOtherGray
import com.takat.finanzas.ui.theme.CategoricalPaletteDark
import com.takat.finanzas.ui.theme.CategoricalPaletteLight
import com.takat.finanzas.ui.theme.EmeraldPrimary
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay

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
            item { CategoryPieChart(uiState.categoryExpenses, uiState.totalExpenseCents) }

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

private data class PieSlice(val label: String, val cents: Long, val color: Color)

private const val PIE_MAX_SLICES = 7

@Composable
private fun CategoryPieChart(expenses: List<CategoryExpense>, totalCents: Long) {
    val palette = if (isSystemInDarkTheme()) CategoricalPaletteDark else CategoricalPaletteLight

    val shown = expenses.take(PIE_MAX_SLICES)
    val overflow = expenses.drop(PIE_MAX_SLICES)
    val slices = shown.mapIndexed { index, expense ->
        PieSlice(
            label = expense.category?.let { "${it.emoji} ${it.name}" } ?: "Sin categoría",
            cents = expense.totalCents,
            color = palette[index % palette.size]
        )
    } + if (overflow.isNotEmpty()) {
        listOf(PieSlice("Otros", overflow.sumOf { it.totalCents }, CategoricalOtherGray))
    } else {
        emptyList()
    }

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
            Canvas(modifier = Modifier.size(120.dp)) {
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = if (totalCents > 0) 360f * slice.cents / totalCents else 0f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                    startAngle += sweep
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.forEach { slice ->
                    val percent = if (totalCents > 0) (slice.cents * 100 / totalCents) else 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(slice.color, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${slice.label} · $percent%",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                Text(
                    "${expense.category?.emoji ?: "❔"} ${expense.category?.name ?: "Sin categoría"}",
                    style = MaterialTheme.typography.bodyLarge
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
