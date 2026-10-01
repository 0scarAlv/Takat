package com.takat.finanzas.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.data.entity.UiStyle
import com.takat.finanzas.ui.classic.ClassicMovementRow
import com.takat.finanzas.ui.theme.LocalUiStyle

/**
 * One movement in a list: a round badge (category icon, or transfer arrows), title + subtitle, and the signed
 * amount. [showDate] drops the date from the subtitle for lists that already group rows under day headers.
 */
@Composable
fun MovementRow(
    movement: Movement,
    currentAccountId: Long?,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: () -> Unit = {}
) {
    if (LocalUiStyle.current == UiStyle.CLASSIC) {
        ClassicMovementRow(movement, currentAccountId, modifier, onClick)
        return
    }
    when (movement) {
        is Movement.TransactionMovement -> {
            val tx = movement.transaction
            val isIncome = tx.amountCents >= 0
            val note = tx.note?.takeIf { it.isNotBlank() }
            val categoryName = movement.category?.name ?: "Sin categoría"
            MovementRowLayout(
                modifier = modifier,
                onClick = onClick,
                badge = {
                    // Income gets a green-tinted badge so it stands out from the (far more common) expenses
                    // even before reading the amount; expenses stay neutral.
                    IconBadge(
                        containerColor = if (isIncome) MaterialTheme.financeColors.positive.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isIncome) MaterialTheme.financeColors.positive else MaterialTheme.colorScheme.onSurface
                    ) {
                        CategoryGlyph(movement.category?.emoji, iconSize = 22.dp, fallbackTextStyle = MaterialTheme.typography.titleMedium)
                    }
                },
                title = note ?: categoryName,
                subtitle = listOfNotNull(note?.let { categoryName }, tx.date.toDisplayDate().takeIf { showDate })
                    .joinToString(" · "),
                trailing = {
                    movement.attachments.firstOrNull()?.let { AttachmentIndicator(it) }
                    Text(
                        tx.amountCents.centsToDisplay(showSign = true),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isIncome) MaterialTheme.financeColors.positive else MaterialTheme.financeColors.negative
                    )
                }
            )
        }

        is Movement.TransferMovement -> {
            val transfer = movement.transfer
            val isOutgoing = currentAccountId == transfer.fromAccountId
            val title = when (currentAccountId) {
                transfer.fromAccountId -> "Transferencia a ${movement.toAccount?.name ?: "cuenta eliminada"}"
                transfer.toAccountId -> "Transferencia de ${movement.fromAccount?.name ?: "cuenta eliminada"}"
                else -> "${movement.fromAccount?.name ?: "?"} → ${movement.toAccount?.name ?: "?"}"
            }
            MovementRowLayout(
                modifier = modifier,
                onClick = onClick,
                badge = {
                    IconBadge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                },
                title = title,
                subtitle = listOfNotNull(
                    movement.category?.name,
                    transfer.note?.takeIf { it.isNotBlank() },
                    transfer.date.toDisplayDate().takeIf { showDate }
                ).joinToString(" · ").ifEmpty { "Transferencia" },
                trailing = {
                    if (currentAccountId != null) {
                        val signedCents = if (isOutgoing) -transfer.amountCents else transfer.amountCents
                        Text(
                            signedCents.centsToDisplay(showSign = true),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOutgoing) MaterialTheme.financeColors.negative else MaterialTheme.financeColors.positive
                        )
                    } else {
                        Text(transfer.amountCents.centsToDisplay(), style = MaterialTheme.typography.titleSmall)
                    }
                }
            )
        }
    }
}

@Composable
private fun MovementRowLayout(
    modifier: Modifier,
    onClick: () -> Unit,
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        badge()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = trailing
        )
    }
}

/** Small preview for a transaction's attachment: the (unencrypted) thumbnail for photos, a document icon otherwise. */
@Composable
private fun AttachmentIndicator(attachment: AttachmentEntity) {
    if (attachment.type == AttachmentType.IMAGE && attachment.thumbnailPath != null) {
        val bitmap = remember(attachment.thumbnailPath) { BitmapFactory.decodeFile(attachment.thumbnailPath) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Comprobante adjunto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            return
        }
    }
    Icon(Icons.Filled.Description, contentDescription = "Comprobante adjunto", modifier = Modifier.size(20.dp))
}
