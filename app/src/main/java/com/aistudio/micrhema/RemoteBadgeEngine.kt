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

/** Emblema criado ou sobrescrito remotamente pelo ADM. */
data class RemoteProfileBadge(
    val id: String,
    val sequenceNo: Int?,
    val name: String,
    val description: String,
    val challenge: String,
    val imageRef: String,
    val special: Boolean
) {
    fun asBiblicalBadge(): BiblicalBadge {
        val nativeAchievement = simpleBiblicalBadges.firstOrNull { it.id == id }
        if (nativeAchievement != null) {
            return nativeAchievement.copy(
                name = name,
                description = description,
                requirement = challenge
            )
        }

        val level = sequenceNo
        val rarity = when (level) {
            in 8..12 -> ProfileEmblemRarity.RARE
            in 13..17 -> ProfileEmblemRarity.EPIC
            in 18..22 -> ProfileEmblemRarity.LEGENDARY
            else -> null
        }
        return BiblicalBadge(
            id = id,
            name = name,
            description = description,
            category = if (level != null) BadgeCategory.LEVEL else BadgeCategory.ACHIEVEMENT,
            level = level,
            frameStyle = BadgeFrameStyle.PROFILE_EMBLEM,
            accentColorHex = 0xFFFFC107,
            requirement = challenge,
            rarity = rarity
        )
    }
}

/**
 * Mantém somente os metadados remotos (imagem/sequence/special).
 * A fonte única usada pela UI continua sendo os catálogos antigos
 * allBiblicalBadges/profileEmblemBadges, agora enriquecidos com os personalizados.
 */
val remoteProfileBadgesState = mutableStateOf<List<RemoteProfileBadge>>(emptyList())

fun isNativeAchievementBadgeId(id: String): Boolean =
    simpleBiblicalBadges.any { it.id == id }

fun remoteProfileBadgeForId(id: String): RemoteProfileBadge? =
    remoteProfileBadgesState.value.firstOrNull { it.id == id }?.takeUnless { remote ->
        isNativeAchievementBadgeId(id) && remote.imageRef.startsWith("builtin-emblem://", ignoreCase = true)
    }

fun currentAllBiblicalBadges(): List<BiblicalBadge> {
    // A leitura do State garante recomposição quando o catálogo remoto termina de sincronizar.
    remoteProfileBadgesState.value
    RemoteBadgeEngineClient.ensureCatalogLoaded()
    return allBiblicalBadges
}

fun currentProfileEmblemBadges(): List<BiblicalBadge> {
    remoteProfileBadgesState.value
    RemoteBadgeEngineClient.ensureCatalogLoaded()
    return profileEmblemBadges
}

/** Catálogo que o ADM pode liberar manualmente: emblemas de perfil + conquistas nativas. */
fun currentAdminUnlockableBadges(): List<BiblicalBadge> {
    remoteProfileBadgesState.value
    RemoteBadgeEngineClient.ensureCatalogLoaded()
    val currentById = allBiblicalBadges.associateBy { it.id }
    val achievements = simpleBiblicalBadges.map { native -> currentById[native.id] ?: native }
    return (profileEmblemBadges + achievements).distinctBy { it.id }
}

private fun publishCatalogIntoLegacySelectors(catalog: List<RemoteProfileBadge>) {
    val previousRemoteIds = remoteProfileBadgesState.value.map { it.id }.toSet()
    val currentRemoteIds = catalog.map { it.id }.toSet()
    val remoteIds = previousRemoteIds + currentRemoteIds
    val mapped = catalog.map { it.asBiblicalBadge() }
    val nativeAchievementIds = simpleBiblicalBadges.map { it.id }.toSet()

    val allList = allBiblicalBadges as? MutableList<BiblicalBadge>
    val profileList = profileEmblemBadges as? MutableList<BiblicalBadge>
    if (allList == null || profileList == null) {
        Log.e("RemoteBadgeEngine", "Catálogos bíblicos não são mutáveis; personalizados não puderam ser publicados.")
        return
    }

    // Catálogo principal: preserva todos os nativos e acrescenta/atualiza personalizados.
    allList.removeAll { it.id in remoteIds }
    allList.addAll(mapped)

    // Catálogo específico de emblemas do perfil: mantém níveis 8–22 + personalizados.
    // As cinco conquistas nativas continuam no catálogo geral, sem virar nível de perfil.
    profileList.clear()
    profileList.addAll(biblicalLevelBadges.filter { (it.level ?: 0) in 8..22 })
    profileList.addAll(mapped.filter { remote ->
        remote.id !in nativeAchievementIds && profileList.none { it.id == remote.id }
    })
}

object RemoteBadgeEngineClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var lastCatalogRefreshAt = 0L
    @Volatile private var reconcileInFlight = false
    @Volatile private var catalogRefreshInFlight = false
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
        if (!force && lastCatalogRefreshAt > 0L && now - lastCatalogRefreshAt < CATALOG_TTL_MS) {
            return remoteProfileBadgesState.value
        }
        val catalog = parseCatalog(call("catalog"))
        withContext(Dispatchers.Main.immediate) {
            publishCatalogIntoLegacySelectors(catalog)
            remoteProfileBadgesState.value = catalog
        }
        lastCatalogRefreshAt = now
        return catalog
    }

    /**
     * Carrega o catálogo automaticamente na primeira consulta feita pela UI.
     * O TTL evita chamadas repetidas e o lock evita várias requisições concorrentes.
     */
    fun ensureCatalogLoaded() {
        val now = System.currentTimeMillis()
        if (lastCatalogRefreshAt > 0L && now - lastCatalogRefreshAt < CATALOG_TTL_MS) return
        if (catalogRefreshInFlight) return
        catalogRefreshInFlight = true
        scope.launch {
            try {
                loadCatalog()
            } catch (error: Exception) {
                Log.w("RemoteBadgeEngine", "Não foi possível carregar o catálogo remoto automaticamente", error)
            } finally {
                catalogRefreshInFlight = false
            }
        }
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

                    val newlyUnlocked = newlyUnlockedIds.mapNotNull { id -> currentAllBiblicalBadges().firstOrNull { it.id == id } }
                    if (newlyUnlocked.isNotEmpty()) badgeAwardNotificationState.value = BadgeAwardNotification(newlyUnlocked)
                }
            } catch (error: Exception) {
                Log.w("RemoteBadgeEngine", "Reconciliação remota de emblemas falhou", error)
            } finally {
                reconcileInFlight = false
            }
        }
    }
}
