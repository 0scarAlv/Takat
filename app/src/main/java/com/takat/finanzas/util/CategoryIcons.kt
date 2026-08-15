package com.takat.finanzas.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Curated set of Material icons for categories, keyed by a stable name string. New/edited categories store
 * the key (e.g. "Restaurant") in [com.takat.finanzas.data.entity.CategoryEntity.emoji] instead of a literal
 * emoji character; [categoryIconOrNull] resolves it back, falling back to null (render as text) for legacy
 * emoji values that don't match any key here.
 */
val CategoryIcons: Map<String, ImageVector> = mapOf(
    "Restaurant" to Icons.Filled.Restaurant,
    "LocalCafe" to Icons.Filled.LocalCafe,
    "ShoppingCart" to Icons.Filled.ShoppingCart,
    "ShoppingBag" to Icons.Filled.ShoppingBag,
    "DirectionsBus" to Icons.Filled.DirectionsBus,
    "DirectionsCar" to Icons.Filled.DirectionsCar,
    "LocalGasStation" to Icons.Filled.LocalGasStation,
    "Home" to Icons.Filled.Home,
    "Bolt" to Icons.Filled.Bolt,
    "Wifi" to Icons.Filled.Wifi,
    "Subscriptions" to Icons.Filled.Subscriptions,
    "SportsEsports" to Icons.Filled.SportsEsports,
    "MusicNote" to Icons.Filled.MusicNote,
    "MenuBook" to Icons.AutoMirrored.Filled.MenuBook,
    "Celebration" to Icons.Filled.Celebration,
    "LocalHospital" to Icons.Filled.LocalHospital,
    "FitnessCenter" to Icons.Filled.FitnessCenter,
    "Spa" to Icons.Filled.Spa,
    "School" to Icons.Filled.School,
    "Checkroom" to Icons.Filled.Checkroom,
    "Pets" to Icons.Filled.Pets,
    "PestControl" to Icons.Filled.PestControl,
    "CardGiftcard" to Icons.Filled.CardGiftcard,
    "Redeem" to Icons.Filled.Redeem,
    "Flight" to Icons.Filled.Flight,
    "Luggage" to Icons.Filled.Luggage,
    "Build" to Icons.Filled.Build,
    "Shield" to Icons.Filled.Shield,
    "ReceiptLong" to Icons.AutoMirrored.Filled.ReceiptLong,
    "Work" to Icons.Filled.Work,
    "Payments" to Icons.Filled.Payments,
    "AttachMoney" to Icons.Filled.AttachMoney,
    "Savings" to Icons.Filled.Savings,
    "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
    "CreditCard" to Icons.Filled.CreditCard,
    "Category" to Icons.Filled.Category,
    "Label" to Icons.AutoMirrored.Filled.Label
)

/** Fallback icon for a missing/deleted category (never stored, only used at render time). */
val UnknownCategoryIcon: ImageVector = Icons.AutoMirrored.Filled.HelpOutline

fun categoryIconOrNull(value: String): ImageVector? = CategoryIcons[value]
