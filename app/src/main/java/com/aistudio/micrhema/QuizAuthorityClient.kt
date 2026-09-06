package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
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

data class QuizNextQuestionState(
    val questionId: String?,
    val answered: Int,
    val total: Int,
    val remaining: Int
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

    /**
     * O cadastro salvo no aparelho pode ter sido consolidado ou migrado enquanto
     * o FirebaseAuth ainda mantém um UID antigo. O Quiz nunca deve exigir logout
     * manual por causa disso: recuperamos silenciosamente a conta pelo telefone,
     * aceitamos o memberId canônico devolvido pelo servidor e substituímos a sessão.
     */
    private suspend fun firebaseToken(member: MemberRequest, forceRefresh: Boolean): String {
        val auth = FirebaseAuth.getInstance()
        var user = auth.currentUser

        if (user?.uid != member.id) {
            val phone = member.phone.filter(Char::isDigit)
            if (phone.length !in 10..13) {
                throw IllegalStateException("Não foi possível restaurar a sessão do Quiz porque o telefone do perfil está inválido.")
            }

            val appContext = FirebaseApp.getInstance().applicationContext
            val recovery = MemberSessionClient.recover(appContext, phone)
            val recoveredMember = recovery.member
                ?: throw IllegalStateException("Não foi possível localizar novamente este cadastro.")
            if (!recovery.found) {
                throw IllegalStateException("Este cadastro não foi encontrado para o telefone atual.")
            }

            // O backend é a autoridade para decidir qual é o memberId canônico.
            // Se o aparelho ainda guardava o ID de uma conta duplicada antiga,
            // atualizamos o próprio objeto usado nesta chamada para a nova identidade.
            member.id = recoveredMember.id
            member.firebaseUid = recoveredMember.firebaseUid
            member.phone = recoveredMember.phone
            member.name = recoveredMember.name
            member.email = recoveredMember.email
            member.address = recoveredMember.address
            member.birthDate = recoveredMember.birthDate
            member.avatarId = recoveredMember.avatarId
            member.equippedBadgeId = recoveredMember.equippedBadgeId
            member.unlockedBadgeIds = recoveredMember.unlockedBadgeIds
            member.badgeActivityIds = recoveredMember.badgeActivityIds
            member.profilePhotoUrl = recoveredMember.profilePhotoUrl
            member.supabaseStoragePath = recoveredMember.supabaseStoragePath
            member.isApproved = recoveredMember.isApproved
            member.isIbr = recoveredMember.isIbr
            member.isAdmin = recoveredMember.isAdmin

            withContext(Dispatchers.Main.immediate) {
                MemberManager.setLoggedInMember(appContext, recoveredMember)
                xpSyncErrorState.value = ""
            }

            // O mesmo desencontro de UID também poderia deixar o cabeçalho da
            // Jornada mostrando 0 XP. Como a sessão acabou de ser corrigida,
            // pedimos uma atualização do ledger sem bloquear a abertura do Quiz.
            XpEngineClient.refresh(appContext, recoveredMember, force = true)
            user = auth.currentUser
        }

        if (user == null || user.uid != member.id) {
            throw IllegalStateException("Não foi possível restaurar a sessão automaticamente. Toque em tentar novamente.")
        }

        return user.getIdToken(forceRefresh).await().token
            ?: throw IllegalStateException("O Firebase não retornou um token válido para o Quiz.")
    }

    private suspend fun executeCall(member: MemberRequest, payload: JSONObject, forceRefresh: Boolean): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("O Quiz central não está configurado nesta versão.")
        }
        val token = firebaseToken(member, forceRefresh)
        val builder = Request.Builder()
            .url("$baseUrl/functions/v1/xp-quiz-direct")
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

    private suspend fun call(member: MemberRequest, payload: JSONObject, allowConflict: Boolean = false): Pair<Int, JSONObject> {
        var (status, body) = executeCall(member, payload, false)
        if (status == 401) {
            val retry = executeCall(member, payload, true)
            status = retry.first
            body = retry.second
        }
        if (status !in 200..299 && !(allowConflict && status == 409)) {
            throw IllegalStateException(body.optString("error").ifBlank { "Falha no Quiz central ($status)." })
        }
        return status to body
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

    suspend fun nextQuestionNow(member: MemberRequest, difficulty: BibleQuizDifficulty): QuizNextQuestionState {
        val answeredIds = member.badgeActivityIds[BadgeActivityKeys.QUIZ_ANSWERED].orEmpty()
        val (_, response) = call(
            member,
            JSONObject()
                .put("action", "status")
                .put("difficulty", difficulty.name.lowercase())
                .put("answeredIds", JSONArray(answeredIds))
        )
        val questionId = response.optJSONObject("question")
            ?.optString("id")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return QuizNextQuestionState(
            questionId = questionId,
            answered = response.optInt("answered", 0).coerceAtLeast(0),
            total = response.optInt("total", 0).coerceAtLeast(0),
            remaining = response.optInt("remaining", 0).coerceAtLeast(0)
        )
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
