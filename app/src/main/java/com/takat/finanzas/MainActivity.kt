package com.takat.finanzas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.takat.finanzas.ui.navigation.TakatNavGraph
import com.takat.finanzas.ui.theme.TakatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TakatTheme {
                TakatNavGraph()
            }
        }
    }
}
