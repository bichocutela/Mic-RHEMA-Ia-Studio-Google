package com.aistudio.micrhema

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Resultado persistente do armazenamento remoto. O caminho é salvo no Firestore; a URL expira. */
data class StorageUploadResult(
    val bucket: String,
    val storagePath: String,
    val signedUrl: String,
    val expiresInSeconds: Long = 15 * 60L
)

object StorageManager {
    private const val PROFILE_BUCKET = "profile-photos"
    private const val DOCUMENT_BUCKET = "church-documents"
    private const val STORAGE_GATEWAY_FUNCTION = "storage-gateway"
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
    private const val MAX_DOCUMENT_BYTES = 50L * 1024L * 1024L

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun gatewayUrl(): String {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("Supabase não está configurado nesta versão do aplicativo.")
        }
        return "$baseUrl/functions/v1/$STORAGE_GATEWAY_FUNCTION"
    }

    private fun publishableKey(): String {
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (key.isBlank() || key == "sb_publishable_replace_me" || key == "dummy") {
            throw IllegalStateException("Chave pública do Supabase não configurada nesta versão do aplicativo.")
        }
        return key
    }

    private suspend fun firebaseIdToken(): String = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Não encontramos sua sessão do Firebase. Entre novamente para enviar arquivos.")

        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                val token = user.getIdToken(attempt == 1).await().token
                if (!token.isNullOrBlank()) return@withContext token
                lastError = IllegalStateException("O Firebase não retornou um token de sessão.")
            } catch (error: Exception) {
                lastError = error
                Log.w(
                    "StorageManager",
                    if (attempt == 0) "Token Firebase indisponível; tentando renovar automaticamente" else "Renovação do token Firebase falhou",
                    error
                )
            }
        }

        throw IllegalStateException(
            "Não foi possível renovar sua sessão para enviar a foto. Verifique sua conexão e tente novamente.",
            lastError
        )
    }

    private fun mimeType(context: Context, uri: Uri): String =
        context.contentResolver.getType(uri)?.lowercase()?.substringBefore(';') ?: ""

    private fun extensionForMime(mime: String): String = when (mime) {
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "application/pdf" -> ".pdf"
        else -> ""
    }

    private fun copyUriToCache(context: Context, uri: Uri, mime: String): File {
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw IllegalArgumentException("O arquivo selecionado não possui um URI válido.")
        }
        val extension = extensionForMime(mime)
        val target = File(context.cacheDir, "supabase_upload_${System.currentTimeMillis()}$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Não foi possível ler o arquivo selecionado.")
        if (!target.exists() || target.length() <= 0L) {
            target.delete()
            throw IllegalArgumentException("O arquivo selecionado está vazio.")
        }
        return target
    }

    private fun responseJson(response: okhttp3.Response): JSONObject {
        val body = response.body?.string().orEmpty()
        val json = runCatching { JSONObject(body) }.getOrElse {
            JSONObject().put("error", "Resposta inválida do armazenamento remoto.")
        }
        if (!response.isSuccessful) {
            throw IllegalStateException(json.optString("error").ifBlank { "Falha HTTP ${response.code} no armazenamento remoto." })
        }
        return json
    }

    private fun resultFromJson(json: JSONObject, fallbackBucket: String): StorageUploadResult {
        val path = json.optString("storage_path").trim()
        if (path.isBlank()) throw IllegalStateException("O Supabase não retornou o caminho do arquivo.")
        return StorageUploadResult(
            bucket = json.optString("bucket").ifBlank { fallbackBucket },
            storagePath = path,
            signedUrl = json.optString("signed_url").trim(),
            expiresInSeconds = json.optLong("expires_in", 15 * 60L)
        )
    }

    private suspend fun uploadToGateway(
        context: Context,
        uri: Uri,
        bucket: String,
        targetUid: String,
        maxBytes: Long,
        onProgress: ((Float) -> Unit)? = null
    ): StorageUploadResult = withContext(Dispatchers.IO) {
        val mime = mimeType(context, uri)
        val allowed = if (bucket == PROFILE_BUCKET) {
            setOf("image/jpeg", "image/png", "image/webp")
        } else {
            setOf("application/pdf")
        }
        if (mime !in allowed) {
            throw IllegalArgumentException(
                if (bucket == PROFILE_BUCKET) "Selecione uma imagem JPEG, PNG ou WebP."
                else "Selecione um arquivo PDF."
            )
        }

        val tempFile = copyUriToCache(context, uri, mime)
        try {
            if (tempFile.length() > maxBytes) {
                throw IllegalArgumentException("O arquivo excede o limite permitido para este tipo de documento.")
            }
            val token = firebaseIdToken()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("bucket", bucket)
                .addFormDataPart("targetUid", targetUid)
                .addFormDataPart("file", tempFile.name, tempFile.asRequestBody(mime.toMediaTypeOrNull()))
                .build()
            val request = Request.Builder()
                .url(gatewayUrl())
                .header("Authorization", "Bearer $token")
                .header("apikey", publishableKey())
                .post(body)
                .build()

            onProgress?.invoke(0.25f)
            client.newCall(request).execute().use { response ->
                onProgress?.invoke(0.85f)
                val result = resultFromJson(responseJson(response), bucket)
                onProgress?.invoke(1f)
                result
            }
        } finally {
            tempFile.delete()
        }
    }

    suspend fun uploadProfilePhoto(context: Context, uri: Uri, uid: String): StorageUploadResult {
        val result = uploadToGateway(context, uri, PROFILE_BUCKET, uid, MAX_IMAGE_BYTES)
        runCatching { saveProfilePhotoLocally(context, uri, uid) }
            .onFailure { Log.w("StorageManager", "Foto sincronizada, mas cache local não foi salvo", it) }
        return result
    }

    /** Compatibilidade de fonte para telas antigas; o retorno agora é um resultado com storage_path. */
    suspend fun uploadProfilePhotoToFirebase(context: Context, uri: Uri, uid: String): StorageUploadResult =
        uploadProfilePhoto(context, uri, uid)

    suspend fun uploadChurchDocument(
        context: Context,
        uri: Uri,
        uid: String,
        onProgress: ((Float) -> Unit)? = null
    ): StorageUploadResult = uploadToGateway(
        context = context,
        uri = uri,
        bucket = DOCUMENT_BUCKET,
        targetUid = uid,
        maxBytes = MAX_DOCUMENT_BYTES,
        onProgress = onProgress
    )

    suspend fun getSignedUrl(bucket: String, storagePath: String, targetUid: String): String = withContext(Dispatchers.IO) {
        val token = firebaseIdToken()
        val payload = JSONObject()
            .put("operation", "signed-url")
            .put("bucket", bucket)
            .put("storagePath", storagePath)
            .put("targetUid", targetUid)
        val request = Request.Builder()
            .url(gatewayUrl())
            .header("Authorization", "Bearer $token")
            .header("apikey", publishableKey())
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).execute().use { response ->
            responseJson(response).optString("signed_url").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("O Supabase não retornou uma URL assinada.")
        }
    }

    suspend fun downloadStorageFileToCache(
        context: Context,
        bucket: String,
        storagePath: String,
        targetUid: String
    ): File = withContext(Dispatchers.IO) {
        val signedUrl = getSignedUrl(bucket, storagePath, targetUid)
        val request = Request.Builder().url(signedUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Não foi possível baixar o certificado para anexá-lo ao e-mail.")
            }
            val body = response.body ?: throw IllegalStateException("O certificado não possui conteúdo para anexar.")
            if (body.contentLength() > MAX_DOCUMENT_BYTES) {
                throw IllegalStateException("O certificado excede o limite permitido para compartilhamento.")
            }
            val destination = File(context.cacheDir, "certificado_ibr_${targetUid}_${System.currentTimeMillis()}.pdf")
            body.byteStream().use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
                throw IllegalStateException("O certificado baixado está vazio.")
            }
            destination
        }
    }

    suspend fun deleteProfilePhoto(uid: String) = withContext(Dispatchers.IO) {
        val token = firebaseIdToken()
        val payload = JSONObject()
            .put("operation", "delete-profile")
            .put("bucket", PROFILE_BUCKET)
            .put("targetUid", uid)
        val request = Request.Builder()
            .url(gatewayUrl())
            .header("Authorization", "Bearer $token")
            .header("apikey", publishableKey())
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).execute().use { response -> responseJson(response) }
    }

    /** Nome mantido para compatibilidade, mas a operação já não usa Firebase Storage. */
    suspend fun deleteProfilePhotoFromFirebase(uid: String) = deleteProfilePhoto(uid)

    suspend fun saveProfilePhotoLocally(context: Context, uri: Uri, uid: String): String =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "profile_photos")
            if (!directory.exists()) directory.mkdirs()
            val destination = File(directory, "${uid}_profile.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: throw Exception("Não foi possível ler a foto selecionada.")
            if (!destination.exists() || destination.length() == 0L) {
                throw Exception("Não foi possível salvar o cache da foto no aparelho.")
            }
            Uri.fromFile(destination).toString()
        }

    fun getLocalProfilePhotoUri(context: Context, uid: String): String {
        val file = File(context.filesDir, "profile_photos/${uid}_profile.jpg")
        return if (file.exists() && file.length() > 0L) Uri.fromFile(file).toString() else ""
    }

    fun deleteLocalProfilePhoto(context: Context, uid: String) {
        File(context.filesDir, "profile_photos/${uid}_profile.jpg").delete()
    }

    /** Mantém uploads de mídia legados fora do escopo dos buckets privados de perfil/documentos. */
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        path: String,
        onProgress: ((Float) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = File(context.cacheDir, "legacy_upload_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return@withContext ""
            if (!tempFile.exists() || tempFile.length() == 0L) return@withContext ""

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = extensionForMime(mimeType).ifBlank {
                when {
                    mimeType.startsWith("video/") -> ".mp4"
                    mimeType.startsWith("audio/") -> ".mp3"
                    else -> ""
                }
            }
            val uploadFile = if (extension.isNotEmpty()) {
                File(tempFile.absolutePath + extension).also { tempFile.renameTo(it) }
            } else tempFile
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("fileToUpload", uploadFile.name, uploadFile.asRequestBody(mimeType.toMediaTypeOrNull()))
                .build()
            onProgress?.invoke(0.3f)
            val request = Request.Builder().url("https://catbox.moe/user/api.php").post(requestBody).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    onProgress?.invoke(1f)
                    body
                } else ""
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Upload legado falhou", e)
            ""
        } finally {
            tempFile?.delete()
        }
    }
}
