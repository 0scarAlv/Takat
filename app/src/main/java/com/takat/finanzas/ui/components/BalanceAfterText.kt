package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.util.centsToDisplay

/** "Quedará: X" preview of an account's balance once the movement being typed is saved. */
@Composable
fun BalanceAfterText(resultingCents: Long, prefix: String = "Quedará", modifier: Modifier = Modifier) {
    Text(
        "$prefix: ${resultingCents.centsToDisplay()}",
        style = MaterialTheme.typography.bodySmall,
        color = if (resultingCents < 0) MaterialTheme.financeColors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 4.dp, start = 4.dp)
    )
}
