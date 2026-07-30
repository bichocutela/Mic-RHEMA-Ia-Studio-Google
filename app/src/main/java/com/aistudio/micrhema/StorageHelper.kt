package com.aistudio.micrhema

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageHelper {
    suspend fun uploadFile(uri: Uri, path: String): String? {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child("${path}/${UUID.randomUUID()}")
            fileRef.putFile(uri).await()
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
