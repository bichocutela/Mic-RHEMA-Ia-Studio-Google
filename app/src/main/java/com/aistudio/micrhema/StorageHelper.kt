package com.aistudio.micrhema

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth

/**
 * Ponto de compatibilidade dos campos de upload existentes.
 * Certificados IBR usam Supabase Storage; mídia legada continua no fluxo antigo até sua migração própria.
 */
object StorageHelper {
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        path: String,
        onProgress: ((Float) -> Unit)? = null,
        targetUid: String? = null
    ): String? {
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()

        if (path == "uploads") {
            val uid = targetUid ?: FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("Sua sessão expirou. Entre novamente para enviar o certificado.")
            return StorageManager.uploadChurchDocument(context, uri, uid, onProgress).storagePath
        }

        // Caminhos de mídia ainda usam o helper legado enquanto não há buckets públicos próprios.
        return StorageManager.uploadFile(context, uri, path, onProgress).takeIf { it.isNotBlank() }
    }
}
