package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AdminLightEffect(
    val id: String,
    val name: String,
    val description: String,
    val effectType: String,
    val tone: String,
    val colorHex: String,
    val purchasable: Boolean = false,
    val xpCost: Int = 0,
    val emblemIds: List<String> = emptyList(),
    val active: Boolean = true
)

object XpLightEffectsAdminClient {
    val catalog = mutableStateOf<List<AdminLightEffect>>(emptyList())
    private val refreshMutex = Mutex()
    private var refreshedAt = 0L

    suspend fun refreshPublicCatalog() = refreshMutex.withLock {
        if (android.os.SystemClock.elapsedRealtime() - refreshedAt < 60_000L && refreshedAt != 0L) return@withLock
        val items = readCatalog()
        withContext(Dispatchers.Main) { catalog.value = items }
        refreshedAt = android.os.SystemClock.elapsedRealtime()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private suspend fun call(action: String, configure: (JSONObject.() -> Unit)? = null): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("A Loja XP não está configurada nesta versão.")
        }
        if (action != "list" && !adminAuthenticatedState.value) {
            throw IllegalStateException("Abra a Área Administrativa antes de editar os efeitos de luz.")
        }

        val payload = JSONObject().put("action", action)
        configure?.invoke(payload)
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-light-effects-admin")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha ao sincronizar efeitos de luz (${response.code})." })
            }
            json
        }
    }

    private fun parse(item: JSONObject): AdminLightEffect {
        val emblems = item.optJSONArray("emblem_ids") ?: JSONArray()
        return AdminLightEffect(
            id = item.optString("id"),
            name = item.optString("name"),
            description = item.optString("description"),
            effectType = item.optString("effect_type", "orbit"),
            tone = item.optString("tone", "medio"),
            colorHex = item.optString("color_hex", "#FFD54F"),
            purchasable = item.optBoolean("purchasable", false),
            xpCost = item.optInt("xp_cost", 0),
            emblemIds = buildList {
                for (index in 0 until emblems.length()) {
                    emblems.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            },
            active = item.optBoolean("active", true)
        )
    }

    private suspend fun readCatalog(): List<AdminLightEffect> {
        val array = call("list").optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(parse(it)) }
            }
        }
    }

    suspend fun load(): List<AdminLightEffect> {
        val items = readCatalog()
        withContext(Dispatchers.Main) { catalog.value = items }
        return items
    }

    suspend fun save(item: AdminLightEffect): AdminLightEffect {
        val response = call("upsert") {
            put("id", item.id)
            put("name", item.name)
            put("description", item.description)
            put("effectType", item.effectType)
            put("tone", item.tone)
            put("colorHex", item.colorHex)
            put("purchasable", item.purchasable)
            put("xpCost", item.xpCost)
            put("emblemIds", JSONArray(item.emblemIds))
            put("active", item.active)
        }
        val saved = parse(response.optJSONObject("item") ?: throw IllegalStateException("O efeito não foi salvo."))
        withContext(Dispatchers.Main) { catalog.value = catalog.value.filterNot { it.id == saved.id } + saved }
        return saved
    }

    suspend fun delete(id: String) {
        call("delete") { put("id", id) }
        withContext(Dispatchers.Main) { catalog.value = catalog.value.filterNot { it.id == id } }
    }
}

/** Only effects granted to this emblem or confirmed in the purchase ledger. */
fun availableProfileLightEffects(context: android.content.Context, memberId: String?, badgeId: String): List<AdminLightEffect> =
    XpLightEffectsAdminClient.catalog.value.filter { item ->
        item.active && if (item.purchasable) {
            memberId != null && XpRewardManager.isOwned(context, item.id, memberId) &&
                (item.emblemIds.isEmpty() || badgeId in item.emblemIds)
        } else badgeId in item.emblemIds
    }
