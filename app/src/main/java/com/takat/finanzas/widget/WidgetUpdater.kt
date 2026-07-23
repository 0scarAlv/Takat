package com.takat.finanzas.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/** Called after any data mutation that could change the totals shown on the home screen widget. */
object WidgetUpdater {
    suspend fun refresh(context: Context) {
        TakatWidget().updateAll(context)
    }
}
