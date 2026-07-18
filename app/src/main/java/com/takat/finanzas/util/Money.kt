package com.takat.finanzas.util

fun Long.centsToDisplay(showSign: Boolean = false): String {
    val negative = this < 0
    val absCents = kotlin.math.abs(this)
    val whole = absCents / 100
    val fraction = absCents % 100
    val wholeGrouped = groupThousands(whole)
    val sign = if (negative) "-" else if (showSign) "+" else ""
    return "$sign$ $wholeGrouped,${fraction.toString().padStart(2, '0')}"
}

private fun groupThousands(value: Long): String {
    val raw = value.toString()
    val builder = StringBuilder()
    for ((index, char) in raw.reversed().withIndex()) {
        if (index != 0 && index % 3 == 0) builder.append('.')
        builder.append(char)
    }
    return builder.reverse().toString()
}

/** Parses user-entered text (accepts either "," or "." as the decimal separator) into cents. */
fun String.parseAmountToCents(): Long? {
    val cleaned = trim().replace(",", ".")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return Math.round(value * 100)
}

fun Long.toEditableAmountString(): String {
    val whole = kotlin.math.abs(this) / 100
    val fraction = kotlin.math.abs(this) % 100
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0')}"
}
