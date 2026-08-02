package com.aistudio.micrhema

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageManager {
    suspend fun uploadFile(context: android.content.Context, uri: Uri, path: String, onProgress: ((Float) -> Unit)? = null): String {
        // If it's already an http or https URL, just return it
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()
        
        val storageRef = Firebase.storage.reference
        val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment ?: "file"}"
        val fileRef = storageRef.child("$path/$fileName")
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val uploadTask = if (inputStream != null) {
                fileRef.putStream(inputStream)
            } else {
                fileRef.putFile(uri)
            }
            
            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    onProgress(progress.toFloat() / 100f)
                }
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("StorageManager", "Upload failed", e)
            ""
        }
    }
}