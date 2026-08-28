package com.aistudio.micrhema

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Disparo automático de notificações via Supabase Edge Function + Firebase FCM. */
object NotificationDispatcher {
    private const val FUNCTION_NAME = "notify-fcm"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun enqueue(topic: String, title: String, body: String, collection: String, documentId: String) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) return

        scope.launch {
            runCatching {
                var finalTitle = title
                var finalBody = body
                var destination = ""
                val category = when (collection) {
                    "ibr_courses" -> {
                        finalTitle = "Novo curso no IBR"
                        destination = "ibr"
                        "courses"
                    }
                    "conteudos_videos" -> {
                        destination = "content"
                        runCatching {
                            val doc = Firebase.firestore.collection(collection).document(documentId).get().await()
                            val videoTitle = doc.getString("title").orEmpty().ifBlank { body }
                            val preacher = doc.getString("preacher")
                                .orEmpty()
                                .ifBlank { doc.getString("pregador").orEmpty() }
                                .ifBlank { doc.getString("artist").orEmpty() }
                                .ifBlank { doc.getString("description").orEmpty() }
                            finalTitle = "Nova pregação: $videoTitle"
                            finalBody = preacher.takeIf { it.isNotBlank() }
                                ?.let { "Pregador: $it" }
                                ?: "Novo vídeo disponível na aba Mídia."
                        }
                        "sermons"
                    }
                    "conteudos_audios", "conteudos_books", "conteudos_albums" -> {
                        destination = "content"
                        "media"
                    }
                    "cultos_agenda", "events" -> {
                        destination = "services"
                        "events"
                    }
                    "devocionais" -> {
                        destination = "devocionais"
                        "daily_devotional"
                    }
                    "bible_news" -> "daily_news"
                    else -> "content_updates"
                }

                val payload = JSONObject()
                    .put("topic", topic)
                    .put("title", finalTitle)
                    .put("body", finalBody)
                    .put("data", JSONObject()
                        .put("collection", collection)
                        .put("documentId", documentId)
                        .put("category", category)
                        .put("destination", destination))
                val request = Request.Builder()
                    .url("$baseUrl/functions/v1/$FUNCTION_NAME")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w("NotificationDispatcher", "Falha ao enviar notificação: HTTP ${response.code}")
                    }
                }
            }.onFailure { error ->
                Log.w("NotificationDispatcher", "Notificação automática indisponível", error)
            }
        }
    }
}
