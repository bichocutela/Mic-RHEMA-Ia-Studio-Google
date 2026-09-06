package com.aistudio.micrhema

import android.content.Context
import org.json.JSONObject

/**
 * Preserva a última tela de resultado da Jornada Bíblica por membro.
 *
 * Fechar o diálogo não significa "continuar": a resposta final permanece salva
 * até que o membro toque explicitamente em "Sortear próxima pergunta".
 */
data class BibleJourneySavedResult(
    val difficulty: BibleQuizDifficulty,
    val questionId: String,
    val selectedOptionIndex: Int,
    val correctOptionIndex: Int,
    val isCorrect: Boolean,
    val hintUsed: BibleQuizHintUsage,
    val baseXp: Int,
    val awardedXp: Int,
    val bibleReference: String,
    val explanation: String,
    val firstAttempt: Boolean,
    val xpGranted: Int,
    val totalXp: Int
) {
    fun toSubmission(question: BibleQuizQuestion): BibleQuizSubmission {
        require(question.id == questionId) { "A pergunta salva não corresponde ao catálogo atual." }
        return BibleQuizSubmission(
            result = BibleQuizAnswerResult(
                questionId = questionId,
                selectedOptionIndex = selectedOptionIndex.coerceIn(question.options.indices),
                correctOptionIndex = correctOptionIndex.coerceIn(question.options.indices),
                isCorrect = isCorrect,
                hintUsed = hintUsed,
                baseXp = baseXp.coerceAtLeast(0),
                awardedXp = awardedXp.coerceAtLeast(0),
                bibleReference = bibleReference.ifBlank { question.bibleReference },
                explanation = explanation.ifBlank { question.explanation },
                book = question.book,
                chapter = question.chapter,
                verse = question.verse,
                endVerse = question.endVerse
            ),
            firstAttempt = firstAttempt,
            xpGranted = xpGranted.coerceAtLeast(0),
            totalXp = totalXp.coerceAtLeast(0)
        )
    }
}

object BibleJourneySessionStore {
    private const val PREFS = "micrhema_bible_journey_session"

    /** O telefone é a identidade portátil da conta e sobrevive à consolidação de UID. */
    private fun key(member: MemberRequest): String {
        val phone = member.phone.filter(Char::isDigit).let { digits ->
            if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
        }
        return if (phone.length in 10..11) "phone:$phone" else "member:${member.id.trim()}"
    }

    fun load(context: Context, member: MemberRequest): BibleJourneySavedResult? {
        if (member.id.isBlank() && member.phone.isBlank()) return null
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(member), null)
            ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val difficulty = BibleQuizDifficulty.valueOf(json.getString("difficulty"))
            val hintUsed = BibleQuizHintUsage.valueOf(json.optString("hintUsed", BibleQuizHintUsage.NONE.name))
            val questionId = json.getString("questionId").trim()
            if (questionId.isBlank()) return@runCatching null
            BibleJourneySavedResult(
                difficulty = difficulty,
                questionId = questionId,
                selectedOptionIndex = json.getInt("selectedOptionIndex"),
                correctOptionIndex = json.getInt("correctOptionIndex"),
                isCorrect = json.optBoolean("isCorrect", false),
                hintUsed = hintUsed,
                baseXp = json.optInt("baseXp", difficulty.baseXp).coerceAtLeast(0),
                awardedXp = json.optInt("awardedXp", 0).coerceAtLeast(0),
                bibleReference = json.optString("bibleReference"),
                explanation = json.optString("explanation"),
                firstAttempt = json.optBoolean("firstAttempt", true),
                xpGranted = json.optInt("xpGranted", 0).coerceAtLeast(0),
                totalXp = json.optInt("totalXp", 0).coerceAtLeast(0)
            )
        }.getOrNull()
    }

    fun save(
        context: Context,
        member: MemberRequest,
        difficulty: BibleQuizDifficulty,
        submission: BibleQuizSubmission
    ) {
        if (member.id.isBlank() && member.phone.isBlank()) return
        val result = submission.result
        val json = JSONObject()
            .put("difficulty", difficulty.name)
            .put("questionId", result.questionId)
            .put("selectedOptionIndex", result.selectedOptionIndex)
            .put("correctOptionIndex", result.correctOptionIndex)
            .put("isCorrect", result.isCorrect)
            .put("hintUsed", result.hintUsed.name)
            .put("baseXp", result.baseXp)
            .put("awardedXp", result.awardedXp)
            .put("bibleReference", result.bibleReference)
            .put("explanation", result.explanation)
            .put("firstAttempt", submission.firstAttempt)
            .put("xpGranted", submission.xpGranted)
            .put("totalXp", submission.totalXp)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(member), json.toString())
            .apply()
    }

    fun clear(context: Context, member: MemberRequest) {
        if (member.id.isBlank() && member.phone.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(member))
            .apply()
    }
}
