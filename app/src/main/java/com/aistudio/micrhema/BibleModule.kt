package com.aistudio.micrhema

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

val chapterCounts = mapOf(
    "Gênesis" to 50, "Êxodo" to 40, "Levítico" to 27, "Números" to 36, "Deuteronômio" to 34,
    "Josué" to 24, "Juízes" to 21, "Rute" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
    "1 Reis" to 22, "2 Reis" to 25, "1 Crônicas" to 29, "2 Crônicas" to 36, "Esdras" to 10,
    "Neemias" to 13, "Ester" to 10, "Jó" to 42, "Salmos" to 150, "Provérbios" to 31,
    "Eclesiastes" to 12, "Cânticos" to 8, "Isaías" to 66, "Jeremias" to 52, "Lamentações" to 5,
    "Ezequiel" to 48, "Daniel" to 12, "Oséias" to 14, "Joel" to 3, "Amós" to 9, "Obadias" to 1,
    "Jonas" to 4, "Miquéias" to 7, "Naum" to 3, "Habacuque" to 3, "Sofonias" to 3, "Ageu" to 2,
    "Zacarias" to 14, "Malaquias" to 4, "Mateus" to 28, "Marcos" to 16, "Lucas" to 24, "João" to 21,
    "Atos" to 28, "Romanos" to 16, "1 Coríntios" to 16, "2 Coríntios" to 13, "Gálatas" to 6,
    "Efésios" to 6, "Filipenses" to 4, "Colossenses" to 4, "1 Tessalonicenses" to 5,
    "2 Tessalonicenses" to 3, "1 Timóteo" to 6, "2 Timóteo" to 4, "Tito" to 3, "Filemom" to 1,
    "Hebreus" to 13, "Tiago" to 5, "1 Pedro" to 5, "2 Pedro" to 3, "1 João" to 5,
    "2 João" to 1, "3 João" to 1, "Judas" to 1, "Apocalipse" to 22
)

private val bibleBookSearchAliases = mapOf(
    "Cânticos" to listOf("Cantares", "Cantares de Salomão"),
    "Apocalipse" to listOf("Revelação"),
    "Filemom" to listOf("Filemon")
)

private data class BibleChapterReference(val book: String, val chapter: Int)

