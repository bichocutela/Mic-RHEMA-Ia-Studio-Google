package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

data class XpDailyMissionState(
    val date: String = "",
    val chapter: Int = 0,
    val chapterTarget: Int = 1,
    val growth: Int = 0,
    val growthTarget: Int = 1,
    val quiz: Int = 0,
    val quizTarget: Int = 3,
    val activeBlocks: Int = 0,
    val activeBlocksTarget: Int = 2,
    val complete: Boolean = false,
    val bonusGranted: Boolean = false
)

data class XpJourneyState(
    val memberId: String,
    val streak: Int,
    val dailyMission: XpDailyMissionState = XpDailyMissionState()
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

private class XpHttpException(val status: Int, message: String) : IllegalStateException(message)

private data class PendingXpAward(
    val activity: String,
    val contentId: String,
    val variant: String = "",
    val selectedOptionIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val stableKey: String get() = "$activity\u001F$contentId\u001F$variant"

    fun toJson(): JSONObject = JSONObject()
        .put("activity", activity)
        .put("contentId", contentId)
        .put("variant", variant)
        .put("createdAt", createdAt)
        .also { json -> selectedOptionIndex?.let { json.put("selectedOptionIndex", it) } }

    companion object {
        fun fromJson(json: JSONObject): PendingXpAward? {
            val activity = json.optString("activity").trim()
            val contentId = json.optString("contentId").trim()
            if (activity.isBlank() || contentId.isBlank()) return null
            return PendingXpAward(
                activity = activity,
                contentId = contentId,
                variant = json.optString("variant").trim(),
                selectedOptionIndex = if (json.has("selectedOptionIndex")) json.optInt("selectedOptionIndex") else null,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

object XpEngineClient {
    private const val QUEUE_PREFS = "micrhema_xp_pending_awards"
    private const val MAX_PENDING = 200
    private val queueLock = Any()
    private val queueableActivities = setOf(
        "active_5min",
        "bible_verse",
        "bible_chapter",
        "devotional",
        "news_read",
        "plan_theme",
        "plan_day",
        "plan_complete",
        "ibr_lesson",
        "prayer_sent"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var lastRefreshAt = 0L
    @Volatile private var lastRefreshMemberId = ""

    private suspend fun firebaseToken(member: MemberRequest, forceRefresh: Boolean): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw XpHttpException(401, "Sua sessão de membro expirou. Entre novamente no MIC Rhema.")
        if (user.uid != member.id) {
            throw XpHttpException(401, "A sessão Firebase não pertence ao membro ativo. Entre novamente.")
        }
        return user.getIdToken(forceRefresh).await().token
            ?: throw XpHttpException(401, "O Firebase não retornou um token válido para a Jornada XP.")
    }

    private suspend fun executeCall(member: MemberRequest, payload: JSONObject, forceRefresh: Boolean): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O motor de XP não está configurado nesta versão.")
        }
        val token = firebaseToken(member, forceRefresh)
        payload.put("memberId", member.id)
        val builder = Request.Builder()
            .url("$baseUrl/functions/v1/xp-engine")
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

    private suspend fun call(member: MemberRequest, payload: JSONObject): JSONObject {
        var (status, body) = executeCall(member, payload, false)
        if (status == 401) {
            val retried = executeCall(member, payload, true)
            status = retried.first
            body = retried.second
        }
        if (status !in 200..299) {
            throw XpHttpException(status, body.optString("error").ifBlank { "Falha no motor de XP ($status)." })
        }
        return body
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

    private fun parseDailyMission(root: JSONObject?): XpDailyMissionState {
        val value = root ?: JSONObject()
        return XpDailyMissionState(
            date = value.optString("date"),
            chapter = value.optInt("chapter", 0).coerceAtLeast(0),
            chapterTarget = value.optInt("chapterTarget", 1).coerceAtLeast(1),
            growth = value.optInt("growth", 0).coerceAtLeast(0),
            growthTarget = value.optInt("growthTarget", 1).coerceAtLeast(1),
            quiz = value.optInt("quiz", 0).coerceAtLeast(0),
            quizTarget = value.optInt("quizTarget", 3).coerceAtLeast(1),
            activeBlocks = value.optInt("activeBlocks", 0).coerceAtLeast(0),
            activeBlocksTarget = value.optInt("activeBlocksTarget", 2).coerceAtLeast(1),
            complete = value.optBoolean("complete", false),
            bonusGranted = value.optBoolean("bonusGranted", false)
        )
    }

    private fun pendingKey(memberId: String) = "member:$memberId"

    private fun readPendingLocked(context: Context, memberId: String): MutableList<PendingXpAward> {
        val raw = context.applicationContext
            .getSharedPreferences(QUEUE_PREFS, Context.MODE_PRIVATE)
            .getString(pendingKey(memberId), "[]")
            .orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                PendingXpAward.fromJson(array.optJSONObject(index) ?: continue)?.let(::add)
            }
        }.toMutableList()
    }

    private fun writePendingLocked(context: Context, memberId: String, pending: List<PendingXpAward>) {
        val array = JSONArray()
        pending.takeLast(MAX_PENDING).forEach { array.put(it.toJson()) }
        context.applicationContext
            .getSharedPreferences(QUEUE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(pendingKey(memberId), array.toString())
            .apply()
    }

    private fun enqueuePending(context: Context, memberId: String, award: PendingXpAward) {
        synchronized(queueLock) {
            val pending = readPendingLocked(context, memberId)
            if (pending.none { it.stableKey == award.stableKey }) pending.add(award)
            writePendingLocked(context, memberId, pending)
        }
    }

    private fun removePending(context: Context, memberId: String, stableKey: String) {
        synchronized(queueLock) {
            val pending = readPendingLocked(context, memberId)
            if (pending.removeAll { it.stableKey == stableKey }) writePendingLocked(context, memberId, pending)
        }
    }

    private fun pendingSnapshot(context: Context, memberId: String): List<PendingXpAward> = synchronized(queueLock) {
        readPendingLocked(context, memberId).toList()
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
            streak = response.optInt("streak", 0).coerceAtLeast(0),
            dailyMission = parseDailyMission(response.optJSONObject("dailyMission"))
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

    suspend fun flushPendingNow(context: Context, member: MemberRequest = loggedInMemberState.value ?: return 0): Int {
        var processed = 0
        for (pending in pendingSnapshot(context, member.id)) {
            try {
                awardNow(
                    member = member,
                    activity = pending.activity,
                    contentId = pending.contentId,
                    variant = pending.variant,
                    selectedOptionIndex = pending.selectedOptionIndex
                )
                removePending(context, member.id, pending.stableKey)
                processed++
            } catch (error: XpHttpException) {
                if (error.status in setOf(400, 403, 409)) {
                    removePending(context, member.id, pending.stableKey)
                } else {
                    break
                }
            } catch (_: Throwable) {
                break
            }
        }
        return processed
    }

    fun refresh(context: Context, member: MemberRequest? = loggedInMemberState.value, force: Boolean = false) {
        val activeMember = member ?: return
        val now = System.currentTimeMillis()
        if (!force && activeMember.id == lastRefreshMemberId && now - lastRefreshAt < 60_000L) {
            scope.launch { flushPendingNow(context.applicationContext, activeMember) }
            return
        }
        scope.launch {
            try {
                refreshNow(activeMember)
                flushPendingNow(context.applicationContext, activeMember)
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
        if (activity !in queueableActivities) {
            Log.d("XpEngineClient", "Atividade $activity usa uma autoridade dedicada e não passa pelo motor genérico.")
            return
        }
        val cleanId = contentId.trim()
        if (cleanId.isBlank()) return
        val pending = PendingXpAward(activity, cleanId, variant.trim(), selectedOptionIndex)
        enqueuePending(context, member.id, pending)

        scope.launch {
            try {
                val result = awardNow(member, pending.activity, pending.contentId, pending.variant, pending.selectedOptionIndex)
                removePending(context, member.id, pending.stableKey)
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (error: XpHttpException) {
                if (error.status in setOf(400, 403, 409)) removePending(context, member.id, pending.stableKey)
                Log.w("XpEngineClient", "Falha ao registrar XP: $activity", error)
                withContext(Dispatchers.Main) { xpSyncErrorState.value = error.message.orEmpty() }
            } catch (error: Throwable) {
                Log.w("XpEngineClient", "XP pendente para nova tentativa: $activity", error)
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
