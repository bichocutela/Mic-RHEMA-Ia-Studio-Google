package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun YoutubeThumbnailImage(
    videoUrl: String,
    explicitThumbnailUrl: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ytId = extractYouTubeVideoId(videoUrl)

    // Para links do YouTube, a miniatura oficial é a fonte mais confiável. Antes,
    // uma URL de thumbnail antiga ou inválida podia impedir o fallback do YouTube e
    // deixar o cartão com o ícone genérico de vídeo.
    val targetUrl = when {
        ytId != null -> "https://i.ytimg.com/vi/$ytId/hqdefault.jpg"
        explicitThumbnailUrl.startsWith("http://", ignoreCase = true) ||
            explicitThumbnailUrl.startsWith("https://", ignoreCase = true) -> explicitThumbnailUrl.trim()
        else -> ""
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (targetUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Miniatura do vídeo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Reproduzir",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(48.dp)
        )
    }
}
