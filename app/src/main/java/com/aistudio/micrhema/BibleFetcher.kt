package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.util.Log


import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class BibleVerse(
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

object BibleFetcher {
    private suspend fun getChapterFromFirestore(book: String, chapter: Int, translation: String): List<BibleVerse> {
        return try {
            val db = FirebaseFirestore.getInstance()
            // Formata o nome do livro para bater com o banco se necessário, 
            // mas o ideal é usar o nome exato.
            val docSnapshot = db.collection("bibles").document(translation).collection("books").document(book).get().await()
            
            if (docSnapshot.exists()) {
                // Como salvamos no script como um mapa: { "1": ["No principio...", ...], "2": [...] }
                val chaptersMap = docSnapshot.get("chapters") as? Map<String, List<String>>
                val verses = mutableListOf<BibleVerse>()
                
                if (chaptersMap != null) {
                    val verseList = chaptersMap[chapter.toString()]
                    if (verseList != null) {
                        for (v in verseList.indices) {
                            verses.add(BibleVerse(bookName = book, chapter = chapter, verse = v + 1, text = verseList[v]))
                        }
                    }
                }
                verses
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BibleFetcher", "Erro ao buscar do Firestore: ${e.message}")
            emptyList()
        }
    }

    suspend fun getChapter(context: Context, book: String, chapter: Int, translation: String): List<BibleVerse> {
        if (translation == "ARA" || translation == "NVI" || translation == "ACF") {
            // 1. Tenta buscar do local primeiro (muito mais rápido, offline e grátis)
            val localResult = LocalBibleFetcher.getChapter(context, book, chapter, translation)
            if (localResult.isNotEmpty()) {
                return localResult
            }
            
            // 2. Fallback para Firestore caso os assets locais sejam removidos no futuro
            Log.d("BibleFetcher", "Buscando do Firestore como fallback...")
            val firestoreResult = getChapterFromFirestore(book, chapter, translation)
            if (firestoreResult.isNotEmpty()) {
                return firestoreResult
            }
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

                // Não usar outra tradução como fallback silencioso: a versão solicitada
                // deve possuir provider próprio ou ser apresentada como indisponível.
                return@withContext emptyList()

                /*
                val translationParam = when (translation) {
                    "ARA" -> "almeida"
                    else -> return@withContext emptyList()
                }
                val urlString = "https://bible-api.com/$formattedBook+$chapter?translation=$translationParam"
                Log.d("BibleFetcher", "Fetching from: $urlString")
                val response = URL(urlString).readText()*/
                emptyList<BibleVerse>()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("BibleFetcher", "Error fetching Bible: ${e.message}")
                emptyList()
            }
        }
    }
}
