package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MiddayNewsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val db = FirebaseFirestore.getInstance()
            val selection = db.collection("settings").document("daily_news").get().await()
            val selectedId = selection.getLong("selectedNewsId")?.toInt() ?: return Result.success()
            val selectionVersion = selection.getLong("updatedAt") ?: selectedId.toLong()

            val editorialSettings = db.collection("settings").document("bible_news_editorial").get().await()
            val hiddenIds = (editorialSettings.get("hiddenIds") as? List<*>)
                .orEmpty()
                .mapNotNull { value ->
                    when (value) {
                        is Number -> value.toInt()
                        is String -> value.toIntOrNull()
                        else -> null
                    }
                }
                .toSet()
            if (selectedId in hiddenIds) return Result.success()

            val selectedDocumentId = selection.getString("selectedDocumentId")
                ?.takeIf { it.isNotBlank() }
                ?: selectedId.toString()
            val news = db.collection("bible_news").document(selectedDocumentId).get().await()
            val selectedTitle = news.getString("title") ?: selection.getString("title") ?: return Result.success()
            val selectedSummary = news.getString("summary")
                ?: selection.getString("summary")
                ?: news.getString("content")
                ?: selection.getString("content")
                ?: "Leia a história bíblica de hoje."

            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            if (prefs.getLong("last_daily_news_selection_version", Long.MIN_VALUE) == selectionVersion) {
                return Result.success()
            }

            NotificationHelper.showNotification(
                context = context,
                title = "Notícia bíblica do dia",
                message = "$selectedTitle — ${selectedSummary.take(180)}",
                category = NotificationHelper.Category.DAILY_NEWS,
                respectPreferences = true,
                destinationRoute = "news_detail/$selectedId",
                destinationDocumentId = selectedDocumentId
            )
            prefs.edit()
                .putLong("last_daily_news_selection_version", selectionVersion)
                .remove("last_daily_news_notification_key")
                .apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("MiddayNewsWorker", "Falha ao verificar notícia diária", e)
            Result.retry()
        }
    }
}
