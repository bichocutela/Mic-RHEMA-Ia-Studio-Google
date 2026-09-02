package com.aistudio.micrhema

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale

/** Resultado navegável da pesquisa profunda da Bíblia. */
data class BibleDeepSearchResult(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val score: Int
)

private data class IndexedBibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val normalizedText: String,
    val words: Set<String>
)

private data class ParsedDeepReference(
    val book: String,
    val chapter: Int,
    val verse: Int?
)

private fun normalizeDeepBibleText(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

private val deepSearchStopWords = setOf(
    "a", "ao", "aos", "as", "com", "como", "da", "das", "de", "do", "dos", "e", "ela", "ele",
    "eles", "em", "essa", "esse", "esta", "estao", "estou", "eu", "foi", "me", "meu", "minha",
    "na", "nas", "no", "nos", "o", "os", "ou", "para", "por", "que", "se", "sem", "ser", "sou",
    "sua", "um", "uma", "voce"
)

/**
 * Expansões curtas para situações comuns. Não substituem o texto bíblico: servem apenas para
 * encontrar referências relacionadas quando o usuário descreve uma situação em linguagem cotidiana.
 */
private val deepSearchThemes = mapOf(
    "ansiedade" to listOf("ansioso", "cuidado", "preocupacao", "medo", "paz"),
    "ansioso" to listOf("ansiedade", "cuidado", "preocupacao", "medo", "paz"),
    "depressao" to listOf("tristeza", "abatido", "esperanca", "consolo"),
    "triste" to listOf("tristeza", "choro", "consolo", "esperanca"),
    "luto" to listOf("morte", "consolo", "choro", "ressurreicao", "esperanca"),
    "perdi" to listOf("perda", "morte", "consolo", "choro", "esperanca"),
    "perda" to listOf("morte", "consolo", "choro", "esperanca"),
    "doente" to listOf("enfermidade", "cura", "sarou", "saude"),
    "doenca" to listOf("enfermidade", "cura", "sarou", "saude"),
    "cura" to listOf("curou", "sarou", "enfermidade", "saude"),
    "briga" to listOf("ira", "contenda", "perdao", "paz"),
    "briguei" to listOf("ira", "contenda", "perdao", "paz"),
    "raiva" to listOf("ira", "furor", "mansidao", "perdao"),
    "perdao" to listOf("perdoar", "perdoou", "misericordia"),
    "traicao" to listOf("adulterio", "infidelidade", "perdao"),
    "casamento" to listOf("marido", "mulher", "esposa", "amor", "matrimonio"),
    "filhos" to listOf("filho", "criancas", "pais", "familia"),
    "familia" to listOf("casa", "filho", "filhos", "pais"),
    "desempregado" to listOf("trabalho", "necessidade", "provisao", "sustento"),
    "trabalho" to listOf("obra", "trabalhar", "labor", "mao"),
    "dinheiro" to listOf("riqueza", "tesouro", "pobreza", "provisao"),
    "medo" to listOf("temor", "coragem", "forte", "paz"),
    "solidao" to listOf("sozinho", "desamparado", "presenca", "consolo"),
    "tentacao" to listOf("tentar", "pecado", "resistir", "livrar"),
    "vicio" to listOf("dominio", "pecado", "liberdade", "carne"),
    "amor" to listOf("amar", "amou", "caridade"),
    "fe" to listOf("crer", "creu", "confianca"),
    "oracao" to listOf("orar", "orei", "suplicar", "clamor"),
    "esperanca" to listOf("esperar", "confiar", "promessa"),
    "salvacao" to listOf("salvar", "salvo", "redencao", "vida eterna")
)

private fun deepSearchTokens(rawQuery: String): List<String> =
    normalizeDeepBibleText(rawQuery)
        .split(" ")
        .filter { it.length >= 2 && it !in deepSearchStopWords }
        .distinct()

private fun parseDeepReference(rawQuery: String): ParsedDeepReference? {
    val normalized = normalizeDeepBibleText(rawQuery)
    if (normalized.isBlank()) return null
    val compact = normalized.replace(" ", "")

    val candidates = chapterCounts.keys.mapNotNull { book ->
        val bookNorm = normalizeDeepBibleText(book)
        val bookCompact = bookNorm.replace(" ", "")
        when {
            normalized.startsWith("$bookNorm ") -> Triple(book, bookNorm.length, normalized.drop(bookNorm.length).trim())
            compact.startsWith(bookCompact) -> Triple(book, bookCompact.length, compact.drop(bookCompact.length))
            else -> null
        }
    }.sortedByDescending { it.second }

    val candidate = candidates.firstOrNull() ?: return null
    val numbers = Regex("^(\\d+)(?:[: ]?(\\d+))?$").matchEntire(candidate.third) ?: return null
    val chapter = numbers.groupValues[1].toIntOrNull() ?: return null
    val verse = numbers.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val maxChapter = chapterCounts[candidate.first] ?: return null
    if (chapter !in 1..maxChapter) return null
    return ParsedDeepReference(candidate.first, chapter, verse)
}

object BibleDeepSearchEngine {
    @Volatile
    private var indexCache: List<IndexedBibleVerse>? = null

    private suspend fun index(context: Context): List<IndexedBibleVerse> {
        indexCache?.let { return it }
        return withContext(Dispatchers.IO) {
            indexCache?.let { return@withContext it }
            val json = InputStreamReader(context.assets.open("bibles/pt_acf.json"), Charsets.UTF_8).use {
                JSONArray(it.readText().removePrefix("\uFEFF"))
            }
            val indexed = ArrayList<IndexedBibleVerse>(32_000)
            for (bookIndex in 0 until json.length()) {
                val bookObject = json.optJSONObject(bookIndex) ?: continue
                val rawBookName = bookObject.optString("name").trim()
                val book = if (rawBookName.isNotBlank()) {
                    rawBookName
                } else {
                    val fallbackBook = chapterCounts.keys.elementAtOrNull(bookIndex)
                    if (fallbackBook == null) continue
                    fallbackBook
                }
                val canonicalBook = chapterCounts.keys.firstOrNull {
                    normalizeDeepBibleText(it) == normalizeDeepBibleText(book)
                } ?: book
                val chapters = bookObject.optJSONArray("chapters") ?: continue
                for (chapterIndex in 0 until chapters.length()) {
                    val verses = chapters.optJSONArray(chapterIndex) ?: continue
                    for (verseIndex in 0 until verses.length()) {
                        val text = verses.optString(verseIndex).trim()
                        if (text.isBlank()) continue
                        val normalized = normalizeDeepBibleText(text)
                        indexed += IndexedBibleVerse(
                            book = canonicalBook,
                            chapter = chapterIndex + 1,
                            verse = verseIndex + 1,
                            text = text,
                            normalizedText = normalized,
                            words = normalized.split(" ").filter { it.isNotBlank() }.toSet()
                        )
                    }
                }
            }
            indexCache = indexed
            indexed
        }
    }

    suspend fun search(context: Context, rawQuery: String, limit: Int = 24): List<BibleDeepSearchResult> {
        val query = normalizeDeepBibleText(rawQuery)
        if (query.length < 2) return emptyList()
        val verses = index(context)

        parseDeepReference(rawQuery)?.let { reference ->
            val exact = verses.firstOrNull {
                it.book == reference.book && it.chapter == reference.chapter &&
                    (reference.verse == null || it.verse == reference.verse)
            }
            if (exact != null) {
                return listOf(
                    BibleDeepSearchResult(exact.book, exact.chapter, exact.verse, exact.text, 10_000)
                )
            }
        }

        val primaryTokens = deepSearchTokens(rawQuery)
        if (primaryTokens.isEmpty()) return emptyList()
        val expandedTokens = primaryTokens
            .flatMap { token -> deepSearchThemes[token].orEmpty().map(::normalizeDeepBibleText) }
            .flatMap { it.split(" ") }
            .filter { it.length >= 2 }
            .distinct()

        return withContext(Dispatchers.Default) {
            verses.asSequence().mapNotNull { verse ->
                var score = 0
                if (query.length >= 4 && verse.normalizedText.contains(query)) score += 1400

                val primaryMatches = primaryTokens.count { token ->
                    verse.words.contains(token) || verse.words.any { word ->
                        token.length >= 4 && (word.startsWith(token) || token.startsWith(word))
                    }
                }
                if (primaryMatches == primaryTokens.size) score += 650
                score += primaryMatches * 150

                val expandedMatches = expandedTokens.count { token ->
                    verse.words.contains(token) || verse.words.any { word ->
                        token.length >= 4 && (word.startsWith(token) || token.startsWith(word))
                    }
                }
                score += expandedMatches * 35

                if (score <= 0 || (primaryMatches == 0 && expandedMatches < 2)) {
                    null
                } else {
                    BibleDeepSearchResult(verse.book, verse.chapter, verse.verse, verse.text, score)
                }
            }.sortedWith(
                compareByDescending<BibleDeepSearchResult> { it.score }
                    .thenBy { chapterCounts.keys.indexOf(it.book).let { index -> if (index < 0) Int.MAX_VALUE else index } }
                    .thenBy { it.chapter }
                    .thenBy { it.verse }
            ).take(limit).toList()
        }
    }
}

/**
 * Pesquisa profunda exibida depois da lista dos 66 livros.
 * Usa o índice ACF local para localizar referências rapidamente e funciona sem depender da internet.
 */
@Composable
fun BibleDeepSearchSection(
    onResultClick: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BibleDeepSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            isSearching = false
            hasSearched = false
            return@LaunchedEffect
        }
        delay(280)
        isSearching = true
        results = runCatching { BibleDeepSearchEngine.search(context, query) }.getOrDefault(emptyList())
        hasSearched = true
        isSearching = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Text(
            "Pesquisa profunda",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Digite um nome, situação, tema, referência ou parte de um versículo. Os resultados mostram o livro, capítulo e versículo e abrem direto na leitura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Pesquisar em toda a Bíblia") },
            placeholder = { Text("Ex.: Deus amou o mundo, ansiedade, Davi, João 3:16") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisa profunda") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa")
                    }
                }
            }
        )

        Text(
            "A localização usa o índice bíblico ACF armazenado no app; ao tocar no resultado, a referência abre na versão que você estiver usando.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(10.dp))
                Text("Procurando referências…")
            }
        } else if (hasSearched && results.isEmpty()) {
            Text(
                "Nenhuma referência encontrada. Tente outra palavra, uma situação mais curta ou um trecho do versículo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            results.forEach { result ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(result.book, result.chapter, result.verse) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${result.book} ${result.chapter}:${result.verse}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                result.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Abrir ${result.book} ${result.chapter}:${result.verse}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
