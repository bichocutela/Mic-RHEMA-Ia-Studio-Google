package com.aistudio.micrhema

/**
 * Catálogo central das molduras de nível e dos emblemas simples do MIC Rhema.
 * Os identificadores são estáveis para que possam ser persistidos no Firebase.
 * No perfil, todos os emblemas podem ser visualizados antes de serem equipados.
 */
data class BiblicalBadge(
    val id: String,
    val name: String,
    val description: String,
    val category: BadgeCategory,
    val level: Int? = null,
    val frameStyle: BadgeFrameStyle,
    val accentColorHex: Long,
    val requirement: String,
    val rarity: ProfileEmblemRarity? = null
)

enum class BadgeCategory { LEVEL, ACHIEVEMENT }

enum class ProfileEmblemRarity(val label: String) {
    RARE("Raro"),
    EPIC("Épico"),
    LEGENDARY("Lendário")
}

enum class BadgeFrameStyle {
    SIMPLE,
    SEEDLING,
    STAR,
    OLIVE_BRANCH,
    GOLDEN_BOOK,
    MASTER_WORD,
    GUARDIAN_SHIELD,
    PROFILE_EMBLEM
}

/** Níveis 1–22. Do nível 8 em diante começam os Emblemas do Perfil aprovados. */
val biblicalLevelBadges: List<BiblicalBadge> = listOf(
    BiblicalBadge("caminhante", "Caminhante", "O início de uma jornada de fé e conhecimento.", BadgeCategory.LEVEL, 1, BadgeFrameStyle.SIMPLE, 0xFF90A4AE, "Criar o perfil e escolher um avatar"),
    BiblicalBadge("semeador", "Semeador", "Quem planta a Palavra no coração todos os dias.", BadgeCategory.LEVEL, 2, BadgeFrameStyle.SEEDLING, 0xFF66BB6A, "Ler 3 devocionais e concluir 1 tema de plano"),
    BiblicalBadge("discipulo", "Discípulo", "Um passo firme no aprendizado da Palavra.", BadgeCategory.LEVEL, 3, BadgeFrameStyle.STAR, 0xFF42A5F5, "Concluir 1 plano, 3 temas e ler 3 capítulos da Bíblia"),
    BiblicalBadge("perseverante", "Perseverante", "Constância para continuar mesmo nos dias difíceis.", BadgeCategory.LEVEL, 4, BadgeFrameStyle.OLIVE_BRANCH, 0xFF9CCC65, "Acumular 60 minutos ativos e realizar 10 atividades"),
    BiblicalBadge("estudante_rhema", "Estudante Rhema", "Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.", BadgeCategory.LEVEL, 5, BadgeFrameStyle.GOLDEN_BOOK, 0xFFFFC107, "Ler 3 livros, assistir 3 vídeos e ouvir 2 áudios"),
    BiblicalBadge("mestre_da_palavra", "Mestre da Palavra", "Conhecimento construído com disciplina e compromisso.", BadgeCategory.LEVEL, 6, BadgeFrameStyle.MASTER_WORD, 0xFFFFA000, "Concluir 1 curso IBR, ler 3 notícias e 10 capítulos da Bíblia"),
    BiblicalBadge("guardiao_da_fe", "Guardião da Fé", "Um testemunho de perseverança, serviço e maturidade.", BadgeCategory.LEVEL, 7, BadgeFrameStyle.GUARDIAN_SHIELD, 0xFF8D6E63, "Realizar todas as áreas de atividade e acumular 180 minutos ativos"),

    // RARO — níveis 8–12
    BiblicalBadge("semente_da_fe", "Semente da Fé", "Uma nova etapa em que conhecimento e prática começam a florescer. Ao conquistar este nível, XP e Loja XP são liberados.", BadgeCategory.LEVEL, 8, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF7CB342, "2 capítulos e 2 respostas corretas neste nível • desbloqueia XP e Loja XP", ProfileEmblemRarity.RARE),
    BiblicalBadge("caminho_da_promessa", "Caminho da Promessa", "Passos firmes guiados pela Palavra e pelas promessas de Deus.", BadgeCategory.LEVEL, 9, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFD4A017, "200 XP + 3 capítulos e 3 respostas corretas neste nível", ProfileEmblemRarity.RARE),
    BiblicalBadge("escudo_da_fe", "Escudo da Fé", "Conhecimento aplicado com convicção e discernimento.", BadgeCategory.LEVEL, 10, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF4F83CC, "350 XP + 5 acertos sem Dica Fácil e 10 minutos ativos neste nível", ProfileEmblemRarity.RARE),
    BiblicalBadge("aguas_vivas", "Águas Vivas", "Uma jornada renovada pela leitura e pelo aprendizado constante.", BadgeCategory.LEVEL, 11, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF29B6F6, "500 XP + 5 capítulos e 5 respostas corretas neste nível", ProfileEmblemRarity.RARE),
    BiblicalBadge("videira_verdadeira", "Videira Verdadeira", "Frutos que aparecem quando a Palavra permanece no coração.", BadgeCategory.LEVEL, 12, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF43A047, "650 XP + 2 devocionais e 6 respostas corretas neste nível", ProfileEmblemRarity.RARE),

    // ÉPICO — níveis 13–17
    BiblicalBadge("luz_do_mundo", "Luz do Mundo", "Conhecimento que ilumina escolhas e fortalece o testemunho.", BadgeCategory.LEVEL, 13, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFFFD54F, "850 XP + 8 acertos sem Dica Fácil e 5 capítulos neste nível", ProfileEmblemRarity.EPIC),
    BiblicalBadge("armadura_de_deus", "Armadura de Deus", "Disciplina espiritual para permanecer firme no aprendizado.", BadgeCategory.LEVEL, 14, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF5472A8, "1050 XP + 10 acertos, 3 difíceis e 15 minutos ativos neste nível", ProfileEmblemRarity.EPIC),
    BiblicalBadge("leao_de_juda", "Leão de Judá", "Coragem, constância e compromisso crescente com a Palavra.", BadgeCategory.LEVEL, 15, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFC98B2E, "1250 XP + 10 capítulos, 10 acertos e 20 minutos ativos neste nível", ProfileEmblemRarity.EPIC),
    BiblicalBadge("chama_do_espirito", "Chama do Espírito", "Uma busca intensa que une conhecimento, dedicação e propósito.", BadgeCategory.LEVEL, 16, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFFF6D00, "1450 XP + 5 acertos difíceis e 10 acertos sem nenhuma dica neste nível", ProfileEmblemRarity.EPIC),
    BiblicalBadge("coroa_da_vida", "Coroa da Vida", "Perseverança reconhecida em uma caminhada de estudo consistente.", BadgeCategory.LEVEL, 17, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF7E57C2, "1650 XP + 12 acertos sem Dica Fácil e 30 minutos ativos neste nível", ProfileEmblemRarity.EPIC),

    // LENDÁRIO — níveis 18–22
    BiblicalBadge("asas_da_promessa", "Asas da Promessa", "Maturidade para avançar com profundidade e constância.", BadgeCategory.LEVEL, 18, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF90CAF9, "1850 XP + 15 capítulos, 8 acertos difíceis e 30 minutos ativos neste nível", ProfileEmblemRarity.LEGENDARY),
    BiblicalBadge("tabernaculo", "Tabernáculo", "Uma jornada ampla que passa por todas as áreas de estudo do MIC Rhema.", BadgeCategory.LEVEL, 19, BadgeFrameStyle.PROFILE_EMBLEM, 0xFF5C6BC0, "2050 XP + usar todas as 8 áreas e acertar 10 perguntas difíceis neste nível", ProfileEmblemRarity.LEGENDARY),
    BiblicalBadge("arca_da_alianca", "Arca da Aliança", "Dedicação excepcional ao estudo bíblico e à formação espiritual.", BadgeCategory.LEVEL, 20, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFFFB300, "2300 XP + 20 capítulos, 12 acertos difíceis e 45 minutos ativos neste nível", ProfileEmblemRarity.LEGENDARY),
    BiblicalBadge("nova_jerusalem", "Nova Jerusalém", "Uma conquista reservada a uma jornada extensa de conhecimento.", BadgeCategory.LEVEL, 21, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFE1BEE7, "2600 XP + 20 acertos, 15 difíceis e 60 minutos ativos neste nível", ProfileEmblemRarity.LEGENDARY),
    BiblicalBadge("gloria_eterna", "Glória Eterna", "O emblema máximo da Jornada Bíblica no MIC Rhema.", BadgeCategory.LEVEL, 22, BadgeFrameStyle.PROFILE_EMBLEM, 0xFFFFD740, "3000 XP + 30 acertos, 20 difíceis e 120 minutos ativos neste nível", ProfileEmblemRarity.LEGENDARY)
)

