package com.takat.finanzas.ui.components

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import com.takat.finanzas.util.toDisplayDate
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The decrypted content of a tapped attachment, kept in memory only while the full-size viewer is open. */
private sealed class AttachmentContent {
    data class ImageContent(val bitmap: ImageBitmap, val bytes: ByteArray) : AttachmentContent()
    data class JsonContent(val text: String, val bytes: ByteArray) : AttachmentContent()
    data class PdfContent(val bytes: ByteArray) : AttachmentContent()
}

@Composable
fun MovementDetailDialog(
    movement: Movement,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<Pair<AttachmentEntity, AttachmentContent>?>(null) }
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val repository = rememberRepository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val bytes = exportBytes
        exportBytes = null
        if (uri != null && bytes != null) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        }
    }
    val saveCopy: (AttachmentEntity, ByteArray) -> Unit = { attachment, bytes ->
        exportBytes = bytes
        exportLauncher.launch(suggestedFileName(attachment))
    }
    val openExternally: (ByteArray) -> Unit = { bytes ->
        val uri = repository.writeAttachmentForExternalView(bytes, "pdf")
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    val openAttachment: (AttachmentEntity) -> Unit = { attachment ->
        scope.launch {
            val bytes = repository.readAttachment(attachment)
            viewer = when (attachment.type) {
                AttachmentType.IMAGE -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?.let { attachment to AttachmentContent.ImageContent(it.asImageBitmap(), bytes) }
                AttachmentType.JSON -> attachment to AttachmentContent.JsonContent(prettyJson(bytes), bytes)
                AttachmentType.PDF -> attachment to AttachmentContent.PdfContent(bytes)
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("¿Eliminar este movimiento?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            }
        )
        return
    }

    viewer?.let { (attachment, content) ->
        AttachmentViewer(
            attachment = attachment,
            content = content,
            onDismiss = { viewer = null },
            onSaveCopy = { bytes -> saveCopy(attachment, bytes) },
            onOpenExternally = openExternally
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del movimiento") },
        text = {
            Column {
                when (movement) {
                    is Movement.TransactionMovement -> {
                        val tx = movement.transaction
                        DetailRow("Tipo", if (tx.amountCents >= 0) "Ingreso" else "Gasto")
                        DetailRow("Monto", tx.amountCents.centsToDisplay(showSign = true))
                        DetailRow("Cuenta", movement.account?.name ?: "Cuenta eliminada")
                        DetailRow("Categoría", movement.category?.name ?: "Sin categoría")
                        DetailRow("Fecha", tx.date.toDisplayDate())
                        DetailRow("Nota", tx.note?.takeIf { it.isNotBlank() } ?: "—")
                        if (movement.attachments.isNotEmpty()) {
                            Text("Adjuntos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                movement.attachments.forEach { attachment ->
                                    AttachmentThumb(attachment, onClick = { openAttachment(attachment) })
                                }
                            }
                        }
                    }

                    is Movement.TransferMovement -> {
                        val transfer = movement.transfer
                        DetailRow("Tipo", "Transferencia")
                        DetailRow("Monto", transfer.amountCents.centsToDisplay())
                        DetailRow("Desde", movement.fromAccount?.name ?: "Cuenta eliminada")
                        DetailRow("Hacia", movement.toAccount?.name ?: "Cuenta eliminada")
                        DetailRow("Motivo", movement.category?.name ?: "Sin motivo")
                        DetailRow("Fecha", transfer.date.toDisplayDate())
                        DetailRow("Nota", transfer.note?.takeIf { it.isNotBlank() } ?: "—")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmingDelete = true }) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/** Full-screen viewer: real size for photos, scrollable formatted text for JSON, metadata + actions for PDF. */
@Composable
private fun AttachmentViewer(
    attachment: AttachmentEntity,
    content: AttachmentContent,
    onDismiss: () -> Unit,
    onSaveCopy: (ByteArray) -> Unit,
    onOpenExternally: (ByteArray) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Row {
                        if (content is AttachmentContent.PdfContent) {
                            IconButton(onClick = { onOpenExternally(content.bytes) }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Abrir", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { onSaveCopy(rawBytesOf(content)) }) {
                            Icon(Icons.Filled.Download, contentDescription = "Guardar copia", tint = Color.White)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    when (content) {
                        is AttachmentContent.ImageContent -> Image(
                            bitmap = content.bitmap,
                            contentDescription = "Comprobante",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        is AttachmentContent.JsonContent -> Text(
                            content.text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                        is AttachmentContent.PdfContent -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Documento PDF", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { onOpenExternally(content.bytes) }) { Text("Abrir") }
                        }
                    }
                }
            }
        }
    }
}

private fun rawBytesOf(content: AttachmentContent): ByteArray = when (content) {
    is AttachmentContent.ImageContent -> content.bytes
    is AttachmentContent.JsonContent -> content.bytes
    is AttachmentContent.PdfContent -> content.bytes
}

private fun suggestedFileName(attachment: AttachmentEntity): String {
    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(attachment.createdAt))
    val extension = when (attachment.type) {
        AttachmentType.IMAGE -> "jpg"
        AttachmentType.JSON -> "json"
        AttachmentType.PDF -> "pdf"
    }
    return "comprobante_$date.$extension"
}

@Composable
private fun AttachmentThumb(attachment: AttachmentEntity, onClick: () -> Unit) {
    if (attachment.type == AttachmentType.IMAGE && attachment.thumbnailPath != null) {
        val bitmap = remember(attachment.thumbnailPath) { BitmapFactory.decodeFile(attachment.thumbnailPath) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Ver comprobante",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
            )
            return
        }
    }
    Icon(
        if (attachment.type == AttachmentType.IMAGE) Icons.Filled.PhotoCamera else Icons.Filled.Description,
        contentDescription = "Ver comprobante",
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
    }
}

private fun prettyJson(bytes: ByteArray): String {
    val text = bytes.toString(Charsets.UTF_8)
    return runCatching { JSONObject(text).toString(2) }
        .recoverCatching { JSONArray(text).toString(2) }
        .getOrDefault(text)
}
