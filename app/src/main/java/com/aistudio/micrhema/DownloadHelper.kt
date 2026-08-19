package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.util.concurrent.TimeUnit

/** Centraliza o destino de conteúdo offline e aplica as preferências de download do usuário. */
object DownloadHelper {
    private const val APP_DOWNLOADS_FOLDER = "micrhema"
    private val oldDownloadAgeMillis = TimeUnit.DAYS.toMillis(30)

    fun getDownloadDirectory(context: Context, storageFolder: String = currentSettingsState.value.storageFolder): File {
        val candidates = context.getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS).filterNotNull()
        val preferred = if (storageFolder == "SD Card" && candidates.size > 1) {
            candidates[1]
        } else {
            candidates.firstOrNull() ?: context.filesDir
        }
        return File(preferred, APP_DOWNLOADS_FOLDER).apply { if (!exists()) mkdirs() }
    }

    fun clearDownloads(context: Context, storageFolder: String = currentSettingsState.value.storageFolder): Boolean {
        val directory = getDownloadDirectory(context, storageFolder)
        return !directory.exists() || directory.deleteRecursively()
    }

    fun cleanOldDownloads(context: Context, storageFolder: String = currentSettingsState.value.storageFolder) {
        val cutoff = System.currentTimeMillis() - oldDownloadAgeMillis
        getDownloadDirectory(context, storageFolder)
            .listFiles()
            ?.filter { it.isFile && it.lastModified() in 1 until cutoff }
            ?.forEach { it.delete() }
    }

    fun downloadFile(context: Context, url: String, title: String, fileName: String) {
        val settings = currentSettingsState.value
        if (settings.autoCleanOldDownloads) cleanOldDownloads(context, settings.storageFolder)

        val target = File(getDownloadDirectory(context, settings.storageFolder), fileName)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("Baixando conteúdo para acesso offline")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(target))
            .setAllowedOverMetered(!settings.wifiOnlyDownloads)
            .setAllowedOverRoaming(!settings.wifiOnlyDownloads)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
