package com.aistudio.micrhema

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun androidx.compose.animation.SharedTransitionScope.ContentAudioPlayerScreen(
    audio: ContentAudio,
    onClose: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
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
    var downloadProgress by remember { mutableStateOf(-1f) }

    LaunchedEffect(audio.id) {
        viewModel.playAudio(context, audio)
    }

    val effectivePos = if (isUserSeeking) sliderPos.toLong() else currentPosition
    val displayAudio = currentAudio ?: audio

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                "Ouvindo Agora",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opções")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Compact Player Card inspired by 4shared
        Card(
            modifier = Modifier.fillMaxWidth().sharedElement(
                state = rememberSharedContentState(key = "audio_card_${audio.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Row: Image on left, Info and Main Controls on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cover Image
                    AsyncImage(
                        model = displayAudio.coverUrl,
                        contentDescription = "Capa da Pregação",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .sharedElement(
                                state = rememberSharedContentState(key = "audio_cover_${audio.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Info and Controls
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayAudio.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            modifier = Modifier.sharedElement(
                                state = rememberSharedContentState(key = "audio_title_${audio.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        Text(
                            text = "Pr. ${displayAudio.artist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Playback Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.seekBackward10() }) {
                                Icon(Icons.Default.FastRewind, contentDescription = "Voltar 10s")
                            }
                            
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { viewModel.togglePlayPause() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            
                            IconButton(onClick = { viewModel.seekForward30() }) {
                                Icon(Icons.Default.FastForward, contentDescription = "Avançar 30s")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAudioTime(effectivePos),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = formatAudioTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (downloadProgress < 0f && !displayAudio.isCached) {
                                    downloadProgress = 0f
                                    viewModel.startDownload(context, { prog ->
                                        downloadProgress = prog
                                    }, {
                                        downloadProgress = -1f
                                    })
                                }
                            }
                            .padding(8.dp)
                    ) {
                        if (downloadProgress >= 0f && downloadProgress < 100f) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (displayAudio.isCached || downloadProgress == 100f) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = "Baixar",
                                modifier = Modifier.size(20.dp),
                                tint = if (displayAudio.isCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (displayAudio.isCached) "Baixado" else "Baixar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Repeat
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleRepeat() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repetir",
                            modifier = Modifier.size(20.dp),
                            tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Repetir",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Speed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.cycleSpeed() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Velocidade",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

