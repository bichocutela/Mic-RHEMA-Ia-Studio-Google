package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val LIVE_ENDPOINT = "https://cwphbkdtorfpgmnlafqb.supabase.co/functions/v1/youtube-live"
private const val DEFAULT_LIVE_HANDLE = "@micrhemaoficial"

data class LiveStreamModel(
    val autoEnabled: Boolean = true,
    val manualEnabled: Boolean = false,
    val youtubeHandle: String = DEFAULT_LIVE_HANDLE,
    val manualUrl: String = "",
    val manualTitle: String = "Estamos ao vivo",
    val channelId: String = "",
    val isLive: Boolean = false,
    val source: String = "none",
    val videoId: String = "",
    val url: String = "",
    val title: String = "Estamos ao vivo",
    val thumbnailUrl: String = "",
    val startedAt: String = "",
    val autoError: String = "",
    val upcomingVideoId: String = "",
    val upcomingTitle: String = "",
    val upcomingScheduledAt: String = ""
)

val liveStreamState = mutableStateOf(LiveStreamModel())

object LiveStreamRepository {
    private var listener: ListenerRegistration? = null
    private val firestore get() = FirebaseFirestore.getInstance()

    fun start() {
        if (listener != null) return
        listener = firestore.collection("settings").document("live_stream")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                liveStreamState.value = LiveStreamModel(
                    autoEnabled = snapshot.getBoolean("autoEnabled") ?: true,
                    manualEnabled = snapshot.getBoolean("manualEnabled") ?: false,
                    youtubeHandle = snapshot.getString("youtubeHandle").orEmpty().ifBlank { DEFAULT_LIVE_HANDLE },
                    manualUrl = snapshot.getString("manualUrl").orEmpty(),
                    manualTitle = snapshot.getString("manualTitle").orEmpty().ifBlank { "Estamos ao vivo" },
                    channelId = snapshot.getString("channelId").orEmpty(),
                    isLive = snapshot.getBoolean("isLive") ?: false,
                    source = snapshot.getString("source").orEmpty().ifBlank { "none" },
                    videoId = snapshot.getString("videoId").orEmpty(),
                    url = snapshot.getString("url").orEmpty(),
                    title = snapshot.getString("title").orEmpty().ifBlank { "Estamos ao vivo" },
                    thumbnailUrl = snapshot.getString("thumbnailUrl").orEmpty(),
                    startedAt = snapshot.getString("startedAt").orEmpty(),
                    autoError = snapshot.getString("autoError").orEmpty(),
                    upcomingVideoId = snapshot.getString("upcomingVideoId").orEmpty(),
                    upcomingTitle = snapshot.getString("upcomingTitle").orEmpty(),
                    upcomingScheduledAt = snapshot.getString("upcomingScheduledAt").orEmpty()
                )
            }
    }

    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        val endpoint = if (force) "$LIVE_ENDPOINT?force=1" else LIVE_ENDPOINT
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = if (force) "POST" else "GET"
            connectTimeout = 12_000
            readTimeout = 18_000
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (force) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write("{}".toByteArray()) }
            }
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("A verificação da transmissão retornou HTTP $code.")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun saveConfig(
        autoEnabled: Boolean,
        manualEnabled: Boolean,
        youtubeHandle: String,
        manualUrl: String,
        manualTitle: String
    ) {
        firestore.collection("settings").document("live_stream")
            .set(
                mapOf(
                    "autoEnabled" to autoEnabled,
                    "manualEnabled" to manualEnabled,
                    "youtubeHandle" to youtubeHandle.trim().ifBlank { DEFAULT_LIVE_HANDLE },
                    "manualUrl" to manualUrl.trim(),
                    "manualTitle" to manualTitle.trim().ifBlank { "Estamos ao vivo" },
                    "updatedAt" to System.currentTimeMillis(),
                    "sourceEditor" to "android"
                ),
                SetOptions.merge()
            ).await()
        refresh(force = true)
    }
}

@Composable
fun HomeScreenWithLive(onNavigate: (String) -> Unit = {}) {
    val live = liveStreamState.value
    var playerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        LiveStreamRepository.start()
        runCatching { LiveStreamRepository.refresh(false) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (live.isLive && (live.videoId.isNotBlank() || live.url.isNotBlank())) {
            Spacer(Modifier.height(8.dp))
            LiveStreamHomeBanner { playerOpen = true }
            Spacer(Modifier.height(8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            HomeScreen(onNavigate = onNavigate)
        }
    }
    if (playerOpen) LiveStreamPlayerDialog { playerOpen = false }
}

private fun extractYoutubeVideoId(value: String): String {
    val input = value.trim()
    val patterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{6,})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
        Regex("youtube\\.com/(?:live|shorts|embed)/([A-Za-z0-9_-]{6,})")
    )
    return patterns.firstNotNullOfOrNull { it.find(input)?.groupValues?.getOrNull(1) }.orEmpty()
}

