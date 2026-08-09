package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateDownloader(private val context: Context) {

    fun downloadUpdate(url: String, version: String): Flow<DownloadState> = callbackFlow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "MIC-Rhema-v$version.apk"
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Atualizando MIC Rhema")
            setDescription("Baixando nova versão...")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        var downloadId: Long = -1
        try {
            downloadId = downloadManager.enqueue(request)
            trySend(DownloadState.Downloading(0))
        } catch (e: Exception) {
            trySend(DownloadState.Error("Falha ao iniciar download: ${e.localizedMessage}"))
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            trySend(DownloadState.Downloaded)
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                            trySend(DownloadState.Error("Falha no download (código: $reason)"))
                        }
                    }
                    cursor?.close()
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Loop para checar progresso periodicamente
        var isRunning = true
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (isRunning) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getInt(bytesDownloadedIndex) else 0
                    val bytesTotal = if (bytesTotalIndex >= 0) cursor.getInt(bytesTotalIndex) else 1
                    val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        isRunning = false
                    } else if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                        trySend(DownloadState.Downloading(progress))
                    }
                }
                cursor?.close()
                delay(500)
            }
        }

        awaitClose {
            isRunning = false
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun installApk(version: String) {
        val fileName = "MIC-Rhema-v$version.apk"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return

        if (!file.name.endsWith(".apk")) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        context.startActivity(intent)
    }
}
