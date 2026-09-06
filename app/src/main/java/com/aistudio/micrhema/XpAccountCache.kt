package com.aistudio.micrhema

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Espelho local do ledger XP.
 *
 * O Supabase continua sendo a autoridade. Este cache serve somente para impedir que
 * a interface volte visualmente para 0 XP entre a abertura do app e a próxima
 * sincronização de rede. O valor remoto sempre substitui o cache quando chega.
 */
object XpAccountCache {
    private const val PREFS = "micrhema_xp_account_cache_v1"
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class ObservedState(
        val memberId: String,
        val phone: String,
        val accountMemberId: String,
        val totalEarned: Int,
        val totalSpent: Int,
        val balance: Int,
        val unlocked: Boolean,
        val updatedAt: String
    )

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        scope.launch {
            snapshotFlow {
                val member = loggedInMemberState.value
                val account = xpAccountState.value
                ObservedState(
                    memberId = member?.id.orEmpty(),
                    phone = member?.phone?.let(::normalizePhone).orEmpty(),
                    accountMemberId = account?.memberId.orEmpty(),
                    totalEarned = account?.totalEarned ?: -1,
                    totalSpent = account?.totalSpent ?: -1,
                    balance = account?.balance ?: -1,
                    unlocked = account?.unlocked ?: false,
                    updatedAt = account?.updatedAt.orEmpty()
                )
            }
                .distinctUntilChanged()
                .collectLatest {
                    val member = loggedInMemberState.value ?: return@collectLatest
                    val account = xpAccountState.value
                    if (account != null && account.memberId == member.id) {
                        save(appContext, member, account)
                    } else {
                        restore(appContext, member)
                    }
                }
        }
    }

    suspend fun restore(context: Context, member: MemberRequest): XpAccount? {
        val cached = withContext(Dispatchers.IO) { read(context.applicationContext, member) }
        withContext(Dispatchers.Main.immediate) {
            val active = loggedInMemberState.value
            val sameAccount = active == null || sameIdentity(active, member)
            if (!sameAccount) return@withContext

            if (cached != null) {
                val current = xpAccountState.value
                if (current == null || current.memberId != member.id) {
                    xpAccountState.value = cached
                }
            } else if (xpAccountState.value?.memberId != member.id) {
                xpAccountState.value = null
            }
        }
        return cached
    }

    fun save(context: Context, member: MemberRequest, account: XpAccount) {
        if (member.id.isBlank()) return
        val json = JSONObject()
            .put("memberId", account.memberId)
            .put("unlocked", account.unlocked)
            .put("totalEarned", account.totalEarned.coerceAtLeast(0))
            .put("totalSpent", account.totalSpent.coerceAtLeast(0))
            .put("balance", account.balance.coerceAtLeast(0))
            .put("migratedLegacyXp", account.migratedLegacyXp.coerceAtLeast(0))
            .put("updatedAt", account.updatedAt)
            .put("cachedAt", System.currentTimeMillis())
            .toString()

        val editor = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(memberKey(member.id), json)

        normalizePhone(member.phone).takeIf { it.length in 10..11 }?.let { phone ->
            editor.putString(phoneKey(phone), json)
        }
        editor.apply()
    }

    private fun read(context: Context, member: MemberRequest): XpAccount? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val phone = normalizePhone(member.phone)
        val raw = when {
            phone.length in 10..11 -> prefs.getString(phoneKey(phone), null)
                ?: prefs.getString(memberKey(member.id), null)
            member.id.isNotBlank() -> prefs.getString(memberKey(member.id), null)
            else -> null
        } ?: return null

        return runCatching {
            val json = JSONObject(raw)
            XpAccount(
                // Ao restaurar pelo telefone, usamos o ID que está ativo agora.
                // Isso mantém o cache compatível com consolidações de UID.
                memberId = member.id,
                unlocked = json.optBoolean("unlocked", false),
                totalEarned = json.optInt("totalEarned", 0).coerceAtLeast(0),
                totalSpent = json.optInt("totalSpent", 0).coerceAtLeast(0),
                balance = json.optInt("balance", 0).coerceAtLeast(0),
                migratedLegacyXp = json.optInt("migratedLegacyXp", 0).coerceAtLeast(0),
                updatedAt = json.optString("updatedAt")
            )
        }.getOrNull()
    }

    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private fun memberKey(memberId: String) = "member:${memberId.trim()}"
    private fun phoneKey(phone: String) = "phone:$phone"

    private fun sameIdentity(a: MemberRequest, b: MemberRequest): Boolean {
        if (a.id.isNotBlank() && a.id == b.id) return true
        val aPhone = normalizePhone(a.phone)
        val bPhone = normalizePhone(b.phone)
        return aPhone.length in 10..11 && aPhone == bPhone
    }
}
