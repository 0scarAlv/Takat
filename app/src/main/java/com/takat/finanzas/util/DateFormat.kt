package com.takat.finanzas.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanishLocale = Locale.Builder().setLanguage("es").setRegion("ES").build()

private val displayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanishLocale)
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", spanishLocale)

fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(displayFormatter)

/** Epoch millis range [start, end) covering the current calendar month in the device's time zone. */
fun currentMonthRange(): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val month = YearMonth.now(zone)
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

fun currentMonthLabel(): String =
    YearMonth.now(ZoneId.systemDefault()).atDay(1).format(monthLabelFormatter)
        .replaceFirstChar { it.uppercase() }
