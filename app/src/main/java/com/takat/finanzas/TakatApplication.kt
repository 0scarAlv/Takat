package com.takat.finanzas

import android.app.Application
import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.attachment.AttachmentStorage
import com.takat.finanzas.data.repository.FinanceRepository

class TakatApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FinanceRepository(AppDatabase.getInstance(this), AttachmentStorage(this), this)
    }
}
