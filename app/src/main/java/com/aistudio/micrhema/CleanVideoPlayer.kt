package com.aistudio.micrhema

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun CleanVideoPlayer(
    videoUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    canDownload: Boolean = false,
    onDownload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isYouTube = remember(videoUrl) { isYoutubeUrl(videoUrl) }

    // Video Player State
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(!isYouTube) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Custom UI States
    var controlsVisible by remember { mutableStateOf(true) }
    var lightsOff by remember { mutableStateOf(false) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Validate URL on change
    LaunchedEffect(videoUrl) {
        errorMessage = null
        if (videoUrl.isBlank()) {
            errorMessage = "O link do vídeo está vazio ou inválido."
            android.util.Log.e("CleanVideoPlayer", "Video URL is blank")
        } else if (!videoUrl.contains("http") && extractYoutubeId(videoUrl) == null) {
            errorMessage = "Link de vídeo não reconhecido. Forneça uma URL válida."
            android.util.Log.e("CleanVideoPlayer", "Invalid URL format: $videoUrl")
        }
    }

    // Show Snackbar when error occurs
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Long
            )
        }
    }

    // ExoPlayer Instance (only instantiated if NOT YouTube)
    val exoPlayer = remember(videoUrl, isYouTube) {
        if (!isYouTube && videoUrl.isNotBlank()) {
            try {
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                            ExoPlayerCache.getCacheDataSourceFactory(context)
                        )
                    )
                    .build().apply {
                        setMediaItem(MediaItem.fromUri(convertGoogleDriveUrl(videoUrl)))
                        prepare()
                        playWhenReady = true
                    }
            } catch (e: Exception) {
                android.util.Log.e("CleanVideoPlayer", "Error initializing ExoPlayer", e)
                errorMessage = "Falha ao inicializar o reprodutor de vídeo."
                null
            }
        } else null
    }

    DisposableEffect(videoUrl, isYouTube, exoPlayer) {
        if (!isYouTube && exoPlayer != null) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = (state == Player.STATE_BUFFERING || state == Player.STATE_IDLE)
                    if (state == Player.STATE_READY) {
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        errorMessage = null
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    isBuffering = false
                    isPlaying = false
                    val detailedMsg = when (error.errorCode) {
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Erro de Conexão: Verifique sua conexão com a internet."
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                            "Erro 404/403: O vídeo não foi encontrado ou o acesso foi negado."
                        else -> "Erro na reprodução: ${error.localizedMessage ?: "Não foi possível carregar esta mídia."}"
                    }
                    android.util.Log.e("CleanVideoPlayer", "ExoPlayer error code ${error.errorCode}: ${error.message}", error)
                    errorMessage = detailedMsg
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        } else {
            onDispose { }
        }
    }

    // Timer for auto-hiding controls when playing
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !isUserSeeking) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Progress updates loop for ExoPlayer
    LaunchedEffect(isPlaying, isYouTube) {
        if (!isYouTube && exoPlayer != null) {
            while (isPlaying) {
                if (!isUserSeeking) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    val effectivePos = if (isUserSeeking) sliderPositionMs.toLong() else currentPositionMs

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (lightsOff) Color.Black else Color.Transparent)
    ) {
        // Full screen / overlay dark backdrop when "Apagar Luzes" is enabled
        if (lightsOff) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
            )
        }

        // Main Video Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(if (lightsOff) 0.dp else 16.dp))
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                }
        ) {
            if (exoPlayer != null) {
                // Direct Video ExoPlayer View
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Buffering Indicator for ExoPlayer
            if (isBuffering && !isYouTube && errorMessage == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }

            // Video Error Fallback Overlay
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Erro de Vídeo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Falha no Carregamento do Vídeo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "O link do vídeo não pôde ser reproduzido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    if (exoPlayer != null) {
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Tentar Novamente", fontSize = 12.sp)
                            }

                            if (videoUrl.isNotBlank() && videoUrl.contains("http")) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(videoUrl)
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Abrir no Navegador", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // CONTROLS OVERLAY (Clean fade in/out)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // TOP BAR CONTROLS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "Apagar / Acender Luzes" Button
                            Surface(
                                onClick = { lightsOff = !lightsOff },
                                shape = RoundedCornerShape(20.dp),
                                color = if (lightsOff) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (lightsOff) Icons.Default.Lightbulb else Icons.Default.NightlightRound,
                                        contentDescription = "Modo Cinema",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (lightsOff) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (lightsOff) "Acender" else "Apagar Luzes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (lightsOff) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                }
                            }

                            // Download Button if allowed & not YouTube
                            if (!isYouTube && canDownload && onDownload != null) {
                                IconButton(
                                    onClick = onDownload,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Baixar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Close Button
                            if (onClose != null) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // CENTER PLAY/PAUSE & REWIND/FORWARD (Only for ExoPlayer streams)
                    if (!isYouTube && exoPlayer != null) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Voltar 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Surface(
                                onClick = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Avançar 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // BOTTOM TIMELINE & TIME
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!isYouTube && exoPlayer != null) {
                            Slider(
                                value = effectivePos.toFloat(),
                                onValueChange = {
                                    isUserSeeking = true
                                    sliderPositionMs = it
                                },
                                onValueChangeFinished = {
                                    isUserSeeking = false
                                    exoPlayer.seekTo(sliderPositionMs.toLong())
                                },
                                valueRange = 0f..(durationMs.toFloat().coerceAtLeast(1f)),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isYouTube) {
                                Text(
                                    text = "${formatAudioTime(effectivePos)} / ${formatAudioTime(durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = "🔴 YouTube Player",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Red.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            if (lightsOff) {
                                Text(
                                    text = "💡 Modo Cinema Ativo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar Host for user error feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        )
    }
}
