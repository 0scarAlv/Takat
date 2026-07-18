package com.takat.finanzas.ui.charts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.takat.finanzas.ui.components.MonthSelector
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay

@Composable
fun ChartsScreen(modifier: Modifier = Modifier) {
    val repository = rememberRepository()
    val viewModel: ChartsViewModel = viewModel(factory = LambdaViewModelFactory { ChartsViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Ingresos vs gastos", style = MaterialTheme.typography.titleMedium)
                MonthSelector(
                    label = uiState.monthLabel,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text("Balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    uiState.balanceCents.centsToDisplay(showSign = true),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.balanceCents < 0) NegativeRed else PositiveGreen
                )
            }
        }

        item {
            val maxCents = maxOf(uiState.incomeCents, uiState.expenseCents).coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountBar("Ingresos", uiState.incomeCents, maxCents, PositiveGreen)
                AmountBar("Gastos", uiState.expenseCents, maxCents, NegativeRed)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AmountBar(label: String, amountCents: Long, maxCents: Long, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(amountCents.centsToDisplay(), fontWeight = FontWeight.SemiBold, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            ) {
                val fraction = (amountCents.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
