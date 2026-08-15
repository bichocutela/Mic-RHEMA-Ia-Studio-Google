package com.aistudio.micrhema

import android.text.Html
import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Catálogo das traduções utilizadas pelo leitor bíblico nativo do MIC Rhema. */
object BollsBibleCatalog {
    data class Translation(
        val code: String,
        val name: String,
        val apiCode: String
    )

    val translations = listOf(
        Translation("ARA", "Almeida Revista e Atualizada", "ARA"),
        Translation("NVI", "Nova Versão Internacional", "NVIPT"),
        Translation("NTLH", "Nova Tradução na Linguagem de Hoje", "NTLH"),
        Translation("NAA", "Nova Almeida Atualizada 2017", "NAA"),
        Translation("ARC", "Almeida Revista e Corrigida 2009", "ARC09"),
        Translation("ACF", "Almeida Corrigida Fiel 2011", "ACF11"),
        Translation("NVT", "Nova Versão Transformadora 2016", "NVT"),
        Translation("NBV", "Nova Bíblia Viva 2007", "NBV07"),
        Translation("KJA", "King James Atualizada 2001", "KJA")
    )

    fun normalize(code: String?): String =
        translations.firstOrNull { it.code.equals(code, ignoreCase = true) }?.code ?: "ARA"

    fun translation(code: String?): Translation =
        translations.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: translations.first()

    fun bookId(book: String): Int = chapterCounts.keys.indexOf(book) + 1
}

object BollsBibleApi {
    private const val baseUrl = "https://bolls.life"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun getChapter(book: String, chapter: Int, versionCode: String): List<BibleVerse> =
        withContext(Dispatchers.IO) {
            val bookId = BollsBibleCatalog.bookId(book)
            val translation = BollsBibleCatalog.translation(versionCode)
            if (bookId < 1 || chapter < 1) return@withContext emptyList()

            val url = "$baseUrl/get-text/${translation.apiCode}/$bookId/$chapter/"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w("BollsBibleApi", "Resposta HTTP ${response.code} para $url")
                        return@withContext emptyList()
                    }

                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) return@withContext emptyList()

                    JsonParser.parseString(body).asJsonArray.mapNotNull { item ->
                        runCatching {
                            val verseObject = item.asJsonObject
                            BibleVerse(
                                bookName = book,
                                chapter = chapter,
                                verse = verseObject.get("verse").asInt,
                                text = cleanText(verseObject.get("text").asString)
                            )
                        }.getOrNull()
                    }
                }
            } catch (error: Exception) {
                Log.e("BollsBibleApi", "Falha ao buscar $book $chapter ${translation.code}", error)
                emptyList()
            }
        }

    suspend fun getVerseComparison(
        book: String,
        chapter: Int,
        verse: Int,
        versionCodes: List<String>
    ): List<Pair<BollsBibleCatalog.Translation, BibleVerse?>> = coroutineScope {
        versionCodes.distinct().map { code ->
            async {
                val translation = BollsBibleCatalog.translation(code)
                translation to getVerse(book, chapter, verse, translation.code)
            }
        }.awaitAll()
    }

    private suspend fun getVerse(
        book: String,
        chapter: Int,
        verse: Int,
        versionCode: String
    ): BibleVerse? = withContext(Dispatchers.IO) {
        val bookId = BollsBibleCatalog.bookId(book)
        val translation = BollsBibleCatalog.translation(versionCode)
        if (bookId < 1 || chapter < 1 || verse < 1) return@withContext null

        val url = "$baseUrl/get-verse/${translation.apiCode}/$bookId/$chapter/$verse/"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("BollsBibleApi", "Resposta HTTP ${response.code} para $url")
                    return@withContext null
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val verseObject = JsonParser.parseString(body).asJsonObject
                BibleVerse(
                    bookName = book,
                    chapter = chapter,
                    verse = verseObject.get("verse").asInt,
                    text = cleanText(verseObject.get("text").asString)
                )
            }
        } catch (error: Exception) {
            Log.e("BollsBibleApi", "Falha ao buscar $book $chapter:$verse ${translation.code}", error)
            null
        }
    }

    private fun cleanText(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}
