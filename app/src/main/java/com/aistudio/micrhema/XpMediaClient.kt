package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class XpMediaResult(
    val mediaType: String,
    val canonicalId: String,
    val qualified: Boolean,
    val tenGranted: Int,
    val completeGranted: Int,
    val account: XpAccount
)

/**
 * Cliente único para progresso de Livro/Áudio/Vídeo.
 *
 * O Android informa apenas telemetria observada. O backend confirma que a mídia
 * pertence ao catálogo oficial, mede o tempo usando relógio do servidor, ignora
 * saltos artificiais e é a única camada que pode chamar xp_record_media_progress.
 */
object XpMediaClient {
    private const val MIN_SEND_INTERVAL_MS = 4_000L
    private const val REJECT_CACHE_MS = 10 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val lastSentAt = ConcurrentHashMap<String, Long>()
    private val rejectedUntil = ConcurrentHashMap<String, Long>()

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    fun recordBook(context: Context, bookUrl: String, fraction: Float) {
        record(
            context = context,
            mediaType = "book",
            contentId = bookUrl,
            positionMs = 0L,
            durationMs = 0L,
            fraction = fraction.coerceIn(0f, 1f),
            isActive = true
        )
    }

    fun recordAudio(
        context: Context,
        audioUrl: String,
        positionMs: Long,
        durationMs: Long,
        isActive: Boolean
    ) {
        record(
            context = context,
            mediaType = "audio",
            contentId = audioUrl,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
            isActive = isActive
        )
    }

    fun recordVideo(
        context: Context,
        videoUrl: String,
        positionMs: Long,
        durationMs: Long,
        isActive: Boolean
    ) {
        record(
            context = context,
            mediaType = "video",
            contentId = videoUrl,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
            isActive = isActive
        )
    }

    private fun record(
        context: Context,
        mediaType: String,
        contentId: String,
        positionMs: Long,
        durationMs: Long,
        fraction: Float,
        isActive: Boolean
    ) {
        val member = loggedInMemberState.value ?: return
        if (!isXpUnlocked(member)) return
        val cleanId = contentId.trim()
        if (cleanId.isBlank()) return

        val key = "${member.id}:$mediaType:${cleanId.hashCode()}"
        val now = System.currentTimeMillis()
        if ((rejectedUntil[key] ?: 0L) > now) return
        val last = lastSentAt[key] ?: 0L
        if (now - last < MIN_SEND_INTERVAL_MS) return
        lastSentAt[key] = now

        val appContext = context.applicationContext
        scope.launch {
            try {
                val response = call(
                    member = member,
                    mediaType = mediaType,
                    contentId = cleanId,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    fraction = fraction,
                    isActive = isActive
                )
                val result = parseResult(member, mediaType, response)
                withContext(Dispatchers.Main) {
                    xpAccountState.value = result.account
                    if (result.qualified && result.canonicalId.isNotBlank()) {
                        val activity = when (mediaType) {
                            "book" -> BadgeActivityKeys.BOOKS
                            "audio" -> BadgeActivityKeys.AUDIOS
                            "video" -> BadgeActivityKeys.VIDEOS
                            else -> ""
                        }
                        if (activity.isNotBlank()) {
                            BadgeActivityTracker.recordVerifiedMedia(appContext, activity, result.canonicalId)
                        }
                    }
                }
            } catch (error: Throwable) {
                val message = error.message.orEmpty()
                if (message.contains("não pertence ao catálogo oficial", ignoreCase = true)) {
                    rejectedUntil[key] = System.currentTimeMillis() + REJECT_CACHE_MS
                    Log.d("XpMediaClient", "Mídia fora do catálogo XP: $mediaType")
                } else {
                    Log.w("XpMediaClient", "Falha ao sincronizar progresso de $mediaType", error)
                }
            }
        }
    }

    private suspend fun call(
        member: MemberRequest,
        mediaType: String,
        contentId: String,
        positionMs: Long,
        durationMs: Long,
        fraction: Float,
        isActive: Boolean
    ): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O progresso XP de mídia não está configurado nesta versão.")
        }

        val payload = JSONObject()
            .put("memberId", member.id)
            .put("phone", normalizePhone(member.phone))
            .put("mediaType", mediaType)
            .put("contentId", contentId)
            .put("positionMs", positionMs)
            .put("durationMs", durationMs)
            .put("fraction", fraction.toDouble())
            .put("isActive", isActive)

        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-media")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha no progresso XP de mídia (${response.code})." })
            }
            json
        }
    }

    private fun parseResult(member: MemberRequest, mediaType: String, root: JSONObject): XpMediaResult {
        val rawAccount = root.optJSONObject("account") ?: JSONObject()
        val account = XpAccount(
            memberId = member.id,
            unlocked = root.optBoolean("unlocked", false),
            totalEarned = rawAccount.optInt("total_earned", rawAccount.optInt("totalEarned", 0)).coerceAtLeast(0),
            totalSpent = rawAccount.optInt("total_spent", rawAccount.optInt("totalSpent", 0)).coerceAtLeast(0),
            balance = rawAccount.optInt("balance", 0).coerceAtLeast(0),
            migratedLegacyXp = rawAccount.optInt("migrated_legacy_xp", rawAccount.optInt("migratedLegacyXp", 0)).coerceAtLeast(0),
            updatedAt = rawAccount.optString("updated_at", rawAccount.optString("updatedAt"))
        )
        return XpMediaResult(
            mediaType = mediaType,
            canonicalId = root.optString("canonicalId"),
            qualified = root.optBoolean("qualified", false),
            tenGranted = root.optInt("tenGranted", 0).coerceAtLeast(0),
            completeGranted = root.optInt("completeGranted", 0).coerceAtLeast(0),
            account = account
        )
    }

    fun clearSession() {
        lastSentAt.clear()
        rejectedUntil.clear()
    }
}
