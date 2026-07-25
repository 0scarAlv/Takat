package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.model.PendingFixedExpense
import com.takat.finanzas.util.centsToDisplay

/** Lets the user tag this transaction as the payment of one of the currently pending fixed expenses. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FixedExpensePicker(
    pending: List<PendingFixedExpense>,
    selectedId: Long?,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pending.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pending.forEach { item ->
            FilterChip(
                selected = item.fixedExpense.id == selectedId,
                onClick = { onToggle(item.fixedExpense.id) },
                label = { Text("${item.fixedExpense.name} · ${item.fixedExpense.amountCents.centsToDisplay()}") }
            )
        }
    }
}
