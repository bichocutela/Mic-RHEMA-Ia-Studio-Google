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

data class XpAccount(
    val memberId: String,
    val unlocked: Boolean,
    val totalEarned: Int,
    val totalSpent: Int,
    val balance: Int,
    val migratedLegacyXp: Int = 0,
    val updatedAt: String = ""
)

data class XpTransaction(
    val id: String,
    val type: String,
    val amount: Int,
    val activity: String,
    val contentId: String,
    val variant: String,
    val receiptId: String,
    val description: String,
    val dateKey: String,
    val createdAt: String
)

data class XpAwardResult(
    val granted: Int,
    val duplicate: Boolean,
    val reason: String,
    val account: XpAccount
)

data class XpJourneyState(
    val memberId: String,
    val streak: Int
)

data class XpHistorySnapshot(
    val memberId: String,
    val transactions: List<XpTransaction>
)

val xpAccountState = mutableStateOf<XpAccount?>(null)
val xpHistoryState = mutableStateOf<XpHistorySnapshot?>(null)
val xpJourneyState = mutableStateOf<XpJourneyState?>(null)
val xpSyncErrorState = mutableStateOf("")

fun isXpUnlocked(member: MemberRequest): Boolean {
    val unlocked = member.unlockedBadgeIds.toSet()
    return biblicalLevelBadges.any { badge ->
        (badge.level ?: 0) >= 8 && badge.id in unlocked
    }
}

object XpEngineClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var lastRefreshAt = 0L
    @Volatile private var lastRefreshMemberId = ""

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private suspend fun call(member: MemberRequest, payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O motor de XP não está configurado nesta versão.")
        }
        payload.put("memberId", member.id)
        payload.put("phone", normalizePhone(member.phone))
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-engine")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha no motor de XP (${response.code})." })
            }
            json
        }
    }

    private fun parseAccount(memberId: String, root: JSONObject): XpAccount {
        val account = root.optJSONObject("account") ?: JSONObject()
        return XpAccount(
            memberId = memberId,
            unlocked = root.optBoolean("unlocked", false),
            totalEarned = account.optInt("total_earned", account.optInt("totalEarned", 0)).coerceAtLeast(0),
            totalSpent = account.optInt("total_spent", account.optInt("totalSpent", 0)).coerceAtLeast(0),
            balance = account.optInt("balance", 0).coerceAtLeast(0),
            migratedLegacyXp = account.optInt("migrated_legacy_xp", account.optInt("migratedLegacyXp", 0)).coerceAtLeast(0),
            updatedAt = account.optString("updated_at", account.optString("updatedAt"))
        )
    }

    suspend fun refreshNow(
        member: MemberRequest = loggedInMemberState.value ?: throw IllegalStateException("Entre no MIC Rhema.")
    ): XpAccount {
        val response = call(member, JSONObject().put("action", "get_account"))
        val account = parseAccount(member.id, response)
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpSyncErrorState.value = ""
        }
        lastRefreshMemberId = member.id
        lastRefreshAt = System.currentTimeMillis()
        return account
    }

    suspend fun loadJourneyStateNow(
        member: MemberRequest = loggedInMemberState.value ?: throw IllegalStateException("Entre no MIC Rhema.")
    ): XpJourneyState {
        val response = call(member, JSONObject().put("action", "journey_state"))
        val account = parseAccount(member.id, response)
        val state = XpJourneyState(
            memberId = member.id,
            streak = response.optInt("streak", 0).coerceAtLeast(0)
        )
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpJourneyState.value = state
            xpSyncErrorState.value = ""
        }
        lastRefreshMemberId = member.id
        lastRefreshAt = System.currentTimeMillis()
        return state
    }

    fun refresh(context: Context, member: MemberRequest? = loggedInMemberState.value, force: Boolean = false) {
        val activeMember = member ?: return
        val now = System.currentTimeMillis()
        if (!force && activeMember.id == lastRefreshMemberId && now - lastRefreshAt < 60_000L) return
        scope.launch {
            try {
                refreshNow(activeMember)
            } catch (error: Throwable) {
                Log.w("XpEngineClient", "Falha ao atualizar saldo XP", error)
                withContext(Dispatchers.Main) { xpSyncErrorState.value = error.message.orEmpty() }
            }
        }
    }

    suspend fun awardNow(
        member: MemberRequest,
        activity: String,
        contentId: String,
        variant: String = "",
        selectedOptionIndex: Int? = null
    ): XpAwardResult {
        val payload = JSONObject()
            .put("action", "award")
            .put("activity", activity)
            .put("contentId", contentId)
            .put("variant", variant)
        if (selectedOptionIndex != null) payload.put("selectedOptionIndex", selectedOptionIndex)
        val response = call(member, payload)
        val account = parseAccount(member.id, response)
        val result = XpAwardResult(
            granted = response.optInt("granted", 0).coerceAtLeast(0),
            duplicate = response.optBoolean("duplicate", false),
            reason = response.optString("reason"),
            account = account
        )
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpSyncErrorState.value = ""
        }
        lastRefreshMemberId = member.id
        lastRefreshAt = System.currentTimeMillis()
        return result
    }

    fun award(
        context: Context,
        activity: String,
        contentId: String,
        variant: String = "",
        selectedOptionIndex: Int? = null,
        onResult: (XpAwardResult) -> Unit = {}
    ) {
        val member = loggedInMemberState.value ?: return
        val isQuizExtra = activity.startsWith("quiz_")
        if (!isXpUnlocked(member) && !isQuizExtra) {
            refresh(context, member)
            return
        }
        scope.launch {
            try {
                val result = awardNow(member, activity, contentId, variant, selectedOptionIndex)
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (error: Throwable) {
                Log.w("XpEngineClient", "Falha ao registrar XP: $activity", error)
                withContext(Dispatchers.Main) { xpSyncErrorState.value = error.message.orEmpty() }
            }
        }
    }

    suspend fun loadHistoryNow(
        member: MemberRequest = loggedInMemberState.value ?: throw IllegalStateException("Entre no MIC Rhema."),
        limit: Int = 50
    ): List<XpTransaction> {
        val response = call(member, JSONObject().put("action", "history").put("limit", limit.coerceIn(1, 100)))
        val account = parseAccount(member.id, response)
        val array = response.optJSONArray("transactions")
        val transactions = buildList {
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        XpTransaction(
                            id = item.optString("id"),
                            type = item.optString("type"),
                            amount = item.optInt("amount", 0),
                            activity = item.optString("activity"),
                            contentId = item.optString("content_id"),
                            variant = item.optString("variant"),
                            receiptId = item.optString("receipt_id"),
                            description = item.optString("description"),
                            dateKey = item.optString("date_key"),
                            createdAt = item.optString("created_at")
                        )
                    )
                }
            }
        }
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpHistoryState.value = XpHistorySnapshot(member.id, transactions)
            xpSyncErrorState.value = ""
        }
        return transactions
    }

    fun clearSession() {
        xpAccountState.value = null
        xpHistoryState.value = null
        xpJourneyState.value = null
        xpSyncErrorState.value = ""
        lastRefreshMemberId = ""
        lastRefreshAt = 0L
    }
}
