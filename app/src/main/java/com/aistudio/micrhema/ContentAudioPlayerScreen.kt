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
    onClose: () -> Unit,
    viewModel: ContentAudioPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentAudio by viewModel.currentAudio.collectAsState()
    
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableStateOf(0f) }
    
    var downloadProgress by remember { mutableStateOf(-1f) } // -1 means not downloading

    LaunchedEffect(audio.id) {
        viewModel.playAudio(context, audio)
    }

    val effectivePos = if (isUserSeeking) sliderPos.toLong() else currentPosition
    val displayAudio = currentAudio ?: audio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                viewModel.stop()
                onClose() 
            }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", modifier = Modifier.size(32.dp))
            }
            Text(
                "Ouvindo Agora",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opções")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Cover Image (Large, Spotify style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            AsyncImage(
                model = displayAudio.coverUrl,
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
                text = "🎵 ${displayAudio.title}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pr. ${displayAudio.artist}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = formatAudioTime(duration),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                Icon(Icons.Default.Forward30, contentDescription = "Avançar 30s", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom Actions (Download, Repeat, Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download Button
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                if (downloadProgress < 0f && !displayAudio.isCached) {
                    downloadProgress = 0f
                    viewModel.startDownload(context, { prog ->
                        downloadProgress = prog
                    }, {
                        downloadProgress = -1f
                    })
                }
            }.padding(8.dp)) {
                if (downloadProgress >= 0f && downloadProgress < 100f) {
                    CircularProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${downloadProgress.toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(
                        imageVector = if (displayAudio.isCached) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = "Baixar",
                        tint = if (displayAudio.isCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (displayAudio.isCached) "Baixado" else "Baixar",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (displayAudio.isCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Repeat Button
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleRepeat() }.padding(8.dp)) {
                Icon(
                    imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Repetir", style = MaterialTheme.typography.labelLarge, color = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            
            // Speed Button
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.cycleSpeed() }.padding(8.dp)) {
                Icon(Icons.Default.Speed, contentDescription = "Velocidade", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
