package com.aistudio.micrhema

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val discipuladoPublisherClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

// The server owns publication and delivery. The persistent queue also retries
// when a device goes offline after saving, so no scheduled work runs on the phone.
internal suspend fun wakeDiscipuladoPublisher() = withContext(Dispatchers.IO) {
    runCatching {
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
        val token = user.getIdToken(false).await().token ?: return@runCatching
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/discipulado-release")
            .header("Authorization", "Bearer $token")
            .post("{\"action\":\"release\"}".toRequestBody("application/json".toMediaType()))
            .build()
        discipuladoPublisherClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) android.util.Log.w("DiscipuladoPublishing", "Envio será retomado pelo servidor")
        }
    }.onFailure { android.util.Log.w("DiscipuladoPublishing", "Envio será retomado pelo servidor") }
    Unit
}
