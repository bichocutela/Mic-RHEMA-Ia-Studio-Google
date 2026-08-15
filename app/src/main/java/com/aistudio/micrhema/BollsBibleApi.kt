package com.aistudio.micrhema

import android.text.Html
import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
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
        Translation("NTLH", "Nova Tradução na Linguagem de Hoje", "NTLH")
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

    private fun cleanText(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}
