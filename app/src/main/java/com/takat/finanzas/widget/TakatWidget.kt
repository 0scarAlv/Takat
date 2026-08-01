package com.takat.finanzas.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.Action
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.takat.finanzas.MainActivity
import com.takat.finanzas.R
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.theme.NegativeRedDark
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.ui.theme.PositiveGreenDark
import com.takat.finanzas.util.centsToDisplay
import kotlinx.coroutines.flow.first

private val ActionKey = ActionParameters.Key<String>(WidgetActions.ACTION_KEY)
private val WidgetBackground = ColorProvider(day = Color(0xFFCBD5E1), night = Color(0xFF1E293B))
private val LabelColor = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
private val DividerColor = ColorProvider(day = Color(0x1A0F172A), night = Color(0x1AF8FAFC))
private val ActionIconColor = ColorProvider(day = Color(0xFF334155), night = Color(0xFFCBD5E1))

class TakatWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as TakatApplication).repository
        val totals = repository.accountTotals().first()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetBackground)
                    .cornerRadius(16.dp)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    StatItem("Líquido", totals.availableCents.centsToDisplay(), amountColor(totals.availableCents))
                    StatItem("Deuda", totals.debtCents.centsToDisplay(), amountColor(-totals.debtCents))
                    StatItem("Actual", totals.capitalCents.centsToDisplay(), amountColor(totals.capitalCents))
                }
                Spacer(GlanceModifier.height(12.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DividerColor)
                ) {}
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    ActionIcon(
                        iconRes = R.drawable.ic_widget_transaction,
                        contentDescription = "Nuevo movimiento",
                        onClick = actionStartActivity<MainActivity>(
                            actionParametersOf(ActionKey to WidgetActions.ACTION_NEW_TRANSACTION)
                        )
                    )
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(DividerColor)
                    ) {}
                    ActionIcon(
                        iconRes = R.drawable.ic_widget_transfer,
                        contentDescription = "Nueva transferencia",
                        onClick = actionStartActivity<MainActivity>(
                            actionParametersOf(ActionKey to WidgetActions.ACTION_NEW_TRANSFER)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionIcon(iconRes: Int, contentDescription: String, onClick: Action) {
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
            colorFilter = ColorFilter.tint(ActionIconColor)
        )
    }
}

@Composable
private fun RowScope.StatItem(label: String, value: String, valueColor: ColorProvider) {
    Column(
        modifier = GlanceModifier.defaultWeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = TextStyle(fontSize = 13.sp, color = LabelColor, textAlign = TextAlign.Center))
        Text(value, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = valueColor, textAlign = TextAlign.Center))
    }
}

private fun amountColor(cents: Long): ColorProvider =
    if (cents < 0) {
        ColorProvider(day = NegativeRed, night = NegativeRedDark)
    } else {
        ColorProvider(day = PositiveGreen, night = PositiveGreenDark)
    }
