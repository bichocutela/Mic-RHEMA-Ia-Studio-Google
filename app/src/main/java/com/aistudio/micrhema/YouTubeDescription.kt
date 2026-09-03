package com.aistudio.micrhema

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Carrega a descrição pública do próprio vídeo do YouTube sem interferir no player.
 * Não usa nem altera chaves do aplicativo. Se o YouTube não devolver os metadados,
 * a tela continua funcionando normalmente e oferece nova tentativa.
 */
object YouTubeDescriptionLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun load(videoUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractYouTubeVideoId(videoUrl) ?: return@withContext null
        cache[videoId]?.let { return@withContext it }

        val request = Request.Builder()
            .url("https://www.youtube.com/watch?v=$videoId&hl=pt-BR")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Mobile Safari/537.36"
            )
            .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.7")
            .get()
            .build()

        val html = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        }.getOrNull() ?: return@withContext null

        val description = extractShortDescription(html)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.take(12_000)

        if (description != null) cache[videoId] = description
        description
    }

    /** Extrai o campo shortDescription do JSON público presente na página do vídeo. */
    private fun extractShortDescription(html: String): String? {
        val marker = "\"shortDescription\":\""
        val markerIndex = html.indexOf(marker)
        if (markerIndex < 0) return null

        val start = markerIndex + marker.length
        var index = start
        var escaped = false

        while (index < html.length) {
            val char = html[index]
            if (!escaped && char == '"') break
            escaped = if (!escaped && char == '\\') true else false
            if (escaped && char != '\\') escaped = false
            index++
        }
        if (index <= start || index >= html.length) return null

        val encoded = html.substring(start, index)
        return runCatching {
            JSONObject("{\"value\":\"$encoded\"}").getString("value")
        }.getOrNull()
    }
}

/**
 * Bloco apresentado abaixo do título/subtítulo do vídeo. A descrição longa pode
 * ser expandida sem aumentar indefinidamente a tela: quando aberta, a área passa
 * a ter rolagem própria.
 */
@Composable
fun YouTubeDescriptionSection(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    if (!isYoutubeUrl(videoUrl)) return

    var description by remember(videoUrl) { mutableStateOf<String?>(null) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }
    var retryCount by remember(videoUrl) { mutableIntStateOf(0) }
    var expanded by remember(videoUrl) { mutableStateOf(false) }

    LaunchedEffect(videoUrl, retryCount) {
        isLoading = true
        description = YouTubeDescriptionLoader.load(videoUrl)
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Subject,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Descrição do YouTube",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Carregando descrição…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            description.isNullOrBlank() -> {
                Text(
                    "Não foi possível carregar a descrição do YouTube agora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { retryCount++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Tentar novamente")
                }
            }

            else -> {
                val text = description.orEmpty()
                val shouldOfferExpansion = text.length > 420 || text.count { it == '\n' } >= 5
                val descriptionScroll = rememberScrollState()

                if (expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(descriptionScroll)
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (shouldOfferExpansion) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Mostrar menos" else "Mostrar mais")
                    }
                }
            }
        }
    }
}
