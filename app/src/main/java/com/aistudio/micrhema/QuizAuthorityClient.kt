package com.aistudio.micrhema

import android.content.Context
import android.util.Log
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
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit

data class QuizAuthorityAnswer(
    val correct: Boolean,
    val duplicate: Boolean,
    val granted: Int,
    val selectedOptionIndex: Int,
    val correctOptionIndex: Int,
    val hintUsed: BibleQuizHintUsage,
    val reference: String,
    val explanation: String,
    val account: XpAccount
)

data class QuizMissionClaimResult(
    val confirmed: Boolean,
    val granted: Int,
    val duplicate: Boolean,
    val reason: String
)

object QuizAuthorityClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val missionInFlight = Collections.synchronizedSet(mutableSetOf<String>())

    private suspend fun firebaseToken(member: MemberRequest): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Sua sessão de membro expirou. Entre novamente no MIC Rhema.")
        if (user.uid != member.id) {
            throw IllegalStateException("A sessão Firebase não pertence ao membro ativo. Entre novamente.")
        }
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("O Firebase não retornou um token válido para o Quiz.")
    }

    private suspend fun call(member: MemberRequest, payload: JSONObject, allowConflict: Boolean = false): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O Quiz central não está configurado nesta versão.")
        }
        val token = firebaseToken(member)
        val request = Request.Builder()
            .url("$baseUrl/functions/v1/xp-quiz")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful && !(allowConflict && response.code == 409)) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha no Quiz central (${response.code})." })
            }
            response.code to json
        }
    }

    private fun parseAccount(memberId: String, root: JSONObject): XpAccount {
        val account = root.optJSONObject("account") ?: JSONObject()
        return XpAccount(
            memberId = memberId,
            unlocked = root.optBoolean("unlocked", xpAccountState.value?.unlocked ?: false),
            totalEarned = account.optInt("total_earned", 0).coerceAtLeast(0),
            totalSpent = account.optInt("total_spent", 0).coerceAtLeast(0),
            balance = account.optInt("balance", 0).coerceAtLeast(0),
            migratedLegacyXp = xpAccountState.value?.takeIf { it.memberId == memberId }?.migratedLegacyXp ?: 0,
            updatedAt = ""
        )
    }

    private fun hintFromVariant(value: String): BibleQuizHintUsage = when (value) {
        "easy_hint" -> BibleQuizHintUsage.EASY
        "subtle_hint" -> BibleQuizHintUsage.HARD
        else -> BibleQuizHintUsage.NONE
    }

    suspend fun recordHintNow(
        member: MemberRequest,
        questionId: String,
        requested: BibleQuizHintUsage
    ): BibleQuizHintUsage {
        if (requested == BibleQuizHintUsage.NONE) return BibleQuizHintUsage.NONE
        val hint = if (requested == BibleQuizHintUsage.EASY) "easy" else "subtle"
        val (_, response) = call(
            member,
            JSONObject()
                .put("action", "hint")
                .put("questionId", questionId)
                .put("hint", hint)
        )
        return hintFromVariant(response.optString("variant"))
    }

    suspend fun submitAnswerNow(
        member: MemberRequest,
        question: BibleQuizQuestion,
        selectedOptionIndex: Int
    ): QuizAuthorityAnswer {
        val (_, response) = call(
            member,
            JSONObject()
                .put("action", "answer")
                .put("questionId", question.id)
                .put("selectedOptionIndex", selectedOptionIndex)
        )
        val account = parseAccount(member.id, response)
        withContext(Dispatchers.Main) {
            xpAccountState.value = account
            xpSyncErrorState.value = ""
        }
        return QuizAuthorityAnswer(
            correct = response.optBoolean("correct", false),
            duplicate = response.optBoolean("duplicate", false),
            granted = response.optInt("granted", 0).coerceAtLeast(0),
            selectedOptionIndex = response.optInt("selectedOptionIndex", selectedOptionIndex).coerceIn(0, 3),
            correctOptionIndex = response.optInt("correctOptionIndex", question.correctOptionIndex).coerceIn(0, 3),
            hintUsed = hintFromVariant(response.optString("variant")),
            reference = response.optString("reference", question.bibleReference),
            explanation = response.optString("explanation", question.explanation),
            account = account
        )
    }

    fun claimMission(
        context: Context,
        member: MemberRequest,
        mission: BibleMissionDefinition,
        onResult: (QuizMissionClaimResult) -> Unit
    ) {
        val key = "${member.id}:${mission.id}"
        if (!missionInFlight.add(key)) return
        scope.launch {
            try {
                val (status, response) = call(
                    member,
                    JSONObject()
                        .put("action", "claim_mission")
                        .put("missionId", mission.id),
                    allowConflict = true
                )
                val confirmed = status in 200..299 && (response.optInt("granted", 0) > 0 || response.optBoolean("duplicate", false))
                val result = QuizMissionClaimResult(
                    confirmed = confirmed,
                    granted = response.optInt("granted", 0).coerceAtLeast(0),
                    duplicate = response.optBoolean("duplicate", false),
                    reason = response.optString("reason")
                )
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (error: Throwable) {
                Log.w("QuizAuthorityClient", "Falha ao validar missão ${mission.id}", error)
                withContext(Dispatchers.Main) {
                    xpSyncErrorState.value = error.message.orEmpty()
                    onResult(QuizMissionClaimResult(false, 0, false, "network_error"))
                }
            } finally {
                missionInFlight.remove(key)
            }
        }
    }
}
