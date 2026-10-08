package com.aistudio.micrhema

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Last trusted member profile encrypted with a non-exportable Android
 * Keystore key. Offline access is tied to the previously active account ID
 * on this device. Older installations may migrate their Firestore disk cache.
 *
 * An offline snapshot never grants administrator permissions. It cannot
 * approve a new registration or mint Firebase tokens without the backend.
 */
object MemberOfflineCache {
    private const val PREFS = "micrhema_verified_member_offline_v1"
    private const val KEY = "encrypted_member"
    private const val TRUSTED_ID = "trusted_session_id"
    private const val MEMBER_PREFS = "micrhema_members_prefs"
    private const val LOGGED_IN_ID = "logged_in_member_id"
    private const val KEY_ALIAS = "micrhema_member_offline_aes_v1"

    private fun cipherKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun phone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private fun serialize(member: MemberRequest): String {
        val data = JSONObject()
            .put("id", member.id)
            .put("firebaseUid", member.id)
            .put("name", member.name)
            .put("ibrCertificateName", member.ibrCertificateName)
            .put("phone", phone(member.phone))
            .put("email", member.email)
            .put("address", member.address)
            .put("birthDate", member.birthDate)
            .put("isApproved", member.isApproved)
            .put("isIbr", member.isIbr)
            .put("isAdmin", false)
            .put("status", member.status)
            .put("avatarId", member.avatarId)
            .put("equippedBadgeId", member.equippedBadgeId)
            .put("unlockedBadgeIds", JSONArray(member.unlockedBadgeIds))
            .put("profilePhotoUrl", member.profilePhotoUrl)
            .put("supabaseStoragePath", member.supabaseStoragePath)
            .put("ibrCertificateUrl", member.ibrCertificateUrl)
            .put("ibrCertificateStoragePath", member.ibrCertificateStoragePath)
            .put("createdAt", member.createdAt)
            .put("updatedAt", member.updatedAt)
        val activities = JSONObject()
        member.badgeActivityIds.forEach { (name, entries) ->
            activities.put(name, JSONArray(entries))
        }
        return data.put("badgeActivityIds", activities).toString()
    }

    fun save(context: Context, member: MemberRequest) {
        if (member.id.isBlank() || phone(member.phone).length !in 10..11) return
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey())
            val encrypted = cipher.iv + cipher.doFinal(serialize(member).toByteArray(Charsets.UTF_8))
            val packed = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, packed)
                .putString(TRUSTED_ID, member.id)
                .apply()
        } catch (error: Exception) {
            Log.w("MemberOfflineCache", "Não foi possível proteger o cache local", error)
        }
    }

    /**
     * Never authenticates from a typed phone alone. The encrypted snapshot
     * must belong to the previously active account, with no explicit logout.
     */
    fun restoreTrusted(context: Context, requestedPhone: String? = null): MemberRequest? {
        val memberPrefs = context.applicationContext.getSharedPreferences(MEMBER_PREFS, Context.MODE_PRIVATE)
        val activeId = memberPrefs.getString(LOGGED_IN_ID, "").orEmpty()
        // A phone alone must never turn a signed-out or new device into an account.
        if (activeId.isBlank()) return null
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.DEFAULT)
            require(bytes.size > 12 + 16)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, cipherKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val decoded = JSONObject(
                String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
            )
            val member = MemberSessionClient.memberFromJson(decoded)
            if (member.id.isBlank() || phone(member.phone).length !in 10..11) return null
            if (member.id != activeId) return null
            if (requestedPhone != null && phone(requestedPhone) != phone(member.phone)) return null
            val trustedId = prefs.getString(TRUSTED_ID, null)
            if (trustedId != member.id) {
                // Upgrade compatibility for an encrypted snapshot created by v712.
                // Its original Firebase login must still match before it is trusted.
                if (MemberFirebaseAuth.forMember(member.id)?.currentUser?.uid != member.id) return null
                prefs.edit().putString(TRUSTED_ID, member.id).apply()
            }
            // The Android Keystore snapshot + persisted active account are sufficient
            // for LOCAL use. Firebase token expiry must not block offline UI.
            // APIs still require their own server-side Firebase authentication.
            member.copy(isAdmin = false)
        } catch (error: Exception) {
            Log.w("MemberOfflineCache", "Cache local indisponível; exigida validação online", error)
            null
        }
    }

    /**
     * One-time migration for older APKs which saved the active member ID but
     * did not yet create the encrypted snapshot. Cache-only: no network read.
     * No identity can be created from a name/phone alone.
     */
    suspend fun restoreLegacy(context: Context, requestedPhone: String? = null): MemberRequest? =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val memberPrefs = appContext.getSharedPreferences(MEMBER_PREFS, Context.MODE_PRIVATE)
            val activeId = memberPrefs.getString(LOGGED_IN_ID, "").orEmpty()
            if (activeId.isBlank()) return@withContext null

            val snapshot = runCatching {
                Firebase.firestore.collection("acessos_pendentes")
                    .document(activeId)
                    .get(Source.CACHE)
                    .await()
            }.getOrNull() ?: return@withContext null

            val oldMember = runCatching {
                snapshot.toObject(MemberRequest::class.java)
            }.getOrNull()?.copy(id = activeId, firebaseUid = activeId, isAdmin = false)
                ?: return@withContext null
            if (phone(oldMember.phone).length !in 10..11) return@withContext null
            if (requestedPhone != null && phone(requestedPhone) != phone(oldMember.phone)) {
                return@withContext null
            }
            // Recheck for logout or account switch while cache was being read.
            if (memberPrefs.getString(LOGGED_IN_ID, "") != activeId) return@withContext null
            save(appContext, oldMember)
            restoreTrusted(appContext, requestedPhone)
        }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).remove(TRUSTED_ID).apply()
    }
}
