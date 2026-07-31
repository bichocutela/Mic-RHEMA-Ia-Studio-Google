package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

object LocalBibleFetcher {
    private var ntlhCache: JSONArray? = null
    private var nviCache: JSONArray? = null
    private var acfCache: JSONArray? = null
    private var araCache: JSONArray? = null

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
        when (version) {
            "NTLH" -> {
                if (ntlhCache == null) {
                    val stream = context.assets.open("bibles/ntlh.json")
                    var jsonString = InputStreamReader(stream, "UTF-8").readText()
                    if (jsonString.startsWith("\uFEFF")) {
                        jsonString = jsonString.substring(1)
                    }
                    ntlhCache = JSONArray(jsonString)
                }
                ntlhCache
            }
            "NVI" -> {
                if (nviCache == null) {
                    val stream = context.assets.open("bibles/nvi.json")
                    var jsonString = InputStreamReader(stream, "UTF-8").readText()
                    if (jsonString.startsWith("\uFEFF")) {
                        jsonString = jsonString.substring(1)
                    }
                    nviCache = JSONArray(jsonString)
                }
                nviCache
            }
            "ACF" -> {
                if (acfCache == null) {
                    val stream = context.assets.open("bibles/acf.json")
                    var jsonString = InputStreamReader(stream, "UTF-8").readText()
                    if (jsonString.startsWith("\uFEFF")) {
                        jsonString = jsonString.substring(1)
                    }
                    acfCache = JSONArray(jsonString)
                }
                acfCache
            }
            "ARA" -> {
                if (araCache == null) {
                    val stream = context.assets.open("bibles/ara.json")
                    var jsonString = InputStreamReader(stream, "UTF-8").readText()
                    if (jsonString.startsWith("\uFEFF")) {
                        jsonString = jsonString.substring(1)
                    }
                    araCache = JSONArray(jsonString)
                }
                araCache
            }
            else -> null
        }
    }

    suspend fun getChapter(context: Context, book: String, chapter: Int, version: String): List<BibleVerse> {
        return withContext(Dispatchers.IO) {
            try {
                val verses = mutableListOf<BibleVerse>()
                val cache = getCache(context, version) ?: return@withContext emptyList()
                val normBook = removeAccents(book)

                val jsonArr = cache
                for (i in 0 until jsonArr.length()) {
                    val bookObj = jsonArr.getJSONObject(i)
                    val name = bookObj.getString("name")
                    val normName = removeAccents(name)
                    if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamentacoes"))) {
                        val chaptersArr = bookObj.getJSONArray("chapters")
                        if (chapter - 1 < chaptersArr.length()) {
                            val verseArray = chaptersArr.getJSONArray(chapter - 1)
                            for (v in 0 until verseArray.length()) {
                                verses.add(BibleVerse(bookName = book, chapter = chapter, verse = v + 1, text = verseArray.getString(v)))
                            }
                        }
                        break
                    }
                }
                verses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
