package com.aistudio.micrhema

/**
 * Catálogo central das molduras de nível e dos emblemas simples do MIC Rhema.
 * Os identificadores são estáveis para que possam ser persistidos no Firebase.
 */
data class BiblicalBadge(
    val id: String,
    val name: String,
    val description: String,
    val category: BadgeCategory,
    val level: Int? = null,
    val frameStyle: BadgeFrameStyle,
    val accentColorHex: Long,
    val requirement: String
)

enum class BadgeCategory {
    LEVEL,
    ACHIEVEMENT
}

enum class BadgeFrameStyle {
    SIMPLE,
    SEEDLING,
    STAR,
    OLIVE_BRANCH,
    GOLDEN_BOOK,
    MASTER_WORD,
    GUARDIAN_SHIELD
}

/** Os sete níveis principais de progresso espiritual e estudo. */
val biblicalLevelBadges: List<BiblicalBadge> = listOf(
    BiblicalBadge(
        id = "caminhante",
        name = "Caminhante",
        description = "O início de uma jornada de fé e conhecimento.",
        category = BadgeCategory.LEVEL,
        level = 1,
        frameStyle = BadgeFrameStyle.SIMPLE,
        accentColorHex = 0xFF90A4AE,
        requirement = "Criar o perfil e escolher um avatar"
    ),
    BiblicalBadge(
        id = "semeador",
        name = "Semeador",
        description = "Quem planta a Palavra no coração todos os dias.",
        category = BadgeCategory.LEVEL,
        level = 2,
        frameStyle = BadgeFrameStyle.SEEDLING,
        accentColorHex = 0xFF66BB6A,
        requirement = "Ler 3 devocionais e concluir 1 tema de plano"
    ),
    BiblicalBadge(
        id = "discipulo",
        name = "Discípulo",
        description = "Um passo firme no aprendizado da Palavra.",
        category = BadgeCategory.LEVEL,
        level = 3,
        frameStyle = BadgeFrameStyle.STAR,
        accentColorHex = 0xFF42A5F5,
        requirement = "Concluir 1 plano, 3 temas e ler 3 capítulos da Bíblia"
    ),
    BiblicalBadge(
        id = "perseverante",
        name = "Perseverante",
        description = "Constância para continuar mesmo nos dias difíceis.",
        category = BadgeCategory.LEVEL,
        level = 4,
        frameStyle = BadgeFrameStyle.OLIVE_BRANCH,
        accentColorHex = 0xFF9CCC65,
        requirement = "Acumular 60 minutos ativos e realizar 10 atividades"
    ),
    BiblicalBadge(
        id = "estudante_rhema",
        name = "Estudante Rhema",
        description = "Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",
        category = BadgeCategory.LEVEL,
        level = 5,
        frameStyle = BadgeFrameStyle.GOLDEN_BOOK,
        accentColorHex = 0xFFFFC107,
        requirement = "Ler 3 livros, assistir 3 vídeos e ouvir 2 áudios"
    ),
    BiblicalBadge(
        id = "mestre_da_palavra",
        name = "Mestre da Palavra",
        description = "Conhecimento construído com disciplina e compromisso.",
        category = BadgeCategory.LEVEL,
        level = 6,
        frameStyle = BadgeFrameStyle.MASTER_WORD,
        accentColorHex = 0xFFFFA000,
        requirement = "Concluir 1 curso IBR, ler 3 notícias e 10 capítulos da Bíblia"
    ),
    BiblicalBadge(
        id = "guardiao_da_fe",
        name = "Guardião da Fé",
        description = "Um testemunho de perseverança, serviço e maturidade.",
        category = BadgeCategory.LEVEL,
        level = 7,
        frameStyle = BadgeFrameStyle.GUARDIAN_SHIELD,
        accentColorHex = 0xFF8D6E63,
        requirement = "Realizar todas as atividades e acumular 180 minutos ativos"
    )
)

/** Emblemas complementares que podem ser desbloqueados independentemente do nível. */
val simpleBiblicalBadges: List<BiblicalBadge> = listOf(
    BiblicalBadge(
        id = "primeira_oracao",
        name = "Primeira Oração",
        description = "Um primeiro momento separado para falar com Deus.",
        category = BadgeCategory.ACHIEVEMENT,
        frameStyle = BadgeFrameStyle.SIMPLE,
        accentColorHex = 0xFF7E57C2,
        requirement = "Registrar o primeiro momento de oração"
    ),
    BiblicalBadge(
        id = "leitor_da_palavra",
        name = "Leitor da Palavra",
        description = "A Bíblia aberta e o coração disposto a aprender.",
        category = BadgeCategory.ACHIEVEMENT,
        frameStyle = BadgeFrameStyle.GOLDEN_BOOK,
        accentColorHex = 0xFF5C6BC0,
        requirement = "Ler 10 capítulos da Bíblia"
    ),
    BiblicalBadge(
        id = "coracao_grato",
        name = "Coração Grato",
        description = "Reconhecimento pelas bênçãos recebidas.",
        category = BadgeCategory.ACHIEVEMENT,
        frameStyle = BadgeFrameStyle.STAR,
        accentColorHex = 0xFFEC407A,
        requirement = "Registrar uma mensagem de gratidão"
    ),
    BiblicalBadge(
        id = "constante",
        name = "Constante",
        description = "Pequenos passos repetidos com fidelidade.",
        category = BadgeCategory.ACHIEVEMENT,
        frameStyle = BadgeFrameStyle.OLIVE_BRANCH,
        accentColorHex = 0xFF26A69A,
        requirement = "Estudar por 7 dias consecutivos"
    ),
    BiblicalBadge(
        id = "certificado_ibr",
        name = "Certificado IBR",
        description = "Uma conquista acadêmica no Instituto Bíblico Rhema.",
        category = BadgeCategory.ACHIEVEMENT,
        frameStyle = BadgeFrameStyle.MASTER_WORD,
        accentColorHex = 0xFFAB47BC,
        requirement = "Receber um certificado IBR"
    )
)

val allBiblicalBadges: List<BiblicalBadge> = biblicalLevelBadges + simpleBiblicalBadges

const val DEFAULT_BIBLICAL_BADGE_ID = "caminhante"

fun biblicalBadgeForId(id: String): BiblicalBadge =
    allBiblicalBadges.firstOrNull { it.id == id } ?: biblicalLevelBadges.first()
