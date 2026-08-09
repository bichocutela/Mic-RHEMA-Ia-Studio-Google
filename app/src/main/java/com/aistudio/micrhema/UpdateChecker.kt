package com.aistudio.micrhema

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String?,
    val releaseNotes: String,
    val updateAvailable: Boolean
)

sealed class UpdateResult {
    data class Success(val info: UpdateInfo) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/bichocutela/Mic-RHEMA-Ia-Studio-Google/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateResult.Error("Erro na resposta do servidor: ${response.code}")
            }

            val responseBody = response.body?.string() ?: return@withContext UpdateResult.Error("Resposta vazia")
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val assets = json.optJSONArray("assets")
            
            var downloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i)
                    val assetName = asset?.optString("name", "") ?: ""
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = asset?.optString("browser_download_url")
                        break
                    }
                }
            }

            val latestVersionStr = tagName.removePrefix("v").trim()
            val updateAvailable = isNewerVersion(currentVersion, latestVersionStr)

            UpdateResult.Success(
                UpdateInfo(
                    latestVersion = latestVersionStr,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    updateAvailable = updateAvailable
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateResult.Error("Erro ao verificar atualizações: ${e.message}")
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
