package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
import java.util.concurrent.TimeUnit

/** Emblema criado remotamente pelo ADM. O PNG continua armazenado no Supabase Storage. */
data class RemoteProfileBadge(
    val id: String,
    val sequenceNo: Int?,
    val name: String,
    val description: String,
    val challenge: String,
    val imageRef: String,
    val special: Boolean
) {
    fun asBiblicalBadge(): BiblicalBadge = BiblicalBadge(
        id = id,
        name = name,
        description = description,
        category = BadgeCategory.ACHIEVEMENT,
        level = sequenceNo,
        frameStyle = BadgeFrameStyle.PROFILE_EMBLEM,
        accentColorHex = 0xFFFFC107,
        requirement = challenge
    )
}

val remoteProfileBadgesState = mutableStateOf<List<RemoteProfileBadge>>(emptyList())

fun remoteProfileBadgeForId(id: String): RemoteProfileBadge? =
    remoteProfileBadgesState.value.firstOrNull { it.id == id }

fun currentAllBiblicalBadges(): List<BiblicalBadge> =
    allBiblicalBadges + remoteProfileBadgesState.value.map { it.asBiblicalBadge() }

fun currentProfileEmblemBadges(): List<BiblicalBadge> =
    profileEmblemBadges + remoteProfileBadgesState.value.map { it.asBiblicalBadge() }

/**
 * Motor remoto dos emblemas personalizados.
 *
 * O aplicativo nunca decide sozinho se um desafio remoto foi cumprido: ele pede
 * ao Supabase para reconciliar as métricas verificadas no ledger central de XP.
 * O resultado volta para o perfil e é sincronizado no Firebase para recuperação
 * em outros aparelhos.
 */
object RemoteBadgeEngineClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var lastCatalogRefreshAt = 0L
    @Volatile private var reconcileInFlight = false
    private const val CATALOG_TTL_MS = 5 * 60 * 1000L

    private suspend fun call(action: String, memberId: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O motor remoto de emblemas não está configurado nesta versão.")
        }

        val payload = JSONObject().put("action", action)
        if (!memberId.isNullOrBlank()) payload.put("memberId", memberId)
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/badge-engine")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val body = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(body.optString("error").ifBlank { "Falha no motor remoto de emblemas (${response.code})." })
            }
            body
        }
    }

    private fun parseCatalog(root: JSONObject): List<RemoteProfileBadge> {
        val array = root.optJSONArray("badges") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val imageRef = item.optString("image_ref").trim()
                if (id.isBlank() || imageRef.isBlank()) continue
                add(
                    RemoteProfileBadge(
                        id = id,
                        sequenceNo = if (item.isNull("sequence_no")) null else item.optInt("sequence_no"),
                        name = item.optString("name").ifBlank { "Emblema" },
                        description = item.optString("description"),
                        challenge = item.optString("challenge"),
                        imageRef = imageRef,
                        special = item.optBoolean("special", false)
                    )
                )
            }
        }
    }

    suspend fun loadCatalog(force: Boolean = false): List<RemoteProfileBadge> {
        val now = System.currentTimeMillis()
        if (!force && remoteProfileBadgesState.value.isNotEmpty() && now - lastCatalogRefreshAt < CATALOG_TTL_MS) {
            return remoteProfileBadgesState.value
        }
        val catalog = parseCatalog(call("catalog"))
        withContext(Dispatchers.Main.immediate) {
            remoteProfileBadgesState.value = catalog
        }
        lastCatalogRefreshAt = now
        return catalog
    }

    fun refreshCatalog(force: Boolean = false) {
        scope.launch {
            runCatching { loadCatalog(force) }
                .onFailure { Log.w("RemoteBadgeEngine", "Não foi possível atualizar o catálogo remoto", it) }
        }
    }

    fun reconcile(context: Context, member: MemberRequest) {
        if (member.id.isBlank() || reconcileInFlight) return
        reconcileInFlight = true
        scope.launch {
            try {
                loadCatalog()
                val result = call("reconcile", member.id)
                val unlockedArray = result.optJSONArray("unlockedBadgeIds")
                val newlyArray = result.optJSONArray("newlyUnlockedIds")
                val remoteUnlocked = buildList {
                    if (unlockedArray != null) for (i in 0 until unlockedArray.length()) {
                        unlockedArray.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                val newlyUnlockedIds = buildList {
                    if (newlyArray != null) for (i in 0 until newlyArray.length()) {
                        newlyArray.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                if (remoteUnlocked.isEmpty() && newlyUnlockedIds.isEmpty()) return@launch

                withContext(Dispatchers.Main.immediate) {
                    val live = loggedInMemberState.value?.takeIf { it.id == member.id } ?: member
                    val mergedIds = (live.unlockedBadgeIds + remoteUnlocked + DEFAULT_BIBLICAL_BADGE_ID).distinct()
                    val changed = mergedIds.toSet() != live.unlockedBadgeIds.toSet()
                    if (changed) {
                        val updated = live.copy(unlockedBadgeIds = mergedIds)
                        BadgeActivityTracker.updateMemberStates(updated)
                        BadgeActivityTracker.syncPortableState(context.applicationContext, updated)
                    }

                    val newlyUnlocked = newlyUnlockedIds.mapNotNull { id ->
                        currentAllBiblicalBadges().firstOrNull { it.id == id }
                    }
                    if (newlyUnlocked.isNotEmpty()) {
                        badgeAwardNotificationState.value = BadgeAwardNotification(newlyUnlocked)
                    }
                }
            } catch (error: Exception) {
                Log.w("RemoteBadgeEngine", "Reconciliação remota de emblemas falhou", error)
            } finally {
                reconcileInFlight = false
            }
        }
    }
}
