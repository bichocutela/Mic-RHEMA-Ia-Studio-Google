package com.aistudio.micrhema

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BollsBibleScreen(
    book: String?,
    chapter: Int?,
    versionCode: String?,
    verse: Int?,
    onBack: () -> Unit,
    onOpenChapter: (String, Int, String) -> Unit,
    onOpenComparison: (String, Int, Int) -> Unit,
    onOpenReference: (String, Int, Int, String) -> Unit
) {
    val context = LocalContext.current
    val currentBook = book?.takeIf { chapterCounts.containsKey(it) } ?: "Gênesis"
    val currentChapter = chapter?.takeIf { it > 0 } ?: 1
    val currentVersion = BollsBibleCatalog.normalize(versionCode)
    val selectedTargetVerse = verse?.takeIf { it > 0 }
    val version = BollsBibleCatalog.translation(currentVersion)
    val maxChapter = chapterCounts[currentBook] ?: 1
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var verses by remember(currentBook, currentChapter, currentVersion) {
        mutableStateOf<List<BibleVerse>>(emptyList())
    }
    var isLoading by remember(currentBook, currentChapter, currentVersion) { mutableStateOf(true) }
    var errorMessage by remember(currentBook, currentChapter, currentVersion) { mutableStateOf<String?>(null) }
    var reloadKey by remember(currentBook, currentChapter, currentVersion) { mutableIntStateOf(0) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var showBookDialog by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var referenceBook by remember(currentBook) { mutableStateOf(currentBook) }
    var referenceChapter by remember(currentBook, currentChapter) { mutableIntStateOf(currentChapter) }
    var referenceVerse by remember(currentBook, currentChapter, selectedTargetVerse) {
        mutableIntStateOf(selectedTargetVerse ?: 1)
    }
    var referenceVerses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoadingReferenceVerses by remember { mutableStateOf(false) }
    var currentBookmark by remember { mutableStateOf(BibleReadingPreferences.getBookmark(context)) }

    LaunchedEffect(Unit) {
        BibleReadingPreferences.loadLocalFavoritesIntoState(context)
    }

    LaunchedEffect(referenceBook, referenceChapter, currentVersion, verses) {
        isLoadingReferenceVerses = true
        referenceVerses = if (referenceBook == currentBook && referenceChapter == currentChapter) {
            verses
        } else {
            BollsBibleApi.getChapter(referenceBook, referenceChapter, currentVersion)
        }
        val availableVerses = referenceVerses.map { it.verse }
        if (availableVerses.isNotEmpty() && referenceVerse !in availableVerses) {
            referenceVerse = availableVerses.first()
        }
        isLoadingReferenceVerses = false
    }

    LaunchedEffect(currentBook, currentChapter, currentVersion, reloadKey) {
        isLoading = true
        errorMessage = null
        verses = BollsBibleApi.getChapter(currentBook, currentChapter, currentVersion)
        if (verses.isEmpty()) {
            errorMessage = "Não foi possível carregar este capítulo. Verifique sua conexão e tente novamente."
        } else {
            BibleReadingPreferences.saveLastReading(
                context,
                BibleReadingPreferences.ReadingPosition(
                    book = currentBook,
                    chapter = currentChapter,
                    verse = selectedTargetVerse ?: verses.first().verse,
                    version = currentVersion
                )
            )
            BadgeActivityTracker.record(
                context,
                BadgeActivityKeys.BIBLE_CHAPTERS,
                "${currentVersion}:${currentBook}:${currentChapter}"
            )
        }
        isLoading = false
    }

    LaunchedEffect(verses, selectedTargetVerse) {
        val index = selectedTargetVerse?.let { target -> verses.indexOfFirst { it.verse == target } } ?: -1
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LaunchedEffect(verses, currentBook, currentChapter, currentVersion) {
        if (verses.isNotEmpty()) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { index ->
                    verses.getOrNull(index)?.let { visibleVerse ->
                        BibleReadingPreferences.saveLastReading(
                            context,
                            BibleReadingPreferences.ReadingPosition(
                                book = currentBook,
                                chapter = currentChapter,
                                verse = visibleVerse.verse,
                                version = currentVersion
                            )
                        )
                    }
                }
        }
    }

    fun openSelectedReference() {
        showReferenceDialog = false
        onOpenReference(referenceBook, referenceChapter, referenceVerse, currentVersion)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$currentBook $currentChapter", fontWeight = FontWeight.Bold)
                        Text(
                            version.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = { showReferenceDialog = true }) {
                        Text("Escolher", fontWeight = FontWeight.Bold)
                    }
                    currentBookmark?.let { bookmark ->
                        IconButton(onClick = {
                            onOpenReference(bookmark.book, bookmark.chapter, bookmark.verse, bookmark.version)
                        }) {
                            Icon(Icons.Default.Bookmark, contentDescription = "Continuar do marcador")
                        }
                    }
                    IconButton(onClick = { onOpenComparison(currentBook, currentChapter, 1) }) {
                        Icon(Icons.Default.CompareArrows, contentDescription = "Comparar versões")
                    }
                    TextButton(onClick = { showVersionDialog = true }) {
                        Text(version.code, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> RhemaLoadingIndicator(message = "Carregando a Bíblia…")
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { reloadKey++ },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Tentar novamente", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(verses, key = { it.verse }) { verseItem ->
                                BibleVerseCard(
                                    verseItem = verseItem,
                                    book = currentBook,
                                    chapter = currentChapter,
                                    version = currentVersion,
                                    context = context,
                                    currentBookmark = currentBookmark,
                                    onBookmarkChanged = { currentBookmark = it },
                                    onNotify = { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentChapter > 1) {
                                        onOpenChapter(currentBook, currentChapter - 1, currentVersion)
                                    }
                                },
                                enabled = currentChapter > 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Text("Anterior", modifier = Modifier.padding(start = 6.dp))
                            }
                            Text(
                                "$currentChapter de $maxChapter",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    if (currentChapter < maxChapter) {
                                        onOpenChapter(currentBook, currentChapter + 1, currentVersion)
                                    }
                                },
                                enabled = currentChapter < maxChapter
                            ) {
                                Text("Próximo", modifier = Modifier.padding(end = 6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReferenceDialog) {
        AlertDialog(
            onDismissRequest = { showReferenceDialog = false },
            title = { Text("Escolher referência") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Escolha o livro, capítulo e versículo que deseja ler.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { showBookDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(referenceBook, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedButton(onClick = { showChapterDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Capítulo $referenceChapter", modifier = Modifier.fillMaxWidth())
                    }
                    Text("Versículo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (isLoadingReferenceVerses) {
                        Text("Carregando versículos disponíveis…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (referenceVerses.isEmpty()) {
                        Text("Não foi possível carregar os versículos deste capítulo.", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.heightIn(max = 220.dp),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(referenceVerses, key = { it.verse }) { verseOption ->
                                OutlinedButton(
                                    onClick = { referenceVerse = verseOption.verse },
                                    contentPadding = PaddingValues(0.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (referenceVerse == verseOption.verse) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (referenceVerse == verseOption.verse) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(verseOption.verse.toString(), fontWeight = if (referenceVerse == verseOption.verse) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { openSelectedReference() }) { Text("Abrir referência") }
            },
            dismissButton = {
                TextButton(onClick = { showReferenceDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showBookDialog) {
        AlertDialog(
            onDismissRequest = { showBookDialog = false },
            title = { Text("Escolher livro") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(chapterCounts.keys.toList()) { bookOption ->
                        TextButton(
                            onClick = {
                                referenceBook = bookOption
                                referenceChapter = 1
                                referenceVerse = 1
                                showBookDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(bookOption, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showChapterDialog) {
        AlertDialog(
            onDismissRequest = { showChapterDialog = false },
            title = { Text("Escolher capítulo de $referenceBook") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((1..(chapterCounts[referenceBook] ?: 1)).toList()) { chapterOption ->
                        OutlinedButton(
                            onClick = {
                                referenceChapter = chapterOption
                                referenceVerse = 1
                                showChapterDialog = false
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(chapterOption.toString())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Versão da Bíblia") },
            text = {
                Column {
                    BollsBibleCatalog.translations.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option.code == currentVersion,
                                onClick = {
                                    showVersionDialog = false
                                    onOpenReference(currentBook, currentChapter, selectedTargetVerse ?: 1, option.code)
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(option.code, fontWeight = FontWeight.Bold)
                                Text(option.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun BibleVerseCard(
    verseItem: BibleVerse,
    book: String,
    chapter: Int,
    version: String,
    context: Context,
    currentBookmark: BibleReadingPreferences.Bookmark?,
    onBookmarkChanged: (BibleReadingPreferences.Bookmark?) -> Unit,
    onNotify: (String) -> Unit
) {
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
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    verseItem.verse.toString(),
                    modifier = Modifier.width(32.dp).padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    verseItem.text,
                    modifier = Modifier.weight(1f).padding(top = 3.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 27.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
            }
        }
    }
}
