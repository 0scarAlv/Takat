package com.takat.finanzas.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.takat.finanzas.util.DebugLog

/** Called after any data mutation that could change the totals shown on the home screen widget. */
object WidgetUpdater {
    suspend fun refresh(context: Context) {
        DebugLog.log("WidgetUpdater.refresh")
        TakatWidget().updateAll(context)
        TakatDailyBudgetWidget().updateAll(context)
    }
}
