package com.takat.finanzas.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Every token Material 3 components read is set explicitly: anything left out falls back to the
// baseline purple palette (the selected SegmentedButton/FilterChip and the NavigationBar indicator
// read secondaryContainer, NavigationBar itself reads surfaceContainer).
private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    inversePrimary = EmeraldPrimaryDark,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald900,
    tertiary = Slate600,
    onTertiary = Color.White,
    tertiaryContainer = Slate100,
    onTertiaryContainer = Slate900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceTint = EmeraldPrimary,
    surfaceDim = Slate200,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate50,
    surfaceContainer = Slate100,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,
    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    outline = Slate500,
    outlineVariant = Slate200,
    scrim = Color.Black,
    error = NegativeRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Slate900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,
    inversePrimary = EmeraldPrimary,
    secondary = AmberAccentDark,
    onSecondary = Slate900,
    secondaryContainer = Emerald800,
    onSecondaryContainer = Emerald100,
    tertiary = Slate300,
    onTertiary = Slate900,
    tertiaryContainer = Slate700,
    onTertiaryContainer = Slate50,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    surfaceTint = EmeraldPrimaryDark,
    surfaceDim = Slate950,
    surfaceBright = Slate700,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    outline = Slate400,
    outlineVariant = Slate700,
    scrim = Color.Black,
    error = NegativeRedDark,
    onError = Slate900
)

/** Money semantics (income/expense/warning) resolved per theme, so screens never hand-pick a light-only color. */
@Immutable
data class FinanceColors(
    val positive: Color,
    val negative: Color,
    val warning: Color
)

private val LightFinanceColors = FinanceColors(positive = PositiveGreen, negative = NegativeRed, warning = AmberAccent)
private val DarkFinanceColors = FinanceColors(positive = PositiveGreenDark, negative = NegativeRedDark, warning = AmberAccentDark)

private val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

val MaterialTheme.financeColors: FinanceColors
    @Composable @ReadOnlyComposable get() = LocalFinanceColors.current

/**
 * Hairline border for cards: white cards on the slate-50 background (light) and slate-800 cards on
 * slate-900 (dark, where the elevation shadow is invisible) barely separate from the background on their own.
 */
val cardBorder: BorderStroke
    @Composable @ReadOnlyComposable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@Composable
fun TakatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
