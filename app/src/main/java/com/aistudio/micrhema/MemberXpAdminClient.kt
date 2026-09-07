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

data class AdminMemberXpSnapshot(
    val memberId: String,
    val totalEarned: Int,
    val totalSpent: Int,
    val balance: Int,
    val transactions: List<XpTransaction>
)

object MemberXpAdminClient {
    private const val ADMIN_EMAIL = "admin@micrhema.app"
    private const val ADMIN_PASSWORD = "igreja10"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private suspend fun ensureRemoteAdminSession(forceRefresh: Boolean): String {
        if (!adminAuthenticatedState.value) {
            throw IllegalStateException("Abra a Área Administrativa antes de alterar XP.")
        }

        val auth = FirebaseAuth.getInstance()
        val currentEmail = auth.currentUser?.email?.trim()?.lowercase()
        if (currentEmail != ADMIN_EMAIL) {
            auth.signOut()
            try {
                auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
            } catch (error: Exception) {
                throw IllegalStateException("Não foi possível sincronizar a sessão administrativa com o Firebase.", error)
            }
        }

        val user = auth.currentUser
            ?: throw IllegalStateException("A sessão administrativa do Firebase não está disponível.")
        if (user.email?.trim()?.lowercase() != ADMIN_EMAIL) {
            throw IllegalStateException("A sessão Firebase atual não é a sessão administrativa.")
        }

        return user.getIdToken(forceRefresh).await().token
            ?: throw IllegalStateException("Não foi possível validar a sessão administrativa.")
    }

    private suspend fun executeCall(
        action: String,
        memberId: String,
        amount: Int?,
        forceRefresh: Boolean
    ): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val apiKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O backend de XP não está configurado nesta versão.")
        }
        require(memberId.isNotBlank()) { "Membro inválido." }

        val payload = JSONObject()
            .put("action", action)
            .put("memberId", memberId)
        amount?.let { payload.put("amount", it) }

        val token = ensureRemoteAdminSession(forceRefresh)
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-member-admin")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            response.code to runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        }
    }

    private suspend fun call(action: String, memberId: String, amount: Int? = null): JSONObject {
        var (status, body) = executeCall(action, memberId, amount, false)
        if (status == 401 || status == 403) {
            val retry = executeCall(action, memberId, amount, true)
            status = retry.first
            body = retry.second
        }
        if (status !in 200..299) {
            throw IllegalStateException(
                body.optString("error").ifBlank { "Falha ao consultar o XP do membro ($status)." }
            )
        }
        return body
    }

    private fun parseSnapshot(root: JSONObject): AdminMemberXpSnapshot {
        val account = root.optJSONObject("account") ?: JSONObject()
        val memberId = account.optString("member_id", root.optString("memberId"))
        val history = root.optJSONArray("transactions")
        val transactions = buildList {
            if (history != null) {
                for (index in 0 until history.length()) {
                    val item = history.optJSONObject(index) ?: continue
                    add(
                        XpTransaction(
                            id = item.optString("id"),
                            type = item.optString("type"),
                            amount = item.optInt("amount", 0).coerceAtLeast(0),
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
        return AdminMemberXpSnapshot(
            memberId = memberId,
            totalEarned = account.optInt("total_earned", 0).coerceAtLeast(0),
            totalSpent = account.optInt("total_spent", 0).coerceAtLeast(0),
            balance = account.optInt("balance", 0).coerceAtLeast(0),
            transactions = transactions
        )
    }

    suspend fun load(memberId: String): AdminMemberXpSnapshot =
        parseSnapshot(call("member_xp", memberId))

    suspend fun addExtra(memberId: String, amount: Int): AdminMemberXpSnapshot {
        require(amount > 0) { "Informe uma quantidade de XP maior que zero." }
        val snapshot = parseSnapshot(call("add_extra", memberId, amount))
        withContext(Dispatchers.Main.immediate) {
            if (loggedInMemberState.value?.id == memberId) {
                xpAccountState.value = XpAccount(
                    memberId = memberId,
                    unlocked = true,
                    totalEarned = snapshot.totalEarned,
                    totalSpent = snapshot.totalSpent,
                    balance = snapshot.balance
                )
            }
        }
        return snapshot
    }
}
