package com.aistudio.micrhema

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object StorageHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFile(context: android.content.Context, uri: Uri, path: String, onProgress: ((Float) -> Unit)? = null): String? {
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
                
                if (tempFile == null || !tempFile.exists()) return@withContext null

                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
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
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("StorageHelper", "Upload failed", e)
                null
            }
        }
    }
}
