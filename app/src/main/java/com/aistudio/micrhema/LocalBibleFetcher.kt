package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

/**
 * Leitura de traduções que realmente estão disponíveis localmente no aplicativo.
 * Não anuncia versões ausentes e nunca substitui uma tradução por outra.
 */
object LocalBibleFetcher {
    private var acfCache: JSONArray? = null

    private fun removeAccents(str: String): String {
        return str.lowercase()
            .replace("á", "a").replace("ã", "a").replace("â", "a").replace("à", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o").replace("ô", "o").replace("õ", "o")
            .replace("ú", "u")
            .replace("ç", "c")
    }

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
}
