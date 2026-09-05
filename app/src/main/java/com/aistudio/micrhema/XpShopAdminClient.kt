package com.aistudio.micrhema

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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

object XpShopAdminClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private suspend fun adminToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Sessão administrativa do Firebase não encontrada.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("O Firebase não retornou o token do administrador.")
    }

    private suspend fun call(action: String, configure: (JSONObject.() -> Unit)? = null): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("A Loja XP não está configurada nesta versão.")
        }
        val payload = JSONObject().put("action", action)
        configure?.invoke(payload)
        val token = adminToken()
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-shop")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha na administração da Loja XP (${response.code})." })
            }
            json
        }
    }

    suspend fun loadCatalog(): List<AdminXpShopItem> {
        val response = call("admin_catalog")
        val array = response.optJSONArray("items") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AdminXpShopItem(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        description = item.optString("description"),
                        cost = item.optInt("cost", 0),
                        category = item.optString("category"),
                        kind = item.optString("kind"),
                        imageUrl = item.optString("image_url"),
                        stock = if (item.isNull("stock")) null else item.optInt("stock"),
                        limitPerMember = item.optInt("limit_per_member", 1).coerceAtLeast(1),
                        active = item.optBoolean("active", true),
                        availableFrom = item.optString("available_from"),
                        availableUntil = item.optString("available_until")
                    )
                )
            }
        }
    }

    suspend fun saveItem(item: AdminXpShopItem): AdminXpShopItem {
        val response = call("admin_upsert_item") {
            put("id", item.id)
            put("name", item.name)
            put("description", item.description)
            put("cost", item.cost)
            put("category", item.category)
            put("kind", item.kind)
            put("imageUrl", item.imageUrl)
            if (item.stock == null) put("stock", JSONObject.NULL) else put("stock", item.stock)
            put("limitPerMember", item.limitPerMember)
            put("active", item.active)
            put("availableFrom", item.availableFrom)
            put("availableUntil", item.availableUntil)
        }
        val raw = response.optJSONObject("item") ?: throw IllegalStateException("A recompensa não foi salva.")
        return AdminXpShopItem(
            id = raw.optString("id"),
            name = raw.optString("name"),
            description = raw.optString("description"),
            cost = raw.optInt("cost"),
            category = raw.optString("category"),
            kind = raw.optString("kind"),
            imageUrl = raw.optString("image_url"),
            stock = if (raw.isNull("stock")) null else raw.optInt("stock"),
            limitPerMember = raw.optInt("limit_per_member", 1).coerceAtLeast(1),
            active = raw.optBoolean("active", true),
            availableFrom = raw.optString("available_from"),
            availableUntil = raw.optString("available_until")
        )
    }

    suspend fun loadRedemptions(status: String = "todos"): List<AdminXpRedemption> {
        val response = call("admin_redemptions") { put("status", status) }
        val array = response.optJSONArray("redemptions") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AdminXpRedemption(
                        id = item.optString("id"),
                        memberId = item.optString("member_id"),
                        memberName = item.optString("member_name"),
                        itemId = item.optString("item_id"),
                        itemName = item.optString("item_name"),
                        cost = item.optInt("cost"),
                        status = item.optString("status"),
                        code = item.optString("redemption_code"),
                        createdAt = item.optString("created_at"),
                        deliveredAt = item.optString("delivered_at")
                    )
                )
            }
        }
    }

    suspend fun updateRedemptionStatus(redemptionId: String, status: String) {
        call("admin_set_redemption_status") {
            put("redemptionId", redemptionId)
            put("status", status)
        }
    }
}
