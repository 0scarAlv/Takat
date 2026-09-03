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

enum class FixedExpenseNotificationKind { UPCOMING, DUE, OVERDUE }

object NotificationHelper {
    const val CHANNEL_ID = "fixed_expenses"
    const val EXTRA_FIXED_EXPENSE_ID = "fixed_expense_id"

    const val PC_ACCESS_CHANNEL_ID = "pc_access"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gastos fijos",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos de gastos fijos pendientes de pago" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val pcAccessChannel = NotificationChannel(
            PC_ACCESS_CHANNEL_ID,
            "Acceso desde PC",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Aviso mientras el panel web para PC está activo" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(pcAccessChannel)
    }

    fun notify(context: Context, kind: FixedExpenseNotificationKind, fixedExpenseId: Long, name: String, amountCents: Long) {
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
        val amount = amountCents.centsToDisplay()
        val (title, text) = when (kind) {
            FixedExpenseNotificationKind.UPCOMING -> "Gasto fijo mañana" to "$name se cobra mañana · $amount"
            FixedExpenseNotificationKind.DUE -> "Gasto fijo pendiente" to "$name · $amount"
            FixedExpenseNotificationKind.OVERDUE -> "Gasto fijo sin pagar" to "$name sigue sin pagarse · $amount"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(fixedExpenseId.toInt(), notification)
    }
}
