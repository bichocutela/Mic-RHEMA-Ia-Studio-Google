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

object NotificationDispatcher {
    private const val FUNCTION_NAME = "notify-fcm"
    private val scheduledOnlyCollections = setOf("devocionais", "cultos_agenda", "bible_news")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    fun enqueue(topic: String, title: String, body: String, collection: String, documentId: String) {
        dispatch(topic, null, title, body, collection, documentId, null)
    }

    fun enqueueToken(token: String, title: String, body: String, collection: String, documentId: String, destination: String) {
        if (token.isBlank()) return
        dispatch(null, token, title, body, collection, documentId, destination)
    }

    private fun dispatch(
        topic: String?,
        deviceToken: String?,
        title: String,
        body: String,
        collection: String,
        documentId: String,
        requestedDestination: String?
    ) {
        if (deviceToken == null && collection in scheduledOnlyCollections) return
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) return

        scope.launch {
            runCatching {
                var finalTitle = title
                var finalBody = body
                var destination = requestedDestination.orEmpty()
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
                            val preacher = doc.getString("preacher").orEmpty()
                                .ifBlank { doc.getString("pregador").orEmpty() }
                                .ifBlank { doc.getString("artist").orEmpty() }
                                .ifBlank { doc.getString("description").orEmpty() }
                            finalTitle = "Nova pregação: $videoTitle"
                            finalBody = preacher.takeIf { it.isNotBlank() }?.let { "Pregador: $it" }
                                ?: "Nova pregação disponível na aba Mídia."
                        }
                        "sermons"
                    }
                    "conteudos_audios", "conteudos_books", "conteudos_albums" -> {
                        destination = "content"
                        "media"
                    }
                    "events" -> {
                        destination = "services"
                        "events"
                    }
                    "prayer_requests" -> {
                        destination = "admin_prayer/$documentId"
                        "prayer"
                    }
                    "prayer_response" -> {
                        if (destination.isBlank()) destination = Screen.Prayer.route
                        "prayer"
                    }
                    else -> "content_updates"
                }

                val payload = JSONObject()
                    .put("title", finalTitle)
                    .put("body", finalBody)
                    .put("data", JSONObject()
                        .put("collection", collection)
                        .put("documentId", documentId)
                        .put("category", category)
                        .put("destination", destination))
                topic?.takeIf { it.isNotBlank() }?.let { payload.put("topic", it) }
                deviceToken?.takeIf { it.isNotBlank() }?.let { payload.put("token", it) }

                val request = Request.Builder()
                    .url("$baseUrl/functions/v1/$FUNCTION_NAME")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) Log.w("NotificationDispatcher", "Falha ao enviar notificação: HTTP ${response.code}")
                }
            }.onFailure { error -> Log.w("NotificationDispatcher", "Notificação automática indisponível", error) }
        }
    }
}
