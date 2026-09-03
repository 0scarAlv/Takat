package com.takat.finanzas.network

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.takat.finanzas.R
import com.takat.finanzas.TakatApplication
import com.takat.finanzas.notifications.NotificationHelper
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps [LocalApiServer] alive while "Acceso desde PC" is toggled on in Settings. Only runs while
 * the user explicitly enabled the toggle — never started implicitly on app launch.
 */
class PcAccessForegroundService : Service() {

    private lateinit var apiServer: LocalApiServer
    private lateinit var mdnsAdvertiser: MdnsAdvertiser
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val app = application as TakatApplication
        apiServer = LocalApiServer(applicationContext, app.repository, app.pairingManager)
        mdnsAdvertiser = MdnsAdvertiser(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DebugLog.log("PcAccessForegroundService: starting")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        apiServer.start()
        scope.launch {
            val nickname = (application as TakatApplication).repository.appSettings().first()?.pcAccessNickname
            mdnsAdvertiser.start(nickname, LocalApiServer.DEFAULT_PORT)
        }
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        DebugLog.log("PcAccessForegroundService: stopping")
        apiServer.stop()
        mdnsAdvertiser.stop()
        scope.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() =
        NotificationCompat.Builder(this, NotificationHelper.PC_ACCESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Takat: acceso desde PC activo")
            .setContentText("Tu PC puede conectarse mientras esto esté activo")
            .setOngoing(true)
            .build()

    companion object {
        private const val NOTIFICATION_ID = 42

        /** In-process flag Settings reads to show the toggle's real state after navigating back. */
        var isRunning: Boolean = false
            private set
    }
}
