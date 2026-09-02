package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/**
 * Estilo Códice Contemporâneo: seleção inline de livro, capítulo, versículo e leitura.
 * A escolha do versículo não sai mais da aba Bíblia nem troca para outra experiência visual.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BibleScreen(
    initialBook: String? = null,
    initialChapter: Int? = null,
    initialVersion: String? = null,
    onOpenBible: (String, Int, String, Int?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val versions = BollsBibleCatalog.translations
    val initialValidBook = initialBook?.takeIf { chapterCounts.containsKey(it) }
    val normalizedInitialVersion = BollsBibleCatalog.normalize(initialVersion)
    val rememberedPosition = remember { BibleReadingPreferences.getLastReading(context) }

    var selectedBook by rememberSaveable { mutableStateOf(initialValidBook ?: "Gênesis") }
    var expandedBook by rememberSaveable { mutableStateOf<String?>(initialValidBook ?: "Gênesis") }
    var expandedChapter by rememberSaveable { mutableStateOf<Int?>(initialChapter?.takeIf { it > 0 }) }
    var activeReadingVerse by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedVersion by rememberSaveable { mutableStateOf(normalizedInitialVersion) }
    var availableVerses by remember(selectedBook, expandedChapter, selectedVersion) {
        mutableStateOf<List<BibleVerse>>(emptyList())
    }
    var isLoadingVerses by remember(selectedBook, expandedChapter, selectedVersion) { mutableStateOf(false) }
    var verseLoadError by remember(selectedBook, expandedChapter, selectedVersion) { mutableStateOf<String?>(null) }
    var showVersionDialog by rememberSaveable { mutableStateOf(false) }
    var showResumeDialog by rememberSaveable {
        mutableStateOf(initialValidBook == null && rememberedPosition != null)
    }

    LaunchedEffect(initialValidBook, initialChapter, normalizedInitialVersion) {
        if (initialValidBook != null) {
            selectedBook = initialValidBook
            expandedBook = initialValidBook
        }
        if (initialChapter != null && initialChapter > 0) {
            expandedChapter = initialChapter
        }
        selectedVersion = normalizedInitialVersion
    }

    LaunchedEffect(selectedBook, expandedChapter, selectedVersion) {
        val chapter = expandedChapter
        if (chapter == null) {
            availableVerses = emptyList()
            isLoadingVerses = false
            verseLoadError = null
            activeReadingVerse = null
        } else {
            isLoadingVerses = true
            verseLoadError = null
            availableVerses = BollsBibleApi.getChapter(selectedBook, chapter, selectedVersion)
            if (availableVerses.isEmpty()) {
                verseLoadError = "Não foi possível carregar os versículos deste capítulo."
            }
            isLoadingVerses = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (activeReadingVerse != null) {
                            activeReadingVerse = null
                        } else {
                            expandedBook = if (expandedBook == selectedBook) null else selectedBook
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (activeReadingVerse != null) "Voltar aos versículos" else "Recolher ou expandir livros"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (activeReadingVerse != null && expandedChapter != null) "$selectedBook ${expandedChapter!!}" else "Livros",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (activeReadingVerse != null) {
                        Text(
                            "Leitura contínua • versículo ${activeReadingVerse}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { showVersionDialog = true }) {
                    Text(selectedVersion, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Escolher versão")
                }
                IconButton(onClick = { showVersionDialog = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções da Bíblia")
                }
            }
        }
    ) { paddingValues ->
        if (activeReadingVerse != null && expandedChapter != null) {
            ContinuousBibleChapterReader(
                book = selectedBook,
                chapter = expandedChapter!!,
                version = selectedVersion,
                verses = availableVerses,
                focusedVerse = activeReadingVerse!!,
                isLoading = isLoadingVerses,
                errorMessage = verseLoadError,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item {
                    Text(
                        "Toque em um livro, escolha o capítulo e depois o versículo. A leitura continuará aqui, sem sair da aba Bíblia.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(chapterCounts.keys.toList(), key = { it }) { book ->
                    val isSelected = selectedBook == book
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    expandedBook = if (expandedBook == book) null else book
                                    expandedChapter = null
                                } else {
                                    selectedBook = book
                                    expandedBook = book
                                    expandedChapter = null
                                }
                            },
                        color = if (isSelected && expandedBook == book) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    book,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        if (expandedBook == book) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected && expandedBook == book) {
                                Spacer(modifier = Modifier.size(12.dp))
                                BibleChapterGrid(
                                    totalChapters = chapterCounts[book] ?: 1,
                                    selectedChapter = expandedChapter,
                                    onChapterClick = { chapter ->
                                        expandedChapter = if (expandedChapter == chapter) null else chapter
                                        activeReadingVerse = null
                                    }
                                )
                                if (expandedChapter != null) {
                                    Spacer(modifier = Modifier.size(12.dp))
                                    BibleVerseInlinePicker(
                                        chapter = expandedChapter!!,
                                        isLoading = isLoadingVerses,
                                        errorMessage = verseLoadError,
                                        verses = availableVerses,
                                        onVerseClick = { verse ->
                                            activeReadingVerse = verse
                                            BibleReadingPreferences.saveLastReading(
                                                context,
                                                BibleReadingPreferences.ReadingPosition(
                                                    book = book,
                                                    chapter = expandedChapter!!,
                                                    verse = verse,
                                                    version = selectedVersion
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
                    )
                }
            }
        }
    }

    if (showResumeDialog && rememberedPosition != null) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Continuar a leitura?") },
            text = {
                Text(
                    "Você parou em ${rememberedPosition.book} ${rememberedPosition.chapter}:${rememberedPosition.verse}. Deseja continuar de onde parou ou começar novamente em Gênesis 1?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    showResumeDialog = false
                    selectedBook = rememberedPosition.book
                    expandedBook = rememberedPosition.book
                    expandedChapter = rememberedPosition.chapter
                    selectedVersion = rememberedPosition.version
                    activeReadingVerse = rememberedPosition.verse
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    BibleReadingPreferences.clearLastReading(context)
                    selectedBook = "Gênesis"
                    expandedBook = "Gênesis"
                    expandedChapter = null
                    activeReadingVerse = null
                    showResumeDialog = false
                }) { Text("Começar em Gênesis") }
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
                                Text(option.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVersionDialog = false }) { Text("Fechar") } }
        )
    }
}

/**
 * Leitura do capítulo dentro da própria aba Bíblia. Mantém o contexto escolhido
 * e apenas transforma a área de conteúdo em leitura, sem navegar para bible_reader.
 */
