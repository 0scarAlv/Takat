package com.takat.finanzas.data

import androidx.room.TypeConverter
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.entity.BudgetBasis
import com.takat.finanzas.data.entity.BudgetPeriodType
import com.takat.finanzas.data.entity.CategoryKind

class Converters {
    @TypeConverter
    fun fromCategoryKind(kind: CategoryKind): String = kind.name

    @TypeConverter
    fun toCategoryKind(value: String): CategoryKind = CategoryKind.valueOf(value)

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)

    @TypeConverter
    fun fromBudgetPeriodType(type: BudgetPeriodType): String = type.name

    @TypeConverter
    fun toBudgetPeriodType(value: String): BudgetPeriodType = BudgetPeriodType.valueOf(value)

    @TypeConverter
    fun fromBudgetBasis(basis: BudgetBasis): String = basis.name

    @TypeConverter
    fun toBudgetBasis(value: String): BudgetBasis = BudgetBasis.valueOf(value)
}
