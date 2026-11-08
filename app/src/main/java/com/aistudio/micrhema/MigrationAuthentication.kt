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

/** Only the destination backend uses the migrated Firebase token contract. */
object MigrationAuthentication {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    val enabled: Boolean
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/') == "https://sinzscjscvgzkvkimcbd.supabase.co"

    suspend fun memberToken(memberId: String? = null): String {
        val auth = memberId?.let { MemberFirebaseAuth.forMember(it) } ?: MemberFirebaseAuth.get()
        val user = auth.currentUser
            ?: throw IllegalStateException("Confirme seu telefone para acessar o cadastro.")
        if (user.isAnonymous) throw IllegalStateException("Confirme seu telefone para acessar o cadastro.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Entre novamente para confirmar sua identidade.")
    }

    suspend fun adminToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Entre novamente na Área Administrativa.")
        val result = user.getIdToken(false).await()
        if (result.claims["isAdmin"] != true) throw IllegalStateException("Entre novamente na Área Administrativa para validar as permissões.")
        return result.token ?: throw IllegalStateException("Sessão administrativa inválida.")
    }

    suspend fun signInAdmin(password: String) = withContext(Dispatchers.IO) {
        require(password.isNotBlank()) { "Informe sua senha administrativa." }
        val payload = JSONObject().put("name", "admin").put("phone", "admin").put("password", password)
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trim().trimEnd('/')}/functions/v1/pwa-auth")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        val token = client.newCall(request).execute().use { response ->
            val body = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrElse { JSONObject() }
            if (!response.isSuccessful || body.optJSONObject("member")?.optBoolean("isAdmin") != true) {
                throw IllegalStateException(body.optString("error").ifBlank { "Não foi possível autenticar o administrador." })
            }
            body.optString("token").also { check(it.isNotBlank()) { "Sessão administrativa incompleta." } }
        }
        FirebaseAuth.getInstance().signInWithCustomToken(token).await()
        adminToken()
    }

    suspend fun submitPrayer(context: android.content.Context, name: String, prayer: String, notificationToken: String): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("migration_prayer_identity", android.content.Context.MODE_PRIVATE)
        val deviceId = prefs.getString("id", null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString("id", it).apply() }
        val deviceSecret = prefs.getString("secret", null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString("secret", it).apply() }
        val identityToken = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("Não foi possível validar a sessão do pedido de oração.")
        val payload = JSONObject().put("action", "submit").put("name", name.trim()).put("request", prayer.trim())
            .put("deviceId", deviceId).put("deviceSecret", deviceSecret).put("notificationToken", notificationToken)
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trim().trimEnd('/')}/functions/v1/pwa-prayer-request")
            .header("Authorization", "Bearer $identityToken")
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val body = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrElse { JSONObject() }
            if (!response.isSuccessful || !body.optBoolean("ok")) throw IllegalStateException(body.optString("error").ifBlank { "Não foi possível enviar o pedido de oração." })
            body.optString("id").also { check(it.isNotBlank()) { "O servidor retornou um pedido incompleto." } }
        }
    }
}
