package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.util.centsToDisplay

/**
 * How much of a credit card's limit is used, as a bar plus "te quedan X". Renders nothing for accounts without
 * a limit. [compact] is the one-line variant for account rows; the full one adds used / limit amounts.
 */
@Composable
fun CreditLimitBar(item: AccountWithBalance, amountsHidden: Boolean, compact: Boolean, modifier: Modifier = Modifier) {
    val limit = item.account.creditLimitCents ?: return
    val available = item.creditAvailableCents ?: return
    val used = item.creditUsedCents
    val fraction = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 1f
    val barColor = when {
        available < 0 -> MaterialTheme.financeColors.negative
        fraction >= 0.8f -> MaterialTheme.financeColors.warning
        else -> MaterialTheme.colorScheme.primary
    }
    fun money(cents: Long) = if (amountsHidden) "••••••" else cents.centsToDisplay()
    val summary = if (available >= 0) "Te quedan ${money(available)} de ${money(limit)}"
    else "Pasaste el límite por ${money(-available)}"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 4.dp else 8.dp)
                .clip(CircleShape)
        )
        if (compact) {
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Usado ${money(used)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Límite ${money(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (available < 0) MaterialTheme.financeColors.negative else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
