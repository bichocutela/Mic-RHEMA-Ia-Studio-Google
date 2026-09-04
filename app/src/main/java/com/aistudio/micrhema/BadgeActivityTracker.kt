package com.aistudio.micrhema

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlin.math.max
import kotlin.math.min

/** Atividades reais que alimentam as missões dos emblemas. */
data class BadgeAwardNotification(val badges: List<BiblicalBadge>)

val badgeAwardNotificationState = mutableStateOf<BadgeAwardNotification?>(null)
/** Emblema que deve ser destacado ao abrir Meu Perfil pela celebração. */
val badgeUnlockFocusState = mutableStateOf<String?>(null)

object BadgeActivityKeys {
    const val PLANS = "plans"
    const val PLAN_THEMES = "plan_themes"
    const val BOOKS = "books"
    const val VIDEOS = "videos"
    const val BIBLE_CHAPTERS = "bible_chapters"
    const val BIBLE_NEWS = "bible_news"
    const val DEVOTIONALS = "devotionals"
    const val AUDIOS = "audios"
    const val ACTIVE_MINUTES = "active_minutes"
}

object BadgeActivityTracker {
    private const val MAX_SESSION_MINUTES = 120
    private var sessionStartedAtElapsed: Long? = null

    fun record(context: Context, activity: String, itemId: String) {
        val member = loggedInMemberState.value ?: return
        val normalizedId = itemId.trim()
        if (normalizedId.isBlank()) return
        val previous = member.badgeActivityIds[activity].orEmpty()
        if (normalizedId in previous) return
        persist(context, member, activity, previous + normalizedId)
    }

    fun reconcile(context: Context, member: MemberRequest) {
        val calculated = calculateBadgeProgress(member)
        if (calculated.unlockedIds.toSet() == member.unlockedBadgeIds.toSet()) return
        val updatedMember = member.copy(unlockedBadgeIds = calculated.unlockedIds)
        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = updatedMember
        if (loggedInMemberState.value?.id == member.id) loggedInMemberState.value = updatedMember
        syncPortableState(context, updatedMember)
    }

    fun startActiveSession() {
        if (sessionStartedAtElapsed == null) sessionStartedAtElapsed = SystemClock.elapsedRealtime()
    }

    fun stopActiveSession(context: Context) {
        val startedAt = sessionStartedAtElapsed ?: return
        sessionStartedAtElapsed = null
        val elapsedMinutes = ((SystemClock.elapsedRealtime() - startedAt) / 60_000L).toInt()
        val minutesToRecord = min(MAX_SESSION_MINUTES, max(0, elapsedMinutes))
        if (minutesToRecord == 0) return

        val member = loggedInMemberState.value ?: return
        val existing = member.badgeActivityIds[BadgeActivityKeys.ACTIVE_MINUTES].orEmpty().toMutableSet()
        val sessionStartMinute = System.currentTimeMillis() / 60_000L
        repeat(minutesToRecord) { offset -> existing.add("${sessionStartMinute + offset}") }
        persist(context, member, BadgeActivityKeys.ACTIVE_MINUTES, existing.toList().sorted())
    }

    private fun persist(context: Context, member: MemberRequest, activity: String, ids: List<String>) {
        val updatedActivities = member.badgeActivityIds.toMutableMap()
        updatedActivities[activity] = ids.distinct()
        val activityMember = member.copy(badgeActivityIds = updatedActivities)
        val calculated = calculateBadgeProgress(activityMember)
        val previousUnlockedIds = member.unlockedBadgeIds.toSet()
        val newlyUnlocked = calculated.unlockedIds
            .filterNot { it in previousUnlockedIds }
            .mapNotNull { id -> biblicalBadgeForId(id).takeIf { it.id == id } }
        if (newlyUnlocked.isNotEmpty()) {
            badgeAwardNotificationState.value = BadgeAwardNotification(newlyUnlocked)
        }
        val updatedMember = activityMember.copy(unlockedBadgeIds = calculated.unlockedIds)
        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = updatedMember
        if (loggedInMemberState.value?.id == member.id) loggedInMemberState.value = updatedMember

        // Não depende mais do armazenamento local nem de uma atualização direta proibida
        // pelas regras do Firestore. O backend grava apenas os campos seguros do membro.
        syncPortableState(context, updatedMember)
    }

    private fun syncPortableState(context: Context, member: MemberRequest) {
        MemberSessionClient.syncMemberState(
            context = context,
            member = member,
            identityPhone = member.phone,
            onFailure = { error ->
                Log.w("BadgeActivityTracker", "Não foi possível sincronizar o progresso do membro", error)
            }
        )
    }
}
