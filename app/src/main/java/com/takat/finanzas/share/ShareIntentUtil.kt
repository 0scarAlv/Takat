package com.takat.finanzas.share

import android.content.Intent
import android.net.Uri
import android.os.Build

/** Extracts the shared content:// URIs from an incoming ACTION_SEND / ACTION_SEND_MULTIPLE intent, if any. */
object ShareIntentUtil {
    fun extractUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtraUri(Intent.EXTRA_STREAM)?.let { listOf(it) } ?: emptyList()
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableExtraUriList(Intent.EXTRA_STREAM)
            else -> emptyList()
        }
    }

    private fun Intent.getParcelableExtraUri(name: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(name)
        }

    private fun Intent.getParcelableExtraUriList(name: String): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(name, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(name)
        }.orEmpty()
}
