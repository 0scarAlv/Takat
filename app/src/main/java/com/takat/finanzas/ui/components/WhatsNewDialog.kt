package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.data.entity.AppSettingsEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.util.Changelog
import com.takat.finanzas.util.ChangelogEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Checks once (on first composition) whether there are unseen changelog entries for the current
 * install and, if so, shows [WhatsNewDialog]. Never fires on a fresh install — see
 * AppDatabase's onCreate callback, which seeds `lastSeenVersionCode` to the current version there.
 */
@Composable
fun WhatsNewGate(repository: FinanceRepository) {
    var pendingEntries by remember { mutableStateOf<List<ChangelogEntry>?>(null) }
    var baseSettings by remember { mutableStateOf<AppSettingsEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val settings = repository.appSettings().first()
        baseSettings = settings
        val entries = Changelog.entriesAfter(settings?.lastSeenVersionCode ?: 0)
        if (entries.isNotEmpty()) {
            pendingEntries = entries
        } else if ((settings?.lastSeenVersionCode ?: 0) != BuildConfig.VERSION_CODE) {
            repository.updateAppSettings((settings ?: AppSettingsEntity()).copy(lastSeenVersionCode = BuildConfig.VERSION_CODE))
        }
    }

    val entries = pendingEntries
    if (entries != null) {
        WhatsNewDialog(
            entries = entries,
            onDismiss = {
                pendingEntries = null
                scope.launch {
                    repository.updateAppSettings(
                        (baseSettings ?: AppSettingsEntity()).copy(lastSeenVersionCode = BuildConfig.VERSION_CODE)
                    )
                }
            }
        )
    }
}

@Composable
private fun WhatsNewDialog(entries: List<ChangelogEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novedades") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                entries.reversed().forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Versión ${entry.versionName}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        entry.changes.forEach { change ->
                            Row {
                                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                                Text(change, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        },
        modifier = Modifier.padding(vertical = 16.dp)
    )
}
