package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
