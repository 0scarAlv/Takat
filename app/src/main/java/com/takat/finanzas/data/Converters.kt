package com.takat.finanzas.data

import androidx.room.TypeConverter
import com.takat.finanzas.data.entity.CategoryKind

class Converters {
    @TypeConverter
    fun fromCategoryKind(kind: CategoryKind): String = kind.name

    @TypeConverter
    fun toCategoryKind(value: String): CategoryKind = CategoryKind.valueOf(value)
}
