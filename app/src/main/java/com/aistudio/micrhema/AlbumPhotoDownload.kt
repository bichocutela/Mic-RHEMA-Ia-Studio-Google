package com.aistudio.micrhema

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Salva a imagem do álbum na galeria, sem pedir permissões de armazenamento no Android 10+. */
suspend fun downloadAlbumPhoto(
    context: Context,
    photoUrl: String,
    destination: Uri? = null
): Boolean = withContext(Dispatchers.IO) {
    var createdUri: Uri? = null
    try {
        if (photoUrl.isBlank()) return@withContext false
        val connection = URL(photoUrl).openConnection().apply {
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("User-Agent", "MIC-Rhema-Android")
        }
        if (connection is HttpURLConnection) {
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode !in 200..299) return@withContext false
        }
        val mime = connection.contentType.orEmpty().substringBefore(";").lowercase()
            .takeIf { it in listOf("image/jpeg", "image/png", "image/webp", "image/gif") }
            ?: "image/jpeg"
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val resolver = context.contentResolver
        val targetUri = if (destination != null) destination else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "MIC-Rhema-${System.currentTimeMillis()}.$extension")
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MIC Rhema")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?.also { createdUri = it } ?: return@withContext false
        }
        connection.getInputStream().use { input ->
            resolver.openOutputStream(targetUri, "w")?.use { output ->
                input.copyTo(output)
            } ?: throw IllegalStateException("Não foi possível escrever a imagem")
        }
        if (createdUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(targetUri, ready, null, null)
        }
        true
    } catch (_: Exception) {
        createdUri?.let { context.contentResolver.delete(it, null, null) }
        false
    }
}
