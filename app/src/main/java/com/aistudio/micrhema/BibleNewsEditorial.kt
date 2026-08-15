package com.aistudio.micrhema

object BibleNewsEditorial {
    val intensityLabels = mapOf(
        1 to "Para refletir",
        2 to "Conflitos e escolhas",
        3 to "Confrontos e consequências",
        4 to "Grandes reviravoltas"
    )

    val categories = listOf(
        "Tudo",
        "Mais recentes",
        "Para refletir",
        "Conflitos e escolhas",
        "Confrontos e consequências",
        "Grandes reviravoltas",
        "Milagres e sinais",
        "Poder e justiça",
        "Família e relacionamentos",
        "Crises e recomeços",
        "Coragem e propósito",
        "Sabedoria para hoje",
        "Provérbios hoje"
    )

    val intensityFilters = listOf(
        "Todas as intensidades",
        "Para refletir",
        "Conflitos e escolhas",
        "Confrontos e consequências",
        "Grandes reviravoltas"
    )

    fun normalizeIntensity(value: Int): Int = value.coerceIn(1, 4)

    fun intensityLabel(value: Int): String = intensityLabels[normalizeIntensity(value)].orEmpty()

    fun decorate(news: BibleNews): BibleNews {
        val category = news.category.ifBlank { inferCategory(news) }
        val intensity = normalizeIntensity(if (news.intensity in 1..4) news.intensity else inferIntensity(news))
        val summary = news.summary.ifBlank { buildSummary(news.content) }
        val tags = if (news.tags.isEmpty()) inferTags(news, category) else news.tags
        val publishedAt = if (news.publishedAt > 0L) news.publishedAt else news.id.toLong()
        val featured = news.featured || news.id >= 26
        val storyKey = news.storyKey.ifBlank { buildStoryKey(news) }
        val warning = news.contentWarning.ifBlank { inferWarning(news, intensity) }
        return news.copy(
            summary = summary,
            category = category,
            intensity = intensity,
            tags = tags,
            contentWarning = warning,
            publishedAt = publishedAt,
            featured = featured,
            storyKey = storyKey
        )
    }

    fun decorateAll(items: List<BibleNews>): List<BibleNews> = items.map(::decorate)

    fun matches(news: BibleNews, query: String, filter: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        val textMatches = normalizedQuery.isBlank() || listOf(
            news.title,
            news.summary,
            news.content,
            news.book,
            news.category,
            news.tags.joinToString(" ")
        ).joinToString(" ").lowercase().contains(normalizedQuery)
        if (!textMatches) return false
        return when {
            filter.isBlank() || filter == "Tudo" -> true
            filter == "Mais recentes" -> true
            intensityLabels.values.contains(filter) -> intensityLabel(news.intensity) == filter
            else -> news.category == filter || news.tags.any { it.equals(filter, ignoreCase = true) }
        }
    }

    private fun inferCategory(news: BibleNews): String {
        val text = "${news.title} ${news.content}".lowercase()
        return when {
            listOf("fogo", "mar", "cura", "cego", "pão", "anjo", "carruagem", "peixe").any { term -> text.contains(term) } -> "Grandes reviravoltas"
            listOf("rei", "palácio", "palacio", "prisão", "prisa", "general", "guerra", "altar").any { term -> text.contains(term) } -> "Poder e justiça"
            listOf("mãe", "mae", "mulher", "menino", "menina", "família", "familia", "multidão").any { term -> text.contains(term) } -> "Família e relacionamentos"
            listOf("arrepend", "oração", "oracao", "salvo", "salvação", "salvacao", "deserto").any { term -> text.contains(term) } -> "Crises e recomeços"
            else -> "Para refletir"
        }
    }

    private fun inferIntensity(news: BibleNews): Int {
        val text = "${news.title} ${news.content}".lowercase()
        return when {
            listOf("morto", "morte", "massacre", "decap", "assassin", "apedrej", "fogo do céu", "fogo do ceu").any { term -> text.contains(term) } -> 3
            listOf("milagre", "cura", "ressusc", "anjo", "mar se abriu", "carruagem", "peixe").any { term -> text.contains(term) } -> 4
            listOf("rei", "guerra", "prisão", "prisao", "gigante", "ameaça", "ameaca").any { term -> text.contains(term) } -> 2
            else -> 1
        }
    }

    private fun inferTags(news: BibleNews, category: String): List<String> {
        val tags = mutableListOf(category)
        val text = "${news.title} ${news.content}".lowercase()
        val keywordTags = listOf(
            "coragem" to listOf("coragem", "gigante", "batalha", "guerra"),
            "milagre" to listOf("milagre", "cura", "fogo", "anjo", "mar"),
            "justiça" to listOf("justiça", "justica", "rei", "sentença", "sentenca"),
            "família" to listOf("mãe", "mae", "mulher", "menino", "irmão", "irmao"),
            "esperança" to listOf("salvo", "salvação", "salvacao", "cura", "arrepend"),
            "oração" to listOf("oração", "oracao", "orou", "louvores")
        )
        keywordTags.forEach { (tag, terms) ->
            if (terms.any { term -> text.contains(term) }) tags += tag
        }
        return tags.distinct()
    }

    private fun inferWarning(news: BibleNews, intensity: Int): String {
        val text = "${news.title} ${news.content}".lowercase()
        return when {
            listOf("morto", "morte", "massacre", "decap", "assassin", "apedrej", "sangue").any { term -> text.contains(term) } -> "violência"
            listOf("luto", "perdeu", "perda", "sofrimento", "hemorragia").any { term -> text.contains(term) } -> "tema sensível"
            intensity >= 3 -> "consequências intensas"
            else -> ""
        }
    }

    private fun buildSummary(content: String): String =
        content.trim().let { text ->
            if (text.length <= 150) text else text.take(147).trimEnd() + "…"
        }

    private fun buildStoryKey(news: BibleNews): String =
        "${news.book}-${news.chapter}-${news.verse}-${news.title.lowercase().hashCode()}"
}
