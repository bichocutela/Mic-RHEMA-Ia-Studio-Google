package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class DevotionalReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val dateKey = today.toString()
        if (prefs.getString("last_daily_devotional_date", null) == dateKey) return Result.success()

        return try {
            val remote = loadRemoteDevotionals()
            val local = loadCachedDevotionals()
            val candidates = (remote + local + DevotionalCalendar2027.items)
                .distinctBy { it.id.ifBlank { "${it.date}:${it.title}" } }
                .filter { it.isApproved }
            val devotional = candidates.firstOrNull { parseDate(it.date) == today }
                ?: candidates.maxByOrNull { it.timestamp }

            NotificationHelper.showNotification(
                context = context,
                title = "Devocional Diário: ${devotional?.title?.takeIf { it.isNotBlank() } ?: "Uma palavra para hoje"}",
                message = devotional?.verse?.takeIf { it.isNotBlank() }
                    ?: devotional?.verseReference?.takeIf { it.isNotBlank() }
                    ?: "Separe alguns minutos para fortalecer sua fé com a Palavra de Deus.",
                category = NotificationHelper.Category.DAILY_DEVOTIONAL,
                respectPreferences = true,
                destinationRoute = "devotionals"
            )
            prefs.edit().putString("last_daily_devotional_date", dateKey).apply()
            Result.success()
        } catch (error: Exception) {
            Log.e("DevotionalReminder", "Falha ao preparar devocional diário", error)
            Result.retry()
        }
    }

    private suspend fun loadRemoteDevotionals(): List<Devotional> {
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return emptyList()
        return runCatching {
            FirebaseFirestore.getInstance()
                .collection("devocionais")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(60)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    runCatching {
                        Devotional(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            date = doc.getString("date").orEmpty(),
                            verse = doc.getString("verse").orEmpty(),
                            verseReference = doc.getString("verseReference").orEmpty(),
                            content = doc.getString("content").orEmpty(),
                            likes = doc.getLong("likes")?.toInt() ?: 0,
                            type = doc.getString("type") ?: "devocional",
                            mediaUrl = doc.getString("mediaUrl").orEmpty(),
                            isApproved = doc.getBoolean("isApproved") ?: true,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.getOrNull()
                }
        }.getOrDefault(emptyList())
    }

    private fun loadCachedDevotionals(): List<Devotional> {
        val json = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            .getString("devotionalsState", null)
            ?: return runCatching { IbrDatabaseHelper(context).getCachedDevotionals() }.getOrDefault(emptyList())
        return runCatching {
            val type = object : TypeToken<List<Devotional>>() {}.type
            Gson().fromJson<List<Devotional>>(json, type)
        }.getOrDefault(emptyList())
    }

    private fun parseDate(raw: String): LocalDate? {
        val value = raw.trim()
        if (value.isBlank()) return null
        val formats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )
        formats.forEach { formatter ->
            try {
                return LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }
}
