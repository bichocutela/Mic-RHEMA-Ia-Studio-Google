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

class StorageGatewayException(
    val httpCode: Int,
    val reasonCode: String?,
    message: String
) : IllegalStateException(message)

object StorageManager {
    private const val PROFILE_BUCKET = "profile-photos"
    private const val DOCUMENT_BUCKET = "church-documents"
    private const val MEDIA_BUCKET = "media-assets"
    private const val STORAGE_GATEWAY_FUNCTION = "storage-gateway"
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
    private const val MAX_DOCUMENT_BYTES = 50L * 1024L * 1024L
    private const val MAX_MEDIA_BYTES = 50L * 1024L * 1024L

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

    private fun Request.Builder.withAdminAuthorization(): Request.Builder {
        if (adminAuthenticatedState.value) {
            header("X-Rhema-Admin-Password", "igreja10")
        }
        return this
    }

    private suspend fun ensureFirebaseUser(context: Context? = null): com.google.firebase.auth.FirebaseUser = withContext(Dispatchers.IO) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: runCatching { auth.signInAnonymously().await().user }.getOrNull()
        user ?: throw IllegalStateException("Não foi possível preparar a sessão Firebase para o armazenamento.")
    }

    suspend fun resolveStorageTargetUid(context: Context? = null): String = withContext(Dispatchers.IO) {
        val user = ensureFirebaseUser(context)
        if (!adminAuthenticatedState.value) {
            val memberContext = context
                ?: throw IllegalStateException("O contexto do perfil não foi disponibilizado para vincular a sessão Firebase.")
            MemberManager.bindFirebaseUidToLoggedInMember(memberContext, user.uid)
        }
        user.uid
    }

    private suspend fun firebaseIdToken(context: Context? = null, forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val user = ensureFirebaseUser(context)
        val token = runCatching { user.getIdToken(forceRefresh).await().token }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("O Firebase não retornou um token de sessão.")
        if (context != null && !adminAuthenticatedState.value) {
            MemberManager.bindFirebaseUidToLoggedInMember(context, user.uid)
        }
        token
    }

    private suspend fun executeGatewayRequest(
        context: Context?,
        requestFactory: suspend (String) -> Request
    ): okhttp3.Response = withContext(Dispatchers.IO) {
        var forceRefresh = false
        repeat(2) { attempt ->
            val token = if (adminAuthenticatedState.value) {
                runCatching { firebaseIdToken(context, forceRefresh) }.getOrDefault("")
            } else {
                firebaseIdToken(context, forceRefresh)
            }
            val response = client.newCall(requestFactory(token)).execute()
            if (response.code != 401 || attempt == 1) return@withContext response
            response.close()
            forceRefresh = true
        }
        error("O gateway não retornou uma resposta válida.")
    }

    private fun mimeType(context: Context, uri: Uri): String {
        val detected = context.contentResolver.getType(uri)?.lowercase()?.substringBefore(';').orEmpty()
        if (detected.isNotBlank()) return detected
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }
        }.getOrDefault("")
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty().lowercase()
    }

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
            throw StorageGatewayException(
                httpCode = response.code,
                reasonCode = json.optString("code").ifBlank { null },
                message = json.optString("error").ifBlank { "Falha HTTP ${response.code} no armazenamento remoto." }
            )
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
        onProgress: ((Float) -> Unit)? = null,
        mimeTypeHint: String? = null
    ): StorageUploadResult = withContext(Dispatchers.IO) {
        val detectedMime = mimeType(context, uri)
        val mime = mimeTypeHint?.lowercase()?.substringBefore(';')
            ?.takeIf { it.isNotBlank() && it != "*/*" && !it.endsWith("/*") }
            ?: detectedMime
        val allowed = when (bucket) {
            PROFILE_BUCKET -> setOf("image/jpeg", "image/png", "image/webp")
            DOCUMENT_BUCKET -> setOf("application/pdf")
            MEDIA_BUCKET -> setOf(
                "image/jpeg", "image/png", "image/webp",
                "application/pdf",
                "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/aac",
                "video/mp4", "video/webm", "video/quicktime", "video/3gpp"
            )
            else -> emptySet()
        }
        if (mime !in allowed) {
            throw IllegalArgumentException(
                when (bucket) {
                    PROFILE_BUCKET -> "Selecione uma imagem JPEG, PNG ou WebP."
                    DOCUMENT_BUCKET -> "Selecione um arquivo PDF."
                    else -> "Selecione um formato de mídia compatível."
                }
            )
        }

        val tempFile = copyUriToCache(context, uri, mime)
        try {
            if (tempFile.length() > maxBytes) {
                throw IllegalArgumentException("O arquivo excede o limite permitido para este tipo de documento.")
            }
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("bucket", bucket)
                .addFormDataPart("targetUid", targetUid)
                .addFormDataPart("file", tempFile.name, tempFile.asRequestBody(mime.toMediaTypeOrNull()))
                .build()

            onProgress?.invoke(0.25f)
            executeGatewayRequest(context) { token ->
                Request.Builder()
                    .url(gatewayUrl())
                    .header("Authorization", "Bearer $token")
                    .header("apikey", publishableKey())
                    .header("X-Rhema-Bucket", bucket)
                    .header("X-Rhema-Operation", "upload")
                    .withAdminAuthorization()
                    .post(body)
                    .build()
            }.use { response ->
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
        onProgress: ((Float) -> Unit)? = null,
        mimeTypeHint: String? = null
    ): StorageUploadResult = uploadToGateway(
        context = context,
        uri = uri,
        bucket = DOCUMENT_BUCKET,
        targetUid = uid,
        maxBytes = MAX_DOCUMENT_BYTES,
        onProgress = onProgress,
        mimeTypeHint = mimeTypeHint
    )

    suspend fun uploadMediaAsset(
        context: Context,
        uri: Uri,
        uid: String,
        onProgress: ((Float) -> Unit)? = null,
        mimeTypeHint: String? = null
    ): StorageUploadResult = uploadToGateway(
        context = context,
        uri = uri,
        bucket = MEDIA_BUCKET,
        targetUid = uid,
        maxBytes = MAX_MEDIA_BYTES,
        onProgress = onProgress,
        mimeTypeHint = mimeTypeHint
    )

    suspend fun getSignedUrl(
        bucket: String,
        storagePath: String,
        targetUid: String,
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("operation", "signed-url")
            .put("bucket", bucket)
            .put("storagePath", storagePath)
            .put("targetUid", targetUid)
        executeGatewayRequest(context) { token ->
            Request.Builder()
                .url(gatewayUrl())
                .header("Authorization", "Bearer $token")
                .header("apikey", publishableKey())
                .header("X-Rhema-Bucket", bucket)
                .header("X-Rhema-Operation", "signed-url")
                .withAdminAuthorization()
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
        }.use { response ->
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
        val signedUrl = getSignedUrl(bucket, storagePath, targetUid, context)
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

    suspend fun deleteProfilePhoto(uid: String, context: Context? = null) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("operation", "delete-profile")
            .put("bucket", PROFILE_BUCKET)
            .put("targetUid", uid)
        executeGatewayRequest(context) { token ->
            Request.Builder()
                .url(gatewayUrl())
                .header("Authorization", "Bearer $token")
                .header("apikey", publishableKey())
                .header("X-Rhema-Bucket", PROFILE_BUCKET)
                .header("X-Rhema-Operation", "delete-profile")
                .withAdminAuthorization()
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
        }.use { response -> responseJson(response) }
    }

    /** Nome mantido para compatibilidade, mas a operação já não usa Firebase Storage. */
    suspend fun deleteProfilePhotoFromFirebase(uid: String, context: Context? = null) = deleteProfilePhoto(uid, context)

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

    /** Compatibilidade para telas antigas: todos os uploads gerais agora usam o Supabase. */
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        path: String,
        onProgress: ((Float) -> Unit)? = null
    ): String {
        val uid = resolveStorageTargetUid(context)
        return uploadMediaAsset(context, uri, uid, onProgress).let { result ->
            result.signedUrl.ifBlank { result.storagePath }
        }
    }
}
