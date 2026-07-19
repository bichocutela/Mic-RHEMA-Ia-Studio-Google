package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ContentScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Livros", "Áudios", "Vídeos")
    
    var selectedBook by remember { mutableStateOf<ContentBook?>(null) }
    var selectedAudio by remember { mutableStateOf<ContentAudio?>(null) }
    var selectedVideo by remember { mutableStateOf<ContentVideo?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedBook == null && selectedVideo == null && selectedAudio == null) {
            GlassTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por título ou descrição...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (selectedBook == null && selectedVideo == null && recentlyViewedState.isNotEmpty() && searchQuery.isEmpty()) {
            Text(
                "Vistos Recentemente", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentlyViewedState) { item ->
                    Card(
                        modifier = Modifier.width(140.dp).clickable {
                            when (item.type) {
                                ContentType.BOOK -> {
                                    selectedTab = 0
                                    selectedBook = contentBooksState.find { it.id == item.id }
                                }
                                ContentType.AUDIO -> {
                                    selectedTab = 1
                                    selectedAudio = contentAudiosState.find { it.id == item.id }
                                }
                                ContentType.VIDEO -> {
                                    selectedTab = 2
                                    selectedVideo = contentVideosState.find { it.id == item.id }
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Box {
                                coil.compose.AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(if(item.type == ContentType.VIDEO) 16f/9f else 1f)
                                )
                                if (item.isCached) {
                                    Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = "Baixado", 
                                        tint = MaterialTheme.colorScheme.primary, 
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                    )
                                }
                                if (item.progress > 0f) {
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedBook == null && selectedVideo == null) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> BooksList(selectedBook, searchQuery, onBookSelected = { 
                    selectedBook = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.author, it.coverUrl, ContentType.BOOK, it.isCached, it.progress))
                    }
                })
                1 -> AudiosList(selectedAudio, searchQuery, onAudioSelected = {
                    selectedAudio = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.artist, it.coverUrl, ContentType.AUDIO, it.isCached, it.progress))
                    }
                })
                2 -> VideosList(selectedVideo, searchQuery, onVideoSelected = {
                    selectedVideo = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.description, it.thumbnailUrl, ContentType.VIDEO, it.isCached, it.progress))
                    }
                })
            }
        }
    }
}

@Composable
fun BooksList(selectedBook: ContentBook?, searchQuery: String, onBookSelected: (ContentBook?) -> Unit) {
    
    val filteredBooks = remember(searchQuery, contentBooksState.toList()) {
        if (searchQuery.isBlank()) {
            contentBooksState
        } else {
            contentBooksState.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (selectedBook != null) {
        BookReader(book = selectedBook!!, onBack = { onBookSelected(null) })
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredBooks) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onBookSelected(book) },
                    
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = "Capa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp, 120.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, style = MaterialTheme.typography.titleLarge)
                            Text(book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (book.isCached) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Baixado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookReader(book: ContentBook, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text("Lendo: ${book.title}", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    text = book.contentText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun AudiosList(selectedAudio: ContentAudio?, searchQuery: String, onAudioSelected: (ContentAudio?) -> Unit) {
    
    val filteredAudios = remember(searchQuery, contentAudiosState.toList()) {
        if (searchQuery.isBlank()) {
            contentAudiosState
        } else {
            contentAudiosState.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    LaunchedEffect(selectedAudio) {
        if (selectedAudio != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(selectedAudio!!.audioUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredAudios) { audio ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onAudioSelected(audio) },
                    
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAudio == audio) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = audio.coverUrl,
                                contentDescription = "Capa",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(audio.title, style = MaterialTheme.typography.titleMedium)
                                Text(audio.artist, style = MaterialTheme.typography.bodySmall)
                            }
                            if (audio.isCached) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Baixado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp).size(24.dp))
                            }
                            if (selectedAudio == audio) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (audio.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { audio.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
        
        // Mini Player
        if (selectedAudio != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedAudio!!.coverUrl,
                        contentDescription = "Capa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedAudio!!.title, style = MaterialTheme.typography.titleMedium)
                        Text(selectedAudio!!.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { 
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                    }
                    IconButton(onClick = { onAudioSelected(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun VideosList(selectedVideo: ContentVideo?, searchQuery: String, onVideoSelected: (ContentVideo?) -> Unit) {
    
    val filteredVideos = remember(searchQuery, contentVideosState.toList()) {
        if (searchQuery.isBlank()) {
            contentVideosState
        } else {
            contentVideosState.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    LaunchedEffect(selectedVideo) {
        if (selectedVideo != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(selectedVideo!!.videoUrl))
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
        }
    }

    if (selectedVideo != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onVideoSelected(null) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Text(selectedVideo!!.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)
            )
            Text(
                text = selectedVideo!!.description,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredVideos) { video ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onVideoSelected(video) },
                    
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)
                        )
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(video.title, style = MaterialTheme.typography.titleLarge)
                                Text(video.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (video.isCached) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Baixado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        if (video.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { video.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
