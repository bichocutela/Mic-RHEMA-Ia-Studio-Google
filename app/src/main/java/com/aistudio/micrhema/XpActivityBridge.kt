package com.aistudio.micrhema

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ponte única entre as ferramentas do MIC Rhema e o ledger central de XP.
 * As telas só informam o evento validado; idempotência e limites ficam no backend.
 */
object XpActivityBridge {
    private const val ACTIVE_PREFS = "micrhema_xp_active_time"
    private val brazilZone: ZoneId = ZoneId.of("America/Recife")

    fun bibleChapter(context: Context, id: String) = award(context, "bible_chapter", id)
    fun devotional(context: Context, id: String) = award(context, "devotional", id)
    fun news(context: Context, id: String) = award(context, "news_read", id)
    fun planTheme(context: Context, id: String) = award(context, "plan_theme", id)
    fun bookEngagement(context: Context, id: String) = award(context, "book_10", id)
    fun bookCompleted(context: Context, id: String) = award(context, "book_complete", id)
    fun audioTenMinutes(context: Context, id: String) = award(context, "audio_10min", id)
    fun audioCompleted(context: Context, id: String) = award(context, "audio_90", id)
    fun videoTenMinutes(context: Context, id: String) = award(context, "video_10min", id)
    fun videoCompleted(context: Context, id: String) = award(context, "video_90", id)
    fun ibrLesson(context: Context, courseId: String, chapterId: String) = award(context, "ibr_lesson", "$courseId:$chapterId")
    fun prayer(context: Context, requestId: String) = award(context, "prayer_sent", requestId)

    fun quiz(context: Context, question: BibleQuizQuestion, hint: BibleQuizHintUsage) {
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
        award(context, activity, question.id, variant)
    }

    fun journeyMission(context: Context, mission: BibleMissionDefinition) {
        val activity = when (mission.difficulty) {
            BibleMissionDifficulty.EASY -> "journey_mission_easy"
            BibleMissionDifficulty.MEDIUM -> "journey_mission_medium"
            BibleMissionDifficulty.HARD -> "journey_mission_hard"
        }
        award(context, activity, mission.id)
    }

    /**
     * Acumula minutos entre sessões. A cada bloco real de 5 minutos envia um recibo
     * único daquele dia. O backend ainda aplica teto diário de 20 XP.
     */
    fun activeMinutes(context: Context, minutes: Int) {
        if (minutes <= 0) return
        val member = loggedInMemberState.value ?: return
        if (!isXpUnlocked(member)) return

        val today = LocalDate.now(brazilZone).toString()
        val prefs = context.applicationContext.getSharedPreferences(ACTIVE_PREFS, Context.MODE_PRIVATE)
        val storedDate = prefs.getString("date", "").orEmpty()
        var remainder = if (storedDate == today) prefs.getInt("remainder", 0).coerceAtLeast(0) else 0
        var sequence = if (storedDate == today) prefs.getInt("sequence", 0).coerceAtLeast(0) else 0
        remainder += minutes

        while (remainder >= 5) {
            sequence++
            remainder -= 5
            award(context, "active_5min", "$today:$sequence")
        }

        prefs.edit()
            .putString("date", today)
            .putInt("remainder", remainder)
            .putInt("sequence", sequence)
            .apply()
    }

    private fun award(context: Context, activity: String, contentId: String, variant: String = "") {
        val cleanId = contentId.trim()
        if (cleanId.isBlank()) return
        XpEngineClient.award(
            context = context,
            activity = activity,
            contentId = cleanId,
            variant = variant
        )
    }
}
