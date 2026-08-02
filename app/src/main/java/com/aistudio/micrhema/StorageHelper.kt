package com.aistudio.micrhema

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageHelper {
    suspend fun uploadFile(uri: Uri, path: String, onProgress: ((Float) -> Unit)? = null): String? {
        // If it's already an http or https URL, just return it
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()
        
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child("${path}/${UUID.randomUUID()}")
            
            val context = Firebase.app.applicationContext
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
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}