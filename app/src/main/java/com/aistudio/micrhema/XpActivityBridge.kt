package com.aistudio.micrhema

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ponte única entre as ferramentas do MIC Rhema e o ledger central de XP.
 * As telas só informam o evento validado; idempotência e limites ficam no backend.
 */
object XpActivityBridge {
    private const val ACTIVE_PREFS = "micrhema_xp_active_time"
    private val brazilZone: ZoneId = ZoneId.of("America/Recife")

    fun bibleVerse(context: Context, id: String) = award(context, "bible_verse", id)
    fun bibleChapter(context: Context, id: String) = award(context, "bible_chapter", id)
    fun devotional(context: Context, id: String) = award(context, "devotional", id)
    fun news(context: Context, id: String) = award(context, "news_read", id)
    fun planTheme(context: Context, id: String) = award(context, "plan_theme", id)
    fun planDay(context: Context, id: String) = award(context, "plan_day", id)
    fun planComplete(context: Context, id: String) = award(context, "plan_complete", id)
    fun bookEngagement(context: Context, id: String) = award(context, "book_10", id)
    fun bookCompleted(context: Context, id: String) = award(context, "book_complete", id)
    fun audioTenMinutes(context: Context, id: String) = award(context, "audio_10min", id)
    fun audioCompleted(context: Context, id: String) = award(context, "audio_90", id)
    fun videoTenMinutes(context: Context, id: String) = award(context, "video_10min", id)
    fun videoCompleted(context: Context, id: String) = award(context, "video_90", id)
    fun ibrLesson(context: Context, courseId: String, chapterId: String) = award(context, "ibr_lesson", "$courseId:$chapterId")
    fun prayer(context: Context, requestId: String) = award(context, "prayer_sent", requestId)

    fun recordedActivity(context: Context, activity: String, itemId: String) {
        val member = loggedInMemberState.value ?: return
        if (!isXpUnlocked(member)) return
        when (activity) {
            BadgeActivityKeys.PLAN_THEMES -> planTheme(context, itemId)
        }
    }

    fun reconcileIbrCompleted(context: Context) {
        val member = loggedInMemberState.value ?: return
        if (!isXpUnlocked(member)) return
        ibrProgressState
            .asSequence()
            .filter { it.isCompleted }
            .forEach { progress -> ibrLesson(context, progress.courseId, progress.chapterId) }
    }

    fun quiz(
        context: Context,
        question: BibleQuizQuestion,
        selectedOptionIndex: Int,
        hint: BibleQuizHintUsage
    ) {
        val activity = when (question.difficulty) {
            BibleQuizDifficulty.EASY -> "quiz_easy"
            BibleQuizDifficulty.MEDIUM -> "quiz_medium"
            BibleQuizDifficulty.HARD -> "quiz_hard"
        }
        val variant = when (hint) {
            BibleQuizHintUsage.NONE -> ""
            BibleQuizHintUsage.HARD -> "subtle_hint"
            BibleQuizHintUsage.EASY -> "easy_hint"
        }
        award(context, activity, question.id, variant, selectedOptionIndex)
    }

    fun journeyMission(context: Context, mission: BibleMissionDefinition) {
        val activity = when (mission.difficulty) {
            BibleMissionDifficulty.EASY -> "journey_mission_easy"
            BibleMissionDifficulty.MEDIUM -> "journey_mission_medium"
            BibleMissionDifficulty.HARD -> "journey_mission_hard"
        }
        award(context, activity, mission.id)
    }

    private fun activeKey(memberId: String, suffix: String): String = "member:$memberId:$suffix"

    fun activeMinutes(context: Context, minutes: Int) {
        if (minutes <= 0) return
        val member = loggedInMemberState.value ?: return
        if (!isXpUnlocked(member)) return

        val today = LocalDate.now(brazilZone).toString()
        val prefs = context.applicationContext.getSharedPreferences(ACTIVE_PREFS, Context.MODE_PRIVATE)
        val dateKey = activeKey(member.id, "date")
        val remainderKey = activeKey(member.id, "remainder")
        val sequenceKey = activeKey(member.id, "sequence")
        val pendingKey = activeKey(member.id, "pending")
        val storedDate = prefs.getString(dateKey, "").orEmpty()

        var remainder = if (storedDate == today) prefs.getInt(remainderKey, 0).coerceAtLeast(0) else 0
        var sequence = if (storedDate == today) prefs.getInt(sequenceKey, 0).coerceAtLeast(0) else 0
        val pending = if (storedDate == today) {
            prefs.getStringSet(pendingKey, emptySet()).orEmpty().toMutableSet()
        } else {
            mutableSetOf()
        }

        remainder += minutes
        while (remainder >= 5) {
            sequence++
            remainder -= 5
            pending.add("$today:$sequence")
        }

        prefs.edit()
            .putString(dateKey, today)
            .putInt(remainderKey, remainder)
            .putInt(sequenceKey, sequence)
            .putStringSet(pendingKey, pending.toSet())
            .apply()

        pending.forEach { contentId ->
            XpEngineClient.award(
                context = context,
                activity = "active_5min",
                contentId = contentId
            ) {
                removePendingActiveReceipt(prefs, pendingKey, contentId)
            }
        }
    }

    private fun removePendingActiveReceipt(prefs: SharedPreferences, pendingKey: String, contentId: String) {
        synchronized(prefs) {
            val current = prefs.getStringSet(pendingKey, emptySet()).orEmpty().toMutableSet()
            if (current.remove(contentId)) {
                prefs.edit().putStringSet(pendingKey, current.toSet()).apply()
            }
        }
    }

    private fun award(
        context: Context,
        activity: String,
        contentId: String,
        variant: String = "",
        selectedOptionIndex: Int? = null
    ) {
        val cleanId = contentId.trim()
        if (cleanId.isBlank()) return
        XpEngineClient.award(
            context = context,
            activity = activity,
            contentId = cleanId,
            variant = variant,
            selectedOptionIndex = selectedOptionIndex
        )
    }
}
