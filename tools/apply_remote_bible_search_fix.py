from pathlib import Path

ROOT = Path("app/src/main/java/com/aistudio/micrhema")

deep_search = r'''package com.aistudio.micrhema

import android.text.Html
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Resultado navegável da pesquisa profunda da Bíblia. */
data class BibleDeepSearchResult(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val score: Int
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

/**
 * Pesquisa em toda a tradução selecionada usando o endpoint oficial de pesquisa do Bolls,
 * que desde julho de 2026 oferece busca semântica para consultas comuns. O leitor do MIC Rhema
 * já usa o mesmo serviço para carregar capítulos, então a pesquisa e a leitura ficam alinhadas.
 */
object BibleDeepSearchEngine {
    private const val baseUrl = "https://bolls.life"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun search(rawQuery: String, versionCode: String, limit: Int = 24): List<BibleDeepSearchResult> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val normalizedVersion = BollsBibleCatalog.normalize(versionCode)

        // Referências explícitas (ex.: João 3:16) devem ser exatas, não semânticas.
        parseDeepReference(query)?.let { reference ->
            val chapterVerses = BollsBibleApi.getChapter(reference.book, reference.chapter, normalizedVersion)
            val exact = if (reference.verse != null) {
                chapterVerses.firstOrNull { it.verse == reference.verse }
            } else {
                chapterVerses.firstOrNull()
            }
            if (exact != null) {
                return listOf(
                    BibleDeepSearchResult(
                        book = reference.book,
                        chapter = reference.chapter,
                        verse = exact.verse,
                        text = exact.text,
                        score = 100_000
                    )
                )
            }
        }

        return searchRemote(query, normalizedVersion, limit)
    }

    private suspend fun searchRemote(
        query: String,
        versionCode: String,
        limit: Int
    ): List<BibleDeepSearchResult> = withContext(Dispatchers.IO) {
        val translation = BollsBibleCatalog.translation(versionCode)
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val safeLimit = limit.coerceIn(1, 40)
        val url = "$baseUrl/v2/find/${translation.apiCode}?search=$encodedQuery&limit=$safeLimit&page=1"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "MIC-Rhema-Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Pesquisa bíblica indisponível (HTTP ${response.code})")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IllegalStateException("Pesquisa bíblica retornou uma resposta vazia")

            val root = JsonParser.parseString(body).asJsonObject
            val resultArray = root.get("results")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return@withContext emptyList()
            val canonicalBooks = chapterCounts.keys.toList()

            resultArray.mapIndexedNotNull { index, item ->
                runCatching {
                    val obj = item.asJsonObject
                    val bookId = obj.get("book")?.asInt ?: return@runCatching null
                    val chapter = obj.get("chapter")?.asInt ?: return@runCatching null
                    val verse = obj.get("verse")?.asInt ?: return@runCatching null
                    val text = obj.get("text")?.asString.orEmpty()
                    val book = canonicalBooks.getOrNull(bookId - 1) ?: return@runCatching null
                    if (chapter !in 1..(chapterCounts[book] ?: 0) || verse < 1) return@runCatching null
                    BibleDeepSearchResult(
                        book = book,
                        chapter = chapter,
                        verse = verse,
                        text = cleanDeepSearchHtml(text),
                        score = 10_000 - index
                    )
                }.getOrNull()
            }
        }
    }

    private fun cleanDeepSearchHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}

/** Pesquisa profunda exibida depois da lista dos 66 livros. */
@Composable
fun BibleDeepSearchSection(
    versionCode: String,
    onResultClick: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BibleDeepSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchFailure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, versionCode) {
        if (query.trim().length < 2) {
            results = emptyList()
            isSearching = false
            hasSearched = false
            searchFailure = null
            return@LaunchedEffect
        }
        // Evita uma requisição a cada tecla enquanto a pessoa ainda está digitando.
        delay(450)
        isSearching = true
        searchFailure = null
        val attempt = runCatching {
            BibleDeepSearchEngine.search(query, versionCode)
        }
        results = attempt.getOrDefault(emptyList())
        searchFailure = attempt.exceptionOrNull()?.message
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
            "Pesquise por palavra, nome, situação, tema, referência ou parte de um versículo. A busca inteligente procura em toda a Bíblia e abre o resultado direto na leitura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Pesquisar em toda a Bíblia") },
            placeholder = { Text("Ex.: curado, ansiedade, Davi, João 3:16") },
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
            "Pesquisando na versão ${BollsBibleCatalog.normalize(versionCode)} selecionada. Para resultados completos, é necessária conexão com a internet.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            isSearching -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Procurando referências…")
                }
            }

            hasSearched && searchFailure != null -> {
                Text(
                    "Não foi possível pesquisar agora. Verifique sua conexão com a internet e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            hasSearched && results.isEmpty() -> {
                Text(
                    "Nenhuma referência encontrada nessa busca. Tente outra palavra, um tema parecido ou um trecho do versículo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            else -> {
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
}
'''

(ROOT / "BibleDeepSearch.kt").write_text(deep_search, encoding="utf-8")

module = ROOT / "BibleModule.kt"
text = module.read_text(encoding="utf-8")
old_call = '''            BibleBookAndChapterPicker(
                selectedBook = selectedBook,
                expandedBook = expandedBook,'''
new_call = '''            BibleBookAndChapterPicker(
                selectedBook = selectedBook,
                expandedBook = expandedBook,
                selectedVersion = selectedVersion,'''
if old_call not in text:
    raise SystemExit("Chamada de BibleBookAndChapterPicker não encontrada")
text = text.replace(old_call, new_call, 1)

old_signature = '''private fun BibleBookAndChapterPicker(
    selectedBook: String,
    expandedBook: String?,
    onBookClick: (String) -> Unit,'''
new_signature = '''private fun BibleBookAndChapterPicker(
    selectedBook: String,
    expandedBook: String?,
    selectedVersion: String,
    onBookClick: (String) -> Unit,'''
if old_signature not in text:
    raise SystemExit("Assinatura de BibleBookAndChapterPicker não encontrada")
text = text.replace(old_signature, new_signature, 1)

old_section = '''        item(key = "deep_search") {
            BibleDeepSearchSection(onResultClick = onDeepSearchResultClick)
        }'''
new_section = '''        item(key = "deep_search") {
            BibleDeepSearchSection(
                versionCode = selectedVersion,
                onResultClick = onDeepSearchResultClick
            )
        }'''
if old_section not in text:
    raise SystemExit("Seção de pesquisa profunda não encontrada")
text = text.replace(old_section, new_section, 1)
module.write_text(text, encoding="utf-8")

# A leitura local também não deve exigir o campo `name`, pois os JSONs atuais usam `abbrev`.
local = ROOT / "LocalBibleFetcher.kt"
local_text = local.read_text(encoding="utf-8")
old_local = '''            for (i in 0 until cache.length()) {
                val bookObj = cache.getJSONObject(i)
                val name = bookObj.getString("name")
                val normName = removeAccents(name)
                if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamenta"))) {'''
new_local = '''            val canonicalBooks = chapterCounts.keys.toList()
            for (i in 0 until minOf(cache.length(), canonicalBooks.size)) {
                val bookObj = cache.getJSONObject(i)
                val name = bookObj.optString("name").takeIf { it.isNotBlank() } ?: canonicalBooks[i]
                val normName = removeAccents(name)
                if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamenta"))) {'''
if old_local in local_text:
    local.write_text(local_text.replace(old_local, new_local, 1), encoding="utf-8")

print("Pesquisa profunda migrada para a busca semântica completa da versão selecionada.")
