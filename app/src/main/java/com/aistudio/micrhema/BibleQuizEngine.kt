package com.aistudio.micrhema

import kotlin.math.roundToInt

/** Dificuldade da pergunta e XP base quando respondida corretamente sem dicas. */
enum class BibleQuizDifficulty(val label: String, val baseXp: Int) {
    EASY("Fácil", 10),
    MEDIUM("Médio", 20),
    HARD("Difícil", 30)
}

/**
 * Dica usada antes da resposta.
 * HARD é uma pista sutil; EASY é uma pista mais direta e reduz mais a recompensa.
 */
enum class BibleQuizHintUsage(val label: String, val rewardMultiplier: Float) {
    NONE("Sem dica", 1f),
    HARD("Dica difícil", 0.90f),
    EASY("Dica fácil", 0.70f)
}

data class BibleQuizQuestion(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val difficulty: BibleQuizDifficulty,
    val hardHint: String,
    val easyHint: String,
    val bibleReference: String,
    val explanation: String,
    val book: String,
    val chapter: Int,
    val verse: Int? = null,
    val endVerse: Int? = null
)

data class BibleQuizAnswerResult(
    val questionId: String,
    val selectedOptionIndex: Int,
    val correctOptionIndex: Int,
    val isCorrect: Boolean,
    val hintUsed: BibleQuizHintUsage,
    val baseXp: Int,
    val awardedXp: Int,
    val bibleReference: String,
    val explanation: String,
    val book: String,
    val chapter: Int,
    val verse: Int?,
    val endVerse: Int?
)

data class BibleQuizQuestionValidation(
    val valid: Boolean,
    val errors: List<String>
)

object BibleQuizEngine {
    /**
     * Valida uma pergunta antes que ela entre no catálogo oficial.
     * Cada item precisa ter exatamente quatro opções e um único índice correto.
     */
    fun validate(question: BibleQuizQuestion): BibleQuizQuestionValidation {
        val errors = buildList {
            if (question.id.isBlank()) add("A pergunta precisa de um ID estável.")
            if (question.prompt.isBlank()) add("O enunciado não pode ficar vazio.")
            if (question.options.size != 4) add("A pergunta precisa ter exatamente 4 opções.")
            if (question.options.any { it.isBlank() }) add("Nenhuma opção pode ficar vazia.")
            if (question.options.map { it.trim().lowercase() }.distinct().size != question.options.size) {
                add("As quatro opções precisam ser diferentes entre si.")
            }
            if (question.correctOptionIndex !in question.options.indices) add("O índice da resposta correta é inválido.")
            if (question.hardHint.isBlank()) add("A dica difícil não pode ficar vazia.")
            if (question.easyHint.isBlank()) add("A dica fácil não pode ficar vazia.")
            if (question.bibleReference.isBlank()) add("A referência bíblica não pode ficar vazia.")
            if (question.explanation.isBlank()) add("A explicação da resposta não pode ficar vazia.")
            if (question.book.isBlank()) add("O livro bíblico não pode ficar vazio.")
            if (question.chapter <= 0) add("O capítulo precisa ser maior que zero.")
            if (question.verse != null && question.verse <= 0) add("O versículo inicial precisa ser maior que zero.")
            if (question.endVerse != null && question.endVerse <= 0) add("O versículo final precisa ser maior que zero.")
            if (question.verse != null && question.endVerse != null && question.endVerse < question.verse) {
                add("O versículo final não pode vir antes do versículo inicial.")
            }
        }
        return BibleQuizQuestionValidation(valid = errors.isEmpty(), errors = errors)
    }

    /**
     * Avalia uma alternativa sem persistir progresso. A persistência e a proteção
     * contra XP repetido são tratadas pela etapa de progressão da Jornada Bíblica.
     */
    fun answer(
        question: BibleQuizQuestion,
        selectedOptionIndex: Int,
        hintUsed: BibleQuizHintUsage = BibleQuizHintUsage.NONE
    ): BibleQuizAnswerResult {
        val validation = validate(question)
        require(validation.valid) { validation.errors.joinToString(" ") }
        require(selectedOptionIndex in question.options.indices) { "Alternativa selecionada inválida." }

        val isCorrect = selectedOptionIndex == question.correctOptionIndex
        val awardedXp = if (isCorrect) {
            (question.difficulty.baseXp * hintUsed.rewardMultiplier).roundToInt()
        } else {
            0
        }

        return BibleQuizAnswerResult(
            questionId = question.id,
            selectedOptionIndex = selectedOptionIndex,
            correctOptionIndex = question.correctOptionIndex,
            isCorrect = isCorrect,
            hintUsed = hintUsed,
            baseXp = question.difficulty.baseXp,
            awardedXp = awardedXp,
            bibleReference = question.bibleReference,
            explanation = question.explanation,
            book = question.book,
            chapter = question.chapter,
            verse = question.verse,
            endVerse = question.endVerse
        )
    }

    fun hintText(question: BibleQuizQuestion, hint: BibleQuizHintUsage): String = when (hint) {
        BibleQuizHintUsage.NONE -> ""
        BibleQuizHintUsage.HARD -> question.hardHint
        BibleQuizHintUsage.EASY -> question.easyHint
    }
}
