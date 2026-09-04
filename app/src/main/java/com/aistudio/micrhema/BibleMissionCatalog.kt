package com.aistudio.micrhema

/** Missões da Jornada Bíblica: sempre calculadas a partir do ponto zero da própria jornada. */
enum class BibleMissionDifficulty(val label: String, val xpReward: Int) {
    EASY("Fácil", 15),
    MEDIUM("Médio", 35),
    HARD("Difícil", 70)
}

enum class BibleMissionMetric {
    BIBLE_CHAPTERS,
    DEVOTIONALS,
    PLAN_THEMES,
    PLANS,
    BOOKS,
    VIDEOS,
    AUDIOS,
    BIBLE_NEWS,
    ACTIVE_MINUTES,
    TOTAL_ACTIVITIES,
    QUIZ_CORRECT,
    QUIZ_CORRECT_NO_EASY_HINT,
    QUIZ_CORRECT_NO_HINT,
    QUIZ_HARD_CORRECT
}

data class BibleMissionObjective(
    val metric: BibleMissionMetric,
    val target: Int,
    val unit: String,
    val label: String
)

data class BibleMissionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: BibleMissionDifficulty,
    val objectives: List<BibleMissionObjective>,
    val xpReward: Int = difficulty.xpReward
)

data class BibleMissionObjectiveProgress(
    val objective: BibleMissionObjective,
    val current: Int
) {
    val completed: Boolean get() = current >= objective.target
    val remaining: Int get() = (objective.target - current).coerceAtLeast(0)
    val fraction: Float
        get() = if (objective.target <= 0) 1f
        else (current.toFloat() / objective.target.toFloat()).coerceIn(0f, 1f)
}

data class BibleMissionProgress(
    val mission: BibleMissionDefinition,
    val objectives: List<BibleMissionObjectiveProgress>
) {
    val completed: Boolean get() = objectives.isNotEmpty() && objectives.all { it.completed }
    val fraction: Float
        get() = if (objectives.isEmpty()) 0f else objectives.map { it.fraction }.average().toFloat().coerceIn(0f, 1f)
}

internal const val BIBLE_JOURNEY_BASELINE_KEY = "__bible_journey_baseline__"

private val bibleMissionActivityKeys = listOf(
    BadgeActivityKeys.PLANS,
    BadgeActivityKeys.PLAN_THEMES,
    BadgeActivityKeys.BOOKS,
    BadgeActivityKeys.VIDEOS,
    BadgeActivityKeys.BIBLE_CHAPTERS,
    BadgeActivityKeys.BIBLE_NEWS,
    BadgeActivityKeys.DEVOTIONALS,
    BadgeActivityKeys.AUDIOS
)

private fun MemberRequest.missionActivityCount(key: String): Int =
    badgeActivityIds[key].orEmpty().distinct().size

internal fun MemberRequest.rawBibleMissionMetricValue(metric: BibleMissionMetric): Int = when (metric) {
    BibleMissionMetric.BIBLE_CHAPTERS -> missionActivityCount(BadgeActivityKeys.BIBLE_CHAPTERS)
    BibleMissionMetric.DEVOTIONALS -> missionActivityCount(BadgeActivityKeys.DEVOTIONALS)
    BibleMissionMetric.PLAN_THEMES -> missionActivityCount(BadgeActivityKeys.PLAN_THEMES)
    BibleMissionMetric.PLANS -> missionActivityCount(BadgeActivityKeys.PLANS)
    BibleMissionMetric.BOOKS -> missionActivityCount(BadgeActivityKeys.BOOKS)
    BibleMissionMetric.VIDEOS -> missionActivityCount(BadgeActivityKeys.VIDEOS)
    BibleMissionMetric.AUDIOS -> missionActivityCount(BadgeActivityKeys.AUDIOS)
    BibleMissionMetric.BIBLE_NEWS -> missionActivityCount(BadgeActivityKeys.BIBLE_NEWS)
    BibleMissionMetric.ACTIVE_MINUTES -> missionActivityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    BibleMissionMetric.TOTAL_ACTIVITIES -> bibleMissionActivityKeys.sumOf(::missionActivityCount)
    BibleMissionMetric.QUIZ_CORRECT -> missionActivityCount(BadgeActivityKeys.QUIZ_CORRECT)
    BibleMissionMetric.QUIZ_CORRECT_NO_EASY_HINT -> missionActivityCount(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT)
    BibleMissionMetric.QUIZ_CORRECT_NO_HINT -> missionActivityCount(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT)
    BibleMissionMetric.QUIZ_HARD_CORRECT -> missionActivityCount(BadgeActivityKeys.QUIZ_HARD_CORRECT)
}

private fun readBibleJourneyBaseline(member: MemberRequest): Map<BibleMissionMetric, Int> {
    return member.badgeActivityIds[BIBLE_JOURNEY_BASELINE_KEY].orEmpty().mapNotNull { entry ->
        val separator = entry.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val metric = runCatching { BibleMissionMetric.valueOf(entry.substring(0, separator)) }.getOrNull()
            ?: return@mapNotNull null
        val value = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
        metric to value
    }.toMap()
}

