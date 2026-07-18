package com.takat.finanzas.util

fun encodeCsvField(value: String): String {
    val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
}

/** Parses a single CSV line into its fields, respecting quoted fields with embedded commas/quotes. */
fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString())
                current.setLength(0)
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString())
    return fields
}

/** Signed decimal string with 2 fixed decimals, e.g. "-71.73". Same rounding as [parseAmountToCents]. */
fun Long.toCsvAmount(): String {
    val negative = this < 0
    val absCents = kotlin.math.abs(this)
    val whole = absCents / 100
    val fraction = absCents % 100
    val sign = if (negative) "-" else ""
    return "$sign$whole.${fraction.toString().padStart(2, '0')}"
}

fun String.csvAmountToCents(): Long? {
    val value = trim().toDoubleOrNull() ?: return null
    return Math.round(value * 100)
}
