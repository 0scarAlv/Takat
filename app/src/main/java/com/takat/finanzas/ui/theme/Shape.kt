package com.takat.finanzas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Softer, rounder corners than the M3 defaults: chips/fields (small) 12dp, list groups and cards (large) 20dp,
 * the hero balance card and sheets (extraLarge) 28dp.
 */
val TakatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
