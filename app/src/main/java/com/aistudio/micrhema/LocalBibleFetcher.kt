package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale

/** Resultado de uma pesquisa profunda na Bíblia local. */
data class BibleSearchResult(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val score: Int
)

/**
 * Leitura de traduções que realmente estão disponíveis localmente no aplicativo.
 * Não anuncia versões ausentes e nunca substitui uma tradução por outra.
 */
object LocalBibleFetcher {
    private var acfCache: JSONArray? = null

    private fun removeAccents(str: String): String =
        Normalizer.normalize(str.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private suspend fun getCache(context: Context, version: String): JSONArray? = withContext(Dispatchers.IO) {
        if (!version.equals("ACF", ignoreCase = true)) return@withContext null
        if (acfCache == null) {
            val stream = context.assets.open("bibles/pt_acf.json")
            var jsonString = InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
            if (jsonString.startsWith("\uFEFF")) jsonString = jsonString.substring(1)
            acfCache = JSONArray(jsonString)
        }
        acfCache
    }

    suspend fun isVersionDownloaded(context: Context, version: String): Boolean = withContext(Dispatchers.IO) {
        if (!version.equals("ACF", ignoreCase = true)) return@withContext false
        runCatching {
            context.assets.open("bibles/pt_acf.json").use { }
            true
        }.getOrDefault(false)
    }

    suspend fun getChapter(
        context: Context,
        book: String,
        chapter: Int,
        version: String
    ): List<BibleVerse> = withContext(Dispatchers.IO) {
        try {
            if (chapter !in 1..(chapterCounts[book] ?: 0)) return@withContext emptyList()
            val cache = getCache(context, version) ?: return@withContext emptyList()
            val normBook = removeAccents(book)
            val verses = mutableListOf<BibleVerse>()

            for (i in 0 until cache.length()) {
                val bookObj = cache.getJSONObject(i)
                val name = bookObj.getString("name")
                val normName = removeAccents(name)
                if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamenta"))) {
                    val chaptersArr = bookObj.getJSONArray("chapters")
                    if (chapter - 1 >= chaptersArr.length()) return@withContext emptyList()
                    val verseArray = chaptersArr.getJSONArray(chapter - 1)
                    for (v in 0 until verseArray.length()) {
                        val text = verseArray.optString(v).trim()
                        if (text.isNotEmpty()) {
                            verses += BibleVerse(
                                bookName = book,
                                chapter = chapter,
                                verse = v + 1,
                                text = text
                            )
                        }
                    }
                    break
                }
            }
            verses
        } catch (error: Exception) {
            android.util.Log.e("LocalBibleFetcher", "Falha ao ler $book $chapter ($version)", error)
            emptyList()
        }
    }

    /**
     * Pesquisa todos os versículos da base ACF embarcada no APK.
     * A busca ignora acentos e pontuação e aceita várias palavras em qualquer ordem.
     * Resultados com a frase completa recebem prioridade, seguidos por resultados
     * que contêm todos os termos e depois por correspondências parciais relevantes.
     */
    suspend fun searchBible(
        context: Context,
        rawQuery: String,
        limit: Int = 40
    ): List<BibleSearchResult> = withContext(Dispatchers.IO) {
        val query = removeAccents(rawQuery)
        if (query.length < 2) return@withContext emptyList()

        val cache = getCache(context, "ACF") ?: return@withContext emptyList()
        val tokens = query.split(' ').filter { it.length >= 2 }.distinct()
        if (tokens.isEmpty()) return@withContext emptyList()

        val canonicalBooks = chapterCounts.keys.toList()
        val results = ArrayList<BibleSearchResult>()

        for (bookIndex in 0 until minOf(cache.length(), canonicalBooks.size)) {
            val bookObject = cache.optJSONObject(bookIndex) ?: continue
            val chapters = bookObject.optJSONArray("chapters") ?: continue
            val canonicalBook = canonicalBooks[bookIndex]
            val normalizedBook = removeAccents(canonicalBook)

            for (chapterIndex in 0 until chapters.length()) {
                val verses = chapters.optJSONArray(chapterIndex) ?: continue
                for (verseIndex in 0 until verses.length()) {
                    val originalText = verses.optString(verseIndex).trim()
                    if (originalText.isEmpty()) continue
                    val normalizedText = removeAccents(originalText)
                    val searchable = "$normalizedBook $normalizedText"

                    val phraseMatch = searchable.contains(query)
                    val matchedTokens = tokens.count { token -> searchable.contains(token) }
                    if (!phraseMatch && matchedTokens == 0) continue

                    val allTokens = matchedTokens == tokens.size
                    val startsWithPhrase = normalizedText.startsWith(query)
                    val score = when {
                        startsWithPhrase -> 0
                        phraseMatch -> 10
                        allTokens -> 20 + normalizedText.length.coerceAtMost(400) / 100
                        matchedTokens >= maxOf(1, (tokens.size + 1) / 2) -> 50 + (tokens.size - matchedTokens) * 5
                        else -> continue
                    }

                    results += BibleSearchResult(
                        book = canonicalBook,
                        chapter = chapterIndex + 1,
                        verse = verseIndex + 1,
                        text = originalText,
                        score = score
                    )
                }
            }
        }

        results.sortedWith(
            compareBy<BibleSearchResult> { it.score }
                .thenBy { chapterCounts.keys.indexOf(it.book) }
                .thenBy { it.chapter }
                .thenBy { it.verse }
        ).take(limit.coerceIn(1, 100))
    }
}
