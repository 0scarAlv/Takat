package com.takat.finanzas.network

import com.takat.finanzas.network.dto.GithubRelease
import com.takat.finanzas.util.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String?,
    val downloadUrl: String,
    val assetName: String
)

/**
 * Takat isn't distributed through Play Store or any other app store, so this is the only update
 * path users get: check the latest GitHub Release of 0scarAlv/Takat and compare its tag against
 * the installed version. Every release since v1.2.0 has shipped with the signed APK attached as a
 * release asset (see release workflow notes), so no extra publishing step is needed beyond what
 * already happens — this just reads it.
 */
object UpdateChecker {
    private const val RELEASES_URL = "https://api.github.com/repos/0scarAlv/Takat/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns null on any failure (offline, rate-limited, no APK asset) or if already up to date — never throws. */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val release = json.decodeFromString<GithubRelease>(body)
            val remoteVersion = release.tagName.removePrefix("v")
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@withContext null
            if (!isNewer(remoteVersion, currentVersionName)) return@withContext null

            UpdateInfo(
                versionName = remoteVersion,
                releaseNotes = release.body,
                downloadUrl = apkAsset.browserDownloadUrl,
                assetName = apkAsset.name
            )
        } catch (e: Exception) {
            DebugLog.log("UpdateChecker: check failed: ${e.message}")
            null
        }
    }

    /** True if [remote] is a strictly higher dotted version than [local] (e.g. "1.14.0" > "1.13.0"). */
    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
