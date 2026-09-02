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
    /** Quantidade canônica de capítulos dos 66 livros, na mesma ordem de [chapterCounts]. */
    private val expectedChapters = listOf(
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31,
        12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28,
        16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5,
        3, 5, 1, 1, 1, 22
    )

    fun validateAsset(context: Context, version: String): BibleValidationReport {
        val fileName = when (version.uppercase()) {
            "ACF" -> "pt_acf.json"
            else -> return BibleValidationReport(
                version,
                0,
                listOf("$version indisponível: sem dataset local associado a essa tradução")
            )
        }

        val invalid = mutableListOf<String>()
        return try {
            val json = InputStreamReader(context.assets.open("bibles/$fileName"), Charsets.UTF_8).use {
                JSONArray(it.readText().removePrefix("\uFEFF"))
            }

            if (json.length() != chapterCounts.size) {
                invalid += "Esperados ${chapterCounts.size} livros, encontrados ${json.length()}"
            }

            val canonicalBooks = chapterCounts.keys.toList()
            val limit = minOf(json.length(), expectedChapters.size, canonicalBooks.size)
            for (bookIndex in 0 until limit) {
                val book = json.optJSONObject(bookIndex)
                if (book == null) {
                    invalid += "Livro ${bookIndex + 1} inválido"
                    continue
                }

                val expectedName = canonicalBooks[bookIndex]
                val name = book.optString("name", expectedName)
                val chapters = book.optJSONArray("chapters")
                val expectedCount = expectedChapters[bookIndex]

                if (chapters == null || chapters.length() != expectedCount) {
                    invalid += "$name: capítulos inválidos (esperado $expectedCount)"
                    continue
                }

                for (chapterIndex in 0 until chapters.length()) {
                    val verses = chapters.optJSONArray(chapterIndex)
                    if (verses == null || verses.length() == 0) {
                        invalid += "$name capítulo ${chapterIndex + 1}: vazio"
                        continue
                    }
                    for (verseIndex in 0 until verses.length()) {
                        if (verses.optString(verseIndex).trim().isEmpty()) {
                            invalid += "$name capítulo ${chapterIndex + 1} versículo ${verseIndex + 1}: vazio"
                        }
                    }
                }
            }

            BibleValidationReport(version.uppercase(), json.length(), invalid)
        } catch (error: Exception) {
            BibleValidationReport(
                version.uppercase(),
                0,
                listOf("Falha ao ler $fileName: ${error.message ?: "erro desconhecido"}")
            )
        }
    }
}
