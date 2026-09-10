package com.aistudio.micrhema

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AdminXpShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val category: String,
    val kind: String,
    val imageUrl: String,
    val stock: Int?,
    val limitPerMember: Int,
    val active: Boolean,
    val availableFrom: String,
    val availableUntil: String
)

data class AdminXpRedemption(
    val id: String,
    val memberId: String,
    val memberName: String,
    val itemId: String,
    val itemName: String,
    val cost: Int,
    val status: String,
    val code: String,
    val createdAt: String,
    val deliveredAt: String
)

data class AdminCustomBadge(
    val id: String,
    val sequenceNo: Int?,
    val name: String,
    val description: String,
    val challenge: String,
    val imageRef: String,
    val special: Boolean,
    val active: Boolean
)

data class AdminProfileCosmetic(
    val id: String,
    val kind: String,
    val name: String,
    val description: String,
    val challenge: String,
    val imageRef: String,
    val active: Boolean,
    val purchasable: Boolean = false,
    val xpCost: Int = 0,
    val emblemIds: List<String> = emptyList(),
    val freeForAll: Boolean = false
)

data class GeneratedBadgeChallenge(
    val difficulty: String,
    val text: String,
    val metric: String,
    val target: Int
)

object XpShopAdminClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private suspend fun call(action: String, configure: (JSONObject.() -> Unit)? = null): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || baseUrl.contains("your-project")) throw IllegalStateException("A Loja XP não está configurada nesta versão.")
        if (action != "cosmetics_catalog" && !adminAuthenticatedState.value) throw IllegalStateException("Abra a Área Administrativa antes de editar a Loja XP.")
        val payload = JSONObject().put("action", action)
        configure?.invoke(payload)
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-shop-admin-simple")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) throw IllegalStateException(json.optString("error").ifBlank { "Falha na administração da Loja XP (${response.code})." })
            json
        }
    }

    suspend fun loadCatalog(): List<AdminXpShopItem> {
        val array = call("admin_catalog").optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(AdminXpShopItem(
                    id = item.optString("id"), name = item.optString("name"), description = item.optString("description"),
                    cost = item.optInt("cost", 0), category = item.optString("category"), kind = item.optString("kind"),
                    imageUrl = item.optString("image_url"), stock = if (item.isNull("stock")) null else item.optInt("stock"),
                    limitPerMember = item.optInt("limit_per_member", 1).coerceAtLeast(1), active = item.optBoolean("active", true),
                    availableFrom = item.optString("available_from"), availableUntil = item.optString("available_until")
                ))
            }
        }
    }

    suspend fun saveItem(item: AdminXpShopItem): AdminXpShopItem {
        val response = call("admin_upsert_item") {
            put("id", item.id); put("name", item.name); put("description", item.description); put("cost", item.cost)
            put("category", item.category); put("kind", item.kind); put("imageUrl", item.imageUrl)
            if (item.stock == null) put("stock", JSONObject.NULL) else put("stock", item.stock)
            put("limitPerMember", item.limitPerMember); put("active", item.active); put("availableFrom", item.availableFrom); put("availableUntil", item.availableUntil)
        }
        val raw = response.optJSONObject("item") ?: throw IllegalStateException("A recompensa não foi salva.")
        return AdminXpShopItem(
            id = raw.optString("id"), name = raw.optString("name"), description = raw.optString("description"), cost = raw.optInt("cost"),
            category = raw.optString("category"), kind = raw.optString("kind"), imageUrl = raw.optString("image_url"),
            stock = if (raw.isNull("stock")) null else raw.optInt("stock"), limitPerMember = raw.optInt("limit_per_member", 1).coerceAtLeast(1),
            active = raw.optBoolean("active", true), availableFrom = raw.optString("available_from"), availableUntil = raw.optString("available_until")
        )
    }

    private fun parseBadge(item: JSONObject): AdminCustomBadge = AdminCustomBadge(
        id = item.optString("id"), sequenceNo = if (item.isNull("sequence_no")) null else item.optInt("sequence_no"), name = item.optString("name"),
        description = item.optString("description"), challenge = item.optString("challenge"), imageRef = item.optString("image_ref"),
        special = item.optBoolean("special", false), active = item.optBoolean("active", true)
    )

    suspend fun loadBadges(): List<AdminCustomBadge> {
        val array = call("admin_badges").optJSONArray("badges") ?: return emptyList()
        return buildList { for (index in 0 until array.length()) array.optJSONObject(index)?.let { add(parseBadge(it)) } }
    }

    suspend fun generateBadgeChallenge(difficulty: String, exclude: String = ""): GeneratedBadgeChallenge {
        val normalized = difficulty.trim().lowercase().takeIf { it in setOf("easy", "medium", "hard") } ?: throw IllegalArgumentException("Dificuldade inválida.")
        val raw = call("admin_generate_badge_challenge") { put("difficulty", normalized); put("exclude", exclude.trim()) }
            .optJSONObject("challenge") ?: throw IllegalStateException("Não foi possível gerar o desafio.")
        return GeneratedBadgeChallenge(
            difficulty = raw.optString("difficulty", normalized), text = raw.optString("text"), metric = raw.optString("metric"), target = raw.optInt("target", 0)
        ).also { require(it.text.isNotBlank() && it.metric.isNotBlank() && it.target > 0) { "O servidor retornou um desafio inválido." } }
    }

    suspend fun saveBadge(item: AdminCustomBadge): AdminCustomBadge {
        val response = call("admin_upsert_badge") {
            put("id", item.id); put("name", item.name); put("description", item.description); put("challenge", item.challenge)
            put("imageRef", item.imageRef); put("special", item.special); put("active", item.active)
            item.sequenceNo?.let { put("sequenceNo", it) }
        }
        return parseBadge(response.optJSONObject("badge") ?: throw IllegalStateException("O emblema não foi salvo."))
    }

    private fun parseCosmetic(item: JSONObject): AdminProfileCosmetic = AdminProfileCosmetic(
        id = item.optString("id"), kind = item.optString("kind"), name = item.optString("name"),
        description = item.optString("description"), challenge = item.optString("challenge"),
        imageRef = item.optString("image_ref"), active = item.optBoolean("active", true),
        purchasable = item.optBoolean("purchasable", false), xpCost = item.optInt("xp_cost", 0),
        emblemIds = item.optJSONArray("emblem_ids")?.let { a -> List(a.length()) { a.optString(it) } } ?: emptyList(),
        freeForAll = item.optBoolean("free_for_all", false)
    )

    suspend fun loadCosmetics(kind: String): List<AdminProfileCosmetic> {
        require(kind in setOf("distintivo", "moldura")) { "Tipo de personalização inválido." }
        val array = call("admin_cosmetics") { put("kind", kind); put("includeBuiltins", true) }.optJSONArray("items") ?: return emptyList()
        return buildList { for (index in 0 until array.length()) array.optJSONObject(index)?.let { add(parseCosmetic(it)) } }
    }

    suspend fun publicCosmetics(): List<AdminProfileCosmetic> {
        return buildList {
            for (kind in listOf("distintivo", "moldura")) {
                val a = call("cosmetics_catalog") { put("kind", kind); put("includeBuiltins", true) }.optJSONArray("items")
                    ?: throw IllegalStateException("Catálogo de personalizações incompleto.")
                for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(parseCosmetic(it)) }
            }
        }
    }

    suspend fun saveCosmetic(item: AdminProfileCosmetic): AdminProfileCosmetic {
        val response = call("admin_upsert_cosmetic") {
            put("id", item.id); put("kind", item.kind); put("name", item.name); put("description", item.description)
            put("challenge", item.challenge); put("imageRef", item.imageRef); put("active", item.active)
            put("purchasable", item.purchasable); put("xpCost", item.xpCost); put("freeForAll", item.freeForAll)
            put("emblemIds", org.json.JSONArray(item.emblemIds))
        }
        val saved = parseCosmetic(response.optJSONObject("item") ?: throw IllegalStateException("A personalização não foi salva."))
        withContext(Dispatchers.Main) { DistinctiveCatalog.items.value = DistinctiveCatalog.items.value.filterNot { it.id == saved.id } + saved }
        return saved
    }

    suspend fun loadRedemptions(status: String = "todos"): List<AdminXpRedemption> {
        val array = call("admin_redemptions") { put("status", status) }.optJSONArray("redemptions") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(AdminXpRedemption(
                    id = item.optString("id"), memberId = item.optString("member_id"), memberName = item.optString("member_name"),
                    itemId = item.optString("item_id"), itemName = item.optString("item_name"), cost = item.optInt("cost"), status = item.optString("status"),
                    code = item.optString("redemption_code"), createdAt = item.optString("created_at"), deliveredAt = item.optString("delivered_at")
                ))
            }
        }
    }

    suspend fun updateRedemptionStatus(redemptionId: String, status: String) {
        call("admin_set_redemption_status") { put("redemptionId", redemptionId); put("status", status) }
    }
}
