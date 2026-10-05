package com.aistudio.micrhema

import android.content.Context
import android.net.Uri

/** Ponto único para os campos de upload administrativos e certificados IBR. */
object StorageHelper {
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        path: String,
        targetUid: String? = null,
        mimeTypeHint: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): String {
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()

        val hintedMime = mimeTypeHint.orEmpty().lowercase().substringBefore(';')
        val detectedMime = context.contentResolver.getType(uri).orEmpty().lowercase().substringBefore(';')
        val displayName = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
                }
        }.getOrDefault("").orEmpty()
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val extensionMime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty().lowercase()
        if (detectedMime == "application/msword" || extension == "doc") {
            // Convert legacy uploads locally; the existing authenticated gateway already accepts DOCX.
            val converted = java.io.File.createTempFile("legacy_word_", ".docx", context.cacheDir)
            val original = java.io.File.createTempFile("legacy_word_", ".doc", context.cacheDir)
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        DocumentFiles.copy(it, original)
                    } ?: throw IllegalArgumentException("Não foi possível ler o documento Word.")
                    LegacyWordText.convertToDocx(original, converted)
                }
                val uid = targetUid ?: StorageManager.resolveStorageTargetUid(context)
                return StorageManager.uploadMediaAsset(
                    context, Uri.fromFile(converted), uid, onProgress,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ).let { it.signedUrl.ifBlank { it.storagePath } }
            } finally {
                converted.delete()
                original.delete()
            }
        }
        val resolvedMime = when {
            hintedMime.isNotBlank() && hintedMime != "*/*" && !hintedMime.endsWith("/*") -> hintedMime
            detectedMime.isNotBlank() -> detectedMime
            extensionMime.isNotBlank() -> extensionMime
            hintedMime == "image/*" -> "image/jpeg"
            hintedMime == "audio/*" -> "audio/mpeg"
            hintedMime == "video/*" -> "video/mp4"
            else -> ""
        }
        if (resolvedMime.isBlank()) {
            throw IllegalArgumentException("Não foi possível identificar o tipo do arquivo selecionado.")
        }

        return if (path == "uploads" && resolvedMime == "application/pdf" && !targetUid.isNullOrBlank()) {
            StorageManager.uploadChurchDocument(context, uri, targetUid, onProgress, resolvedMime).storagePath
        } else {
            val uid = targetUid ?: StorageManager.resolveStorageTargetUid(context)
            StorageManager.uploadMediaAsset(context, uri, uid, onProgress, resolvedMime).let { result ->
                result.signedUrl.ifBlank { result.storagePath }
            }
        }
    }
}
