package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * Download reutilizável para materiais de estudo.
 * Aceita URLs públicas do Supabase, links do Google Drive e URLs HTTP(S) comuns.
 */
object StudyMaterialDownload {
    private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private const val EPUB_MIME = "application/epub+zip"

    fun enqueuePdf(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "pdf", "application/pdf", "PDF")
    }

    fun enqueueDocx(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "docx", DOCX_MIME, "Word")
    }

    fun enqueueEpub(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "epub", EPUB_MIME, "EPUB")
    }

    /**
     * Mantém a assinatura antiga usada nas telas IBR, mas agora abre Word/EPUB dentro do MIC Rhema.
     */
    fun openDocument(context: Context, sourceUrl: String, label: String) {
        val clean = sourceUrl.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "O $label não possui um arquivo válido para abrir.", Toast.LENGTH_LONG).show()
            return
        }
        val normalized = "$label $clean".lowercase()
        val contentType = if (normalized.contains("epub")) "epub" else "word"
        documentReaderRequestState.value = DocumentReaderRequest(
            sourceUrl = clean,
            title = if (contentType == "epub") "Material EPUB" else "Material em Word",
            contentType = contentType
        )
    }

    fun openExternalDocument(context: Context, sourceUrl: String) {
        val clean = sourceUrl.trim()
        val resolvedUrl = if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) {
            convertGoogleDriveUrl(clean)
        } else clean
        if (resolvedUrl.isBlank()) {
            Toast.makeText(context, "O documento não possui um endereço válido.", Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            val uri = Uri.parse(resolvedUrl)
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        }.onFailure {
            Toast.makeText(context, "Não foi possível abrir o documento em outro aplicativo.", Toast.LENGTH_LONG).show()
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
