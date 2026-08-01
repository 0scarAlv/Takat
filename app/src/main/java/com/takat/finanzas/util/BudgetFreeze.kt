package com.takat.finanzas.util

import com.takat.finanzas.data.entity.BudgetSettingsEntity
import java.time.LocalDate

data class BudgetFreezeResult(
    val lastLiveValueCents: Long,
    val lastLiveValueEpochDay: Long,
    val frozenBudgetCents: Long,
    val frozenBudgetEpochDay: Long,
    val changed: Boolean
)

/**
 * Decides whether the static "presupuesto diario" needs to roll over to a new day. When it does, the new
 * frozen value is whatever live value was last observed on a prior day (not today) — the snapshot taken
 * "at midnight" is really just the last live figure recorded before the day changed. Falls back to the
 * current live value when there's no valid prior-day observation (sentinel epoch day 0, or first run).
 */
fun computeBudgetFreeze(settings: BudgetSettingsEntity, liveValueCents: Long, today: LocalDate): BudgetFreezeResult {
    val todayEpochDay = today.toEpochDay()
    val frozenIsStale = settings.frozenBudgetEpochDay != todayEpochDay
    val newFrozenCents = when {
        !frozenIsStale -> settings.frozenBudgetCents
        settings.lastLiveValueEpochDay in 1 until todayEpochDay -> settings.lastLiveValueCents
        else -> liveValueCents
    }
    val newFrozenEpochDay = if (frozenIsStale) todayEpochDay else settings.frozenBudgetEpochDay
    val changed = frozenIsStale ||
        settings.lastLiveValueCents != liveValueCents ||
        settings.lastLiveValueEpochDay != todayEpochDay
    return BudgetFreezeResult(
        lastLiveValueCents = liveValueCents,
        lastLiveValueEpochDay = todayEpochDay,
        frozenBudgetCents = newFrozenCents,
        frozenBudgetEpochDay = newFrozenEpochDay,
        changed = changed
    )
}
