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

        return try {
            val hintedMime = mimeTypeHint.orEmpty().lowercase().substringBefore(';')
            val detectedMime = context.contentResolver.getType(uri).orEmpty().lowercase().substringBefore(';')
            val mimeType = if (hintedMime.isBlank() || hintedMime == "*/*" || hintedMime.endsWith("/*")) {
                detectedMime
            } else {
                hintedMime
            }

            if (path == "uploads" && mimeType == "application/pdf" && !targetUid.isNullOrBlank()) {
                StorageManager.uploadChurchDocument(context, uri, targetUid, onProgress).storagePath
            } else {
                val uid = targetUid ?: StorageManager.resolveStorageTargetUid()
                StorageManager.uploadMediaAsset(context, uri, uid, onProgress).let { result ->
                    result.signedUrl.ifBlank { result.storagePath }
                }
            }
        } catch (error: Exception) {
            android.util.Log.e("StorageHelper", "Falha no upload Supabase", error)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                android.widget.Toast.makeText(
                    context,
                    "Não foi possível enviar o arquivo: ${error.message ?: "verifique a sessão ADM e a conexão"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            ""
        }
    }
}
