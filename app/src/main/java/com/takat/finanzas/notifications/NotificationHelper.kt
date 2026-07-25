package com.takat.finanzas.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.takat.finanzas.MainActivity
import com.takat.finanzas.R
import com.takat.finanzas.util.centsToDisplay

object NotificationHelper {
    const val CHANNEL_ID = "fixed_expenses"
    const val EXTRA_FIXED_EXPENSE_ID = "fixed_expense_id"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gastos fijos",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos de gastos fijos pendientes de pago" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyDue(context: Context, fixedExpenseId: Long, name: String, amountCents: Long) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FIXED_EXPENSE_ID, fixedExpenseId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            fixedExpenseId.toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_transaction)
            .setContentTitle("Gasto fijo pendiente")
            .setContentText("$name · ${amountCents.centsToDisplay()}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(fixedExpenseId.toInt(), notification)
    }
}
