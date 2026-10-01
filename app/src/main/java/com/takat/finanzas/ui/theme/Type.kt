package com.takat.finanzas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val base = Typography()

// Tabular (fixed-width) digits everywhere, so amounts stacked in lists and cards line up by column
// instead of shifting with each digit's natural width.
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

val Typography = Typography(
    displayLarge = base.displayLarge.tabular(),
    displayMedium = base.displayMedium.tabular(),
    displaySmall = base.displaySmall.tabular(),
    headlineLarge = base.headlineLarge.tabular(),
    headlineMedium = base.headlineMedium.tabular(),
    headlineSmall = base.headlineSmall.tabular(),
    titleLarge = base.titleLarge.tabular(),
    titleMedium = base.titleMedium.tabular(),
    titleSmall = base.titleSmall.tabular(),
    bodyLarge = base.bodyLarge.tabular(),
    bodyMedium = base.bodyMedium.tabular(),
    bodySmall = base.bodySmall.tabular(),
    labelLarge = base.labelLarge.tabular(),
    labelMedium = base.labelMedium.tabular(),
    // The 11sp default is the smallest text in the app (frequency tags, chart axes); 12sp reads noticeably better.
    labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp).tabular()
)
