package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalsScreen(initialDevotionalId: String? = null) {
    var selectedDevotional by remember { mutableStateOf<Devotional?>(null) }
    
    androidx.activity.compose.BackHandler(enabled = selectedDevotional != null) {
        selectedDevotional = null
    }

    LaunchedEffect(initialDevotionalId, devotionalsState) {
        if (initialDevotionalId != null) {
            val dev = devotionalsState.find { it.id == initialDevotionalId }
            if (dev != null) {
                selectedDevotional = dev
            }
        }
    }
    
    if (selectedDevotional != null) {
        DevotionalDetailScreen(
            devotional = selectedDevotional!!,
            onBack = { selectedDevotional = null }
        )
    } else {
        var isRefreshing by remember { mutableStateOf(false) }
        var newestFirst by rememberSaveable { mutableStateOf(true) }
        var sortMenuExpanded by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val today = java.time.LocalDate.now()
        val availableDevotionals = remember(devotionalsState.toList(), newestFirst, today) {
            val dated = DevotionalDateUtils.availableUntilToday(devotionalsState.toList(), today)
                .sortedWith(
                    compareBy<Devotional> { DevotionalDateUtils.parse(it.date) ?: java.time.LocalDate.MIN }
                        .thenBy { it.timestamp }
                )
            if (newestFirst) dated.asReversed() else dated
        }
        
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        forceRefreshData()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Devocionais",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        FilledTonalButton(
                            onClick = { sortMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Ordenar devocionais",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (newestFirst) "Recentes" else "Antigos")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mais recente") },
                                onClick = {
                                    newestFirst = true
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mais antigo") },
                                onClick = {
                                    newestFirst = false
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (devotionalsState.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum devocional encontrado.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(availableDevotionals, key = { it.id.ifBlank { "${it.date}:${it.title}" } }) { devotional ->
                            DevotionalCard(devotional = devotional) {
                                selectedDevotional = devotional
                            }
                        }
                    }
                }
            }
            } // end PullToRefreshBox
        }
    }
}

@Composable
fun DevotionalCard(devotional: Devotional, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        Column {
            if (devotional.mediaUrl.isNotBlank()) {
                val isYt = isYoutubeUrl(devotional.mediaUrl)
                val thumb = if (isYt) getYoutubeThumbnailUrl(devotional.mediaUrl) else devotional.mediaUrl
                if (thumb != null) {
                    coil.compose.AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clickable {
                            if (isYt) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(devotional.mediaUrl))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = devotional.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = DevotionalDateUtils.display(devotional.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = devotional.verse,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalDetailScreen(devotional: Devotional, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(devotional.id) {
        BadgeActivityTracker.record(context, BadgeActivityKeys.DEVOTIONALS, devotional.id)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                val isFavorite = favoriteItemsState.any { it.type == "devotional" && it.reference == devotional.title }
                IconButton(onClick = {
                    if (isFavorite) {
                        val fav = favoriteItemsState.find { it.type == "devotional" && it.reference == devotional.title }
                        if (fav != null) {
                            BibleReadingPreferences.removeLocalFavorite(context, fav.id)
                            removeFavorite(fav.id)
                        }
                    } else {
                        val favorite = FavoriteItem(
                            id = "devotional-${devotional.id}",
                            type = "devotional",
                            reference = devotional.title,
                            text = devotional.verse
                        )
                        addFavorite(favorite)
                        BibleReadingPreferences.saveLocalFavorite(context, favorite)
                    }
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (devotional.mediaUrl.isNotBlank()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val isYt = isYoutubeUrl(devotional.mediaUrl)
                    val thumb = if (isYt) getYoutubeThumbnailUrl(devotional.mediaUrl) else devotional.mediaUrl
                    if (thumb != null) {
                        coil.compose.AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(16.dp)).clickable {
                                if (isYt) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(devotional.mediaUrl))
                                    context.startActivity(intent)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                Text(
                    text = devotional.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = DevotionalDateUtils.display(devotional.date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = devotional.verse,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = devotional.verseReference,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = devotional.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
