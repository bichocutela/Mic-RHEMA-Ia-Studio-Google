package com.aistudio.micrhema

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private fun decoratedBibleNews(): List<BibleNews> = currentResolvedBibleNews()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(onNavigateToDetail: (Int) -> Unit, onBack: () -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("Tudo") }
    var visibleCount by rememberSaveable { mutableIntStateOf(20) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val allNews = decoratedBibleNews()
    val availableFilters = BibleNewsEditorial.categories.filter { filter ->
        filter == "Tudo" || filter == "Mais recentes" || allNews.any { BibleNewsEditorial.matches(it, "", filter) }
    }
    val filteredNews = allNews
        .filter { BibleNewsEditorial.matches(it, query, selectedFilter) }
        .sortedWith(
            if (selectedFilter == "Mais recentes") {
                compareByDescending<BibleNews> { it.publishedAt }
            } else {
                compareByDescending<BibleNews> { it.featured }
                    .thenByDescending { it.publishedAt }
            }
        )
    val visibleNews = filteredNews.take(visibleCount)

    LaunchedEffect(query, selectedFilter) {
        visibleCount = 20
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = 80.dp),
        topBar = {
            TopAppBar(
                title = { Text("Notícias Bíblicas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    try {
                        BibleNewsPagination.refresh()
                    } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Encontre uma história bíblica para este momento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pesquise por personagem, livro, tema ou escolha a intensidade narrativa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Buscar notícias") },
                        placeholder = { Text("Ex.: Davi, coragem, Provérbios…") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(availableFilters, key = { it }) { filter ->
                            val count = if (filter == "Tudo" || filter == "Mais recentes") {
                                allNews.size
                            } else {
                                allNews.count { BibleNewsEditorial.matches(it, "", filter) }
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("$filter ($count)") }
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = if (filteredNews.size == 1) "1 notícia encontrada" else "${filteredNews.size} notícias encontradas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (visibleNews.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Nenhuma notícia encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tente outra busca ou escolha outra categoria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (query.isNotBlank() || selectedFilter != "Tudo") {
                                TextButton(onClick = {
                                    query = ""
                                    selectedFilter = "Tudo"
                                }) {
                                    Text("Limpar filtros")
                                }
                            }
                        }
                    }
                } else {
                    items(visibleNews, key = { it.id }) { news ->
                        NewsCard(news = news, onClick = { onNavigateToDetail(news.id) })
                    }
                    if (visibleCount < filteredNews.size || BibleNewsPagination.hasMore) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    if (visibleCount < filteredNews.size) {
                                        visibleCount += 20
                                    } else if (!isLoadingMore) {
                                        coroutineScope.launch {
                                            isLoadingMore = true
                                            try {
                                                BibleNewsPagination.loadNextPage()
                                                visibleCount += 20
                                            } finally {
                                                isLoadingMore = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoadingMore,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isLoadingMore) "Carregando…" else "Carregar mais notícias")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BibleNewsImage(
    news: BibleNews,
    modifier: Modifier,
    contentDescription: String?
) {
    val resolvedUrl = remember(news.id, news.imageUrl) { BibleNewsVisuals.imageUrlFor(news) }
    var imageFailed by remember(news.id, resolvedUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!imageFailed) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = BibleNewsVisuals.monochromeFilter,
                onError = { imageFailed = true }
            )
        }
        if (imageFailed) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(38.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${news.book} • ${news.category}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NewsMetaLabel(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewsCard(news: BibleNews, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            BibleNewsImage(
                news = news,
                contentDescription = news.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NewsMetaLabel(news.category)
                    NewsMetaLabel(BibleNewsEditorial.intensityLabel(news.intensity))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = news.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = news.summary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${news.book} ${news.chapter}:${news.verse}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (news.contentWarning.isNotBlank()) {
                        Text(
                            news.contentWarning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    newsId: Int,
    onBack: () -> Unit,
    onNavigateToBible: (String, Int, String?) -> Unit
) {
    val news = decoratedBibleNews().find { it.id == newsId } ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(news.id) {
        BadgeActivityTracker.record(context, BadgeActivityKeys.BIBLE_NEWS, news.id.toString())
    }

    Scaffold(
        modifier = Modifier.padding(bottom = 80.dp),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            BibleNewsImage(
                news = news,
                contentDescription = news.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewsMetaLabel(news.category)
                    NewsMetaLabel(BibleNewsEditorial.intensityLabel(news.intensity))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${news.book} ${news.chapter}:${news.verse} (NTLH)",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (news.contentWarning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Aviso: ${news.contentWarning}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Ir Para a História",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable {
                            BibleNewsPendingNavigation.remember(news.book, news.chapter, news.verse)
                            onNavigateToBible(news.book, news.chapter, "NTLH")
                        }
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
