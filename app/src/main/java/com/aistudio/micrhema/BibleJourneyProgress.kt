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

    private val quizQuestionsById: Map<String, BibleQuizQuestion> by lazy(LazyThreadSafetyMode.NONE) {
        buildMap {
            BibleQuizCatalog.questions.forEach { put(it.id, it) }
            BibleQuizDifficulty.entries.forEach { difficulty ->
                BibleQuizExpansion.byDifficulty(difficulty).forEach { put(it.id, it) }
            }
        }
    }

    private fun parseXp(entry: String): Int = entry.substringAfterLast('=', "0").toIntOrNull()?.coerceAtLeast(0) ?: 0

    private fun inferredHint(member: MemberRequest, questionId: String): BibleQuizHintUsage {
        val noHint = questionId in member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT)
        if (noHint) return BibleQuizHintUsage.NONE
        val noEasyHint = questionId in member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT)
        return if (noEasyHint) BibleQuizHintUsage.HARD else BibleQuizHintUsage.EASY
    }

    private fun recoverableQuizXp(member: MemberRequest, questionId: String): Int {
        val question = quizQuestionsById[questionId] ?: return 0
        return BibleQuizEngine.answer(
            question = question,
            selectedOptionIndex = question.correctOptionIndex,
            hintUsed = inferredHint(member, questionId)
        ).awardedXp
    }

    private fun localTotalXp(member: MemberRequest): Int {
        val awards = member.ids(BadgeActivityKeys.XP_AWARDS)
        val stored = awards.sumOf(::parseXp)
        val receipts = awards.map { it.substringBefore('=') }.toSet()
        val missingQuizXp = member.ids(BadgeActivityKeys.QUIZ_CORRECT).sumOf { questionId ->
            if ("quiz:$questionId" in receipts) 0 else recoverableQuizXp(member, questionId)
        }
        return stored + missingQuizXp
    }

    fun totalXp(member: MemberRequest): Int {
        val local = localTotalXp(member)
        val central = xpAccountState.value
            ?.takeIf { it.memberId == member.id }
            ?.totalEarned
            ?: 0
        return maxOf(local, central)
    }

    fun stats(member: MemberRequest): BibleJourneyStats = BibleJourneyStats(
        totalXp = totalXp(member),
        answeredQuestions = member.ids(BadgeActivityKeys.QUIZ_ANSWERED).size,
        correctAnswers = member.ids(BadgeActivityKeys.QUIZ_CORRECT).size,
        correctWithoutEasyHint = member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT).size,
        correctWithoutHint = member.ids(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT).size,
        hardCorrectAnswers = member.ids(BadgeActivityKeys.QUIZ_HARD_CORRECT).size,
        completedMissionIds = member.ids(BadgeActivityKeys.JOURNEY_MISSIONS).toSet()
    )

    fun reconcileQuizXp(context: Context, sourceMember: MemberRequest): MemberRequest {
        val correctIds = sourceMember.ids(BadgeActivityKeys.QUIZ_CORRECT).toSet()
        if (correctIds.isEmpty()) return sourceMember

        val xpAwards = sourceMember.ids(BadgeActivityKeys.XP_AWARDS).toMutableList()
        val existingReceipts = xpAwards.map { it.substringBefore('=') }.toMutableSet()
        var changed = false

        correctIds.forEach { questionId ->
            val receipt = "quiz:$questionId"
            if (receipt in existingReceipts) return@forEach
            val recoveredXp = recoverableQuizXp(sourceMember, questionId)
            if (recoveredXp > 0) {
                xpAwards.add("$receipt=$recoveredXp")
                existingReceipts.add(receipt)
                changed = true
            }
        }

        if (!changed) return sourceMember

        val activities = sourceMember.badgeActivityIds.toMutableMap()
        activities[BadgeActivityKeys.XP_AWARDS] = xpAwards.distinct()
        val recovered = sourceMember.copy(badgeActivityIds = activities)
        BadgeActivityTracker.updateMemberStates(recovered)
        BadgeActivityTracker.syncPortableState(context, recovered)
        return recovered
    }

    fun submitQuizAnswer(
        context: Context,
        question: BibleQuizQuestion,
        selectedOptionIndex: Int,
        hintUsed: BibleQuizHintUsage = BibleQuizHintUsage.NONE
    ): BibleQuizSubmission {
        val rawMember = loggedInMemberState.value
            ?: throw IllegalStateException("Entre no MIC Rhema para registrar seu progresso.")
        val recoveredMember = reconcileQuizXp(context, rawMember)
        val member = ensureCurrentLevelMissionBaseline(ensureBibleJourneyBaseline(recoveredMember), allowIbrDependentBaseline = true)
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
        val grantedXp = if (evaluated.isCorrect) evaluated.awardedXp else 0
        if (evaluated.isCorrect) {
            add(BadgeActivityKeys.QUIZ_CORRECT, question.id)
            if (hintUsed != BibleQuizHintUsage.EASY) add(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT, question.id)
            if (hintUsed == BibleQuizHintUsage.NONE) add(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT, question.id)
            if (question.difficulty == BibleQuizDifficulty.HARD) add(BadgeActivityKeys.QUIZ_HARD_CORRECT, question.id)
        }

        val answeredMember = member.copy(badgeActivityIds = activities)
        BadgeActivityTracker.updateMemberStates(answeredMember)
        BadgeActivityTracker.syncPortableState(context, answeredMember)

        // Toda primeira tentativa vai ao backend. O servidor conhece a alternativa
        // correta e grava a primeira resposta; somente uma primeira resposta correta
        // pode gerar XP. Repetições nunca convertem um erro anterior em recompensa.
        XpActivityBridge.quiz(context, question, selectedOptionIndex, hintUsed)

        val rewardedMember = reconcileMissionRewards(context, answeredMember)
        BadgeActivityTracker.reconcile(context, rewardedMember)

        return BibleQuizSubmission(
            result = evaluated,
            firstAttempt = true,
            xpGranted = grantedXp,
            totalXp = totalXp(loggedInMemberState.value ?: rewardedMember)
        )
    }

    fun reconcileMissionRewards(context: Context, sourceMember: MemberRequest): MemberRequest {
        val member = ensureBibleJourneyBaseline(sourceMember)
        if (!isXpUnlocked(member)) {
            if (member.badgeActivityIds != sourceMember.badgeActivityIds) {
                BadgeActivityTracker.updateMemberStates(member)
                BadgeActivityTracker.syncPortableState(context, member)
            }
            return member
        }

        val progress = calculateBibleMissionProgress(member)
        val alreadyAwarded = member.ids(BadgeActivityKeys.JOURNEY_MISSIONS).toMutableSet()
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
            XpActivityBridge.journeyMission(context, item.mission)
        }

        val activities = member.badgeActivityIds.toMutableMap()
        activities[BadgeActivityKeys.JOURNEY_MISSIONS] = alreadyAwarded.sorted()
        val rewarded = member.copy(badgeActivityIds = activities)
        BadgeActivityTracker.updateMemberStates(rewarded)
        BadgeActivityTracker.syncPortableState(context, rewarded)
        return rewarded
    }
}
