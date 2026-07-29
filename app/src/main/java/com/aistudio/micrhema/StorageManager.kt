package com.aistudio.micrhema

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageManager {
    suspend fun uploadFile(uri: Uri, path: String): String {
        // If it's already an http or https URL, just return it
        if (uri.scheme == "http" || uri.scheme == "https") return uri.toString()
        
        val storageRef = Firebase.storage.reference
        val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment ?: "file"}"
        val fileRef = storageRef.child("$path/$fileName")
        
        return try {
            fileRef.putFile(uri).await()
            val downloadUrl = fileRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
