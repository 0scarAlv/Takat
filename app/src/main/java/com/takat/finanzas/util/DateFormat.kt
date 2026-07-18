package com.takat.finanzas.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val spanishLocale = Locale.Builder().setLanguage("es").setRegion("ES").build()

private val displayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanishLocale)
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", spanishLocale)

fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(displayFormatter)

fun Long.toCsvDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun String.csvDateToMillis(): Long? = try {
    LocalDateTime.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}

/** Epoch millis range [start, end) covering the given calendar month in the device's time zone. */
fun monthRange(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

fun monthLabel(month: YearMonth): String =
    month.atDay(1).format(monthLabelFormatter).replaceFirstChar { it.uppercase() }
