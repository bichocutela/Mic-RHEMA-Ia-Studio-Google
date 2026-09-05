package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class XpShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val category: String,
    val kind: String,
    val imageUrl: String,
    val stock: Int?,
    val limitPerMember: Int,
    val active: Boolean
)

data class XpRedemption(
    val id: String,
    val itemId: String,
    val itemName: String,
    val cost: Int,
    val status: String,
    val code: String,
    val createdAt: String,
    val deliveredAt: String = ""
)

data class XpRedeemResult(
    val redemption: XpRedemption,
    val account: XpAccount
)

data class XpShopCatalogState(
    val memberId: String,
    val items: List<XpShopItem>
)

data class XpRedemptionsState(
    val memberId: String,
    val redemptions: List<XpRedemption>
)

val xpShopItemsState = mutableStateOf<XpShopCatalogState?>(null)
val xpRedemptionsState = mutableStateOf<XpRedemptionsState?>(null)
val xpShopErrorState = mutableStateOf("")

object XpShopClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private suspend fun call(member: MemberRequest, action: String, itemId: String = ""): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("A Loja XP não está configurada nesta versão.")
        }
        val payload = JSONObject()
            .put("action", action)
            .put("memberId", member.id)
            .put("phone", normalizePhone(member.phone))
        if (itemId.isNotBlank()) payload.put("itemId", itemId)

        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-shop")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha na Loja XP (${response.code})." })
            }
            json
        }
    }

    private fun parseAccount(memberId: String, root: JSONObject): XpAccount {
        val account = root.optJSONObject("account") ?: JSONObject()
        return XpAccount(
            memberId = memberId,
            unlocked = root.optBoolean("unlocked", false),
            totalEarned = account.optInt("total_earned", 0).coerceAtLeast(0),
            totalSpent = account.optInt("total_spent", 0).coerceAtLeast(0),
            balance = account.optInt("balance", 0).coerceAtLeast(0),
            migratedLegacyXp = account.optInt("migrated_legacy_xp", 0).coerceAtLeast(0),
            updatedAt = account.optString("updated_at")
        )
    }

    suspend fun loadCatalog(member: MemberRequest): List<XpShopItem> {
        val response = call(member, "catalog")
        val account = parseAccount(member.id, response)
        val array = response.optJSONArray("items")
        val items = buildList {
            if (array != null) for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    XpShopItem(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        description = item.optString("description"),
                        cost = item.optInt("cost", 0),
                        category = item.optString("category"),
                        kind = item.optString("kind"),
                        imageUrl = item.optString("image_url"),
                        stock = if (item.isNull("stock")) null else item.optInt("stock"),
                        limitPerMember = item.optInt("limit_per_member", 1).coerceAtLeast(1),
                        active = item.optBoolean("active", true)
                    )
                )
            }
        }
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpShopItemsState.value = XpShopCatalogState(member.id, items)
            xpShopErrorState.value = ""
        }
        return items
    }

    suspend fun loadRedemptions(member: MemberRequest): List<XpRedemption> {
        val response = call(member, "my_redemptions")
        val account = parseAccount(member.id, response)
        val array = response.optJSONArray("redemptions")
        val items = buildList {
            if (array != null) for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    XpRedemption(
                        id = item.optString("id"),
                        itemId = item.optString("item_id"),
                        itemName = item.optString("item_name"),
                        cost = item.optInt("cost", 0),
                        status = item.optString("status"),
                        code = item.optString("redemption_code"),
                        createdAt = item.optString("created_at"),
                        deliveredAt = item.optString("delivered_at")
                    )
                )
            }
        }
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpRedemptionsState.value = XpRedemptionsState(member.id, items)
            xpShopErrorState.value = ""
        }
        return items
    }

    suspend fun redeem(member: MemberRequest, item: XpShopItem): XpRedeemResult {
        val response = call(member, "redeem", item.id)
        val account = parseAccount(member.id, response)
        val raw = response.optJSONObject("redemption") ?: throw IllegalStateException("O resgate não foi confirmado.")
        val redemption = XpRedemption(
            id = raw.optString("redemption_id"),
            itemId = item.id,
            itemName = raw.optString("item_name", item.name),
            cost = raw.optInt("item_cost", item.cost),
            status = raw.optString("redemption_status"),
            code = raw.optString("redemption_code"),
            createdAt = ""
        )
        withContext(Dispatchers.Main) {
            val current = xpRedemptionsState.value
                ?.takeIf { it.memberId == member.id }
                ?.redemptions
                .orEmpty()
            xpAccountState.value = account
            xpRedemptionsState.value = XpRedemptionsState(
                member.id,
                listOf(redemption) + current.filterNot { it.id == redemption.id }
            )
            xpShopErrorState.value = ""
        }
        return XpRedeemResult(redemption, account)
    }

    fun clearSession() {
        xpShopItemsState.value = null
        xpRedemptionsState.value = null
        xpShopErrorState.value = ""
    }
}
