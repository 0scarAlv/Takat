package com.takat.finanzas.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.takat.finanzas.util.UnknownCategoryIcon
import com.takat.finanzas.util.categoryIconOrNull

/**
 * Renders a category's stored glyph: a Material [Icon] when [value] matches a known icon name (new/edited
 * categories), the literal text otherwise (legacy emoji), or [UnknownCategoryIcon] when [value] is null.
 */
@Composable
fun CategoryGlyph(
    value: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    iconSize: Dp = 24.dp,
    fallbackTextStyle: TextStyle = LocalTextStyle.current
) {
    val icon = value?.let { categoryIconOrNull(it) }
    when {
        value == null -> Icon(UnknownCategoryIcon, contentDescription = null, tint = tint, modifier = modifier.size(iconSize))
        icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(iconSize))
        else -> Text(value, style = fallbackTextStyle, modifier = modifier)
    }
}

/** [CategoryGlyph] followed by the category name, for inline "icon + label" rows (legend, chips). */
@Composable
fun CategoryLabel(
    value: String?,
    name: String,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalContentColor.current,
    iconSize: Dp = 18.dp,
    textStyle: TextStyle = LocalTextStyle.current,
    spacing: Dp = 6.dp
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CategoryGlyph(value, tint = iconTint, iconSize = iconSize, fallbackTextStyle = textStyle)
        Spacer(Modifier.width(spacing))
        Text(name, style = textStyle)
    }
}
