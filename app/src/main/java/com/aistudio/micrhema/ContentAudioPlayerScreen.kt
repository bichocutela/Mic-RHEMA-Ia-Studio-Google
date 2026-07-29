package com.aistudio.micrhema

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentAudioPlayerScreen(
    audio: ContentAudio,
    viewModel: ContentAudioPlayerViewModel = viewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    LaunchedEffect(audio) {
        viewModel.playAudio(audio)
    }

    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPos by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableFloatStateOf(0f) }

    val effectivePos = if (isUserSeeking) sliderPos.toLong() else currentPos
    
    var downloadProgress by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    viewModel.stop()
                    onClose() 
                }
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", modifier = Modifier.size(32.dp))
            }
            Text(
                "Ouvindo Agora",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opções")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cover Image (Large, Spotify style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            AsyncImage(
                model = audio.coverUrl,
                contentDescription = "Capa da Pregação",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title and Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = audio.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Bar
        Slider(
            value = effectivePos.toFloat(),
            onValueChange = {
                isUserSeeking = true
                sliderPos = it
            },
            onValueChangeFinished = {
                isUserSeeking = false
                viewModel.seekTo(sliderPos.toLong())
            },
            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatAudioTime(effectivePos),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = formatAudioTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { viewModel.seekBackward10() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", modifier = Modifier.size(36.dp))
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(80.dp)
                    .clickable { viewModel.togglePlayPause() },
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.seekForward30() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Forward30, contentDescription = "Avançar 30s", modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Actions (Download, Repeat, Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download
            Box(contentAlignment = Alignment.Center) {
                if (downloadProgress >= 0f && downloadProgress < 100f) {
                    CircularProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                IconButton(onClick = {
                    if (downloadProgress < 0f && !audio.isCached) {
                        downloadProgress = 0f
                        DownloadHelper.downloadFile(
                            context = context,
                            url = audio.audioUrl,
                            title = audio.title,
                            fileName = "micrhema_audio_${audio.id}.mp3"
                        )
                        // Mock progress for UI feedback since DownloadManager doesn't expose a simple callback here easily
                        // Real progress requires a broadcast receiver, assuming it's quick enough we just show a spinner
                    }
                }) {
                    Icon(
                        imageVector = if (audio.isCached) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Baixar",
                        tint = if (audio.isCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Speed
            TextButton(onClick = { viewModel.cycleSpeed() }) {
                Text(
                    "${playbackSpeed}x",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Repeat
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
