package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MediaUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private data class MediaCandidate(
        val collection: String,
        val type: String,
        val id: String,
        val title: String,
        val preacher: String = ""
    )

    override suspend fun doWork(): Result {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val db = FirebaseFirestore.getInstance()
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet("notified_media_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            val videos = db.collection("conteudos_videos").get().await().documents.map { document ->
                MediaCandidate(
                    collection = "conteudos_videos",
                    type = "video",
                    id = document.id,
                    title = document.getString("title").orEmpty().ifBlank { "Nova pregação" },
                    preacher = preacherFrom(document)
                )
            }
            val audios = db.collection("conteudos_audios").get().await().documents.map { document ->
                MediaCandidate(
                    collection = "conteudos_audios",
                    type = "audio",
                    id = document.id,
                    title = document.getString("title").orEmpty().ifBlank { "Novo áudio" }
                )
            }
            val allItems = (videos + audios).sortedByDescending { it.id.toLongOrNull() ?: 0L }

            if (allItems.isEmpty()) return Result.success()
            if (knownIds.isEmpty()) {
                prefs.edit().putStringSet("notified_media_ids", allItems.map { it.id }.toSet()).apply()
                return Result.success()
            }

            val newItems = allItems.filter { it.id !in knownIds }.take(4)
            newItems.forEach { item ->
                val eventKey = "content:${item.collection}:${item.id}"
                if (NotificationHelper.claimNotificationEvent(context, eventKey)) {
                    if (item.type == "video") {
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Nova pregação: ${item.title}",
                            message = item.preacher.takeIf { it.isNotBlank() }
                                ?.let { "Pregador: $it" }
                                ?: "Nova pregação disponível na aba Mídia.",
                            category = NotificationHelper.Category.SERMONS,
                            respectPreferences = true,
                            destinationRoute = "content"
                        )
                    } else {
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Novo áudio disponível",
                            message = item.title,
                            category = NotificationHelper.Category.MEDIA,
                            respectPreferences = true,
                            destinationRoute = "content"
                        )
                    }
                }
                // Mesmo quando o FCM ganhou a corrida, este ID deixa de ser candidato local.
                knownIds.add(item.id)
            }

            if (knownIds.size > 100) {
                val trimmed = allItems.map { it.id }.filter { it in knownIds }.take(100).toSet()
                knownIds.clear()
                knownIds.addAll(trimmed)
            }
            prefs.edit().putStringSet("notified_media_ids", knownIds).apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaUpdateWorker", "Falha ao verificar novas mídias", e)
            Result.retry()
        }
    }

    private fun preacherFrom(document: DocumentSnapshot): String =
        document.getString("preacher").orEmpty()
            .ifBlank { document.getString("pregador").orEmpty() }
            .ifBlank { document.getString("artist").orEmpty() }
            .ifBlank { document.getString("description").orEmpty() }
}