@Composable
fun LiveStreamHomeBanner(onOpen: () -> Unit) {
    val state = liveStreamState.value
    if (!state.isLive || (state.videoId.isBlank() && state.url.isBlank())) return
    val transition = rememberInfiniteTransition(label = "livePulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "livePulseAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onOpen() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEC))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(14.dp).alpha(pulse).background(Color(0xFFE53935), CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ESTAMOS AO VIVO", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                Text(state.title.ifBlank { "Transmissão MIC Rhema" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Toque para assistir dentro do aplicativo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.LiveTv, contentDescription = "Assistir ao vivo", tint = Color(0xFFC62828))
        }
    }
}

@Composable
fun LiveStreamPlayerDialog(onDismiss: () -> Unit) {
    val state = liveStreamState.value
    val context = LocalContext.current
    val videoId = state.videoId.ifBlank { extractYoutubeVideoId(state.url) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 26.dp, bottom = 26.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("● AO VIVO", color = Color(0xFFE53935), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        Text(state.title.ifBlank { "Transmissão MIC Rhema" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
                if (videoId.isNotBlank()) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                loadUrl("https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0")
                            }
                        },
                        update = { webView ->
                            val expected = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
                            if (webView.url != expected) webView.loadUrl(expected)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
                        Text("Esta transmissão não oferece player incorporado.", color = Color.White, modifier = Modifier.padding(24.dp))
                    }
                }
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (state.source == "manual") "Transmissão ativada manualmente pelo painel." else "Transmissão detectada automaticamente no canal MIC Rhema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.url.isNotBlank()) {
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.url))) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir no YouTube")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditLiveStreamSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remote = liveStreamState.value
    var autoEnabled by remember(remote.autoEnabled) { mutableStateOf(remote.autoEnabled) }
    var manualEnabled by remember(remote.manualEnabled) { mutableStateOf(remote.manualEnabled) }
    var handle by remember(remote.youtubeHandle) { mutableStateOf(remote.youtubeHandle.ifBlank { DEFAULT_LIVE_HANDLE }) }
    var manualUrl by remember(remote.manualUrl) { mutableStateOf(remote.manualUrl) }
    var manualTitle by remember(remote.manualTitle) { mutableStateOf(remote.manualTitle.ifBlank { "Estamos ao vivo" }) }
    var saving by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        LiveStreamRepository.start()
        runCatching { LiveStreamRepository.refresh(false) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Transmissão híbrida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("O modo manual tem prioridade. Se estiver desligado, o MIC Rhema usa a detecção automática do canal do YouTube.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Detecção automática", fontWeight = FontWeight.SemiBold)
                Text("Verifica o canal @micrhemaoficial a cada 2 minutos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = autoEnabled, onCheckedChange = { autoEnabled = it })
        }
        OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("Canal ou @handle do YouTube") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Forçar transmissão manual", fontWeight = FontWeight.SemiBold)
                Text("Use quando a live estiver em outro canal. Enquanto estiver ativo, substitui o automático.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = manualEnabled, onCheckedChange = { manualEnabled = it })
        }
        OutlinedTextField(value = manualUrl, onValueChange = { manualUrl = it }, label = { Text("Link manual da live") }, placeholder = { Text("https://youtube.com/watch?v=...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = manualTitle, onValueChange = { manualTitle = it }, label = { Text("Título exibido") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (remote.isLive) Color(0xFFFFECEC) else MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (remote.isLive) "● AO VIVO AGORA" else "OFFLINE", color = if (remote.isLive) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold)
                if (remote.channelId.isNotBlank()) Text("Canal: ${remote.youtubeHandle} • ${remote.channelId}", style = MaterialTheme.typography.bodySmall)
                if (remote.isLive) Text(remote.title, fontWeight = FontWeight.Bold)
                if (remote.autoError.isNotBlank()) Text("Última verificação: ${remote.autoError}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            enabled = !saving && (!manualEnabled || manualUrl.isNotBlank()),
            onClick = {
                scope.launch {
                    saving = true
                    try {
                        LiveStreamRepository.saveConfig(autoEnabled, manualEnabled, handle, manualUrl, manualTitle)
                        Toast.makeText(context, "Configuração da transmissão salva.", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "Não foi possível salvar.", Toast.LENGTH_LONG).show()
                    } finally { saving = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (saving) "Salvando…" else "Salvar transmissão")
        }
        OutlinedButton(
            enabled = !checking,
            onClick = {
                scope.launch {
                    checking = true
                    try {
                        LiveStreamRepository.refresh(true)
                        Toast.makeText(context, "Canal verificado agora.", Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) {
                        Toast.makeText(context, error.message ?: "Falha ao verificar.", Toast.LENGTH_LONG).show()
                    } finally { checking = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (checking) "Verificando…" else "Verificar agora")
        }
        Spacer(Modifier.height(24.dp))
    }
}
