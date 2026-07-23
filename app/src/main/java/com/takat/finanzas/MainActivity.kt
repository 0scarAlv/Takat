package com.takat.finanzas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.takat.finanzas.ui.navigation.TakatNavGraph
import com.takat.finanzas.ui.theme.TakatTheme
import com.takat.finanzas.widget.WidgetActions

class MainActivity : ComponentActivity() {
    private val pendingWidgetAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetAction.value = intent?.getStringExtra(WidgetActions.ACTION_KEY)
        setContent {
            TakatTheme {
                TakatNavGraph(pendingWidgetAction = pendingWidgetAction)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction.value = intent.getStringExtra(WidgetActions.ACTION_KEY)
    }
}
