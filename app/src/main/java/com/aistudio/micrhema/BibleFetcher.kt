package com.aistudio.micrhema

import android.content.Context

data class BibleVerse(
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

/**
 * Fachada única de leitura bíblica.
 * Quando existe um dataset local da própria tradução selecionada, ele é usado primeiro.
 * Se não houver, a mesma tradução é buscada no Bolls — nunca há troca silenciosa de versão.
 */
object BibleFetcher {
    suspend fun getChapter(
        context: Context,
        book: String,
        chapter: Int,
        translation: String
    ): List<BibleVerse> {
        val normalized = BollsBibleCatalog.normalize(translation)
        val supported = BollsBibleCatalog.translations.any {
            it.code.equals(normalized, ignoreCase = true)
        }
        if (!supported || chapter < 1 || chapter > (chapterCounts[book] ?: 0)) return emptyList()

        if (LocalBibleFetcher.isVersionDownloaded(context, normalized)) {
            val local = LocalBibleFetcher.getChapter(context, book, chapter, normalized)
            if (local.isNotEmpty()) return local
        }

        return BollsBibleApi.getChapter(book, chapter, normalized)
    }
}
