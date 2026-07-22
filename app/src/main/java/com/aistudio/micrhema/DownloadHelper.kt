package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object DownloadHelper {
    fun downloadFile(context: Context, url: String, title: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setDescription("Baixando conteúdo para acesso offline")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
