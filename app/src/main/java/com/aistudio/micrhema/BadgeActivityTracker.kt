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
        val preparedMember = ensureCurrentLevelMissionBaseline(member)
        val calculated = calculateBadgeProgress(preparedMember)
        val previousUnlockedIds = preparedMember.unlockedBadgeIds.toSet()
        val newlyUnlocked = calculated.unlockedIds
            .filterNot { it in previousUnlockedIds }
            .mapNotNull { id -> biblicalBadgeForId(id).takeIf { it.id == id } }

        if (newlyUnlocked.isNotEmpty()) {
            badgeAwardNotificationState.value = BadgeAwardNotification(newlyUnlocked)
        }

        val unlockedMember = preparedMember.copy(unlockedBadgeIds = calculated.unlockedIds)
        val updatedMember = ensureCurrentLevelMissionBaseline(unlockedMember)

        val changed =
            updatedMember.unlockedBadgeIds.toSet() != member.unlockedBadgeIds.toSet() ||
                updatedMember.badgeActivityIds != member.badgeActivityIds
        if (!changed) return

        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = updatedMember
        if (loggedInMemberState.value?.id == member.id) loggedInMemberState.value = updatedMember
        syncPortableState(context, updatedMember)
    }

    fun prepareForIbrMissionAction(context: Context) {
        val member = loggedInMemberState.value ?: return
        val preparedMember = ensureCurrentLevelMissionBaseline(
            member = member,
            allowIbrDependentBaseline = true
        )
        if (preparedMember.badgeActivityIds == member.badgeActivityIds) return

        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = preparedMember
        loggedInMemberState.value = preparedMember
        syncPortableState(context, preparedMember)
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
        val preparedMember = ensureCurrentLevelMissionBaseline(
            member = member,
            allowIbrDependentBaseline = true
        )
        val updatedActivities = preparedMember.badgeActivityIds.toMutableMap()
        updatedActivities[activity] = ids.distinct()
        val activityMember = preparedMember.copy(badgeActivityIds = updatedActivities)
        val calculated = calculateBadgeProgress(activityMember)
        val previousUnlockedIds = preparedMember.unlockedBadgeIds.toSet()
        val newlyUnlocked = calculated.unlockedIds
            .filterNot { it in previousUnlockedIds }
            .mapNotNull { id -> biblicalBadgeForId(id).takeIf { it.id == id } }

        if (newlyUnlocked.isNotEmpty()) {
            badgeAwardNotificationState.value = BadgeAwardNotification(newlyUnlocked)
        }

        val unlockedMember = activityMember.copy(unlockedBadgeIds = calculated.unlockedIds)
        val updatedMember = ensureCurrentLevelMissionBaseline(unlockedMember)

        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = updatedMember
        if (loggedInMemberState.value?.id == member.id) loggedInMemberState.value = updatedMember

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
