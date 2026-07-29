package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null
    val cleanUrl = url.trim()
    val lowerUrl = cleanUrl.lowercase()
    return when {
        lowerUrl.contains("v=") -> cleanUrl.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("/").substringBefore("#")
        lowerUrl.contains("youtu.be/") -> cleanUrl.substringAfter("youtu.be/", "youtu.be/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTU.BE/") else it }
        lowerUrl.contains("youtube.com/shorts/") -> cleanUrl.substringAfter("youtube.com/shorts/", "youtube.com/shorts/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/SHORTS/") else it }
        lowerUrl.contains("youtube.com/live/") -> cleanUrl.substringAfter("youtube.com/live/", "youtube.com/live/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/LIVE/") else it }
        lowerUrl.contains("youtube.com/embed/") -> cleanUrl.substringAfter("youtube.com/embed/", "youtube.com/embed/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/EMBED/") else it }
        !lowerUrl.contains("http") && !lowerUrl.contains("/") && cleanUrl.length >= 8 -> cleanUrl
        else -> null
    }
}

fun isYoutubeUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val cleanUrl = url.trim().lowercase()
    return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") || extractYoutubeId(cleanUrl) != null
}

@Composable
fun YoutubePlayer(
    videoUrl: String,
    youtubeId: String = "",
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val extractedId = remember(videoUrl, youtubeId) {
        extractYoutubeId(videoUrl) ?: youtubeId.ifEmpty { null }
    }

    if (extractedId == null) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
            Text("Vídeo inválido", color = MaterialTheme.colorScheme.onErrorContainer)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111116)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Assistir Vídeo",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        onError?.invoke("Não foi possível abrir o link do vídeo.")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Assistir Vídeo Externamente", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "O vídeo será aberto no YouTube ou no navegador.",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
