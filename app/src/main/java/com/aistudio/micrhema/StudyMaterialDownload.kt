package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * Download reutilizável para materiais de estudo em PDF.
 * Aceita URLs públicas do Supabase, links do Google Drive e URLs HTTP(S) comuns.
 */
object StudyMaterialDownload {
    fun enqueuePdf(context: Context, sourceUrl: String, title: String) {
        val resolvedUrl = convertGoogleDriveUrl(sourceUrl.trim())
        if (!resolvedUrl.startsWith("http://", ignoreCase = true) &&
            !resolvedUrl.startsWith("https://", ignoreCase = true)
        ) {
            Toast.makeText(context, "O PDF não possui um link válido para download.", Toast.LENGTH_LONG).show()
            return
        }

        val safeBaseName = title
            .trim()
            .ifBlank { "conteudo-para-estudo" }
            .replace(Regex("[^\\p{L}\\p{N}._ -]+"), "")
            .replace(Regex("\\s+"), " ")
            .take(80)
            .trim()
            .ifBlank { "conteudo-para-estudo" }
        val fileName = if (safeBaseName.endsWith(".pdf", ignoreCase = true)) safeBaseName else "$safeBaseName.pdf"

        runCatching {
            val request = DownloadManager.Request(Uri.parse(resolvedUrl))
                .setTitle(fileName)
                .setDescription("Conteúdo para estudo — MIC Rhema")
                .setMimeType("application/pdf")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onSuccess {
            Toast.makeText(context, "Download do PDF iniciado.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Não foi possível iniciar o download do PDF.", Toast.LENGTH_LONG).show()
        }
    }
}
