package com.aistudio.micrhema

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object DevotionalRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("devocionais")

    fun getDevotionalsFlow(): Flow<List<Devotional>> = callbackFlow {
        val listenerRegistration = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DevotionalRepository", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val devotionals = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val title = doc.getString("title") ?: ""
                            val date = doc.getString("date") ?: ""
                            val verse = doc.getString("verse") ?: ""
                            val verseReference = doc.getString("verseReference") ?: ""
                            val content = doc.getString("content") ?: ""
                            val likes = doc.getLong("likes")?.toInt() ?: 0
                            val type = doc.getString("type") ?: "devocional"
                            val mediaUrl = doc.getString("mediaUrl") ?: ""
                            val isApproved = doc.getBoolean("isApproved") ?: true
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            
                            Devotional(id, title, date, verse, verseReference, content, likes, type, mediaUrl, isApproved, timestamp)
                        } catch (e: Exception) {
                            Log.e("DevotionalRepository", "Error parsing devotional", e)
                            null
                        }
                    }
                    trySend(devotionals).isSuccess
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addDevotional(devotional: Devotional): Result<Unit> {
        return try {
            collection.document(devotional.id).set(devotional).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DevotionalRepository", "Error adding devotional", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDevotional(devotionalId: String): Result<Unit> {
        return try {
            collection.document(devotionalId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DevotionalRepository", "Error deleting devotional", e)
            Result.failure(e)
        }
    }
}
