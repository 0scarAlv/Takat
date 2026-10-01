package com.takat.finanzas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.takat.finanzas.R

/** Inter, bundled (OFL): neutral and precise for numbers, with the tabular-digit feature used below. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val base = Typography()

// Tabular (fixed-width) digits everywhere, so amounts stacked in lists and cards line up by column
// instead of shifting with each digit's natural width.
private fun TextStyle.inter(weight: FontWeight? = null, tracking: Float? = null) = copy(
    fontFamily = Inter,
    fontWeight = weight ?: fontWeight,
    letterSpacing = tracking?.sp ?: letterSpacing,
    fontFeatureSettings = "tnum"
)

/** Pre-3.0 typography (system font, tabular digits), used by the classic design. */
val ClassicTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFeatureSettings = "tnum"),
    displayMedium = base.displayMedium.copy(fontFeatureSettings = "tnum"),
    displaySmall = base.displaySmall.copy(fontFeatureSettings = "tnum"),
    headlineLarge = base.headlineLarge.copy(fontFeatureSettings = "tnum"),
    headlineMedium = base.headlineMedium.copy(fontFeatureSettings = "tnum"),
    headlineSmall = base.headlineSmall.copy(fontFeatureSettings = "tnum"),
    titleLarge = base.titleLarge.copy(fontFeatureSettings = "tnum"),
    titleMedium = base.titleMedium.copy(fontFeatureSettings = "tnum"),
    titleSmall = base.titleSmall.copy(fontFeatureSettings = "tnum"),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = "tnum"),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = "tnum"),
    bodySmall = base.bodySmall.copy(fontFeatureSettings = "tnum"),
    labelLarge = base.labelLarge.copy(fontFeatureSettings = "tnum"),
    labelMedium = base.labelMedium.copy(fontFeatureSettings = "tnum"),
    labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum")
)

// Display/headline sizes carry the big money figures: semibold with slightly negative tracking reads
// as one compact number instead of a spaced-out row of digits.
val Typography = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.SemiBold, -1.5f),
    displayMedium = base.displayMedium.inter(FontWeight.SemiBold, -1f),
    displaySmall = base.displaySmall.inter(FontWeight.SemiBold, -0.5f),
    headlineLarge = base.headlineLarge.inter(FontWeight.SemiBold, -0.5f),
    headlineMedium = base.headlineMedium.inter(FontWeight.SemiBold, -0.25f),
    headlineSmall = base.headlineSmall.inter(FontWeight.SemiBold),
    titleLarge = base.titleLarge.inter(FontWeight.SemiBold),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold, 0f),
    titleSmall = base.titleSmall.inter(FontWeight.SemiBold, 0f),
    bodyLarge = base.bodyLarge.inter(tracking = 0f),
    bodyMedium = base.bodyMedium.inter(tracking = 0f),
    bodySmall = base.bodySmall.inter(tracking = 0f),
    labelLarge = base.labelLarge.inter(FontWeight.SemiBold),
    labelMedium = base.labelMedium.inter(FontWeight.Medium),
    // The 11sp default is the smallest text in the app (frequency tags, chart axes); 12sp reads noticeably better.
    labelSmall = base.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp).inter(FontWeight.Medium)
)
