package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.util.Log

data class BibleVerse(
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

object BibleFetcher {
    suspend fun getChapter(context: Context, book: String, chapter: Int, translation: String): List<BibleVerse> {
        if (translation == "NTLH" || translation == "NVI" || translation == "ACF") {
            return LocalBibleFetcher.getChapter(context, book, chapter, translation)
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Formatting book name for URL (e.g. "1 João" -> "1+Joao")
                var formattedBook = book.replace(" ", "+")
                
                // Some normalization for Portuguese names
                formattedBook = formattedBook
                    .replace("ê", "e")
                    .replace("é", "e")
                    .replace("í", "i")
                    .replace("ã", "a")
                    .replace("á", "a")
                    .replace("ó", "o")
                    .replace("ú", "u")
                    .replace("ç", "c")

                val translationParam = when(translation) {
                    "ARA" -> "almeida"
                    else -> "almeida"
                }

                val urlString = "https://bible-api.com/$formattedBook+$chapter?translation=$translationParam"
                Log.d("BibleFetcher", "Fetching from: $urlString")

                val response = URL(urlString).readText()
                val json = JSONObject(response)
                
                val versesArray = json.getJSONArray("verses")
                val resultList = mutableListOf<BibleVerse>()
                
                for (i in 0 until versesArray.length()) {
                    val verseObj = versesArray.getJSONObject(i)
                    resultList.add(
                        BibleVerse(
                            bookName = verseObj.getString("book_name"),
                            chapter = verseObj.getInt("chapter"),
                            verse = verseObj.getInt("verse"),
                            text = verseObj.getString("text").trim()
                        )
                    )
                }
                resultList
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("BibleFetcher", "Error fetching Bible: ${e.message}")
                emptyList()
            }
        }
    }
}
