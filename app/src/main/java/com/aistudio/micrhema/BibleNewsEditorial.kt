package com.aistudio.micrhema

import java.text.Normalizer

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
        val canonicalTitle = BibleNewsData.newsList.firstOrNull { it.id == news.id }?.title
        val shouldUseCanonicalTitle = canonicalTitle != null && (
            news.title == news.title.uppercase() ||
                news.title.contains("parte", ignoreCase = true)
            )
        val titleBeforeCleanup = if (shouldUseCanonicalTitle) canonicalTitle.orEmpty() else news.title
        val cleanTitle = normalizeTitle(titleBeforeCleanup)
        val source = if (cleanTitle == news.title) news else news.copy(title = cleanTitle)
        val category = source.category.ifBlank { inferCategory(source) }
        val intensity = normalizeIntensity(if (source.intensity in 1..4) source.intensity else inferIntensity(source))
        val summary = source.summary.ifBlank { buildSummary(source.content) }
        val tags = if (source.tags.isEmpty()) inferTags(source, category) else source.tags
        val publishedAt = if (source.publishedAt > 0L) source.publishedAt else source.id.toLong()
        val featured = source.featured
        val storyKey = source.storyKey.ifBlank { buildStoryKey(source) }
        val warning = source.contentWarning.ifBlank { inferWarning(source, intensity) }
        return source.copy(
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

    fun mergeUnique(items: List<BibleNews>): List<BibleNews> =
        items.map(::decorate)
            .groupBy { editorialKey(it) }
            .values
            .map { group ->
                val selected = group.maxByOrNull { it.publishedAt } ?: group.first()
                if (selected.imageUrl.isNotBlank()) selected
                else group.firstOrNull { it.imageUrl.isNotBlank() }?.copy(
                    title = selected.title,
                    summary = selected.summary,
                    category = selected.category,
                    intensity = selected.intensity,
                    tags = selected.tags,
                    contentWarning = selected.contentWarning,
                    publishedAt = selected.publishedAt,
                    featured = selected.featured,
                    storyKey = selected.storyKey
                ) ?: selected
            }
            .sortedByDescending { it.publishedAt }

    /**
     * O Firestore é a camada de sobrescrita. O catálogo empacotado só entra
     * quando ainda não existe um registro remoto com o mesmo ID. Isso evita que
     * uma edição remota dispute prioridade com a cópia local do APK.
     */
    fun withEditorialCatalog(items: List<BibleNews>): List<BibleNews> {
        val remote = decorateAll(items)
        val remoteIds = remote.map { it.id }.toSet()
        val bundledFallback = (BibleNewsData.newsList + BibleNewsEditorialCatalog.additionalNews)
            .filterNot { it.id in remoteIds }
        return mergeUnique(remote + bundledFallback)
            .filterNot { it.id in hiddenBibleNewsIdsState }
    }

    fun matches(news: BibleNews, query: String, filter: String): Boolean {
        val normalizedQuery = normalizeSearch(query)
        val searchableText = listOf(
            news.title,
            news.summary,
            news.content,
            news.book,
            news.category,
            news.tags.joinToString(" ")
        ).joinToString(" ")
        val textMatches = normalizedQuery.isBlank() || normalizeSearch(searchableText).contains(normalizedQuery)
        if (!textMatches) return false
        return when {
            filter.isBlank() || filter == "Tudo" -> true
            filter == "Mais recentes" -> true
            intensityLabels.values.contains(filter) -> intensityLabel(news.intensity) == filter
            else -> news.category == filter || news.tags.any { it.equals(filter, ignoreCase = true) }
        }
    }

    private fun normalizeSearch(value: String): String =
        Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")

    private fun inferCategory(news: BibleNews): String {
        val text = "${news.title} ${news.content}".lowercase()
        return when {
            listOf("fogo", "mar", "cura", "cego", "pão", "anjo", "carruagem", "peixe", "ressusc").any { term -> text.contains(term) } -> "Milagres e sinais"
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

    private fun normalizeTitle(title: String): String =
        title.replace(Regex("\\s*[-–—:]?\\s*\\(?parte\\s+\\d+(?:\\s*(?:de|/|-)\\s*\\d+)?\\)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .removeSuffix("-")
            .trim()

    private fun editorialKey(news: BibleNews): String {
        if (news.id > 0) return "id-${news.id}"
        if (news.storyKey.isNotBlank()) return "story-${news.storyKey.lowercase()}"
        return normalizeTitle(news.title)
            .lowercase()
            .replace(Regex("[^a-z0-9áéíóúãõç ]"), "")
            .trim()
    }

    private fun buildStoryKey(news: BibleNews): String =
        "${news.book}-${news.chapter}-${news.verse}-${normalizeTitle(news.title).lowercase().hashCode()}"
}
