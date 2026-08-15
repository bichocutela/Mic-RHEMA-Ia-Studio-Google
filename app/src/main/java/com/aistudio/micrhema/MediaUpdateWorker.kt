package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MediaUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val db = FirebaseFirestore.getInstance()
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet("notified_media_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

            val videos = db.collection("conteudos_videos").get().await().documents
                .map { it.id to (it.getString("title") ?: "Novo vídeo") }
            val audios = db.collection("conteudos_audios").get().await().documents
                .map { it.id to (it.getString("title") ?: "Novo áudio") }
            val allItems = (videos.map { Triple("vídeo", it.first, it.second) } + audios.map { Triple("áudio", it.first, it.second) })
                .sortedByDescending { it.second.toLongOrNull() ?: 0L }

            if (allItems.isEmpty()) return Result.success()
            if (knownIds.isEmpty()) {
                prefs.edit().putStringSet("notified_media_ids", allItems.map { it.second }.toSet()).apply()
                return Result.success()
            }

            val newItems = allItems.filter { it.second !in knownIds }.take(4)
            newItems.forEach { (type, id, title) ->
                NotificationHelper.showNotification(
                    context = context,
                    title = "Novo $type em Mídia",
                    message = title,
                    category = NotificationHelper.Category.MEDIA,
                    respectPreferences = true
                )
                knownIds.add(id)
            }
            if (knownIds.size > 100) {
                val trimmed = allItems.map { it.second }.filter { it in knownIds }.take(100).toSet()
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
}
