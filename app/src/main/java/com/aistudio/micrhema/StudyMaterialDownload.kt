package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * Download reutilizável para materiais de estudo em PDF.
 * Aceita URLs públicas do Supabase, links do Google Drive e URLs HTTP(S) comuns.
 */
object StudyMaterialDownload {
    private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    fun enqueuePdf(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "pdf", "application/pdf", "PDF")
    }

    fun enqueueDocx(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "docx", DOCX_MIME, "Word")
    }

    fun openDocument(context: Context, sourceUrl: String, label: String) {
        val resolvedUrl = convertGoogleDriveUrl(sourceUrl.trim())
        if (!resolvedUrl.startsWith("http://", ignoreCase = true) &&
            !resolvedUrl.startsWith("https://", ignoreCase = true)
        ) {
            Toast.makeText(context, "O $label não possui um link válido para abrir.", Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, "Não foi possível abrir o $label neste aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    private fun enqueue(
        context: Context,
        sourceUrl: String,
        title: String,
        extension: String,
        mimeType: String,
        label: String
    ) {
        val resolvedUrl = convertGoogleDriveUrl(sourceUrl.trim())
        if (!resolvedUrl.startsWith("http://", ignoreCase = true) &&
            !resolvedUrl.startsWith("https://", ignoreCase = true)
        ) {
            Toast.makeText(context, "O arquivo $label não possui um link válido para download.", Toast.LENGTH_LONG).show()
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
        val fileName = if (safeBaseName.endsWith(".$extension", ignoreCase = true)) safeBaseName else "$safeBaseName.$extension"

        runCatching {
            val request = DownloadManager.Request(Uri.parse(resolvedUrl))
                .setTitle(fileName)
                .setDescription("Conteúdo para estudo — MIC Rhema")
                .setMimeType(mimeType)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onSuccess {
            Toast.makeText(context, "Download do arquivo $label iniciado.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Não foi possível iniciar o download do arquivo $label.", Toast.LENGTH_LONG).show()
        }
    }
}
