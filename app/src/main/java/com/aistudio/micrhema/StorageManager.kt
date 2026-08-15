package com.aistudio.micrhema

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import com.google.firebase.storage.FirebaseStorage

object StorageManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFile(context: android.content.Context, uri: Uri, path: String, onProgress: ((Float) -> Unit)? = null): String {
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()
        
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                // Using Catbox free API to bypass Firebase Storage billing requirements
                tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (tempFile == null || !tempFile.exists()) return@withContext ""

                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                // Adding an extension to the file so it has the right format
                var extension = ""
                when {
                    mimeType.startsWith("image/jpeg") -> extension = ".jpg"
                    mimeType.startsWith("image/png") -> extension = ".png"
                    mimeType.startsWith("video/mp4") -> extension = ".mp4"
                    mimeType.startsWith("audio/mpeg") -> extension = ".mp3"
                    mimeType.startsWith("application/pdf") -> extension = ".pdf"
                }
                val uploadFile = if (extension.isNotEmpty()) File(tempFile.absolutePath + extension).also { tempFile.renameTo(it) } else tempFile

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("reqtype", "fileupload")
                    .addFormDataPart("fileToUpload", uploadFile.name, uploadFile.asRequestBody(mimeType.toMediaTypeOrNull()))
                    .build()
                
                onProgress?.invoke(0.3f)
                
                val request = Request.Builder()
                    .url("https://catbox.moe/user/api.php")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                uploadFile.delete()
                
                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    onProgress?.invoke(1.0f)
                    responseBody
                } else {
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("StorageManager", "Upload failed", e)
                ""
            }
        }
    }

    suspend fun uploadProfilePhotoToFirebase(context: android.content.Context, uri: Uri, uid: String): String {
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw IllegalArgumentException("A imagem selecionada não possui um URI válido.")
        }

        return withContext(Dispatchers.IO) {
            var firebaseError: Exception? = null
            try {
                val storage = FirebaseStorage.getInstance()
                val imageRef = storage.reference.child("profile_photos/$uid/profile.jpg")
                imageRef.putFile(uri).await()
                val remoteUrl = imageRef.downloadUrl.await().toString()
                runCatching { saveProfilePhotoLocally(context, uri, uid) }
                    .onFailure { error ->
                        android.util.Log.w("StorageManager", "Foto sincronizada, mas cache local não foi salvo", error)
                    }
                return@withContext remoteUrl
            } catch (e: Exception) {
                firebaseError = e
                android.util.Log.e("StorageManager", "Firebase Storage recusou o upload; tentando armazenamento remoto alternativo", e)
            }

            val fallbackUrl = runCatching {
                uploadFile(context, uri, "profile_photos")
            }.getOrNull().orEmpty()
            if (fallbackUrl.isNotBlank() && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
                runCatching { saveProfilePhotoLocally(context, uri, uid) }
                    .onFailure { error ->
                        android.util.Log.w("StorageManager", "Foto remota salva, mas cache local não foi salvo", error)
                    }
                return@withContext fallbackUrl
            }

            throw IllegalStateException(
                "Não foi possível enviar a foto ao Firebase Storage${firebaseError?.message?.let { ": $it" } ?: ". Verifique a conexão e as regras do Storage."}",
                firebaseError
            )
        }
    }

    suspend fun deleteProfilePhotoFromFirebase(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                FirebaseStorage.getInstance()
                    .reference
                    .child("profile_photos/$uid/profile.jpg")
                    .delete()
                    .await()
            } catch (e: com.google.firebase.storage.StorageException) {
                if (e.errorCode != com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                    throw e
                }
            }
        }
    }

    suspend fun saveProfilePhotoLocally(context: android.content.Context, uri: Uri, uid: String): String =
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

    fun getLocalProfilePhotoUri(context: android.content.Context, uid: String): String {
        val file = File(context.filesDir, "profile_photos/${uid}_profile.jpg")
        return if (file.exists() && file.length() > 0L) Uri.fromFile(file).toString() else ""
    }

    fun deleteLocalProfilePhoto(context: android.content.Context, uid: String) {
        File(context.filesDir, "profile_photos/${uid}_profile.jpg").delete()
    }
}
