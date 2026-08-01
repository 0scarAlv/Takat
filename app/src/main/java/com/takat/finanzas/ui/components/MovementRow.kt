package com.takat.finanzas.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate

@Composable
fun MovementRow(
    movement: Movement,
    currentAccountId: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    when (movement) {
        is Movement.TransactionMovement -> {
            val tx = movement.transaction
            val isIncome = tx.amountCents >= 0
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(movement.category?.emoji ?: "❔", style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            movement.category?.name ?: "Sin categoría",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = listOfNotNull(tx.note?.takeIf { it.isNotBlank() }, tx.date.toDisplayDate())
                            .joinToString(" · ")
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movement.attachments.firstOrNull()?.let { AttachmentIndicator(it) }
                    Text(
                        tx.amountCents.centsToDisplay(showSign = true),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isIncome) PositiveGreen else NegativeRed
                    )
                }
            }
        }

        is Movement.TransferMovement -> {
            val transfer = movement.transfer
            val isOutgoing = currentAccountId == transfer.fromAccountId
            val title = when (currentAccountId) {
                transfer.fromAccountId -> "Transferencia a ${movement.toAccount?.name ?: "cuenta eliminada"}"
                transfer.toAccountId -> "Transferencia de ${movement.fromAccount?.name ?: "cuenta eliminada"}"
                else -> "${movement.fromAccount?.name ?: "?"} → ${movement.toAccount?.name ?: "?"}"
            }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⇄", style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = listOfNotNull(
                            movement.category?.let { "${it.emoji} ${it.name}" },
                            transfer.note?.takeIf { it.isNotBlank() },
                            transfer.date.toDisplayDate()
                        ).joinToString(" · ")
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (currentAccountId != null) {
                    val signedCents = if (isOutgoing) -transfer.amountCents else transfer.amountCents
                    Text(
                        signedCents.centsToDisplay(showSign = true),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOutgoing) NegativeRed else PositiveGreen
                    )
                } else {
                    Text(transfer.amountCents.centsToDisplay(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
