package com.aistudio.micrhema

import android.content.Context
import androidx.compose.runtime.mutableStateOf
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

data class XpEntitlement(
    val id: String,
    val itemId: String,
    val itemName: String,
    val kind: String,
    val unlockedAt: String
)

data class XpRedeemResult(
    val redemption: XpRedemption,
    val account: XpAccount
)

data class XpShopCatalogState(val memberId: String, val items: List<XpShopItem>)
data class XpRedemptionsState(val memberId: String, val redemptions: List<XpRedemption>)
data class XpEntitlementsState(val memberId: String, val entitlements: List<XpEntitlement>)

val xpShopItemsState = mutableStateOf<XpShopCatalogState?>(null)
val xpRedemptionsState = mutableStateOf<XpRedemptionsState?>(null)
val xpEntitlementsState = mutableStateOf<XpEntitlementsState?>(null)
val xpShopErrorState = mutableStateOf("")

private class XpShopHttpException(val status: Int, message: String) : IllegalStateException(message)

object XpShopClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private suspend fun firebaseToken(member: MemberRequest, forceRefresh: Boolean): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw XpShopHttpException(401, "Sua sessão de membro expirou. Entre novamente no MIC Rhema.")
        if (user.uid != member.id) {
            throw XpShopHttpException(401, "A sessão Firebase não pertence ao membro ativo. Entre novamente.")
        }
        return user.getIdToken(forceRefresh).await().token
            ?: throw XpShopHttpException(401, "O Firebase não retornou um token válido para a Loja XP.")
    }

    private suspend fun executeCall(
        member: MemberRequest,
        action: String,
        itemId: String,
        expectedCost: Int?,
        forceRefresh: Boolean
    ): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("A Loja XP não está configurada nesta versão.")
        }
        val token = firebaseToken(member, forceRefresh)
        val payload = JSONObject()
            .put("action", action)
            .put("memberId", member.id)
        if (itemId.isNotBlank()) payload.put("itemId", itemId)
        if (expectedCost != null) payload.put("expectedCost", expectedCost)

        val builder = Request.Builder()
            .url("$baseUrl/functions/v1/xp-shop")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
        if (anonKey.isNotBlank()) builder.header("apikey", anonKey)
        val request = builder
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            response.code to runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        }
    }

    private suspend fun call(member: MemberRequest, action: String, itemId: String = "", expectedCost: Int? = null): JSONObject {
        var (status, body) = executeCall(member, action, itemId, expectedCost, false)
        if (status == 401) {
            val retry = executeCall(member, action, itemId, expectedCost, true)
            status = retry.first
            body = retry.second
        }
        if (status !in 200..299) {
            throw XpShopHttpException(status, body.optString("error").ifBlank { "Falha na Loja XP ($status)." })
        }
        return body
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

    private fun parseEntitlements(root: JSONObject): List<XpEntitlement> {
        val array = root.optJSONArray("entitlements") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    XpEntitlement(
                        id = item.optString("id"),
                        itemId = item.optString("item_id"),
                        itemName = item.optString("item_name"),
                        kind = item.optString("kind"),
                        unlockedAt = item.optString("unlocked_at")
                    )
                )
            }
        }
    }

    private fun syncEntitlements(context: Context?, memberId: String, entitlements: List<XpEntitlement>) {
        context?.let { XpRewardManager.syncOwned(it, memberId, entitlements.map { entitlement -> entitlement.itemId }) }
        xpEntitlementsState.value = XpEntitlementsState(memberId, entitlements)
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

    suspend fun loadRedemptions(member: MemberRequest, context: Context? = null): List<XpRedemption> {
        val response = call(member, "my_redemptions")
        val account = parseAccount(member.id, response)
        val array = response.optJSONArray("redemptions")
        val redemptions = buildList {
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
        val entitlements = parseEntitlements(response)
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpRedemptionsState.value = XpRedemptionsState(member.id, redemptions)
            syncEntitlements(context, member.id, entitlements)
            xpShopErrorState.value = ""
        }
        return redemptions
    }

    suspend fun redeem(member: MemberRequest, item: XpShopItem, context: Context? = null): XpRedeemResult {
        val response = call(member, "redeem", item.id, item.cost)
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
        val current = xpRedemptionsState.value
            ?.takeIf { it.memberId == member.id }
            ?.redemptions
            .orEmpty()
        val updated = listOf(redemption) + current.filterNot { it.id == redemption.id }
        val entitlements = parseEntitlements(response)
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpRedemptionsState.value = XpRedemptionsState(member.id, updated)
            if (response.has("entitlements")) syncEntitlements(context, member.id, entitlements)
            xpShopErrorState.value = ""
        }
        return XpRedeemResult(redemption, account)
    }

    fun clearSession() {
        xpShopItemsState.value = null
        xpRedemptionsState.value = null
        xpEntitlementsState.value = null
        xpShopErrorState.value = ""
    }
}