@Composable
private fun ContinuousBibleChapterReader(
    book: String,
    chapter: Int,
    version: String,
    verses: List<BibleVerse>,
    focusedVerse: Int,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(verses, focusedVerse) {
        val targetIndex = verses.indexOfFirst { it.verse == focusedVerse }
        if (targetIndex >= 0) listState.scrollToItem(targetIndex)
    }

    when {
        isLoading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            RhemaLoadingIndicator(message = "Carregando a Bíblia…")
        }
        errorMessage != null -> Box(modifier = modifier.padding(28.dp), contentAlignment = Alignment.Center) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        else -> LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "$book $chapter • $version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(verses, key = { it.verse }) { verseItem ->
                val isFocused = verseItem.verse == focusedVerse
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            BibleReadingPreferences.saveLastReading(
                                context,
                                BibleReadingPreferences.ReadingPosition(
                                    book = book,
                                    chapter = chapter,
                                    verse = verseItem.verse,
                                    version = version
                                )
                            )
                        },
                    color = if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            verseItem.verse.toString(),
                            modifier = Modifier.width(40.dp).padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            verseItem.text,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            lineHeight = 31.sp
                        )
                    }
                }
            }
        }
    }
}

/** Estilo Códice Contemporâneo: pequenos capítulos que se expandem no próprio livro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BibleChapterGrid(
    totalChapters: Int,
    selectedChapter: Int?,
    onChapterClick: (Int) -> Unit
) {
    FlowRow(
        maxItemsInEachRow = 5,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..totalChapters).forEach { chapter ->
            val isSelected = chapter == selectedChapter
            Surface(
                modifier = Modifier
                    .size(width = 56.dp, height = 46.dp)
                    .clickable { onChapterClick(chapter) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(chapter.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Estilo Códice Contemporâneo: os versículos aparecem logo abaixo do capítulo escolhido. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BibleVerseInlinePicker(
    chapter: Int,
    isLoading: Boolean,
    errorMessage: String?,
    verses: List<BibleVerse>,
    onVerseClick: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Versículos do capítulo $chapter",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Toque em um versículo para continuar a leitura aqui mesmo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            when {
                isLoading -> Text("Carregando versículos…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                errorMessage != null -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
                else -> FlowRow(
                    maxItemsInEachRow = 7,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    verses.forEach { verse ->
                        OutlinedButton(
                            onClick = { onVerseClick(verse.verse) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(width = 40.dp, height = 38.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(verse.verse.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
