package com.takat.finanzas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.takat.finanzas.data.entity.ThemeMode
import com.takat.finanzas.notifications.NotificationHelper
import com.takat.finanzas.share.ShareIntentUtil
import com.takat.finanzas.ui.components.WhatsNewGate
import com.takat.finanzas.ui.navigation.TakatNavGraph
import com.takat.finanzas.ui.theme.TakatTheme
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.widget.WidgetActions

class MainActivity : ComponentActivity() {
    private val pendingWidgetAction = mutableStateOf<String?>(null)
    private val pendingFixedExpenseId = mutableStateOf<Long?>(null)
    private val pendingShareUris = mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetAction.value = intent?.getStringExtra(WidgetActions.ACTION_KEY)
        pendingFixedExpenseId.value = readFixedExpenseId(intent)
        pendingShareUris.value = ShareIntentUtil.extractUris(intent)
        setContent {
            val repository = rememberRepository()
            val appSettings by repository.appSettings().collectAsState(initial = null)
            val darkTheme = when (appSettings?.themeMode ?: ThemeMode.SYSTEM) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            TakatTheme(darkTheme = darkTheme) {
                TakatNavGraph(
                    pendingWidgetAction = pendingWidgetAction,
                    pendingFixedExpenseId = pendingFixedExpenseId,
                    pendingShareUris = pendingShareUris
                )
                WhatsNewGate(repository)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction.value = intent.getStringExtra(WidgetActions.ACTION_KEY)
        pendingFixedExpenseId.value = readFixedExpenseId(intent)
        pendingShareUris.value = ShareIntentUtil.extractUris(intent)
    }

    private fun readFixedExpenseId(intent: Intent?): Long? {
        val id = intent?.getLongExtra(NotificationHelper.EXTRA_FIXED_EXPENSE_ID, -1L) ?: -1L
        return id.takeIf { it >= 0 }
    }
}
