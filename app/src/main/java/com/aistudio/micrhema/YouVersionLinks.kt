package com.aistudio.micrhema

import android.net.Uri

/** Catálogo de versões e rota interna do leitor bíblico. */
object YouVersionLinks {
    data class Version(
        val code: String,
        val name: String,
        val bibleId: String
    )

    val versions = listOf(
        Version("ARA", "Almeida Revista e Atualizada", "1608"),
        Version("NVI", "Nova Versão Internacional", "129"),
        Version("NTLH", "Nova Tradução na Linguagem de Hoje", "211")
    )

    private val bookCodes = mapOf(
        "Gênesis" to "GEN",
        "Êxodo" to "EXO",
        "Levítico" to "LEV",
        "Números" to "NUM",
        "Deuteronômio" to "DEU",
        "Josué" to "JOS",
        "Juízes" to "JDG",
        "Rute" to "RUT",
        "1 Samuel" to "1SA",
        "2 Samuel" to "2SA",
        "1 Reis" to "1KI",
        "2 Reis" to "2KI",
        "1 Crônicas" to "1CH",
        "2 Crônicas" to "2CH",
        "Esdras" to "EZR",
        "Neemias" to "NEH",
        "Ester" to "EST",
        "Jó" to "JOB",
        "Salmos" to "PSA",
        "Provérbios" to "PRO",
        "Eclesiastes" to "ECC",
        "Cânticos" to "SNG",
        "Isaías" to "ISA",
        "Jeremias" to "JER",
        "Lamentações" to "LAM",
        "Ezequiel" to "EZK",
        "Daniel" to "DAN",
        "Oséias" to "HOS",
        "Joel" to "JOL",
        "Amós" to "AMO",
        "Obadias" to "OBA",
        "Jonas" to "JON",
        "Miquéias" to "MIC",
        "Naum" to "NAM",
        "Habacuque" to "HAB",
        "Sofonias" to "ZEP",
        "Ageu" to "HAG",
        "Zacarias" to "ZEC",
        "Malaquias" to "MAL",
        "Mateus" to "MAT",
        "Marcos" to "MRK",
        "Lucas" to "LUK",
        "João" to "JHN",
        "Atos" to "ACT",
        "Romanos" to "ROM",
        "1 Coríntios" to "1CO",
        "2 Coríntios" to "2CO",
        "Gálatas" to "GAL",
        "Efésios" to "EPH",
        "Filipenses" to "PHP",
        "Colossenses" to "COL",
        "1 Tessalonicenses" to "1TH",
        "2 Tessalonicenses" to "2TH",
        "1 Timóteo" to "1TI",
        "2 Timóteo" to "2TI",
        "Tito" to "TIT",
        "Filemom" to "PHM",
        "Hebreus" to "HEB",
        "Tiago" to "JAS",
        "1 Pedro" to "1PE",
        "2 Pedro" to "2PE",
        "1 João" to "1JN",
        "2 João" to "2JN",
        "3 João" to "3JN",
        "Judas" to "JUD",
        "Apocalipse" to "REV"
    )

    fun version(code: String?): Version =
        versions.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: versions.first()

    fun chapterUrl(book: String, chapter: Int, versionCode: String = "ARA"): String? {
        if (chapter < 1) return null
        val version = versions.firstOrNull { it.code.equals(versionCode, ignoreCase = true) } ?: return null
        val bookCode = bookCodes[book] ?: return null
        return "https://www.bible.com/pt/bible/${version.bibleId}/$bookCode.$chapter.${version.code}"
    }

    fun encodedBook(book: String): String = Uri.encode(book)

    fun internalRoute(book: String, chapter: Int, versionCode: String = "ARA", verse: Int? = null): String {
        val resolvedVerse = verse?.takeIf { it > 0 }
            ?: BibleNewsPendingNavigation.consume(book, chapter)
        val verseParameter = resolvedVerse?.takeIf { it > 0 }?.let { "&verse=$it" }.orEmpty()
        return "bible_reader?book=${Uri.encode(book)}&chapter=$chapter&version=${Uri.encode(versionCode)}$verseParameter"
    }
}
