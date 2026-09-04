package com.aistudio.micrhema

import android.content.Context

/** Resultado persistente de uma tentativa do Quiz Bíblico. */
data class BibleQuizSubmission(
    val result: BibleQuizAnswerResult,
    val firstAttempt: Boolean,
    val xpGranted: Int,
    val totalXp: Int
)

data class BibleJourneyStats(
    val totalXp: Int,
    val answeredQuestions: Int,
    val correctAnswers: Int,
    val correctWithoutEasyHint: Int,
    val correctWithoutHint: Int,
    val hardCorrectAnswers: Int,
    val completedMissionIds: Set<String>
) {
    val accuracyPercent: Int
        get() = if (answeredQuestions <= 0) 0 else ((correctAnswers * 100f) / answeredQuestions).toInt().coerceIn(0, 100)
}

object BibleJourneyProgressTracker {
    private fun MemberRequest.ids(key: String): List<String> = badgeActivityIds[key].orEmpty().distinct()

    private fun parseXp(entry: String): Int = entry.substringAfterLast('=', "0").toIntOrNull()?.coerceAtLeast(0) ?: 0

    fun totalXp(member: MemberRequest): Int = member.ids(BadgeActivityKeys.XP_AWARDS).sumOf(::parseXp)

    fun stats(member: MemberRequest): BibleJourneyStats = BibleJourneyStats(
        totalXp = totalXp(member),
        answeredQuestions = member.ids(BadgeActivityKeys.QUIZ_ANSWERED).size,
        correctAnswers = member.ids(BadgeActivityKeys.QUIZ_CORRECT).size,
        correctWithoutEasyHint = member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT).size,
        correctWithoutHint = member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT).size,
        hardCorrectAnswers = member.ids(BadgeActivityKeys.QUIZ_HARD_CORRECT).size,
        completedMissionIds = member.ids(BadgeActivityKeys.JOURNEY_MISSIONS).toSet()
    )

    /**
     * A primeira resposta de cada pergunta é a única que altera progresso e concede XP.
     * Depois disso a questão continua disponível para estudo, mas vale 0 XP.
     */
    fun submitQuizAnswer(
        context: Context,
        question: BibleQuizQuestion,
        selectedOptionIndex: Int,
        hintUsed: BibleQuizHintUsage = BibleQuizHintUsage.NONE
    ): BibleQuizSubmission {
        val rawMember = loggedInMemberState.value
            ?: throw IllegalStateException("Entre no MIC Rhema para registrar seu progresso.")
        val member = ensureCurrentLevelMissionBaseline(ensureBibleJourneyBaseline(rawMember), allowIbrDependentBaseline = true)
        val evaluated = BibleQuizEngine.answer(question, selectedOptionIndex, hintUsed)
        val alreadyAnswered = question.id in member.ids(BadgeActivityKeys.QUIZ_ANSWERED)

        if (alreadyAnswered) {
            return BibleQuizSubmission(
                result = evaluated,
                firstAttempt = false,
                xpGranted = 0,
                totalXp = totalXp(member)
            )
        }

        val activities = member.badgeActivityIds.toMutableMap()
        fun add(key: String, value: String) {
            activities[key] = (activities[key].orEmpty() + value).distinct()
        }

        add(BadgeActivityKeys.QUIZ_ANSWERED, question.id)
        if (evaluated.isCorrect) {
            add(BadgeActivityKeys.QUIZ_CORRECT, question.id)
            if (hintUsed != BibleQuizHintUsage.EASY) add(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT, question.id)
            if (hintUsed == BibleQuizHintUsage.NONE) add(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT, question.id)
            if (question.difficulty == BibleQuizDifficulty.HARD) add(BadgeActivityKeys.QUIZ_HARD_CORRECT, question.id)
            if (evaluated.awardedXp > 0) {
                add(BadgeActivityKeys.XP_AWARDS, "quiz:${question.id}=${evaluated.awardedXp}")
            }
        }

        val answeredMember = member.copy(badgeActivityIds = activities)
        BadgeActivityTracker.updateMemberStates(answeredMember)
        BadgeActivityTracker.syncPortableState(context, answeredMember)

        val rewardedMember = reconcileMissionRewards(context, answeredMember)
        BadgeActivityTracker.reconcile(context, rewardedMember)

        return BibleQuizSubmission(
            result = evaluated,
            firstAttempt = true,
            xpGranted = evaluated.awardedXp,
            totalXp = totalXp(loggedInMemberState.value ?: rewardedMember)
        )
    }

    /**
     * Concede a recompensa de cada missão concluída uma única vez. O próprio ID da
     * missão funciona como recibo, e cada lançamento de XP também tem chave estável.
     */
    fun reconcileMissionRewards(context: Context, sourceMember: MemberRequest): MemberRequest {
        val member = ensureBibleJourneyBaseline(sourceMember)
        val progress = calculateBibleMissionProgress(member)
        val alreadyAwarded = member.ids(BadgeActivityKeys.JOURNEY_MISSIONS).toMutableSet()
        val xpAwards = member.ids(BadgeActivityKeys.XP_AWARDS).toMutableList()
        val newlyCompleted = progress.filter { it.completed && it.mission.id !in alreadyAwarded }
        if (newlyCompleted.isEmpty()) {
            if (member.badgeActivityIds != sourceMember.badgeActivityIds) {
                BadgeActivityTracker.updateMemberStates(member)
                BadgeActivityTracker.syncPortableState(context, member)
            }
            return member
        }

        newlyCompleted.forEach { item ->
            alreadyAwarded.add(item.mission.id)
            val awardId = "mission:${item.mission.id}"
            if (xpAwards.none { it.substringBefore('=') == awardId }) {
                xpAwards.add("$awardId=${item.mission.xpReward}")
            }
        }

        val activities = member.badgeActivityIds.toMutableMap()
        activities[BadgeActivityKeys.JOURNEY_MISSIONS] = alreadyAwarded.sorted()
        activities[BadgeActivityKeys.XP_AWARDS] = xpAwards.distinct()
        val rewarded = member.copy(badgeActivityIds = activities)
        BadgeActivityTracker.updateMemberStates(rewarded)
        BadgeActivityTracker.syncPortableState(context, rewarded)
        return rewarded
    }
}
