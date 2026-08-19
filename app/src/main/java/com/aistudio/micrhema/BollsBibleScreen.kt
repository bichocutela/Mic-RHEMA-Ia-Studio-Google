package com.aistudio.micrhema

import android.content.Context
import android.widget.Toast
import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Leitor editorial inspirado na prévia aprovada: leitura ampla, ações por versículo
 * e navegação de capítulo em uma faixa inferior discreta.
 */
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
    val readingSettings = currentSettingsState.value
    val readingFontFamily = when (readingSettings.readingFont) {
        ReadingFont.SERIF -> FontFamily.Serif
        ReadingFont.INTER -> FontFamily.Default
        ReadingFont.OPEN_SANS, ReadingFont.ROBOTO -> FontFamily.SansSerif
    }
    val selectedTargetVerse = verse?.takeIf { it > 0 }
    val version = BollsBibleCatalog.translation(currentVersion)
    val maxChapter = chapterCounts[currentBook] ?: 1
    val listState = rememberLazyListState()

    var verses by remember(currentBook, currentChapter, currentVersion) { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoading by remember(currentBook, currentChapter, currentVersion) { mutableStateOf(true) }
    var errorMessage by remember(currentBook, currentChapter, currentVersion) { mutableStateOf<String?>(null) }
    var reloadKey by remember(currentBook, currentChapter, currentVersion) { mutableIntStateOf(0) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var showBookDialog by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var referenceBook by remember(currentBook) { mutableStateOf(currentBook) }
    var referenceChapter by remember(currentBook, currentChapter) { mutableIntStateOf(currentChapter) }
    var referenceVerse by remember(currentBook, currentChapter, selectedTargetVerse) { mutableIntStateOf(selectedTargetVerse ?: 1) }
    var referenceVerses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoadingReferenceVerses by remember { mutableStateOf(false) }
    var currentBookmark by remember { mutableStateOf(BibleReadingPreferences.getBookmark(context)) }
    DisposableEffect(readingSettings.keepScreenOn, readingSettings.internalBrightness) {
        val activity = context as? Activity
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
        if (window != null) {
            if (readingSettings.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                screenBrightness = readingSettings.internalBrightness.coerceIn(0.05f, 1f)
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null && previousBrightness != null) {
                window.attributes = window.attributes.apply { screenBrightness = previousBrightness }
            }
        }
    }

    LaunchedEffect(readingSettings.autoScroll, verses.size) {
        if (!readingSettings.autoScroll || verses.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(7000)
            val nextIndex = (listState.firstVisibleItemIndex + 1).coerceAtMost(verses.lastIndex)
            if (nextIndex == listState.firstVisibleItemIndex) break
            listState.animateScrollToItem(nextIndex)
        }
    }
    var focusedVerse by remember(currentBook, currentChapter, currentVersion, selectedTargetVerse) {
        mutableStateOf(selectedTargetVerse)
    }

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
            if (readingSettings.autoSavePosition) {
                BibleReadingPreferences.saveLastReading(
                    context,
                    BibleReadingPreferences.ReadingPosition(
                        book = currentBook,
                        chapter = currentChapter,
                        verse = selectedTargetVerse ?: verses.first().verse,
                        version = currentVersion
                    )
                )
            }
            BadgeActivityTracker.record(
                context,
                BadgeActivityKeys.BIBLE_CHAPTERS,
                "$currentVersion:$currentBook:$currentChapter"
            )
        }
        isLoading = false
    }

    LaunchedEffect(verses, selectedTargetVerse) {
        val index = selectedTargetVerse?.let { target -> verses.indexOfFirst { it.verse == target } } ?: -1
        if (index >= 0) {
            listState.animateScrollToItem(index)
            focusedVerse = selectedTargetVerse
            delay(1300)
            focusedVerse = null
        }
    }

    LaunchedEffect(verses, currentBook, currentChapter, currentVersion, readingSettings.autoSavePosition) {
        if (verses.isNotEmpty() && readingSettings.autoSavePosition) {
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "$currentBook $currentChapter",
                                fontSize = 25.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                version.code,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showReferenceDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Escolher", fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onOpenComparison(currentBook, currentChapter, selectedTargetVerse ?: 1) }) {
                            Icon(Icons.Default.CompareArrows, contentDescription = "Comparar versões")
                        }
                        TextButton(
                            onClick = { showVersionDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(version.code, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
                }
            }
        },
        bottomBar = {
            if (!isLoading && errorMessage == null) {
                BibleChapterNavigator(
                    currentChapter = currentChapter,
                    maxChapter = maxChapter,
                    onPrevious = {
                        if (currentChapter > 1) onOpenChapter(currentBook, currentChapter - 1, currentVersion)
                    },
                    onNext = {
                        if (currentChapter < maxChapter) onOpenChapter(currentBook, currentChapter + 1, currentVersion)
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { reloadKey++ }, modifier = Modifier.padding(top = 20.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Tentar novamente", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 28.dp, end = 22.dp, top = 20.dp, bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(verses, key = { it.verse }) { verseItem ->
                            BibleVerseReadingRow(
                                verseItem = verseItem,
                                book = currentBook,
                                chapter = currentChapter,
                                version = currentVersion,
                                context = context,
                                isFocused = focusedVerse == verseItem.verse,
                                readingFontFamily = readingFontFamily,
                                readingModeEnabled = readingSettings.readingModeEnabled,
                                currentBookmark = currentBookmark,
                                onBookmarkChanged = { currentBookmark = it },
                                onNotify = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                            )
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
                    currentBookmark?.let { bookmark ->
                        TextButton(
                            onClick = {
                                showReferenceDialog = false
                                onOpenReference(bookmark.book, bookmark.chapter, bookmark.verse, bookmark.version)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null)
                            Text(" Abrir marcador salvo", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
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
                                    colors = ButtonDefaults.outlinedButtonColors(
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
            confirmButton = { Button(onClick = { openSelectedReference() }) { Text("Abrir referência") } },
            dismissButton = { TextButton(onClick = { showReferenceDialog = false }) { Text("Cancelar") } }
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
                        ) { Text(bookOption, modifier = Modifier.fillMaxWidth()) }
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
                        ) { Text(chapterOption.toString()) }
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
                LazyColumn(modifier = Modifier.heightIn(max = 430.dp)) {
                    items(BollsBibleCatalog.translations, key = { it.code }) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showVersionDialog = false
                                    onOpenReference(currentBook, currentChapter, selectedTargetVerse ?: 1, option.code)
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option.code == currentVersion, onClick = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
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

/** Faixa inferior oval, com as mesmas ações diretas da prévia: anterior, progresso e próximo. */
@Composable
private fun BibleChapterNavigator(
    currentChapter: Int,
    maxChapter: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentChapter > 1,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text("Anterior", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.SemiBold)
            }
            Text(
                "$currentChapter de $maxChapter",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onNext,
                enabled = currentChapter < maxChapter,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text("Próximo", modifier = Modifier.padding(end = 7.dp), fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

/** Linha editorial: número azul, texto bíblico amplo e as três ações rápidas da prévia. */
@Composable
private fun BibleVerseReadingRow(
    verseItem: BibleVerse,
    book: String,
    chapter: Int,
    version: String,
    context: Context,
    isFocused: Boolean,
    readingFontFamily: FontFamily,
    readingModeEnabled: Boolean,
    currentBookmark: BibleReadingPreferences.Bookmark?,
    onBookmarkChanged: (BibleReadingPreferences.Bookmark?) -> Unit,
    onNotify: (String) -> Unit
) {
    val verseKey = BibleReadingPreferences.key(book, chapter, verseItem.verse, version)
    var isHighlighted by remember(verseKey) { mutableStateOf(BibleReadingPreferences.isHighlighted(context, verseKey)) }
    var isFavorite by remember(verseKey) { mutableStateOf(BibleReadingPreferences.isFavorite(context, verseKey)) }
    val isBookmark = currentBookmark?.let {
        BibleReadingPreferences.key(it.book, it.chapter, it.verse, it.version) == verseKey
    } == true
    val targetBackground = when {
        isFocused -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.36f)
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        else -> Color.Transparent
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 220),
        label = "verse-focus"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(start = 4.dp, end = 2.dp, top = 8.dp, bottom = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    verseItem.verse.toString(),
                    modifier = Modifier.width(48.dp).padding(top = 7.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    verseItem.text,
                    modifier = Modifier.weight(1f).padding(top = 1.dp),
                    fontFamily = readingFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 34.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (!readingModeEnabled) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 3.dp),
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