private fun normalizeBibleBookTerm(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

private fun bibleBookSearchKeys(book: String): List<String> {
    val canonical = normalizeBibleBookTerm(book)
    val withoutOrdinal = canonical.replace(Regex("^[12]\\s+"), "")
    val aliases = bibleBookSearchAliases[book].orEmpty().map(::normalizeBibleBookTerm)
    return buildList {
        add(canonical)
        add(canonical.replace(" ", ""))
        add(withoutOrdinal)
        add(withoutOrdinal.replace(" ", ""))
        aliases.forEach { alias ->
            add(alias)
            add(alias.replace(" ", ""))
        }
    }.filter { it.isNotBlank() }.distinct()
}

private fun bibleBookEditDistance(left: String, right: String): Int {
    if (left == right) return 0
    if (left.isEmpty()) return right.length
    if (right.isEmpty()) return left.length

    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)

    for (i in left.indices) {
        current[0] = i + 1
        for (j in right.indices) {
            val insertion = current[j] + 1
            val deletion = previous[j + 1] + 1
            val substitution = previous[j + 1] + if (left[i] == right[j]) 0 else 1
            current[j + 1] = minOf(insertion, deletion, substitution)
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.length]
}

private fun bibleBookDirectScore(query: String, book: String): Int? {
    val keys = bibleBookSearchKeys(book)
    return keys.mapNotNull { key ->
        when {
            key == query -> 0
            key.startsWith(query) -> 1
            key.contains(query) -> 2
            else -> null
        }
    }.minOrNull()
}

private fun searchBibleBooks(rawQuery: String): List<String> {
    val books = chapterCounts.keys.toList()
    val query = normalizeBibleBookTerm(rawQuery)
    if (query.isBlank()) return books

    val compactQuery = query.replace(" ", "")
    val directMatches = books.mapIndexedNotNull { index, book ->
        val score = minOf(
            bibleBookDirectScore(query, book) ?: Int.MAX_VALUE,
            bibleBookDirectScore(compactQuery, book) ?: Int.MAX_VALUE
        )
        score.takeIf { it != Int.MAX_VALUE }?.let { Triple(book, it, index) }
    }.sortedWith(compareBy<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
        .map { it.first }

    if (directMatches.isNotEmpty()) return directMatches
    if (query.length < 3) return emptyList()

    val threshold = when {
        query.length <= 4 -> 1
        query.length <= 7 -> 2
        query.length <= 11 -> 3
        else -> 4
    }

    return books.mapIndexed { index, book ->
        val distance = bibleBookSearchKeys(book).minOf { key ->
            minOf(
                bibleBookEditDistance(query, key),
                bibleBookEditDistance(compactQuery, key.replace(" ", ""))
            )
        }
        Triple(book, distance, index)
    }.filter { it.second <= threshold }
        .sortedWith(compareBy<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
        .take(6)
        .map { it.first }
}

private fun validChapter(book: String, chapter: Int?): Int? {
    val max = chapterCounts[book] ?: return null
    return chapter?.takeIf { it in 1..max }
}

private fun adjacentBibleChapter(book: String, chapter: Int, direction: Int): BibleChapterReference? {
    if (direction == 0) return BibleChapterReference(book, chapter)
    val books = chapterCounts.keys.toList()
    val bookIndex = books.indexOf(book)
    if (bookIndex < 0) return null
    val maxChapter = chapterCounts[book] ?: return null

    if (direction < 0) {
        if (chapter > 1) return BibleChapterReference(book, chapter - 1)
        if (bookIndex == 0) return null
        val previousBook = books[bookIndex - 1]
        return BibleChapterReference(previousBook, chapterCounts[previousBook] ?: 1)
    }

    if (chapter < maxChapter) return BibleChapterReference(book, chapter + 1)
    if (bookIndex >= books.lastIndex) return null
    return BibleChapterReference(books[bookIndex + 1], 1)
}

/**
 * Leitor bíblico principal do MIC Rhema.
 * Livro e capítulo são escolhidos sem trocar de experiência visual; ao tocar no capítulo,
 * a leitura abre imediatamente e continua para capítulos/livros vizinhos na mesma tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    initialBook: String? = null,
    initialChapter: Int? = null,
    initialVersion: String? = null,
    initialVerse: Int? = null,
    @Suppress("UNUSED_PARAMETER")
    onOpenBible: (String, Int, String, Int?) -> Unit = { _, _, _, _ -> },
    onBack: (() -> Unit)? = null,
    onOpenComparison: ((String, Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val versions = BollsBibleCatalog.translations
    val initialValidBook = initialBook?.takeIf { chapterCounts.containsKey(it) }
    val startBook = initialValidBook ?: "Gênesis"
    val startChapter = validChapter(startBook, initialChapter)
    val normalizedInitialVersion = BollsBibleCatalog.normalize(initialVersion)
    val rememberedPosition = remember { BibleReadingPreferences.getLastReading(context) }
    val journeyMember = loggedInMemberState.value

    var selectedBook by rememberSaveable { mutableStateOf(startBook) }
    var expandedBook by rememberSaveable { mutableStateOf<String?>(startBook) }
    var expandedChapter by rememberSaveable { mutableStateOf(startChapter) }
    var activeReadingVerse by rememberSaveable {
        mutableStateOf(startChapter?.let { (initialVerse ?: 1).coerceAtLeast(1) })
    }
    var selectedVersion by rememberSaveable { mutableStateOf(normalizedInitialVersion) }
    var availableVerses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoadingVerses by remember { mutableStateOf(false) }
    var verseLoadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showVersionDialog by rememberSaveable { mutableStateOf(false) }
    var showBibleJourney by rememberSaveable { mutableStateOf(false) }
    var showResumeDialog by rememberSaveable {
        mutableStateOf(initialValidBook == null && initialChapter == null && rememberedPosition != null)
    }

    fun openChapter(reference: BibleChapterReference, verse: Int = 1) {
        selectedBook = reference.book
        expandedBook = reference.book
        expandedChapter = reference.chapter
        activeReadingVerse = verse.coerceAtLeast(1)
    }

    LaunchedEffect(initialValidBook, initialChapter, normalizedInitialVersion, initialVerse) {
        if (initialValidBook != null) {
            val chapter = validChapter(initialValidBook, initialChapter)
            selectedBook = initialValidBook
            expandedBook = initialValidBook
            expandedChapter = chapter
            activeReadingVerse = chapter?.let { (initialVerse ?: 1).coerceAtLeast(1) }
        }
        selectedVersion = normalizedInitialVersion
    }

    LaunchedEffect(selectedBook, expandedChapter, selectedVersion, reloadKey) {
        val chapter = expandedChapter
        if (chapter == null) {
            availableVerses = emptyList()
            isLoadingVerses = false
            verseLoadError = null
            return@LaunchedEffect
        }

        isLoadingVerses = true
        verseLoadError = null
        try {
            val loaded = BibleFetcher.getChapter(context, selectedBook, chapter, selectedVersion)
            availableVerses = loaded
            if (loaded.isEmpty()) {
                verseLoadError = "Não foi possível carregar este capítulo. Tente novamente."
            } else {
                val firstVerse = loaded.first().verse
                val lastVerse = loaded.last().verse
                if (activeReadingVerse == null || activeReadingVerse !in firstVerse..lastVerse) {
                    activeReadingVerse = firstVerse
                }
                BadgeActivityTracker.record(
                    context,
                    BadgeActivityKeys.BIBLE_CHAPTERS,
                    "$selectedVersion:$selectedBook:$chapter"
                )
            }
        } finally {
            isLoadingVerses = false
        }
    }

    val isReading = activeReadingVerse != null && expandedChapter != null
    val previousReference = expandedChapter?.let { adjacentBibleChapter(selectedBook, it, -1) }
    val nextReference = expandedChapter?.let { adjacentBibleChapter(selectedBook, it, 1) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isReading || onBack != null) {
                            IconButton(
                                onClick = {
                                    if (onBack != null) {
                                        onBack()
                                    } else {
                                        activeReadingVerse = null
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isReading) "$selectedBook ${expandedChapter ?: 1}" else "Bíblia Sagrada",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isReading) "Leitura contínua" else "Escolha um livro e um capítulo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isReading && onOpenComparison != null && expandedChapter != null) {
                            IconButton(
                                onClick = {
                                    onOpenComparison(
                                        selectedBook,
                                        expandedChapter ?: 1,
                                        activeReadingVerse ?: 1
                                    )
                                }
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = "Comparar versões")
                            }
                        }

                        if (journeyMember != null) {
                            IconButton(onClick = { showBibleJourney = true }) {
                                Icon(Icons.Default.Star, contentDescription = "Abrir Jornada Bíblica")
                            }
                        }

                        TextButton(onClick = { showVersionDialog = true }) {
                            Text(selectedVersion, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Escolher versão")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
                }
            }
        }
    ) { paddingValues ->
        if (isReading && expandedChapter != null) {
            key(selectedBook, expandedChapter, selectedVersion) {
                ContinuousBibleChapterReader(
                    book = selectedBook,
                    chapter = expandedChapter ?: 1,
                    version = selectedVersion,
                    verses = availableVerses,
                    focusedVerse = activeReadingVerse ?: 1,
                    isLoading = isLoadingVerses,
                    errorMessage = verseLoadError,
                    previousReference = previousReference,
                    nextReference = nextReference,
                    onPrevious = { previousReference?.let { openChapter(it) } },
                    onNext = { nextReference?.let { openChapter(it) } },
                    onRetry = { reloadKey++ },
                    onChooseAnotherReference = { activeReadingVerse = null },
                    onCompareVerse = onOpenComparison,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        } else {
            BibleBookAndChapterPicker(
                selectedBook = selectedBook,
                expandedBook = expandedBook,
                selectedVersion = selectedVersion,
                onBookClick = { book ->
                    selectedBook = book
                    expandedBook = if (expandedBook == book) null else book
                    expandedChapter = null
                    availableVerses = emptyList()
                    verseLoadError = null
                },
                onChapterClick = { book, chapter ->
                    openChapter(BibleChapterReference(book, chapter))
                },
                onDeepSearchResultClick = { book, chapter, verse ->
                    openChapter(BibleChapterReference(book, chapter), verse)
                },
                onOpenJourney = journeyMember?.let { { showBibleJourney = true } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    if (showResumeDialog && rememberedPosition != null) {
        val rememberedBook = rememberedPosition.book.takeIf { chapterCounts.containsKey(it) }
        val rememberedChapter = rememberedBook?.let { validChapter(it, rememberedPosition.chapter) }
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Continuar a leitura?") },
            text = {
                Text(
                    "Você parou em ${rememberedPosition.book} ${rememberedPosition.chapter}:${rememberedPosition.verse}. Deseja continuar de onde parou?"
                )
            },
            confirmButton = {
                Button(
                    enabled = rememberedBook != null && rememberedChapter != null,
                    onClick = {
                        showResumeDialog = false
                        selectedVersion = BollsBibleCatalog.normalize(rememberedPosition.version)
                        openChapter(
                            BibleChapterReference(rememberedBook ?: "Gênesis", rememberedChapter ?: 1),
                            rememberedPosition.verse
                        )
                    }
                ) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showResumeDialog = false }) { Text("Escolher outro livro") }
            }
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Versão da Bíblia") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(versions, key = { it.code }) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVersion = option.code
                                    showVersionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedVersion == option.code, onClick = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(option.code, fontWeight = FontWeight.Bold)
                                Text(
                                    option.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVersionDialog = false }) { Text("Fechar") } }
        )
    }

    if (showBibleJourney && journeyMember != null) {
        BibleJourneyDialog(
            member = journeyMember,
            onDismiss = { showBibleJourney = false }
        )
    }
}

@Composable
private fun BibleBookAndChapterPicker(
    selectedBook: String,
    expandedBook: String?,
    selectedVersion: String,
    onBookClick: (String) -> Unit,
    onChapterClick: (String, Int) -> Unit,
    onDeepSearchResultClick: (String, Int, Int) -> Unit,
    onOpenJourney: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleBooks = remember(searchQuery) { searchBibleBooks(searchQuery) }
    val bestMatch = visibleBooks.firstOrNull()

    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Column {
                onOpenJourney?.let { openJourney ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clickable(onClick = openJourney),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Jornada Bíblica",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Quiz com 4 alternativas, dicas, missões Fácil/Médio/Difícil, XP e Emblemas do Perfil.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    "Toque em um livro e escolha o capítulo. A leitura começa imediatamente e você pode seguir para o próximo capítulo sem voltar à lista.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    singleLine = true,
                    label = { Text("Pesquisar livro") },
                    placeholder = { Text("Ex.: genesis, joao, apocalpse") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar livro")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa")
                            }
                        }
                    }
                )

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = if (bestMatch != null) {
                            "Entendi: $bestMatch"
                        } else {
                            "Não encontrei um livro parecido. Tente outro nome."
                        },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bestMatch != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = if (bestMatch != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        items(visibleBooks, key = { it }) { book ->
            val isExpanded = expandedBook == book
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBookClick(book) },
                color = if (isExpanded) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                } else {
                    Color.Transparent
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            book,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedBook == book) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Recolher capítulos" else "Mostrar capítulos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.size(10.dp))
                        BibleChapterStrip(
                            totalChapters = chapterCounts[book] ?: 1,
                            onChapterClick = { chapter -> onChapterClick(book, chapter) }
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            )
        }

        item(key = "deep_search") {
            BibleDeepSearchSection(
                versionCode = selectedVersion,
                onResultClick = onDeepSearchResultClick
            )
        }
    }
}

@Composable
private fun BibleChapterStrip(
    totalChapters: Int,
    onChapterClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items((1..totalChapters).toList(), key = { it }) { chapter ->
            Surface(
                modifier = Modifier
                    .size(width = 52.dp, height = 44.dp)
                    .clickable { onChapterClick(chapter) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(chapter.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Leitura do capítulo com posição correta, ações discretas e continuidade entre capítulos. */
@Composable
private fun ContinuousBibleChapterReader(
    book: String,
    chapter: Int,
    version: String,
    verses: List<BibleVerse>,
    focusedVerse: Int,
    isLoading: Boolean,
    errorMessage: String?,
    previousReference: BibleChapterReference?,
    nextReference: BibleChapterReference?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onChooseAnotherReference: () -> Unit,
    onCompareVerse: ((String, Int, Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val readingSettings = currentSettingsState.value
    val readingFontFamily = when (readingSettings.readingFont) {
        ReadingFont.SERIF -> FontFamily.Serif
        ReadingFont.INTER -> FontFamily.Default
        ReadingFont.OPEN_SANS, ReadingFont.ROBOTO -> FontFamily.SansSerif
    }
    var currentBookmark by remember { mutableStateOf(BibleReadingPreferences.getBookmark(context)) }
    var actionsVerse by remember { mutableStateOf<Int?>(null) }
    var transientFocusedVerse by remember(focusedVerse) { mutableStateOf<Int?>(focusedVerse) }
    var showVerseDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(readingSettings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (window != null) {
            if (readingSettings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(Unit) {
        BibleReadingPreferences.loadLocalFavoritesIntoState(context)
    }

    LaunchedEffect(verses, focusedVerse) {
        val targetIndex = verses.indexOfFirst { it.verse == focusedVerse }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex + 1)
            transientFocusedVerse = focusedVerse
            delay(1300)
            transientFocusedVerse = null
        }
    }

    LaunchedEffect(readingSettings.autoScroll, verses.size) {
        if (!readingSettings.autoScroll || verses.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(7000)
            val currentIndex = listState.firstVisibleItemIndex
            val lastReaderIndex = verses.size
            val nextIndex = (currentIndex + 1).coerceAtMost(lastReaderIndex)
            if (nextIndex == currentIndex) break
            listState.animateScrollToItem(nextIndex)
        }
    }

    LaunchedEffect(verses, book, chapter, version, readingSettings.autoSavePosition) {
        if (verses.isNotEmpty() && readingSettings.autoSavePosition) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { readerIndex ->
                    val verseIndex = (readerIndex - 1).coerceAtLeast(0)
                    verses.getOrNull(verseIndex)?.let { visibleVerse ->
                        BibleReadingPreferences.saveLastReading(
                            context,
                            BibleReadingPreferences.ReadingPosition(
                                book = book,
                                chapter = chapter,
                                verse = visibleVerse.verse,
                                version = version
                            )
                        )
                    }
                }
        }
    }

    when {
        isLoading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            RhemaLoadingIndicator(message = "Carregando a Bíblia…")
        }

        errorMessage != null -> Column(
            modifier = modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("Tentar novamente")
            }
            TextButton(onClick = onChooseAnotherReference) {
                Text("Escolher outro livro")
            }
        }

        verses.isEmpty() -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nenhum versículo disponível neste capítulo.")
        }

        else -> LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "reader_header") {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$book $chapter • $version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showVerseDialog = true }) {
                            Text("Ir para versículo")
                        }
                    }
                    Text(
                        "Toque em um versículo para mostrar marcação, favorito e marcador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(verses, key = { "verse_${it.verse}" }) { verseItem ->
                ContinuousBibleVerseRow(
                    verseItem = verseItem,
                    book = book,
                    chapter = chapter,
                    version = version,
                    isFocused = verseItem.verse == transientFocusedVerse,
                    showActions = !readingSettings.readingModeEnabled && actionsVerse == verseItem.verse,
                    readingFontFamily = readingFontFamily,
                    currentBookmark = currentBookmark,
                    onTap = {
                        BibleReadingPreferences.saveLastReading(
                            context,
                            BibleReadingPreferences.ReadingPosition(book, chapter, verseItem.verse, version)
                        )
                        actionsVerse = if (actionsVerse == verseItem.verse) null else verseItem.verse
                    },
                    onBookmarkChanged = { currentBookmark = it },
                    onCompare = onCompareVerse?.let {
                        { it(book, chapter, verseItem.verse) }
                    },
                    onNotify = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                )
            }

            item(key = "reader_navigation") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onPrevious,
                            enabled = previousReference != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(previousReference?.let { "${it.book} ${it.chapter}" } ?: "Anterior")
                        }
                        Button(
                            onClick = onNext,
                            enabled = nextReference != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(nextReference?.let { "${it.book} ${it.chapter}" } ?: "Próximo")
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                    TextButton(
                        onClick = onChooseAnotherReference,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                    ) {
                        Text("Escolher outro livro ou capítulo")
                    }
                }
            }
        }
    }

    if (showVerseDialog) {
        AlertDialog(
            onDismissRequest = { showVerseDialog = false },
            title = { Text("Ir para versículo") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(verses, key = { it.verse }) { verse ->
                        TextButton(
                            onClick = {
                                showVerseDialog = false
                                actionsVerse = null
                                transientFocusedVerse = verse.verse
                                BibleReadingPreferences.saveLastReading(
                                    context,
                                    BibleReadingPreferences.ReadingPosition(book, chapter, verse.verse, version)
                                )
                                coroutineScope.launch {
                                    val index = verses.indexOfFirst { it.verse == verse.verse }
                                    if (index >= 0) listState.animateScrollToItem(index + 1)
                                    delay(1300)
                                    transientFocusedVerse = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${verse.verse}. ${verse.text}",
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVerseDialog = false }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun ContinuousBibleVerseRow(
    verseItem: BibleVerse,
    book: String,
    chapter: Int,
    version: String,
    isFocused: Boolean,
    showActions: Boolean,
    readingFontFamily: FontFamily,
    currentBookmark: BibleReadingPreferences.Bookmark?,
    onTap: () -> Unit,
    onBookmarkChanged: (BibleReadingPreferences.Bookmark?) -> Unit,
    onCompare: (() -> Unit)?,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val verseKey = BibleReadingPreferences.key(book, chapter, verseItem.verse, version)
    var isHighlighted by remember(verseKey) {
        mutableStateOf(BibleReadingPreferences.isHighlighted(context, verseKey))
    }
    var isFavorite by remember(verseKey) {
        mutableStateOf(BibleReadingPreferences.isFavorite(context, verseKey))
    }
    val isBookmark = currentBookmark?.let {
        BibleReadingPreferences.key(it.book, it.chapter, it.verse, it.version) == verseKey
    } == true

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        color = when {
            isFocused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    verseItem.verse.toString(),
                    modifier = Modifier.width(36.dp).padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    verseItem.text,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = readingFontFamily,
                    fontSize = 20.sp,
                    lineHeight = 31.sp
                )
            }

            if (showActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isHighlighted = BibleReadingPreferences.toggleHighlight(context, verseKey)
                        onNotify(if (isHighlighted) "Versículo marcado" else "Marcação removida")
                    }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (isHighlighted) "Remover marcação" else "Marcar versículo",
                            tint = if (isHighlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        val favoriteId = "bible_$verseKey"
                        val favorite = FavoriteItem(
                            id = favoriteId,
                            type = "bible",
                            reference = "$book $chapter:${verseItem.verse} ($version)",
                            text = verseItem.text
                        )
                        isFavorite = !isFavorite
                        BibleReadingPreferences.setFavorite(context, verseKey, isFavorite)
                        if (isFavorite) {
                            addFavorite(favorite)
                            BibleReadingPreferences.saveLocalFavorite(context, favorite)
                            onNotify("Versículo adicionado aos favoritos")
                        } else {
                            removeFavorite(favoriteId)
                            BibleReadingPreferences.removeLocalFavorite(context, favoriteId)
                            onNotify("Versículo removido dos favoritos")
                        }
                    }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remover dos favoritos" else "Favoritar versículo",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        val bookmark = BibleReadingPreferences.Bookmark(book, chapter, verseItem.verse, version)
                        if (isBookmark) {
                            BibleReadingPreferences.clearBookmark(context)
                            onBookmarkChanged(null)
                            onNotify("Marcador removido")
                        } else {
                            BibleReadingPreferences.saveBookmark(context, bookmark)
                            onBookmarkChanged(bookmark)
                            onNotify("Marcador salvo neste versículo")
                        }
                    }) {
                        Icon(
                            if (isBookmark) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmark) "Remover marcador" else "Colocar marcador neste versículo",
                            tint = if (isBookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onCompare != null) {
                        IconButton(onClick = onCompare) {
                            Icon(Icons.Default.CompareArrows, contentDescription = "Comparar este versículo")
                        }
                    }
                }
            }
        }
    }
}