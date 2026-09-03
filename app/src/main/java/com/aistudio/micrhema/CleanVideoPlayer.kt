package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Player de vídeo reutilizável oficial do aplicativo.
 *
 * Pode ser usado por Mídia, IBR e futuras telas sem duplicar implementação.
 * YouTube usa a biblioteca Android YouTube Player; MP4/links diretos usam ExoPlayer.
 * Links completos, Shorts, Live, youtu.be e IDs puros do YouTube são aceitos.
 */
@Composable
fun CleanVideoPlayer(
    videoUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    canDownload: Boolean = false,
    onDownload: (() -> Unit)? = null,
    showTitleBar: Boolean = true,
    showExternalButton: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isYouTube = remember(videoUrl) { isYoutubeUrl(videoUrl) }
    val youtubeId = remember(videoUrl) { extractYouTubeVideoId(videoUrl) }
    val externalVideoUrl = remember(videoUrl, youtubeId, isYouTube) {
        if (isYouTube && youtubeId != null) "https://www.youtube.com/watch?v=$youtubeId" else videoUrl.trim()
    }

    var errorMessage by remember(videoUrl) { mutableStateOf<String?>(null) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(videoUrl, isYouTube, youtubeId) {
        errorMessage = when {
            videoUrl.isBlank() -> "O link do vídeo está vazio."
            isYouTube && youtubeId == null -> "Não foi possível reconhecer o link do YouTube."
            else -> null
        }
    }

    val exoPlayer = remember(videoUrl, isYouTube, retryKey) {
        if (!isYouTube && videoUrl.isNotBlank()) {
            runCatching {
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                            ExoPlayerCache.getCacheDataSourceFactory(context)
                        )
                    )
                    .build()
                    .apply {
                        setMediaItem(MediaItem.fromUri(convertGoogleDriveUrl(videoUrl)))
                        prepare()
                        playWhenReady = false
                    }
            }.getOrElse {
                errorMessage = "Não foi possível iniciar o player de vídeo."
                null
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                    if (playbackState == Player.STATE_READY) errorMessage = null
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    isLoading = false
                    errorMessage = "Não foi possível reproduzir este vídeo. Verifique o link ou a conexão."
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showTitleBar || (!isYouTube && canDownload && onDownload != null) || onClose != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showTitleBar) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!isYouTube && canDownload && onDownload != null) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Baixar vídeo")
                    }
                }

                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar player")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when {
                errorMessage != null -> {
                    PlayerErrorState(
                        message = errorMessage ?: "Falha ao carregar o vídeo.",
                        onRetry = {
                            errorMessage = null
                            isLoading = true
                            retryKey++
                        },
                        onOpenExternal = if (externalVideoUrl.startsWith("http", ignoreCase = true)) {
                            {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalVideoUrl)))
                                }
                            }
                        } else null
                    )
                }

                isYouTube && youtubeId != null -> {
                    val playerView = remember(youtubeId, retryKey) {
                        YouTubePlayerView(context).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    isLoading = false
                                    errorMessage = null
                                    youTubePlayer.cueVideo(youtubeId, 0f)
                                }

                                override fun onStateChange(
                                    youTubePlayer: YouTubePlayer,
                                    state: PlayerConstants.PlayerState
                                ) {
                                    isLoading = state == PlayerConstants.PlayerState.BUFFERING
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    isLoading = false
                                    errorMessage = when (error) {
                                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                                            "Este vídeo não permite reprodução incorporada. Abra no YouTube."
                                        else -> "O YouTube não conseguiu reproduzir este vídeo."
                                    }
                                }
                            })
                        }
                    }
                    DisposableEffect(playerView, lifecycleOwner) {
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(playerView)
                            playerView.release()
                        }
                    }
                    AndroidView(
                        factory = { playerView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                exoPlayer != null -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                controllerAutoShow = true
                            }
                        },
                        update = { view: PlayerView -> view.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isLoading && errorMessage == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    color = Color.White
                )
            }
        }

        if (isYouTube && showExternalButton) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalVideoUrl)))
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Abrir no YouTube")
            }
        }
    }
}

@Composable
private fun PlayerErrorState(
    message: String,
    onRetry: () -> Unit,
    onOpenExternal: (() -> Unit)?
) {
    Surface(color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Tentar novamente")
                }
                if (onOpenExternal != null) {
                    Button(onClick = onOpenExternal) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Abrir fora")
                    }
                }
            }
        }
    }
}