/** Emblemas complementares que podem ser desbloqueados independentemente do nível. */
val simpleBiblicalBadges: List<BiblicalBadge> = listOf(
    BiblicalBadge("primeira_oracao", "Primeira Oração", "Um primeiro momento separado para falar com Deus.", BadgeCategory.ACHIEVEMENT, frameStyle = BadgeFrameStyle.SIMPLE, accentColorHex = 0xFF7E57C2, requirement = "Registrar o primeiro momento de oração"),
    BiblicalBadge("leitor_da_palavra", "Leitor da Palavra", "A Bíblia aberta e o coração disposto a aprender.", BadgeCategory.ACHIEVEMENT, frameStyle = BadgeFrameStyle.GOLDEN_BOOK, accentColorHex = 0xFF5C6BC0, requirement = "Ler 10 capítulos da Bíblia"),
    BiblicalBadge("coracao_grato", "Coração Grato", "Reconhecimento pelas bênçãos recebidas.", BadgeCategory.ACHIEVEMENT, frameStyle = BadgeFrameStyle.STAR, accentColorHex = 0xFFEC407A, requirement = "Registrar uma mensagem de gratidão"),
    BiblicalBadge("constante", "Constante", "Pequenos passos repetidos com fidelidade.", BadgeCategory.ACHIEVEMENT, frameStyle = BadgeFrameStyle.OLIVE_BRANCH, accentColorHex = 0xFF26A69A, requirement = "Estudar por 7 dias consecutivos"),
    BiblicalBadge("certificado_ibr", "Certificado IBR", "Uma conquista acadêmica no Instituto Bíblico Rhema.", BadgeCategory.ACHIEVEMENT, frameStyle = BadgeFrameStyle.MASTER_WORD, accentColorHex = 0xFFAB47BC, requirement = "Receber um certificado IBR")
)

val allBiblicalBadges: List<BiblicalBadge> = biblicalLevelBadges + simpleBiblicalBadges
val profileEmblemBadges: List<BiblicalBadge> = biblicalLevelBadges.filter { (it.level ?: 0) in 8..22 }

const val DEFAULT_BIBLICAL_BADGE_ID = "caminhante"

fun biblicalBadgeForId(id: String): BiblicalBadge =
    allBiblicalBadges.firstOrNull { it.id == id } ?: biblicalLevelBadges.first()
