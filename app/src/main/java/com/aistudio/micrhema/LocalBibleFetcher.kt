package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

object LocalBibleFetcher {
    private var ntlhCache: JSONObject? = null
    private var nviCache: JSONArray? = null
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

    private suspend fun getCache(context: Context, version: String): Any? = withContext(Dispatchers.IO) {
        when (version) {
            "NTLH" -> {
                if (ntlhCache == null) {
                    val stream = context.assets.open("bibles/ntlh.json")
                    val jsonString = InputStreamReader(stream).readText()
                    ntlhCache = JSONObject(jsonString)
                }
                ntlhCache
            }
            "NVI" -> {
                if (nviCache == null) {
                    val stream = context.assets.open("bibles/nvi.json")
                    // remove BOM if exists
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
            else -> null
        }
    }

    suspend fun getChapter(context: Context, book: String, chapter: Int, version: String): List<BibleVerse> {
        return withContext(Dispatchers.IO) {
            try {
                val verses = mutableListOf<BibleVerse>()
                val cache = getCache(context, version) ?: return@withContext emptyList()

                val normBook = removeAccents(book)

                if (version == "NTLH") {
                    val jsonObj = cache as JSONObject
                    var targetKey = ""
                    for (key in jsonObj.keys()) {
                        val normKey = removeAccents(key)
                        if (normKey == normBook || (normBook == "exodo" && normKey == "ex") || (normBook == "1 timoteo" && normKey == "1tn")) {
                            targetKey = key
                            break
                        }
                    }
                    if (targetKey.isNotEmpty() && jsonObj.has(targetKey)) {
                        val bookObj = jsonObj.getJSONObject(targetKey)
                        val chapterKey = chapter.toString()
                        if (bookObj.has(chapterKey)) {
                            val verseArray = bookObj.getJSONArray(chapterKey)
                            for (i in 0 until verseArray.length()) {
                                verses.add(BibleVerse(bookName = book, chapter = chapter, verse = i + 1, text = verseArray.getString(i)))
                            }
                        }
                    }
                } else {
                    val jsonArr = cache as JSONArray
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
                }
                verses
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
