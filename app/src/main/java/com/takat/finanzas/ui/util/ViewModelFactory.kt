package com.takat.finanzas.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.data.repository.FinanceRepository

class LambdaViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}

@Composable
fun rememberRepository(): FinanceRepository {
    val context = LocalContext.current
    return (context.applicationContext as TakatApplication).repository
}
