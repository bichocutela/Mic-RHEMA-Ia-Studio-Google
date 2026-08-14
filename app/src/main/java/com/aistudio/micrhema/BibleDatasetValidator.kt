package com.aistudio.micrhema

import android.content.Context
import org.json.JSONArray
import java.io.InputStreamReader

/** Valida a estrutura dos datasets locais; não valida nem distribui conteúdo licenciado. */
data class BibleValidationReport(
    val version: String,
    val bookCount: Int,
    val invalidEntries: List<String>
) {
    val isValid: Boolean get() = invalidEntries.isEmpty()
}

object BibleDatasetValidator {
    private val expectedChapters = listOf(
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 4, 4, 6, 4, 3, 1, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22
    )

    fun validateAsset(context: Context, version: String): BibleValidationReport {
        val fileName = when (version) {
            "ACF" -> "pt_acf.json"
            "NVI" -> "pt_nvi.json"
            else -> return BibleValidationReport(version, 0, listOf("$version indisponível: sem fonte local autorizada"))
        }
        val invalid = mutableListOf<String>()
        return try {
            val json = InputStreamReader(context.assets.open("bibles/$fileName"), Charsets.UTF_8).use { JSONArray(it.readText()) }
            if (json.length() != 66) invalid += "Esperados 66 livros, encontrados ${json.length()}"
            val limit = minOf(json.length(), expectedChapters.size)
            for (bookIndex in 0 until limit) {
                val book = json.optJSONObject(bookIndex)
                if (book == null) {
                    invalid += "Livro ${bookIndex + 1} inválido"
                    continue
                }
                val name = book.optString("name", "Livro ${bookIndex + 1}")
                val chapters = book.optJSONArray("chapters")
                if (chapters == null || chapters.length() != expectedChapters[bookIndex]) {
                    invalid += "$name: capítulos inválidos (esperado ${expectedChapters[bookIndex]})"
                    continue
                }
                for (chapterIndex in 0 until chapters.length()) {
                    val verses = chapters.optJSONArray(chapterIndex)
                    if (verses == null || verses.length() == 0) invalid += "$name capítulo ${chapterIndex + 1}: vazio"
                    else for (verseIndex in 0 until verses.length()) {
                        if (verses.optString(verseIndex).trim().isEmpty()) invalid += "$name capítulo ${chapterIndex + 1} versículo ${verseIndex + 1}: vazio"
                    }
                }
            }
            if (json.length() > 6 && json.optJSONObject(6)?.optJSONArray("chapters")?.length() != 21) invalid += "Juízes: deve conter 21 capítulos"
            BibleValidationReport(version, json.length(), invalid)
        } catch (e: Exception) {
            BibleValidationReport(version, 0, listOf("Falha ao ler $fileName: ${e.message}"))
        }
    }
}
