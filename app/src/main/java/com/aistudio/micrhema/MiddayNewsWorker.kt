package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.random.Random

class MiddayNewsWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString("last_daily_news_date", null) == today) return Result.success()

        return try {
            val db = if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null
            val hiddenIds = db?.let { database ->
                runCatching {
                    val settings = database.collection("settings").document("bible_news_editorial").get().await()
                    (settings.get("hiddenIds") as? List<*>).orEmpty().mapNotNull { value ->
                        when (value) {
                            is Number -> value.toInt()
                            is String -> value.toIntOrNull()
                            else -> null
                        }
                    }.toSet()
                }.getOrDefault(emptySet())
            } ?: emptySet()

            val remote = db?.let { database -> loadRemote(database) }.orEmpty()
            val combined = BibleNewsEditorial.withEditorialCatalog(remote.ifEmpty { BibleNewsData.newsList })
                .filter { it.id !in hiddenIds }
                .distinctBy { it.id }
            if (combined.isEmpty()) return Result.retry()

            val previousId = prefs.getInt("last_daily_news_id", -1)
            val pool = combined.filter { it.id != previousId }.ifEmpty { combined }
            val seed = today.hashCode() xor System.nanoTime().toInt()
            val selected = pool.random(Random(seed))
            val documentId = selected.id.toString()
            val summary = selected.summary.ifBlank { selected.content }.replace(Regex("\\s+"), " ").trim()

            NotificationHelper.showNotification(
                context = context,
                title = "Notícia bíblica do dia",
                message = "${selected.title} — ${summary.take(180)}",
                category = NotificationHelper.Category.DAILY_NEWS,
                respectPreferences = true,
                destinationRoute = "news_detail/${selected.id}",
                destinationDocumentId = documentId
            )
            prefs.edit()
                .putString("last_daily_news_date", today)
                .putInt("last_daily_news_id", selected.id)
                .remove("last_daily_news_selection_version")
                .apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("MiddayNewsWorker", "Falha ao preparar notícia bíblica diária", e)
            Result.retry()
        }
    }

    private suspend fun loadRemote(db: FirebaseFirestore): List<BibleNews> {
        return runCatching {
            db.collection("bible_news").get().await().documents.mapNotNull { doc ->
                val id = doc.getLong("id")?.toInt() ?: doc.id.toIntOrNull() ?: return@mapNotNull null
                val title = doc.getString("title").orEmpty()
                val content = doc.getString("content").orEmpty()
                if (title.isBlank() || content.isBlank()) return@mapNotNull null
                BibleNews(
                    id = id,
                    title = title,
                    content = content,
                    book = doc.getString("book").orEmpty(),
                    chapter = doc.getLong("chapter")?.toInt() ?: 0,
                    verse = doc.getLong("verse")?.toInt() ?: 0,
                    imageUrl = doc.getString("imageUrl").orEmpty(),
                    summary = doc.getString("summary").orEmpty(),
                    category = doc.getString("category").orEmpty(),
                    intensity = doc.getLong("intensity")?.toInt() ?: 0,
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
                    contentWarning = doc.getString("contentWarning").orEmpty(),
                    publishedAt = doc.getLong("publishedAt") ?: 0L,
                    featured = doc.getBoolean("featured") ?: false,
                    storyKey = doc.getString("storyKey").orEmpty()
                )
            }
        }.getOrDefault(emptyList())
    }
}
