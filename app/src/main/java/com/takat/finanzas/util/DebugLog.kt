package com.takat.finanzas.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Local, opt-in diagnostic log. Off by default and writes nothing until the user turns on
 * "Grabar registro de depuración" in Ajustes. Meant for cases like a bug that only reproduces on
 * someone else's phone (e.g. the widget not refreshing at midnight): they start the recording,
 * let the problem happen again, then share the resulting file so it can be read here without
 * needing to plug that phone into this machine.
 */
object DebugLog {
    private const val PREFS_NAME = "takat_debug_prefs"
    private const val KEY_ENABLED = "logging_enabled"

    /** Once the log file passes this size it's trimmed, so a forgotten recording can't grow forever. */
    private const val MAX_BYTES = 512 * 1024L

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val fileNameFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val lock = ReentrantLock()
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val isEnabled: Boolean
        get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) log("--- registro iniciado ---") else log("--- registro detenido ---")
    }

    fun log(message: String) {
        if (!::appContext.isInitialized || !isEnabled) return
        val line = "${LocalDateTime.now().format(timestampFormat)}  $message\n"
        lock.withLock {
            try {
                val file = logFile()
                if (file.length() > MAX_BYTES) trim(file)
                file.appendText(line)
            } catch (_: Exception) {
                // Logging must never crash the app it's trying to help diagnose.
            }
        }
    }

    fun clear() {
        lock.withLock { runCatching { logFile().writeText("") } }
    }

    /** Copies the current log into the FileProvider-shared cache dir and returns a content:// Uri, or null if empty. */
    fun prepareShareFile(context: Context): Uri? {
        val source = logFile()
        if (!source.exists() || source.length() == 0L) return null
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val dest = File(dir, "takat_log_${LocalDateTime.now().format(fileNameFormat)}.txt")
        source.copyTo(dest, overwrite = true)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
    }

    private fun logFile(): File = File(appContext.filesDir, "debug_log.txt").also {
        if (!it.exists()) it.createNewFile()
    }

    /** Drops the oldest third of the lines, keeping the file bounded without losing the most recent activity. */
    private fun trim(file: File) {
        val lines = file.readLines()
        file.writeText(lines.drop(lines.size / 3).joinToString("\n", postfix = "\n"))
    }
}
