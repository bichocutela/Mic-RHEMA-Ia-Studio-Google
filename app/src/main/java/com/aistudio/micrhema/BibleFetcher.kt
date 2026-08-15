package com.aistudio.micrhema

import android.content.Context

/**
 * Fachada de leitura bíblica usada pelas telas legadas e pelo leitor nativo.
 * O texto solicitado vem do Bolls; não há fallback silencioso para outra versão.
 */
object BibleFetcher {
    suspend fun getChapter(
        @Suppress("UNUSED_PARAMETER") context: Context,
        book: String,
        chapter: Int,
        translation: String
    ): List<BibleVerse> {
        val supported = setOf("ARA", "NVI", "NTLH").any {
            it.equals(translation, ignoreCase = true)
        }
        if (!supported) return emptyList()
        return BollsBibleApi.getChapter(book, chapter, translation)
    }
}
