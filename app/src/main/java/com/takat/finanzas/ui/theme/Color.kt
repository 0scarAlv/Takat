package com.takat.finanzas.ui.theme

import androidx.compose.ui.graphics.Color

// Light-mode text/fill colors are the 600-700 steps, not the 500s: emerald-500 on white is only 2.5:1
// (red-500 3.8:1, amber-500 2.2:1), below the 4.5:1 WCAG AA minimum for normal text. Dark-mode values
// are the lighter 300-400 steps, which already clear 5:1 on the slate-800 surface.
val EmeraldPrimary = Color(0xFF047857)      // emerald-700, 5.5:1 with white
val EmeraldPrimaryDark = Color(0xFF34D399)  // emerald-400
val Emerald100 = Color(0xFFD1FAE5)
val Emerald800 = Color(0xFF065F46)
val Emerald900 = Color(0xFF064E3B)

val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val AmberAccent = Color(0xFFB45309)      // amber-700, 5.0:1 on white
val AmberAccentDark = Color(0xFFFBBF24)  // amber-400

val PositiveGreen = Color(0xFF047857)      // emerald-700, 5.5:1 on white
val PositiveGreenDark = Color(0xFF34D399)
val NegativeRed = Color(0xFFDC2626)        // red-600, 4.8:1 on white
val NegativeRedDark = Color(0xFFF87171)

val AccountSwatches = listOf(
    0xFF10B981.toInt(), // emerald
    0xFF3B82F6.toInt(), // blue
    0xFFF59E0B.toInt(), // amber
    0xFFEF4444.toInt(), // red
    0xFF8B5CF6.toInt(), // violet
    0xFFEC4899.toInt(), // pink
    0xFF06B6D4.toInt(), // cyan
    0xFF64748B.toInt()  // slate
)

/** Spoken names for [AccountSwatches], index-aligned, so TalkBack can announce which color is which. */
val AccountSwatchNames = listOf("Verde", "Azul", "Ámbar", "Rojo", "Violeta", "Rosa", "Celeste", "Gris")

/** Monochromatic emerald ramp for the category donut chart, anchored on emerald-500/600 (one step lighter than the light-mode EmeraldPrimary, which is emerald-700). */
val CategoricalPaletteLight = listOf(
    Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7), Color(0xFFA7F3D0)
)
val CategoricalPaletteDark = listOf(
    Color(0xFF6EE7B7), Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)
)
val CategoricalOtherGray = Color(0xFF8B8F98)
