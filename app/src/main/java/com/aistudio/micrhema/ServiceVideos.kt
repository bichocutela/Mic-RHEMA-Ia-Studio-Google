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
    var id: String = "",
    var title: String = "",
    var date: String = "",
    var videoUrl: String = "",
    var thumbnailUrl: String = ""
)

@Composable
fun ServiceVideosGallery() {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<ServiceVideoModel?>(null) }

    LaunchedEffect(Unit) {
        try {
            if (isOfflineModeState.value) return@LaunchedEffect
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return@LaunchedEffect
            val db = FirebaseFirestore.getInstance()
            val result = db.collection("service_videos").get().await()
            val fetchedVideos = result.documents.mapNotNull { doc ->
                doc.toObject(ServiceVideoModel::class.java)?.copy(id = doc.id)
            }
            if (fetchedVideos.isNotEmpty()) {
                fetchedVideos.forEach { fetched ->
                    if (serviceVideosState.none { it.id == fetched.id }) {
                        serviceVideosState.add(fetched)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val videos = serviceVideosState.toList()

    if (selectedVideo != null) {
        // Video Player Modal or Full Section
        Column(modifier = Modifier.fillMaxWidth()) {
            val authorizedUser = loggedInMemberState.value?.let { it.isApproved || it.isIbr || it.isVip } ?: false
            
            CleanVideoPlayer(
                videoUrl = selectedVideo!!.videoUrl,
                title = selectedVideo!!.title,
                onClose = { selectedVideo = null },
                canDownload = authorizedUser,
                onDownload = {
                    DownloadHelper.downloadFile(
                        context = context,
                        url = selectedVideo!!.videoUrl,
                        title = selectedVideo!!.title,
                        fileName = "micrhema_culto_${selectedVideo!!.id}.mp4"
                    )
                }
            )
            
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
                        ShimmerSkeletonItem(width = 200.dp, height = 120.dp, shape = RoundedCornerShape(16.dp))
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
                                .width(230.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column {
                                Box {
                                    AsyncImage(
                                        model = video.thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80" },
                                        contentDescription = video.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clickable {
                                                if (isYoutubeUrl(video.videoUrl)) {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(video.videoUrl))
                                                        context.startActivity(intent)
                                                    } catch(e: Exception) {
                                                        android.widget.Toast.makeText(context, "Erro ao abrir", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    selectedVideo = video 
                                                }
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Áudio", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(video.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(video.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { 
                                                if (isYoutubeUrl(video.videoUrl)) {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(video.videoUrl))
                                                        context.startActivity(intent)
                                                    } catch(e: Exception) {
                                                        android.widget.Toast.makeText(context, "Erro ao abrir", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    selectedVideo = video 
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Vídeo", style = MaterialTheme.typography.labelMedium)
                                        }
                                        
                                        OutlinedButton(
                                            onClick = {
                                                GlobalAudioPlayer.playTrack(
                                                    context = context,
                                                    track = AudioTrack(
                                                        id = video.id,
                                                        title = video.title,
                                                        subtitle = video.date,
                                                        audioUrl = video.videoUrl,
                                                        coverUrl = video.thumbnailUrl,
                                                        category = "Culto Gravado"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Áudio", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
