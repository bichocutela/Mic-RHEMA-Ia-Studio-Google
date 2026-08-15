package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val news = db.collection("bible_news").document(selectedId.toString()).get().await()
            val selectedTitle = news.getString("title") ?: selection.getString("title") ?: return Result.success()
            val selectedSummary = news.getString("summary")
                ?: selection.getString("summary")
                ?: news.getString("content")
                ?: selection.getString("content")
                ?: "Leia a história bíblica de hoje."

            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val notificationKey = "$dateKey:$selectedId"
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            if (prefs.getString("last_daily_news_notification_key", null) == notificationKey) {
                return Result.success()
            }

            NotificationHelper.showNotification(
                context = context,
                title = "Notícia bíblica do dia",
                message = "$selectedTitle — ${selectedSummary.take(180)}",
                category = NotificationHelper.Category.DAILY_NEWS,
                respectPreferences = true
            )
            prefs.edit().putString("last_daily_news_notification_key", notificationKey).apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("MiddayNewsWorker", "Falha ao verificar notícia diária", e)
            Result.retry()
        }
    }
}