fun ensureBibleJourneyBaseline(member: MemberRequest): MemberRequest {
    if (member.badgeActivityIds.containsKey(BIBLE_JOURNEY_BASELINE_KEY)) return member
    val snapshot = BibleMissionMetric.entries.map { metric ->
        "${metric.name}=${member.rawBibleMissionMetricValue(metric)}"
    }
    val activities = member.badgeActivityIds.toMutableMap()
    activities[BIBLE_JOURNEY_BASELINE_KEY] = snapshot
    return member.copy(badgeActivityIds = activities)
}

fun bibleJourneyMetricValue(member: MemberRequest, metric: BibleMissionMetric): Int {
    val prepared = ensureBibleJourneyBaseline(member)
    val baseline = readBibleJourneyBaseline(prepared)
    val current = prepared.rawBibleMissionMetricValue(metric)
    return (current - (baseline[metric] ?: current)).coerceAtLeast(0)
}

object BibleMissionCatalog {
    val missions: List<BibleMissionDefinition> = listOf(
        // Fácil
        BibleMissionDefinition(
            id = "easy_first_chapter",
            title = "Primeiro passo na Palavra",
            description = "Leia um capítulo completo da Bíblia.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 1, "capítulo", "Ler capítulo da Bíblia"))
        ),
        BibleMissionDefinition(
            id = "easy_devotional",
            title = "Momento de reflexão",
            description = "Conclua um devocional no MIC Rhema.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.DEVOTIONALS, 1, "devocional", "Concluir devocional"))
        ),
        BibleMissionDefinition(
            id = "easy_plan_theme",
            title = "Explore um tema",
            description = "Conheça um tema dos planos bíblicos.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.PLAN_THEMES, 1, "tema", "Explorar tema de plano"))
        ),
        BibleMissionDefinition(
            id = "easy_five_minutes",
            title = "Cinco minutos com propósito",
            description = "Permaneça cinco minutos ativos estudando no aplicativo.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.ACTIVE_MINUTES, 5, "minutos", "Tempo ativo"))
        ),
        BibleMissionDefinition(
            id = "easy_three_activities",
            title = "Comece a jornada",
            description = "Realize três atividades válidas em áreas de conteúdo do MIC Rhema.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.TOTAL_ACTIVITIES, 3, "atividades", "Atividades válidas"))
        ),
        BibleMissionDefinition(
            id = "easy_first_quiz_correct",
            title = "Primeira resposta certa",
            description = "Acerte sua primeira pergunta no Quiz Bíblico.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT, 1, "acerto", "Responder corretamente"))
        ),
        BibleMissionDefinition(
            id = "easy_three_quiz_correct",
            title = "Conhecimento em crescimento",
            description = "Acerte três perguntas bíblicas diferentes.",
            difficulty = BibleMissionDifficulty.EASY,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT, 3, "acertos", "Responder corretamente"))
        ),

        // Médio
        BibleMissionDefinition(
            id = "medium_three_chapters",
            title = "Aprofunde a leitura",
            description = "Leia três capítulos diferentes da Bíblia.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 3, "capítulos", "Ler capítulos da Bíblia"))
        ),
        BibleMissionDefinition(
            id = "medium_devotional_and_plan",
            title = "Palavra e aplicação",
            description = "Conclua dois devocionais e explore dois temas de planos bíblicos.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.DEVOTIONALS, 2, "devocionais", "Concluir devocionais"),
                BibleMissionObjective(BibleMissionMetric.PLAN_THEMES, 2, "temas", "Explorar temas de planos")
            )
        ),
        BibleMissionDefinition(
            id = "medium_active_student",
            title = "Estudo com constância",
            description = "Acumule quinze minutos ativos e cinco atividades válidas.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.ACTIVE_MINUTES, 15, "minutos", "Tempo ativo"),
                BibleMissionObjective(BibleMissionMetric.TOTAL_ACTIVITIES, 5, "atividades", "Atividades válidas")
            )
        ),
        BibleMissionDefinition(
            id = "medium_media_study",
            title = "Estudo em vários formatos",
            description = "Leia um livro e assista a um vídeo da área de mídia.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.BOOKS, 1, "livro", "Ler livro"),
                BibleMissionObjective(BibleMissionMetric.VIDEOS, 1, "vídeo", "Assistir vídeo")
            )
        ),
        BibleMissionDefinition(
            id = "medium_bible_and_news",
            title = "Bíblia e contexto",
            description = "Leia três capítulos da Bíblia e duas notícias bíblicas.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 3, "capítulos", "Ler capítulos da Bíblia"),
                BibleMissionObjective(BibleMissionMetric.BIBLE_NEWS, 2, "notícias", "Ler notícias bíblicas")
            )
        ),
        BibleMissionDefinition(
            id = "medium_five_quiz_correct",
            title = "Cinco respostas certas",
            description = "Acerte cinco perguntas bíblicas diferentes.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT, 5, "acertos", "Responder corretamente"))
        ),
        BibleMissionDefinition(
            id = "medium_three_without_easy_hint",
            title = "Sem atalho fácil",
            description = "Acerte três perguntas sem usar a Dica Fácil.",
            difficulty = BibleMissionDifficulty.MEDIUM,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT_NO_EASY_HINT, 3, "acertos", "Acertar sem Dica Fácil"))
        ),

        // Difícil
        BibleMissionDefinition(
            id = "hard_ten_chapters",
            title = "Mergulho na Palavra",
            description = "Leia dez capítulos diferentes da Bíblia.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 10, "capítulos", "Ler capítulos da Bíblia"))
        ),
        BibleMissionDefinition(
            id = "hard_hour_of_study",
            title = "Uma hora de dedicação",
            description = "Acumule sessenta minutos ativos e quinze atividades válidas.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.ACTIVE_MINUTES, 60, "minutos", "Tempo ativo"),
                BibleMissionObjective(BibleMissionMetric.TOTAL_ACTIVITIES, 15, "atividades", "Atividades válidas")
            )
        ),
        BibleMissionDefinition(
            id = "hard_multimedia",
            title = "Estudante completo",
            description = "Leia três livros, assista a três vídeos e ouça dois áudios.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.BOOKS, 3, "livros", "Ler livros"),
                BibleMissionObjective(BibleMissionMetric.VIDEOS, 3, "vídeos", "Assistir vídeos"),
                BibleMissionObjective(BibleMissionMetric.AUDIOS, 2, "áudios", "Ouvir áudios")
            )
        ),
        BibleMissionDefinition(
            id = "hard_word_and_context",
            title = "Palavra em profundidade",
            description = "Leia dez capítulos, três notícias bíblicas e explore três temas de planos.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 10, "capítulos", "Ler capítulos da Bíblia"),
                BibleMissionObjective(BibleMissionMetric.BIBLE_NEWS, 3, "notícias", "Ler notícias bíblicas"),
                BibleMissionObjective(BibleMissionMetric.PLAN_THEMES, 3, "temas", "Explorar temas de planos")
            )
        ),
        BibleMissionDefinition(
            id = "hard_full_journey",
            title = "Jornada completa",
            description = "Use todas as principais áreas de estudo e mantenha constância de tempo.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(
                BibleMissionObjective(BibleMissionMetric.BIBLE_CHAPTERS, 10, "capítulos", "Ler capítulos da Bíblia"),
                BibleMissionObjective(BibleMissionMetric.DEVOTIONALS, 3, "devocionais", "Concluir devocionais"),
                BibleMissionObjective(BibleMissionMetric.PLAN_THEMES, 3, "temas", "Explorar temas de planos"),
                BibleMissionObjective(BibleMissionMetric.BOOKS, 1, "livro", "Ler livro"),
                BibleMissionObjective(BibleMissionMetric.VIDEOS, 1, "vídeo", "Assistir vídeo"),
                BibleMissionObjective(BibleMissionMetric.AUDIOS, 1, "áudio", "Ouvir áudio"),
                BibleMissionObjective(BibleMissionMetric.BIBLE_NEWS, 1, "notícia", "Ler notícia bíblica"),
                BibleMissionObjective(BibleMissionMetric.ACTIVE_MINUTES, 90, "minutos", "Tempo ativo")
            )
        ),
        BibleMissionDefinition(
            id = "hard_ten_without_easy_hint",
            title = "Conhecimento sem atalhos",
            description = "Acerte dez perguntas sem usar a Dica Fácil.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT_NO_EASY_HINT, 10, "acertos", "Acertar sem Dica Fácil"))
        ),
        BibleMissionDefinition(
            id = "hard_five_hard_questions",
            title = "Desafio das perguntas difíceis",
            description = "Acerte cinco perguntas classificadas como difíceis.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_HARD_CORRECT, 5, "acertos", "Acertar pergunta difícil"))
        ),
        BibleMissionDefinition(
            id = "hard_five_without_hint",
            title = "Resposta de primeira",
            description = "Acerte cinco perguntas sem usar nenhuma dica.",
            difficulty = BibleMissionDifficulty.HARD,
            objectives = listOf(BibleMissionObjective(BibleMissionMetric.QUIZ_CORRECT_NO_HINT, 5, "acertos", "Acertar sem dica"))
        )
    )

    fun byDifficulty(difficulty: BibleMissionDifficulty): List<BibleMissionDefinition> =
        missions.filter { it.difficulty == difficulty }
}

fun calculateBibleMissionProgress(member: MemberRequest): List<BibleMissionProgress> {
    val prepared = ensureBibleJourneyBaseline(member)
    return BibleMissionCatalog.missions.map { mission ->
        BibleMissionProgress(
            mission = mission,
            objectives = mission.objectives.map { objective ->
                BibleMissionObjectiveProgress(
                    objective = objective,
                    current = bibleJourneyMetricValue(prepared, objective.metric)
                )
            }
        )
    }
}
