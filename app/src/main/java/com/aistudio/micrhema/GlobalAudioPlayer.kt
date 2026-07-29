package com.aistudio.micrhema

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class AudioTrack(
    val id: String,
    val title: String,
    val subtitle: String = "Culto Gravado",
    val audioUrl: String,
    val coverUrl: String = "",
    val category: String = "Culto Gravado"
)

object GlobalAudioPlayer {
    val currentTrack = mutableStateOf<AudioTrack?>(null)
    val isPlaying = mutableStateOf(false)
    val isBuffering = mutableStateOf(false)
    val currentPositionMs = mutableStateOf(0L)
    val durationMs = mutableStateOf(0L)
    val playbackSpeed = mutableStateOf(1.0f)
    val isExpanded = mutableStateOf(false)

    private var playerController: Player? = null
    private var mediaControllerFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    fun getOrCreatePlayer(context: Context, onReady: (Player) -> Unit) {
        val appContext = context.applicationContext
        if (playerController != null) {
            onReady(playerController!!)
            return
        }


        val sessionToken = androidx.media3.session.SessionToken(
            appContext,
            android.content.ComponentName(appContext, AudioService::class.java)
        )

        val controllerFuture = androidx.media3.session.MediaController.Builder(appContext, sessionToken).buildAsync()
        mediaControllerFuture = controllerFuture

        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                playerController = controller
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying.value = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering.value = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
                            durationMs.value = controller.duration.coerceAtLeast(0L)
                        }
                    }
                })
                isPlaying.value = controller.isPlaying
                isBuffering.value = controller.playbackState == Player.STATE_BUFFERING
                if (controller.playbackState == Player.STATE_READY) {
                    durationMs.value = controller.duration.coerceAtLeast(0L)
                }
                onReady(controller)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to local ExoPlayer instance if binding fails
                val fallbackPlayer = ExoPlayer.Builder(appContext)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                            ExoPlayerCache.getCacheDataSourceFactory(appContext)
                        )
                    )
                    .build()
                fallbackPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying.value = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering.value = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
                            durationMs.value = fallbackPlayer.duration.coerceAtLeast(0L)
                        }
                    }
                })
                playerController = fallbackPlayer
                onReady(fallbackPlayer)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(appContext))
    }

    fun playTrack(context: Context, track: AudioTrack) {
        getOrCreatePlayer(context) { player ->
            if (currentTrack.value?.id == track.id && currentTrack.value?.audioUrl == track.audioUrl) {
                if (!player.isPlaying) {
                    player.play()
                } else {
                    player.pause()
                }
                return@getOrCreatePlayer
            }

            currentTrack.value = track
            currentPositionMs.value = 0L
            durationMs.value = 0L

            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.subtitle)
                .setDisplayTitle(track.title)
                .setSubtitle(track.subtitle)
                .setArtworkUri(if (track.coverUrl.isNotEmpty()) android.net.Uri.parse(track.coverUrl) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(track.audioUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playbackParameters = PlaybackParameters(playbackSpeed.value)
            player.playWhenReady = true
        }
    }

    fun togglePlayPause(context: Context) {
        getOrCreatePlayer(context) { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(context: Context, positionMs: Long) {
        getOrCreatePlayer(context) { player ->
            player.seekTo(positionMs)
            currentPositionMs.value = positionMs
        }
    }

    fun seekForward(context: Context, ms: Long = 10000L) {
        getOrCreatePlayer(context) { player ->
            val target = (player.currentPosition + ms).coerceAtMost(player.duration.coerceAtLeast(0L))
            player.seekTo(target)
            currentPositionMs.value = target
        }
    }

    fun seekBackward(context: Context, ms: Long = 10000L) {
        getOrCreatePlayer(context) { player ->
            val target = (player.currentPosition - ms).coerceAtLeast(0L)
            player.seekTo(target)
            currentPositionMs.value = target
        }
    }

    fun setSpeed(context: Context, speed: Float) {
        playbackSpeed.value = speed
        getOrCreatePlayer(context) { player ->
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    fun stopAndClose(context: Context) {
        getOrCreatePlayer(context) { player ->
            player.stop()
            player.clearMediaItems()
            currentTrack.value = null
            isPlaying.value = false
            isExpanded.value = false
        }
    }

    fun updateProgress(context: Context) {
        val player = playerController ?: return
        if (player.isPlaying) {
            currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
            durationMs.value = player.duration.coerceAtLeast(0L)
        }
    }
}

fun formatAudioTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun PersistentAudioPlayerBar(modifier: Modifier = Modifier) {
    val track = GlobalAudioPlayer.currentTrack.value ?: return
    val context = LocalContext.current
    val isPlaying = GlobalAudioPlayer.isPlaying.value
    val isBuffering = GlobalAudioPlayer.isBuffering.value
    val currentPos = GlobalAudioPlayer.currentPositionMs.value
    val duration = GlobalAudioPlayer.durationMs.value

    val progress = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            GlobalAudioPlayer.updateProgress(context)
            delay(500)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { GlobalAudioPlayer.isExpanded.value = true },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Linear Progress Line
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork / Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${track.subtitle} • ${formatAudioTime(currentPos)} / ${formatAudioTime(duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause Button
                IconButton(
                    onClick = { GlobalAudioPlayer.togglePlayPause(context) },
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Expand Button
                IconButton(
                    onClick = { GlobalAudioPlayer.isExpanded.value = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expandir Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Close Button
                IconButton(
                    onClick = { GlobalAudioPlayer.stopAndClose(context) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedAudioPlayerModal() {
    val track = GlobalAudioPlayer.currentTrack.value ?: return
    val context = LocalContext.current
    val isPlaying = GlobalAudioPlayer.isPlaying.value
    val isBuffering = GlobalAudioPlayer.isBuffering.value
    val currentPos = GlobalAudioPlayer.currentPositionMs.value
    val duration = GlobalAudioPlayer.durationMs.value
    val currentSpeed = GlobalAudioPlayer.playbackSpeed.value

    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPos by remember { mutableFloatStateOf(0f) }

    val effectivePos = if (isUserSeeking) sliderPos.toLong() else currentPos

    val rotationAnim by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            GlobalAudioPlayer.updateProgress(context)
            delay(500)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { GlobalAudioPlayer.isExpanded.value = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { GlobalAudioPlayer.isExpanded.value = false }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", modifier = Modifier.size(32.dp))
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = track.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                IconButton(onClick = {
                    DownloadHelper.downloadFile(
                        context = context,
                        url = track.audioUrl,
                        title = track.title,
                        fileName = "micrhema_audio_${track.id}.mp3"
                    )
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Baixar Áudio", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Vinyl / Artwork Canvas
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .rotate(if (isPlaying) rotationAnim else 0f),
                contentAlignment = Alignment.Center
            ) {
                if (track.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                // Center vinyl hole
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Preletor
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = effectivePos.toFloat(),
                onValueChange = {
                    isUserSeeking = true
                    sliderPos = it
                },
                onValueChangeFinished = {
                    isUserSeeking = false
                    GlobalAudioPlayer.seekTo(context, sliderPos.toLong())
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAudioTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Controls: Rewind 10s | Play/Pause | Forward 10s
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = { GlobalAudioPlayer.seekBackward(context, 10000L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Voltar 10s",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { GlobalAudioPlayer.togglePlayPause(context) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { GlobalAudioPlayer.seekForward(context, 10000L) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Avançar 10s",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Speed Selector Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Velocidade:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = currentSpeed == speed,
                        onClick = { GlobalAudioPlayer.setSpeed(context, speed) },
                        label = { Text("${speed}x", fontSize = 11.sp) },
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }
        }
    }
}
