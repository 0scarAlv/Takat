package com.takat.finanzas.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.takat.finanzas.MainActivity
import com.takat.finanzas.R

private val QuickActionsActionKey = ActionParameters.Key<String>(WidgetActions.ACTION_KEY)
private val QuickActionsBackground = ColorProvider(day = Color(0xFFCBD5E1), night = Color(0xFF1E293B))
private val QuickActionsDivider = ColorProvider(day = Color(0x1A0F172A), night = Color(0x1AF8FAFC))
private val QuickActionsIconColor = ColorProvider(day = Color(0xFF334155), night = Color(0xFFCBD5E1))

/** Standalone widget: just 3 quick-action buttons, no balances shown. Separate from [TakatWidget]. */
class TakatQuickActionsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(QuickActionsBackground)
                    .cornerRadius(16.dp)
            ) {
                QuickActionIcon(
                    iconRes = R.drawable.ic_widget_transaction,
                    contentDescription = "Nuevo movimiento",
                    onClick = actionStartActivity<MainActivity>(
                        actionParametersOf(QuickActionsActionKey to WidgetActions.ACTION_NEW_TRANSACTION)
                    )
                )
                Divider()
                QuickActionIcon(
                    iconRes = R.drawable.ic_widget_open,
                    contentDescription = "Abrir Takat",
                    onClick = actionStartActivity<MainActivity>()
                )
                Divider()
                QuickActionIcon(
                    iconRes = R.drawable.ic_widget_transfer,
                    contentDescription = "Nueva transferencia",
                    onClick = actionStartActivity<MainActivity>(
                        actionParametersOf(QuickActionsActionKey to WidgetActions.ACTION_NEW_TRANSFER)
                    )
                )
            }
        }
    }
}

@Composable
private fun RowScope.Divider() {
    Box(
        modifier = GlanceModifier
            .fillMaxHeight()
            .width(1.dp)
            .background(QuickActionsDivider)
    ) {}
}

@Composable
private fun RowScope.QuickActionIcon(iconRes: Int, contentDescription: String, onClick: Action) {
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(26.dp),
            colorFilter = ColorFilter.tint(QuickActionsIconColor)
        )
    }
}
