package com.aistudio.micrhema
import androidx.compose.ui.draw.blur

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.activity.compose.BackHandler
import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    val tabs = listOf("Livros", "Áudios", "Vídeos", "Fotos")
    
    var selectedBook by remember { mutableStateOf<ContentBook?>(null) }
    var selectedAudio by remember { mutableStateOf<ContentAudio?>(null) }
    var selectedVideo by remember { mutableStateOf<ContentVideo?>(null) }
    var selectedAlbum by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLocalLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1200); isLocalLoading = false }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedBook == null && selectedVideo == null && selectedAudio == null && selectedAlbum == null) {
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

        if (selectedBook == null && selectedVideo == null && selectedAlbum == null) {
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
                0 -> BooksList(selectedBook, searchQuery, isLocalLoading, onBookSelected = { 
                    selectedBook = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.author, it.coverUrl, ContentType.BOOK, it.isCached, it.progress))
                    }
                })
                1 -> AudiosList(selectedAudio, searchQuery, isLocalLoading, onAudioSelected = {
                    selectedAudio = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.artist, it.coverUrl, ContentType.AUDIO, it.isCached, it.progress))
                    }
                })
                2 -> VideosList(selectedVideo, searchQuery, isLocalLoading, onVideoSelected = {
                    selectedVideo = it
                    if (it != null) {
                        addRecentlyViewed(RecentlyViewedItem(it.id, it.title, it.description, it.thumbnailUrl, ContentType.VIDEO, it.isCached, it.progress))
                    }
                })
                3 -> AlbumsList(selectedAlbum, searchQuery, isLocalLoading, onAlbumSelected = {
                    selectedAlbum = it
                })
            }
        }
    }
}

@Composable
fun BooksList(selectedBook: ContentBook?, searchQuery: String, isLocalLoading: Boolean, onBookSelected: (ContentBook?) -> Unit) {
    
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
    } else if (isLocalLoading) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(80.dp, 120.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonItem(width = 200.dp, height = 24.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonItem(width = 150.dp, height = 16.dp)
                        }
                    }
                }
            }
        }
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
fun AudiosList(selectedAudio: ContentAudio?, searchQuery: String, isLocalLoading: Boolean, onAudioSelected: (ContentAudio?) -> Unit) {
    
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
        ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(com.aistudio.micrhema.ExoPlayerCache.getCacheDataSourceFactory(context))).build()
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
            if (selectedAudio!!.lastPosition > 0L) {
                exoPlayer.seekTo(selectedAudio!!.lastPosition)
            }
            exoPlayer.play()
        } else {
            exoPlayer.stop()
        }
    }

    LaunchedEffect(selectedAudio) {
        if (selectedAudio != null) {
            while(true) {
                kotlinx.coroutines.delay(1000)
                if (exoPlayer.isPlaying) {
                    selectedAudio!!.lastPosition = exoPlayer.currentPosition
                }
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        if (isLocalLoading) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(4) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                SkeletonItem(width = 180.dp, height = 20.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                SkeletonItem(width = 120.dp, height = 14.dp)
                            }
                        }
                    }
                }
            }
        } else {
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
fun VideosList(selectedVideo: ContentVideo?, searchQuery: String, isLocalLoading: Boolean, onVideoSelected: (ContentVideo?) -> Unit) {
    
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
        ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(com.aistudio.micrhema.ExoPlayerCache.getCacheDataSourceFactory(context))).build()
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
            var isVideoBuffering by remember { mutableStateOf(true) }
            DisposableEffect(exoPlayer) {
                val listener = object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isVideoBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING || playbackState == androidx.media3.common.Player.STATE_IDLE
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isVideoBuffering) {
                    Box(modifier = Modifier.fillMaxSize().background(shimmerBrush()))
                }

                
                // Custom Video Overlay
                val authorizedUser = loggedInMemberState.value?.let { it.isApproved || it.isIbr || it.isVip } ?: false
                if (authorizedUser) {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            DownloadHelper.downloadFile(
                                context = context,
                                url = selectedVideo!!.videoUrl,
                                title = selectedVideo!!.title,
                                fileName = "micrhema_video_${selectedVideo!!.id}.mp4"
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Baixar Vídeo (Offline)",
                            tint = Color.White
                        )
                    }
                }
            }
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


@Composable
fun AlbumsList(
    selectedAlbum: ContentPhotoAlbum?,
    searchQuery: String,
    isLocalLoading: Boolean,
    onAlbumSelected: (ContentPhotoAlbum?) -> Unit
) {
    if (selectedAlbum != null) {
        AlbumDetail(album = selectedAlbum, onBack = { onAlbumSelected(null) })
    } else {
        val matchedAlbums = contentAlbumsState.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
        }

        if (isLocalLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (matchedAlbums.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum álbum encontrado.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(matchedAlbums) { album ->
                    AlbumCard(album = album, onClick = { onAlbumSelected(album) })
                }
            }
        }
    }
}

@Composable
fun AlbumCard(album: ContentPhotoAlbum, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (album.coverUrl != null) {
                coil.compose.AsyncImage(
                    model = album.coverUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoAlbum, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(album.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(album.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${album.photos.size} foto(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AlbumDetail(album: ContentPhotoAlbum, onBack: () -> Unit) {
    var initialPhotoIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        if (initialPhotoIndex != null) {
            initialPhotoIndex = null
        } else {
            onBack()
        }
    }

    if (initialPhotoIndex != null) {
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = initialPhotoIndex!!,
            pageCount = { album.photos.size }
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    model = album.photos[page].url,
                    onClose = { initialPhotoIndex = null }
                )
            }
            
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (album.photos[pagerState.currentPage].caption.isNotBlank()) {
                    Text(
                        text = album.photos[pagerState.currentPage].caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${album.photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            IconButton(
                onClick = { initialPhotoIndex = null },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Text("Álbum", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(album.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(album.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val chunks = album.photos.chunked(2)
                itemsIndexed(chunks) { rowIndex, rowPhotos ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowPhotos.forEachIndexed { colIndex, photo ->
                            val globalIndex = rowIndex * 2 + colIndex
                            Column(modifier = Modifier.weight(1f)) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                        .data(getThumbnailUrl(photo.url))
                                        .crossfade(true)
                                        .build(),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize().background(shimmerBrush()))
                                    },
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { initialPhotoIndex = globalIndex }
                                )
                                if (photo.caption.isNotBlank()) {
                                    Text(
                                        text = photo.caption,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowPhotos.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


@Composable
fun ZoomableImage(model: Any?, onClose: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    coil.compose.SubcomposeAsyncImage(
        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(model)
            .crossfade(true)
            .build(),
        loading = {
            coil.compose.AsyncImage(
                model = getThumbnailUrl(model.toString()),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().blur(8.dp)
            )
        },
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset = androidx.compose.ui.geometry.Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y
                        )
                    } else {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClose() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                )
            }
    )
}


fun getThumbnailUrl(url: String): String {
    return if (url.contains("unsplash.com")) {
        val withoutWidth = url.replace(Regex("&w=\\d+"), "").replace(Regex("\\?w=\\d+"), "?")
        if (withoutWidth.contains("?")) {
            "$withoutWidth&w=300"
        } else {
            "$withoutWidth?w=300"
        }
    } else {
        url
    }
}
