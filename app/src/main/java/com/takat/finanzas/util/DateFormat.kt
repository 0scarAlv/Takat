package com.takat.finanzas.util

import com.takat.finanzas.data.entity.BudgetPeriodType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
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

fun LocalDate.toDisplayDate(): String = format(displayFormatter)

/** Earliest QUINCENA/MES payment date on or after [today]; falls back to month-end when [dayOfMonth] doesn't exist in a given month. */
fun nextPaymentDate(today: LocalDate, periodType: BudgetPeriodType, dayOfMonth: Int): LocalDate {
    val month = YearMonth.from(today)
    return when (periodType) {
        BudgetPeriodType.QUINCENA -> {
            val fifteenth = month.atDay(15)
            if (today <= fifteenth) fifteenth else month.atEndOfMonth()
        }
        BudgetPeriodType.MES -> {
            val candidate = month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            if (candidate >= today) {
                candidate
            } else {
                val next = month.plusMonths(1)
                next.atDay(dayOfMonth.coerceAtMost(next.lengthOfMonth()))
            }
        }
    }
}

fun daysRemaining(today: LocalDate, nextPaymentDate: LocalDate): Long =
    ChronoUnit.DAYS.between(today, nextPaymentDate)
