package com.aistudio.micrhema

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Sessão portátil do membro.
 * O telefone localiza a conta no backend e o Firebase recebe um UID estável igual ao ID do membro,
 * permitindo recuperar IBR/favoritos em qualquer aparelho sem criar uma nova solicitação.
 */
object MemberSessionClient {
    data class RecoveryResult(
        val found: Boolean,
        val member: MemberRequest? = null,
        val duplicateCount: Int = 0
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private suspend fun call(payload: JSONObject): JSONObject {
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) {
            throw IllegalStateException("A sincronização de conta não está configurada nesta versão.")
        }

        val request = Request.Builder()
            .url("$baseUrl/functions/v1/member-session")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                throw IllegalStateException(json.optString("error").ifBlank { "Falha ao sincronizar a conta (${response.code})." })
            }
            json
        }
    }

    suspend fun recover(context: Context, phone: String): RecoveryResult {
        val cleanPhone = normalizePhone(phone)
        val response = call(JSONObject().put("action", "recover").put("phone", cleanPhone))
        if (!response.optBoolean("found", false)) return RecoveryResult(found = false)

        val customToken = response.optString("customToken")
        if (customToken.isBlank()) throw IllegalStateException("O servidor não retornou uma sessão válida.")
        FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()

        val memberJson = response.optJSONObject("member")
            ?: throw IllegalStateException("O perfil recuperado está incompleto.")
        val member = memberFromJson(memberJson)
        if (member.id.isBlank()) throw IllegalStateException("O perfil recuperado não possui identificador.")

        return RecoveryResult(
            found = true,
            member = member,
            duplicateCount = response.optInt("duplicateCount", 0)
        )
    }

    fun syncMemberState(
        context: Context,
        member: MemberRequest,
        identityPhone: String = member.phone,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        scope.launch {
            runCatching {
                val payload = JSONObject()
                    .put("action", "sync_state")
                    .put("memberId", member.id)
                    .put("identityPhone", normalizePhone(identityPhone))
                    .put("phone", normalizePhone(member.phone))
                    .put("name", member.name)
                    .put("email", member.email)
                    .put("address", member.address)
                    .put("birthDate", member.birthDate)
                    .put("avatarId", member.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID })
                    .put("equippedBadgeId", member.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID })
                    .put("unlockedBadgeIds", JSONArray(member.unlockedBadgeIds))
                    .put("badgeActivityIds", activitiesToJson(member.badgeActivityIds))
                    .put("profilePhotoUrl", member.profilePhotoUrl)
                    .put("supabaseStoragePath", member.supabaseStoragePath)
                call(payload)
            }.onSuccess {
                launch(Dispatchers.Main) { onSuccess() }
            }.onFailure { error ->
                launch(Dispatchers.Main) { onFailure(error as? Exception ?: IllegalStateException(error.message)) }
            }
        }
    }

    private fun activitiesToJson(map: Map<String, List<String>>): JSONObject = JSONObject().apply {
        map.forEach { (key, values) -> put(key, JSONArray(values.distinct())) }
    }

    private fun memberFromJson(json: JSONObject): MemberRequest {
        val activities = mutableMapOf<String, List<String>>()
        json.optJSONObject("badgeActivityIds")?.let { activityJson ->
            val keys = activityJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = activityJson.optJSONArray(key) ?: JSONArray()
                activities[key] = (0 until array.length()).mapNotNull { index ->
                    array.optString(index).takeIf { it.isNotBlank() }
                }
            }
        }

        val unlockedArray = json.optJSONArray("unlockedBadgeIds") ?: JSONArray()
        val unlocked = (0 until unlockedArray.length()).mapNotNull { index ->
            unlockedArray.optString(index).takeIf { it.isNotBlank() }
        }.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }

        return MemberRequest(
            id = json.optString("id"),
            firebaseUid = json.optString("firebaseUid"),
            name = json.optString("name"),
            ibrCertificateName = json.optString("ibrCertificateName").ifBlank { json.optString("name") },
            phone = normalizePhone(json.optString("phone")),
            email = json.optString("email"),
            isApproved = json.optBoolean("isApproved", false),
            isVip = false,
            isIbr = json.optBoolean("isIbr", false),
            isAdmin = json.optBoolean("isAdmin", false),
            ibrCertificateUrl = json.optString("ibrCertificateUrl"),
            ibrCertificateStoragePath = json.optString("ibrCertificateStoragePath"),
            status = json.optString("status").ifBlank {
                if (json.optBoolean("isApproved", false) || json.optBoolean("isIbr", false)) "aprovado" else "pendente"
            },
            title = json.optString("title"),
            type = json.optString("type").ifBlank { "acesso" },
            content = json.optString("content"),
            mediaUrl = json.optString("mediaUrl"),
            profilePhotoUrl = json.optString("profilePhotoUrl"),
            avatarId = json.optString("avatarId").ifBlank { DEFAULT_BIBLICAL_AVATAR_ID },
            unlockedBadgeIds = unlocked,
            equippedBadgeId = json.optString("equippedBadgeId").ifBlank { DEFAULT_BIBLICAL_BADGE_ID },
            badgeActivityIds = activities,
            supabaseStoragePath = json.optString("supabaseStoragePath"),
            address = json.optString("address"),
            birthDate = json.optString("birthDate"),
            createdAt = json.optLong("createdAt", 0L),
            updatedAt = json.optLong("updatedAt", 0L)
        )
    }
}
