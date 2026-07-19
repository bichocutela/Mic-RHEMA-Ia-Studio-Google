package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class DevotionalSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = FirebaseFirestore.getInstance()
            val result = db.collection("devotionals")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!result.isEmpty) {
                val latestDoc = result.documents.first()
                val latestId = latestDoc.id
                val latestTitle = latestDoc.getString("title") ?: "Novo Devocional"

                val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
                val lastSeenId = prefs.getString("last_notified_devotional_id", null)

                if (lastSeenId != null && lastSeenId != latestId) {
                    NotificationHelper.showNotification(
                        context,
                        "Novo Devocional Disponível! 📖",
                        latestTitle
                    )
                }

                prefs.edit().putString("last_notified_devotional_id", latestId).apply()
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("DevotionalSyncWorker", "Error fetching latest devotional", e)
            return Result.failure()
        }
    }
}
