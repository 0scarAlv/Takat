package com.takat.finanzas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.util.CategoryIcons
import com.takat.finanzas.util.categoryIconOrNull

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // Los chips ya reservan 48dp de área táctil (32dp visibles): sin espacio extra quedan 16dp entre filas.
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { CategoryGlyph(category.emoji, iconSize = 18.dp) }
            )
        }
        AssistChip(onClick = onAddNew, label = { Text("+ Nueva") })
    }
}

/**
 * Compact version of [CategoryPicker]: a single field showing the chosen category that opens a bottom
 * sheet with the full grid. Picking one closes the sheet; "+ Nueva" closes it and hands off to [onAddNew].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryField(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedId }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            placeholder = { Text("Elegí una") },
            leadingIcon = selected?.let { { CategoryGlyph(it.emoji, iconSize = 20.dp) } },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Read-only field: an overlay catches the tap anywhere on it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = "Elegir categoría") { sheetOpen = true }
        )
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Categoría", style = MaterialTheme.typography.titleMedium)
                CategoryPicker(
                    categories = categories,
                    selectedId = selectedId,
                    onSelect = {
                        onSelect(it)
                        sheetOpen = false
                    },
                    onAddNew = {
                        sheetOpen = false
                        onAddNew()
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * Create or edit a category. Pass [existing] to edit it in place (prefills name/icon/salary flag,
 * calls [onConfirm] with the same id-less name/icon/salary triple — the caller decides whether that's
 * an insert or an update). [showSalaryOption] hides the salary toggle for expense-only contexts, where
 * it wouldn't apply.
 */
@Composable
fun AddCategoryDialog(
    existing: CategoryEntity? = null,
    showSalaryOption: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, isSalary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedIcon by remember {
        mutableStateOf(existing?.emoji?.takeIf { categoryIconOrNull(it) != null } ?: CategoryIcons.keys.first())
    }
    var isSalary by remember { mutableStateOf(existing?.isSalary ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text(if (existing != null) "Editar categoría" else "Nueva categoría") },
        text = {
            Column(modifier = Modifier.imePadding()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Text("Ícono", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                ) {
                    items(CategoryIcons.entries.toList()) { (key, icon) ->
                        val selected = key == selectedIcon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = key,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (showSalaryOption) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Es mi salario", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Un ingreso con esta categoría inicia la quincena de inmediato",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = isSalary, onCheckedChange = { isSalary = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon, isSalary)
            }) { Text(if (existing != null) "Guardar" else "Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
