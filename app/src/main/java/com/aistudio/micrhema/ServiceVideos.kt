package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ServiceVideoModel(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = ""
)

@Composable
fun ServiceVideosGallery() {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<ServiceVideoModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedVideo by remember { mutableStateOf<ServiceVideoModel?>(null) }

    LaunchedEffect(Unit) {
        try {
            if (isOfflineModeState.value) {
                videos = listOf(
                    ServiceVideoModel(
                        id = "mock_1",
                        title = "Culto de Domingo - Família (Offline)",
                        date = "Domingo, 10h",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnailUrl = "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80"
                    )
                )
                isLoading = false
                return@LaunchedEffect
            }
            val db = FirebaseFirestore.getInstance()
            val result = db.collection("service_videos")
                .get()
                .await()
            val fetchedVideos = result.documents.mapNotNull { doc ->
                doc.toObject(ServiceVideoModel::class.java)?.copy(id = doc.id)
            }
            if (fetchedVideos.isEmpty()) {
                videos = listOf(
                    ServiceVideoModel(
                        id = "mock_1",
                        title = "Culto de Domingo - Família",
                        date = "Domingo, 10h",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnailUrl = "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80"
                    ),
                    ServiceVideoModel(
                        id = "mock_2",
                        title = "Culto de Celebração",
                        date = "Domingo, 18h",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        thumbnailUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65?w=500&q=80"
                    )
                )
            } else {
                videos = fetchedVideos
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (selectedVideo != null) {
        // Video Player Modal or Full Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(selectedVideo!!.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedVideo = null }) {
                    Text("Fechar")
                }
            }
            
            val isYouTube = selectedVideo!!.videoUrl.contains("youtube.com") || selectedVideo!!.videoUrl.contains("youtu.be")
            
            if (isYouTube) {
                YoutubePlayer(
                    videoUrl = selectedVideo!!.videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                val exoPlayer = remember(selectedVideo!!.videoUrl) {
                    ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(com.aistudio.micrhema.ExoPlayerCache.getCacheDataSourceFactory(context))).build().apply {
                        setMediaItem(MediaItem.fromUri(selectedVideo!!.videoUrl))
                        prepare()
                        playWhenReady = true
                    }
                }
                
                DisposableEffect(selectedVideo!!.videoUrl) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(16.dp))) {
                    
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Custom Video Overlay
                    val authorizedUser = loggedInMemberState.value?.let { it.isApproved || it.isIbr || it.isVip } ?: false
                    if (authorizedUser) {
                        IconButton(
                            onClick = {
                                DownloadHelper.downloadFile(
                                    context = context,
                                    url = selectedVideo!!.videoUrl,
                                    title = selectedVideo!!.title,
                                    fileName = "micrhema_culto_${selectedVideo!!.id}.mp4"
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar Culto (Offline)",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cultos Gravados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) {
                        SkeletonItem(width = 200.dp, height = 120.dp, shape = RoundedCornerShape(16.dp))
                    }
                }
            } else if (videos.isEmpty()) {
                Text(
                    text = "Nenhum vídeo disponível no momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos) { video ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { selectedVideo = video },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column {
                                AsyncImage(
                                    model = video.thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80" },
                                    contentDescription = video.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(video.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(video.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
