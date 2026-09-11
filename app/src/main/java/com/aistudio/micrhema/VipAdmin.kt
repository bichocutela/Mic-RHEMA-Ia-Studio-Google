package com.aistudio.micrhema

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

fun addVipBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_books").document(item.id).set(item)
    }
}
fun removeVipBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_books").document(item.id).delete()
    }
}
fun addVipAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_audios").document(item.id).set(item)
    }
}
fun removeVipAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_audios").document(item.id).delete()
    }
}
fun addVipVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_videos").document(item.id).set(item)
    }
}
fun removeVipVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_videos").document(item.id).delete()
    }
}
fun addVipAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_albums").document(item.id).set(item)
    }
}
fun removeVipAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("vip_albums").document(item.id).delete()
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVipSection() {
    var vipTab by remember { mutableStateOf("overview") } // overview, midia, cursos or certificados
    val tabs = listOf(
        "overview" to "Visão geral",
        "midia" to "Conteúdo",
        "cursos" to "Módulos",
        "certificados" to "Certificados"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { (id, title) ->
                FilterChip(
                    selected = vipTab == id,
                    onClick = { vipTab = id },
                    label = { Text(title, maxLines = 1) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (vipTab == "overview") {
                EditIbrOverviewSection(onOpenTab = { vipTab = it })
            } else if (vipTab == "midia") {
                EditVipContentSection()
            } else if (vipTab == "cursos") {
                EditVipIbrSection()
            } else {
                EditIbrCertificatesSection()
            }
        }
    }
}

@Composable
fun EditIbrOverviewSection(onOpenTab: (String) -> Unit = {}) {
    val totalCourses = ibrCoursesState.size
    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }
    val totalIbrMembers = memberRequestsState.count { it.isIbr }
    val progressByMember = rememberIbrProgressByMember()
    val certificatesPending = memberRequestsState.count { member ->
        member.isIbr && member.ibrCertificateUrl.isBlank() && member.ibrCertificateStoragePath.isBlank() &&
            isIbrCompleteFor(progressByMember[member.id].orEmpty())
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Central de gestão IBR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Acompanhe os indicadores e acesse cada área do Instituto Bíblico Rhema.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                IbrAdminMetricCard("Cursos", totalCourses.toString(), Icons.Default.MenuBook, Modifier.weight(1f)) { onOpenTab("cursos") }
                IbrAdminMetricCard("Aulas", totalLessons.toString(), Icons.Default.Class, Modifier.weight(1f)) { onOpenTab("cursos") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                IbrAdminMetricCard("Alunos IBR", totalIbrMembers.toString(), Icons.Default.People, Modifier.weight(1f))
                IbrAdminMetricCard("Cert. pendentes", certificatesPending.toString(), Icons.Default.EmojiEvents, Modifier.weight(1f)) { onOpenTab("certificados") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text("Use as abas acima para administrar conteúdo, módulos/capítulos e certificados separadamente.", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun IbrAdminMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable



fun EditVipContentSection() {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isUploading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var uploadProgress by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            LocalDataManager.saveAll(context)
        }
    }
    var editingBook by remember { mutableStateOf<ContentBook?>(null) }
    var editingAudio by remember { mutableStateOf<ContentAudio?>(null) }
    var editingVideo by remember { mutableStateOf<ContentVideo?>(null) }
    var editingAlbum by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var albumToDelete by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var audioToDelete by remember { mutableStateOf<ContentAudio?>(null) }
    var videoToDelete by remember { mutableStateOf<ContentVideo?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var contentFilter by remember { mutableStateOf("Todos") }
    var addContentType by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("IBR - Conteúdo Geral", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione e edite os materiais dos alunos sem sair desta tela.", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Livros", "Áudios", "Vídeos", "Fotos").forEach { option ->
                FilterChip(selected = contentFilter == option, onClick = { contentFilter = option }, label = { Text(option) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Livros" to "+ Livro", "Áudios" to "+ Áudio", "Vídeos" to "+ Vídeo", "Fotos" to "+ Álbum").forEach { (type, label) ->
                AssistChip(onClick = { addContentType = if (addContentType == type) null else type }, label = { Text(label) })
            }
        }
        // SMART IMPORTER
        var smartUrl by remember { mutableStateOf("") }
        var isSmartLoading by remember { mutableStateOf(false) }
        var smartMessage by remember { mutableStateOf("") }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Smart Import Google Drive 🚀", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Cole um link do Google Drive. O sistema detectará automaticamente PDF, EPUB, Word (DOCX), Áudio (MP3), Vídeo (MP4) ou Imagem.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                GlassTextField(value = smartUrl, onValueChange = { smartUrl = it }, label = { Text("Link do Google Drive") }, modifier = Modifier.fillMaxWidth())
                if (isSmartLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp).size(24.dp))
                } else {
                    GlassButton(onClick = {
                        if (smartUrl.isBlank()) return@GlassButton
                        isSmartLoading = true
                        smartMessage = ""
                        coroutineScope.launch {
                            val type = GoogleDriveService.identifyFileType(smartUrl)
                            isSmartLoading = false
                            when (type) {
                                GoogleDriveService.FileType.PDF -> {
                                    smartMessage = "Livro PDF detectado e adicionado!"
                                    addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = "Novo Livro Importado", author = "Desconhecido", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "", bookUrl = GoogleDriveService.getDirectDownloadLink(smartUrl)))
                                }
                                GoogleDriveService.FileType.EPUB -> {
                                    smartMessage = "Livro EPUB detectado e adicionado!"
                                    addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = "Novo Livro EPUB Importado", author = "Documento EPUB", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "", bookUrl = GoogleDriveService.getDirectDownloadLink(smartUrl), type = "epub"))
                                }
                                GoogleDriveService.FileType.WORD -> {
                                    smartMessage = "Documento Word detectado e adicionado!"
                                    addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = "Novo Documento Word Importado", author = "Documento Word", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "", bookUrl = GoogleDriveService.getDirectDownloadLink(smartUrl), type = "word"))
                                }
                                GoogleDriveService.FileType.AUDIO -> {
                                    smartMessage = "Áudio detectado e adicionado!"
                                    addVipAudio(ContentAudio(id = System.currentTimeMillis().toString(), title = "Novo Áudio Importado", artist = "Desconhecido", audioUrl = GoogleDriveService.getDirectDownloadLink(smartUrl), coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"))
                                }
                                GoogleDriveService.FileType.VIDEO -> {
                                    smartMessage = "Vídeo detectado e adicionado!"
                                    val finalThumb = getYoutubeThumbnailUrl(smartUrl) ?: "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80"
                                    addVipVideo(ContentVideo(id = System.currentTimeMillis().toString(), title = "Novo Vídeo Importado", description = "", videoUrl = GoogleDriveService.getDirectDownloadLink(smartUrl), thumbnailUrl = finalThumb))
                                }
                                else -> {
                                    smartMessage = "Tipo de arquivo não suportado ou link inválido."
                                }
                            }
                            smartUrl = ""
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Identificar e Adicionar")
                    }
                }
                if (smartMessage.isNotEmpty()) {
                    Text(smartMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        
        // ADD BOOK
        if (addContentType == "Livros") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Livro", fontWeight = FontWeight.Bold)
                var title by remember { mutableStateOf("") }
                var author by remember { mutableStateOf("") }
                var coverUrl by remember { mutableStateOf("") }
                GlassTextField(value = title, onValueChange = { title = it }, label = { Text("Título do Livro") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = author, onValueChange = { author = it }, label = { Text("Autor (e.g. PDF/Epub Simulado)") }, modifier = Modifier.fillMaxWidth())
                LocalUploadField(value = coverUrl, onValueChange = { coverUrl = it }, label = "Capa do Livro (URL da imagem)", mimeType = "image/*")
                var bookUrl by remember { mutableStateOf("") }
                LocalUploadField(value = bookUrl, onValueChange = { bookUrl = it }, label = "Arquivo do Livro (URL ou PDF/Epub)", mimeType = "*/*")
                GlassButton(onClick = {
                    if (isUploading) return@GlassButton
                    isUploading = true
                    coroutineScope.launch {
                        try {
                            uploadProgress = 0f
                            val finalCover = if (coverUrl.isNotBlank() && !coverUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(coverUrl), "books/covers") { progress -> uploadProgress = progress / 2f } else convertGoogleDriveUrl(coverUrl).ifEmpty { "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" }
                            val finalBookUrl = if (bookUrl.isNotBlank() && !bookUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(bookUrl), "books/files") { progress -> uploadProgress = 0.5f + (progress / 2f) } else convertGoogleDriveUrl(bookUrl)
                            addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = title, author = author, coverUrl = finalCover, contentText = "Conteúdo do livro carregado...", bookUrl = finalBookUrl))
                            title = ""
                            author = ""
                            coverUrl = ""
                            bookUrl = ""
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Upload do livro não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Livro")
                }
            }
        }
        }
        
        if ((contentFilter == "Todos" || contentFilter == "Livros") && vipBooksState.isNotEmpty()) {
            Text("Livros Cadastrados", fontWeight = FontWeight.Bold)
            vipBooksState.forEach { book ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold)
                        Text(book.author, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingBook = book }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { removeVipBook(book) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        
        Divider()
        
        // ADD AUDIO
        if (addContentType == "Áudios") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Áudio", fontWeight = FontWeight.Bold)
                var audioTitle by remember { mutableStateOf("") }
                var audioArtist by remember { mutableStateOf("") }
                var audioUrl by remember { mutableStateOf("") }
                var audioCover by remember { mutableStateOf("") }
                GlassTextField(value = audioTitle, onValueChange = { audioTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = audioArtist, onValueChange = { audioArtist = it }, label = { Text("Artista/Preletor") }, modifier = Modifier.fillMaxWidth())
                LocalUploadField(value = audioUrl, onValueChange = { audioUrl = it }, label = "URL ou Arquivo Local MP3", mimeType = "audio/*")
                GlassButton(onClick = {
                    val finalAudioUrl = convertGoogleDriveUrl(audioUrl)
                    if (finalAudioUrl.isBlank()) {
                        android.widget.Toast.makeText(context, "Informe ou selecione um arquivo de áudio.", android.widget.Toast.LENGTH_SHORT).show()
                        return@GlassButton
                    }
                    addVipAudio(ContentAudio(id = System.currentTimeMillis().toString(), title = audioTitle, artist = audioArtist, audioUrl = finalAudioUrl, coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"))
                    audioTitle = ""
                    audioArtist = ""
                    audioUrl = ""
                    audioCover = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Áudio")
                }
            }
        }
        }
        
        if ((contentFilter == "Todos" || contentFilter == "Áudios") && vipAudiosState.isNotEmpty()) {
            Text("Áudios Cadastrados", fontWeight = FontWeight.Bold)
            vipAudiosState.forEach { audio ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(audio.title, fontWeight = FontWeight.Bold)
                        Text(audio.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingAudio = audio }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { audioToDelete = audio }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        
        Divider()
        
        // ADD VIDEO
        if (addContentType == "Vídeos") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Vídeo", fontWeight = FontWeight.Bold)
                var videoTitle by remember { mutableStateOf("") }
                var videoDesc by remember { mutableStateOf("") }
                var videoUrl by remember { mutableStateOf("") }
                GlassTextField(value = videoTitle, onValueChange = { videoTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = videoDesc, onValueChange = { videoDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                LocalUploadField(value = videoUrl, onValueChange = { videoUrl = it }, label = "URL ou Arquivo Local MP4", mimeType = "video/*")
                
                if (isYoutubeUrl(videoUrl) || videoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prévia do vídeo", style = MaterialTheme.typography.labelMedium)
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f), shape = RoundedCornerShape(12.dp)) {
                        if (isYoutubeUrl(videoUrl)) {
                            YoutubeThumbnailImage(videoUrl = videoUrl)
                        } else {
                            coil.compose.AsyncImage(model = videoUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    if (videoTitle.isNotBlank()) Text(videoTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    if (videoDesc.isNotBlank()) Text(videoDesc, style = MaterialTheme.typography.bodySmall)
                }

                GlassButton(onClick = {
                    if (isUploading) return@GlassButton
                    isUploading = true
                    coroutineScope.launch {
                        try {
                            uploadProgress = 0f
                            val finalVideoUrl = if (videoUrl.isNotBlank() && !videoUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(videoUrl), "videos/files") { progress -> uploadProgress = progress } else convertGoogleDriveUrl(videoUrl).ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }
                            addVipVideo(ContentVideo(id = System.currentTimeMillis().toString(), title = videoTitle, description = videoDesc, videoUrl = finalVideoUrl, thumbnailUrl = ""))
                            videoTitle = ""
                            videoDesc = ""
                            videoUrl = ""
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Upload do vídeo não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Vídeo")
                }
            }
        }
        }
        
        if ((contentFilter == "Todos" || contentFilter == "Vídeos") && vipVideosState.isNotEmpty()) {
            Text("Vídeos Cadastrados", fontWeight = FontWeight.Bold)
            vipVideosState.forEach { video ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(video.title, fontWeight = FontWeight.Bold)
                        Text(video.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingVideo = video }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { videoToDelete = video }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    


        // ADD ALBUM
        if (addContentType == "Fotos") {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Álbum de Fotos", fontWeight = FontWeight.Bold)
                var albumTitle by remember { mutableStateOf("") }
                var albumDesc by remember { mutableStateOf("") }
                var albumDriveUrl by remember { mutableStateOf("") }
                var customCoverUrl by remember { mutableStateOf<String?>(null) }
                var isUploadingCover by remember { mutableStateOf(false) }
                var coverProgress by remember { mutableFloatStateOf(0f) }
                var isGenerating by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                val coverPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        isUploadingCover = true
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                coverProgress = 0f
                                val url = StorageHelper.uploadFile(context, uri, "covers", mimeTypeHint = "image/*") { progress -> coverProgress = progress }
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { customCoverUrl = url }
                            } catch (error: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "Upload da capa não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingCover = false }
                            }
                        }
                    }
                }

                GlassTextField(value = albumTitle, onValueChange = { albumTitle = it }, label = { Text("Título do Álbum") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = albumDesc, onValueChange = { albumDesc = it }, label = { Text("Descrição do Álbum") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = albumDriveUrl, onValueChange = { albumDriveUrl = it }, label = { Text("Link da Pasta/Fotos do Google Drive (Opcional)") }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { if (!isUploadingCover) coverPicker.launch("image/*") }) {
                        if (isUploadingCover) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text(if (customCoverUrl != null) "Capa Pronta" else "Selecionar Capa")
                        }
                    }
                    if (customCoverUrl != null) {
                        IconButton(onClick = { customCoverUrl = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Button(onClick = {
                    val finalDriveUrl = convertGoogleDriveUrl(albumDriveUrl)
                    if (customCoverUrl != null) {
                        addVipAlbum(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = customCoverUrl!!, photos = listOf(AlbumPhoto(url = customCoverUrl!!, caption = "")), driveFolderUrl = finalDriveUrl))
                        albumTitle = ""
                        albumDesc = ""
                        albumDriveUrl = ""
                        customCoverUrl = null
                    } else {
                        isGenerating = true
                        scope.launch {
                            val generatedCover = generatePlaceholderAlbumCover("A beautiful abstract aesthetic background suitable for a photo album cover titled '$albumTitle'. Minimalist, pastel colors.")
                            val finalCoverUrl = generatedCover ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" // Fallback se a API não estiver configurada
                            addVipAlbum(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = finalCoverUrl, driveFolderUrl = finalDriveUrl))
                            isGenerating = false
                            albumTitle = ""
                            albumDesc = ""
                            albumDriveUrl = ""
                            customCoverUrl = null
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp), enabled = !isGenerating && albumTitle.isNotBlank()) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerando Capa...")
                    } else {
                        Text("Criar Álbum")
                    }
                }
            }
        }
        }
        
        if ((contentFilter == "Todos" || contentFilter == "Fotos") && vipAlbumsState.isNotEmpty()) {
            Text("Álbuns Cadastrados", fontWeight = FontWeight.Bold)
            vipAlbumsState.forEach { album ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.title, fontWeight = FontWeight.Bold)
                        Text(album.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingAlbum = album }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { albumToDelete = album }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (audioToDelete != null) {
        AlertDialog(
            onDismissRequest = { audioToDelete = null },
            title = { Text("Excluir áudio?") },
            text = { Text("O áudio será removido do IBR e do Supabase quando estiver armazenado nele.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = audioToDelete ?: return@TextButton
                    coroutineScope.launch {
                        try {
                            StorageManager.deleteMediaAssetsFromReferences(context, listOf(target.audioUrl, target.coverUrl))
                            removeVipAudio(target)
                            audioToDelete = null
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Áudio não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { audioToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Excluir vídeo?") },
            text = { Text("O vídeo será removido do IBR e do Supabase quando estiver armazenado nele.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = videoToDelete ?: return@TextButton
                    coroutineScope.launch {
                        try {
                            StorageManager.deleteMediaAssetsFromReferences(context, listOf(target.videoUrl, target.thumbnailUrl))
                            removeVipVideo(target)
                            videoToDelete = null
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Vídeo não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { videoToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (albumToDelete != null) {
        val deleteScope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { if (!isDeleting) albumToDelete = null },
            title = { Text("Excluir Álbum") },
            text = { 
                if (isDeleting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Removendo álbum e ${albumToDelete!!.photos.size} fotos do armazenamento...")
                    }
                } else {
                    Text("Tem certeza que deseja excluir '${albumToDelete!!.title}'? Todas as imagens associadas serão apagadas do armazenamento.")
                }
            },
            confirmButton = {
                if (!isDeleting) {
                    TextButton(onClick = {
                        isDeleting = true
                        deleteScope.launch {
                            val target = albumToDelete!!
                            try {
                                val refs = buildList {
                                    target.coverUrl?.let { add(it) }
                                    addAll(target.photos.map { it.url })
                                }
                                StorageManager.deleteMediaAssetsFromReferences(context, refs)
                                removeVipAlbum(target)
                                albumToDelete = null
                                android.widget.Toast.makeText(context, "Álbum IBR e arquivos excluídos.", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Álbum não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                if (!isDeleting) {
                    TextButton(onClick = { albumToDelete = null }) { Text("Cancelar") }
                }
            }
        )
    }

    // EDIT DIALOGS
    if (editingBook != null) {
        var editTitle by remember(editingBook) { mutableStateOf(editingBook!!.title) }
        var editAuthor by remember(editingBook) { mutableStateOf(editingBook!!.author) }
        var editCoverUrl by remember(editingBook) { mutableStateOf(editingBook!!.coverUrl) }
        var editContent by remember(editingBook) { mutableStateOf(editingBook!!.contentText) }
        var editBookUrl by remember(editingBook) { mutableStateOf(editingBook!!.bookUrl) }
        
        AlertDialog(
            onDismissRequest = { editingBook = null },
            title = { Text("Editar Livro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("Autor") })
                    LocalUploadField(value = editCoverUrl, onValueChange = { editCoverUrl = it }, label = "Capa do Livro (URL)", mimeType = "image/*")
                    GlassTextField(value = editContent, onValueChange = { editContent = it }, label = { Text("Conteúdo") })
                    LocalUploadField(value = editBookUrl, onValueChange = { editBookUrl = it }, label = "Arquivo do Livro", mimeType = "*/*")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = vipBooksState.indexOfFirst { it.id == editingBook!!.id }
                    if (idx != -1) {
                        val updated = editingBook!!.copy(title = editTitle, author = editAuthor, coverUrl = convertGoogleDriveUrl(editCoverUrl), contentText = editContent, bookUrl = convertGoogleDriveUrl(editBookUrl))
                        addVipBook(updated)
                    }
                    editingBook = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingBook = null }) { Text("Cancelar") } }
        )
    }
    
    if (editingAudio != null) {
        var editTitle by remember(editingAudio) { mutableStateOf(editingAudio!!.title) }
        var editArtist by remember(editingAudio) { mutableStateOf(editingAudio!!.artist) }
        var editUrl by remember(editingAudio) { mutableStateOf(editingAudio!!.audioUrl) }
        
        AlertDialog(
            onDismissRequest = { editingAudio = null },
            title = { Text("Editar Áudio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artista") })
                    LocalUploadField(value = editUrl, onValueChange = { editUrl = it }, label = "URL ou Arquivo Local MP3", mimeType = "audio/*")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = vipAudiosState.indexOfFirst { it.id == editingAudio!!.id }
                    if (idx != -1) {
                        val updated = editingAudio!!.copy(title = editTitle, artist = editArtist, audioUrl = editUrl)
                        addVipAudio(updated)
                    }
                    editingAudio = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingAudio = null }) { Text("Cancelar") } }
        )
    }
    
    if (editingVideo != null) {
        var editTitle by remember(editingVideo) { mutableStateOf(editingVideo!!.title) }
        var editDesc by remember(editingVideo) { mutableStateOf(editingVideo!!.description) }
        var editUrl by remember(editingVideo) { mutableStateOf(editingVideo!!.videoUrl) }
        
        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Editar Vídeo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descrição") })
                    LocalUploadField(value = editUrl, onValueChange = { editUrl = it }, label = "URL ou Arquivo Local MP4", mimeType = "video/*")
                    
                    if (isYoutubeUrl(editUrl) || editUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Prévia do vídeo", style = MaterialTheme.typography.labelMedium)
                        Card(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f), shape = RoundedCornerShape(12.dp)) {
                            if (isYoutubeUrl(editUrl)) {
                                YoutubeThumbnailImage(videoUrl = editUrl)
                            } else {
                                coil.compose.AsyncImage(model = editUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                        if (editTitle.isNotBlank()) Text(editTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        if (editDesc.isNotBlank()) Text(editDesc, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = vipVideosState.indexOfFirst { it.id == editingVideo!!.id }
                    if (idx != -1) {
                        val updated = editingVideo!!.copy(title = editTitle, description = editDesc, videoUrl = editUrl)
                        addVipVideo(updated)
                    }
                    editingVideo = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingVideo = null }) { Text("Cancelar") } }
        )
    }
    if (editingAlbum != null) {
        var editTitle by remember(editingAlbum) { mutableStateOf(editingAlbum!!.title) }
        var editDesc by remember(editingAlbum) { mutableStateOf(editingAlbum!!.description) }
        var editDriveUrl by remember(editingAlbum) { mutableStateOf(editingAlbum!!.driveFolderUrl) }
        
        var photoUriInput by remember { mutableStateOf<android.net.Uri?>(null) }
        var isUploadingPhoto by remember { mutableStateOf(false) }
        var photoProgress by remember { mutableFloatStateOf(0f) }
        var newPhotoDriveUrl by remember { mutableStateOf("") }
        var photoToDelete by remember(editingAlbum) { mutableStateOf<AlbumPhoto?>(null) }
        var isDeletingPhoto by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                isUploadingPhoto = true
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        photoProgress = 0f
                        val url = StorageHelper.uploadFile(context, uri, "album_photos", mimeTypeHint = "image/*") { progress -> photoProgress = progress }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val updatedAlbum = editingAlbum!!.copy(
                                photos = editingAlbum!!.photos + AlbumPhoto(url = url, caption = ""),
                                coverUrl = editingAlbum!!.coverUrl ?: url
                            )
                            val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                            if (index != -1) {
                                vipAlbumsState[index] = updatedAlbum
                                addVipAlbum(updatedAlbum)
                                editingAlbum = updatedAlbum
                            }
                        }
                    } catch (error: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Upload da foto não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingPhoto = false }
                    }
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { editingAlbum = null },
            title = { Text("Editar Álbum") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).imePadding().verticalScroll(rememberScrollState())) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassTextField(value = editDriveUrl, onValueChange = { editDriveUrl = it }, label = { Text("Link Google Drive da Pasta/Álbum") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { photoPicker.launch("image/*") }) {
                            Text("Adicionar Foto do Dispositivo")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GlassTextField(
                            value = newPhotoDriveUrl, 
                            onValueChange = { newPhotoDriveUrl = it }, 
                            label = { Text("URL Google Drive da Foto") }, 
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newPhotoDriveUrl.isNotBlank()) {
                                val url = convertGoogleDriveUrl(newPhotoDriveUrl)
                                val updatedAlbum = editingAlbum!!.copy(
                                    photos = editingAlbum!!.photos + AlbumPhoto(url = url, caption = ""),
                                    coverUrl = editingAlbum!!.coverUrl ?: url
                                )
                                val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                if (index != -1) {
                                    vipAlbumsState[index] = updatedAlbum
                                    addVipAlbum(updatedAlbum)
                                    editingAlbum = updatedAlbum
                                }
                                newPhotoDriveUrl = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar Foto", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fotos no Álbum (${editingAlbum!!.photos.size}):", fontWeight = FontWeight.Bold)
                    editingAlbum!!.photos.forEach { photo ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                coil.compose.AsyncImage(
                                    model = photo.url,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                GlassTextField(
                                    value = photo.caption,
                                    onValueChange = { newCaption ->
                                        val updatedPhotos = editingAlbum!!.photos.map { if (it.url == photo.url) it.copy(caption = newCaption) else it }
                                        val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos)
                                        val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                        if (index != -1) {
                                            vipAlbumsState[index] = updatedAlbum
                                            addVipAlbum(updatedAlbum)
                                            addVipAlbum(updatedAlbum)
                                            editingAlbum = updatedAlbum
                                        }
                                    },
                                    label = { Text("Legenda") },
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { photoToDelete = photo }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover Foto", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                    if (index != -1) {
                        val updated = editingAlbum!!.copy(title = editTitle, description = editDesc, driveFolderUrl = convertGoogleDriveUrl(editDriveUrl))
                        vipAlbumsState[index] = updated
                        addVipAlbum(updated)
                    }
                    editingAlbum = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editingAlbum = null }) { Text("Cancelar") }
            }
        )

        if (photoToDelete != null) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingPhoto) photoToDelete = null },
                title = { Text("Excluir foto do IBR?") },
                text = { Text("A foto será removida do álbum e do Supabase quando pertencer ao armazenamento do MIC Rhema.") },
                confirmButton = {
                    TextButton(enabled = !isDeletingPhoto, onClick = {
                        val targetPhoto = photoToDelete ?: return@TextButton
                        isDeletingPhoto = true
                        scope.launch {
                            try {
                                StorageManager.deleteMediaAssetIfSupabase(context, targetPhoto.url)
                                val updatedPhotos = editingAlbum!!.photos.filter { it.url != targetPhoto.url }
                                val updatedCover = if (editingAlbum!!.coverUrl == targetPhoto.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                val index = vipAlbumsState.indexOfFirst { it.id == updatedAlbum.id }
                                if (index >= 0) vipAlbumsState[index] = updatedAlbum
                                addVipAlbum(updatedAlbum)
                                editingAlbum = updatedAlbum
                                photoToDelete = null
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Foto não excluída: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeletingPhoto = false
                            }
                        }
                    }) { Text(if (isDeletingPhoto) "Excluindo..." else "Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(enabled = !isDeletingPhoto, onClick = { photoToDelete = null }) { Text("Cancelar") } }
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVipIbrSection() {
    val context = LocalContext.current
    
    // Add Course Form States
    var courseTheme by remember { mutableStateOf("Teologia") }
    var courseTitle by remember { mutableStateOf("") }
    var courseDescription by remember { mutableStateOf("") }
    var courseImageUrl by remember { mutableStateOf("") }
    var showNewCourseCategory by remember { mutableStateOf(false) }
    var newCourseCategory by remember { mutableStateOf("") }
    var showCreateCourseForm by remember { mutableStateOf(false) }
    var showAddChapterForm by remember { mutableStateOf(false) }
    var expandedCourseIds by remember { mutableStateOf(setOf<String>()) }
    

    // Add Chapter Form States
    var editingCourse by remember { mutableStateOf<IbrCourse?>(null) }
    var editingChapter by remember { mutableStateOf<IbrChapter?>(null) }
    var courseToDelete by remember { mutableStateOf<IbrCourse?>(null) }
    var chapterToDelete by remember { mutableStateOf<Pair<IbrCourse, IbrChapter>?>(null) }

    var selectedCourseForChapter by remember { mutableStateOf<IbrCourse?>(null) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterDescription by remember { mutableStateOf("") }
    var chapterDuration by remember { mutableStateOf("30") }
    var chapterType by remember { mutableStateOf("VIDEO") } // VIDEO, AUDIO, TEXT
    var chapterAccessMode by remember { mutableStateOf(IBR_LESSON_FREE) }
    var isYoutube by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var studyPdfUrl by remember { mutableStateOf("") }
    var studyDocxUrl by remember { mutableStateOf("") }
    
    var courseSearch by remember { mutableStateOf("") }
    var courseThemeFilter by remember { mutableStateOf("Todos") }
    val courseThemes = listOf("Todos") + (ibrCourseCategoriesState + ibrCoursesState.map { it.theme })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }
    val visibleCourses = ibrCoursesState
        .filter { course ->
            val query = courseSearch.trim()
            (query.isBlank() || course.title.contains(query, ignoreCase = true) || course.description.contains(query, ignoreCase = true)) &&
                (courseThemeFilter == "Todos" || course.theme == courseThemeFilter)
        }
        .sortedBy { it.title.lowercase() }

    LaunchedEffect(Unit) {
        loadIbrCourseCategories()
    }

    LaunchedEffect(ibrCoursesState.size) {
        if (selectedCourseForChapter == null || ibrCoursesState.none { it.id == selectedCourseForChapter?.id }) {
            selectedCourseForChapter = ibrCoursesState.firstOrNull()
        }
    }

    LazyColumn(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section Title
        item {
            Column {
                Text(
                    text = "IBR - Cursos Exclusivos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cursos e aulas do IBR, organizados de forma simples.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showCreateCourseForm = !showCreateCourseForm; if (showCreateCourseForm) showAddChapterForm = false },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (showCreateCourseForm) "Fechar" else "+ Curso") }
                    Button(
                        onClick = { showAddChapterForm = !showAddChapterForm; if (showAddChapterForm) showCreateCourseForm = false },
                        modifier = Modifier.weight(1f),
                        enabled = ibrCoursesState.isNotEmpty()
                    ) { Text(if (showAddChapterForm) "Fechar" else "+ Aula") }
                }
            }
        }

        // 1. CREATE NEW COURSE CARD
        if (showCreateCourseForm) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🆕 Criar Novo Curso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    GlassTextField(
                        value = courseTitle,
                        onValueChange = { courseTitle = it },
                        label = { Text("Título do Curso") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )

                    GlassTextField(
                        value = courseDescription,
                        onValueChange = { courseDescription = it },
                        label = { Text("Descrição Curta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )
                    LocalUploadField(
                        value = courseImageUrl,
                        onValueChange = { courseImageUrl = it },
                        label = "Capa do curso (opcional)",
                        mimeType = "image/*"
                    )

                    // Theme selector chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tema / Categoria:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ibrCourseCategoriesState.forEach { theme ->
                                FilterChip(
                                    selected = courseTheme == theme,
                                    onClick = { courseTheme = theme },
                                    label = { Text(theme) }
                                )
                            }
                            AssistChip(onClick = { showNewCourseCategory = !showNewCourseCategory }, label = { Text("+") })
                        }
                        if (showNewCourseCategory) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextField(
                                    value = newCourseCategory,
                                    onValueChange = { newCourseCategory = it },
                                    label = { Text("Nova categoria") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val name = newCourseCategory.trim()
                                    if (name.isNotBlank()) {
                                        saveIbrCourseCategory(name)
                                        courseTheme = name
                                        newCourseCategory = ""
                                        showNewCourseCategory = false
                                    }
                                }) { Text("Salvar") }
                            }
                        }
                    }

                    GlassButton(
                        onClick = {
                            if (courseTitle.isNotBlank()) {
                                val newCourse = IbrCourse(
                                    id = "course_${System.currentTimeMillis()}",
                                    title = courseTitle,
                                    description = courseDescription,
                                    theme = courseTheme,
                                    imageUrl = courseImageUrl.trim(),
                                    chapters = mutableStateListOf()
                                )
                                addIbrCourse(newCourse)
                                NotificationHelper.showNotification(
                                    context,
                                    "Curso Criado! 🎓",
                                    "O curso '$courseTitle' foi adicionado com sucesso."
                                )
                                courseTitle = ""
                                courseDescription = ""
                                courseImageUrl = ""
                                showCreateCourseForm = false
                            } else {
                                NotificationHelper.showNotification(context, "Erro", "Preencha o título do curso.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Adicionar Curso", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }

        // 2. ADD CHAPTER CARD (Only shown if courses exist)
        if (ibrCoursesState.isNotEmpty() && showAddChapterForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("➕ Adicionar Aula/Capítulo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        
                        // Selected Course picker
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Selecionar Curso Destino:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ibrCoursesState.forEach { c ->
                                    FilterChip(
                                        selected = selectedCourseForChapter?.id == c.id,
                                        onClick = { selectedCourseForChapter = c },
                                        label = { Text(c.title, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        GlassTextField(
                            value = chapterTitle,
                            onValueChange = { chapterTitle = it },
                            label = { Text("Título da Aula") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        GlassTextField(
                            value = chapterDescription,
                            onValueChange = { chapterDescription = it },
                            label = { Text("Descrição / Conteúdo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Text("Tipo da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("VIDEO" to "Vídeo", "AUDIO" to "Áudio", "TEXT" to "Texto").forEach { (id, title) ->
                                FilterChip(
                                    selected = chapterType == id,
                                    onClick = {
                                        chapterType = id
                                        if (id != "VIDEO") isYoutube = false
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }

                        GlassTextField(
                            value = chapterDuration,
                            onValueChange = { chapterDuration = it },
                            label = { Text("Duração (minutos)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Text("Liberação da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(IBR_LESSON_FREE to "Livre", IBR_LESSON_AFTER_PREVIOUS to "Após aula anterior", IBR_LESSON_MANUAL_LOCKED to "Bloqueada").forEach { (id, title) ->
                                FilterChip(selected = chapterAccessMode == id, onClick = { chapterAccessMode = id }, label = { Text(title) })
                            }
                        }
                        Text(
                            when (chapterAccessMode) {
                                IBR_LESSON_AFTER_PREVIOUS -> "Libera automaticamente quando a aula anterior for concluída."
                                IBR_LESSON_MANUAL_LOCKED -> "Fica bloqueada até o ADM mudar esta opção."
                                else -> "Pode ser aberta a qualquer momento dentro do módulo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        when (chapterType) {
                            "VIDEO" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vídeo do YouTube", style = MaterialTheme.typography.labelMedium)
                                    Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                                }
                                if (isYoutube) {
                                    GlassTextField(
                                        value = videoUrl,
                                        onValueChange = { videoUrl = it },
                                        label = { Text("Link do YouTube") },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("https://youtube.com/watch?v=...") }
                                    )
                                } else {
                                    LocalUploadField(
                                        value = videoUrl,
                                        onValueChange = { videoUrl = it },
                                        label = "Vídeo da aula",
                                        mimeType = "video/*"
                                    )
                                }
                            }
                            "AUDIO" -> LocalUploadField(
                                value = audioUrl,
                                onValueChange = { audioUrl = it },
                                label = "Áudio da aula",
                                mimeType = "audio/*"
                            )
                            else -> GlassTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                label = { Text("Texto da aula") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                maxLines = 10
                            )
                        }

                        LocalUploadField(
                            value = studyPdfUrl,
                            onValueChange = { studyPdfUrl = it },
                            label = "Conteúdo para estudo — PDF ou link do Drive (opcional)",
                            mimeType = "application/pdf"
                        )
                        LocalUploadField(
                            value = studyDocxUrl,
                            onValueChange = { studyDocxUrl = it },
                            label = "Conteúdo para estudo — Word .docx (opcional)",
                            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                        Text(
                            "PDF e Word aparecerão abaixo da aula para o aluno abrir ou baixar quando estiverem disponíveis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        GlassButton(
                            onClick = {
                                if (chapterTitle.isNotBlank() && selectedCourseForChapter != null) {
                                    val duration = chapterDuration.toIntOrNull() ?: 30
                                    val detectedYoutubeId = if (chapterType == "VIDEO") extractYouTubeVideoId(videoUrl).orEmpty() else ""
                                    val detectedYoutube = chapterType == "VIDEO" && (isYoutube || detectedYoutubeId.isNotBlank() || isYoutubeUrl(videoUrl))
                                    val newChapter = IbrChapter(
                                        id = "chap_${System.currentTimeMillis()}",
                                        title = chapterTitle,
                                        description = chapterDescription,
                                        durationMinutes = duration,
                                        type = chapterType,
                                        videoUrl = if (chapterType == "VIDEO") videoUrl else "",
                                        audioUrl = if (chapterType == "AUDIO") audioUrl else "",
                                        textContent = if (chapterType == "TEXT") textContent else "",
                                        studyPdfUrl = studyPdfUrl.trim(),
                                        studyDocxUrl = studyDocxUrl.trim(),
                                        accessMode = chapterAccessMode,
                                        isYoutube = detectedYoutube,
                                        youtubeId = detectedYoutubeId
                                    )
                                    // Add to the selected course chapters list
                                    val targetCourse = ibrCoursesState.find { it.id == selectedCourseForChapter!!.id }
                                    if (targetCourse != null) {
                                        val updatedChapters = targetCourse.chapters.toMutableList().apply { add(newChapter) }
                                        val updatedCourse = targetCourse.copy(chapters = updatedChapters)
                                        val index = ibrCoursesState.indexOf(targetCourse)
                                        if (index != -1) {
                                            addIbrCourse(updatedCourse)
                                        }
                                        selectedCourseForChapter = updatedCourse
                                    }
                                    
                                    NotificationHelper.showNotification(
                                        context,
                                        "Aula Adicionada! 🎓",
                                        "A aula '$chapterTitle' foi adicionada ao curso '${targetCourse?.title}'"
                                    )
                                    // Clear form
                                    chapterTitle = ""
                                    chapterDescription = ""
                                    videoUrl = ""
                                    audioUrl = ""
                                    textContent = ""
                                    chapterType = "VIDEO"
                                    chapterAccessMode = IBR_LESSON_FREE
                                    studyPdfUrl = ""
                                    studyDocxUrl = ""
                                    isYoutube = false
                                    showAddChapterForm = false
                                } else {
                                    NotificationHelper.showNotification(context, "Erro", "Preencha o título da aula.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Adicionar Aula", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        
        // Upload em lote removido: o painel utiliza os campos de upload reais acima,
        // evitando criar aulas fictícias ou URLs de demonstração.
        // 3. LIST OF EXISTING COURSES AND CHAPTERS
        item {
            Text("📚 Cursos e Aulas Ativas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                value = courseSearch,
                onValueChange = { courseSearch = it },
                label = { Text("Buscar curso ou descrição") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                courseThemes.forEach { theme ->
                    FilterChip(
                        selected = courseThemeFilter == theme,
                        onClick = { courseThemeFilter = theme },
                        label = { Text(theme) }
                    )
                }
            }
            Text(
                "${visibleCourses.size} curso(s) exibido(s) • ordem alfabética",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (visibleCourses.isEmpty()) {
            item {
                Text(
                    if (ibrCoursesState.isEmpty()) "Nenhum curso ou aula cadastrado ainda." else "Nenhum curso corresponde à busca ou ao tema selecionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            items(visibleCourses) { course ->
                val expanded = expandedCourseIds.contains(course.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(course.theme.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${course.chapters.size} aula(s) • ${course.chapters.sumOf { it.durationMinutes }} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = {
                                    expandedCourseIds = if (expanded) expandedCourseIds - course.id else expandedCourseIds + course.id
                                }) {
                                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Recolher" else "Abrir")
                                }
                                IconButton(onClick = { editingCourse = course }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar Curso", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { courseToDelete = course }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar Curso", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (expanded) {
                        Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("Aulas (${course.chapters.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        
                        if (course.chapters.isEmpty()) {
                            Text("Sem aulas cadastradas neste curso.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            course.chapters.forEachIndexed { idx, ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ch.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else if (ch.audioUrl.isNotEmpty()) "Áudio 🎵" else "Texto 📖"} • ${ibrChapterAccessLabel(ch.accessMode)}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}${if (ch.studyDocxUrl.isNotBlank()) " • Word 📝" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingCourse = course
                                                editingChapter = ch
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Aula", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { chapterToDelete = course to ch },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Deletar Aula", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }

    }

    if (courseToDelete != null) {
        val target = courseToDelete!!
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Excluir curso?") },
            text = { Text("O curso '${target.title}' e suas aulas serão removidos do IBR.") },
            confirmButton = {
                TextButton(onClick = {
                    removeIbrCourse(target)
                    courseToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (chapterToDelete != null) {
        val (targetCourse, targetChapter) = chapterToDelete!!
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Excluir aula?") },
            text = { Text("A aula '${targetChapter.title}' será removida deste curso.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = targetCourse.copy(chapters = targetCourse.chapters.filterNot { it.id == targetChapter.id })
                    val index = ibrCoursesState.indexOfFirst { it.id == targetCourse.id }
                    if (index >= 0) ibrCoursesState[index] = updated
                    saveIbrCourseSilently(updated)
                    chapterToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { chapterToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (editingCourse != null && editingChapter == null) {
        var editTitle by remember(editingCourse) { mutableStateOf(editingCourse!!.title) }
        var editDescription by remember(editingCourse) { mutableStateOf(editingCourse!!.description) }
        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }
        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }
        var editCourseAccessMode by remember(editingCourse) { mutableStateOf(editingCourse!!.accessMode.ifBlank { IBR_COURSE_AUTO }.uppercase()) }
        var showNewEditCategory by remember(editingCourse) { mutableStateOf(false) }
        var newEditCategory by remember(editingCourse) { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { editingCourse = null },
            title = { Text("Editar Curso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    Text("Tema / Categoria", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (ibrCourseCategoriesState + listOf(editTheme)).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach { theme ->
                            FilterChip(selected = editTheme == theme, onClick = { editTheme = theme }, label = { Text(theme) })
                        }
                        AssistChip(onClick = { showNewEditCategory = !showNewEditCategory }, label = { Text("+") })
                    }
                    if (showNewEditCategory) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlassTextField(value = newEditCategory, onValueChange = { newEditCategory = it }, label = { Text("Nova categoria") }, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val name = newEditCategory.trim()
                                if (name.isNotBlank()) {
                                    saveIbrCourseCategory(name)
                                    editTheme = name
                                    newEditCategory = ""
                                    showNewEditCategory = false
                                }
                            }) { Text("Salvar") }
                        }
                    }
                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")
                    Text("Acesso ao curso", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(IBR_COURSE_AUTO to "Automático", IBR_COURSE_UNLOCKED to "Desbloqueado", IBR_COURSE_LOCKED to "Bloqueado").forEach { (id, title) ->
                            FilterChip(selected = editCourseAccessMode == id, onClick = { editCourseAccessMode = id }, label = { Text(title) })
                        }
                    }
                    Text(
                        when (editCourseAccessMode) {
                            IBR_COURSE_UNLOCKED -> "Pode ser aberto mesmo antes do módulo anterior ser concluído."
                            IBR_COURSE_LOCKED -> "Fica bloqueado até o ADM desbloquear."
                            else -> "Mantém a regra atual: libera após concluir o módulo anterior."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = ibrCoursesState.indexOfFirst { it.id == editingCourse!!.id }
                    if (idx != -1) {
                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme, imageUrl = editImageUrl.trim(), accessMode = editCourseAccessMode)
                        ibrCoursesState[idx] = updated
                        saveIbrCourseSilently(updated)
                    }
                    editingCourse = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingCourse = null }) { Text("Cancelar") } }
        )
    }

    if (editingCourse != null && editingChapter != null) {
        var editTitle by remember(editingChapter) { mutableStateOf(editingChapter!!.title) }
        var editDescription by remember(editingChapter) { mutableStateOf(editingChapter!!.description) }
        var editDuration by remember(editingChapter) { mutableStateOf(editingChapter!!.durationMinutes.toString()) }
        var editType by remember(editingChapter) { mutableStateOf(editingChapter!!.type.ifBlank { "VIDEO" }.uppercase()) }
        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }
        var editAudioUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.audioUrl) }
        var editTextContent by remember(editingChapter) { mutableStateOf(editingChapter!!.textContent) }
        var editStudyPdfUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyPdfUrl) }
        var editStudyDocxUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyDocxUrl) }
        var editAccessMode by remember(editingChapter) { mutableStateOf(editingChapter!!.accessMode.ifBlank { IBR_LESSON_FREE }.uppercase()) }
        
        AlertDialog(
            onDismissRequest = {
                editingChapter = null
                editingCourse = null
            },
            title = { Text("Editar Aula") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).imePadding().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })
                    Text("Tipo da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("VIDEO" to "Vídeo", "AUDIO" to "Áudio", "TEXT" to "Texto").forEach { (id, title) ->
                            FilterChip(selected = editType == id, onClick = { editType = id }, label = { Text(title) })
                        }
                    }
                    Text("Liberação da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(IBR_LESSON_FREE to "Livre", IBR_LESSON_AFTER_PREVIOUS to "Após anterior", IBR_LESSON_MANUAL_LOCKED to "Bloqueada").forEach { (id, title) ->
                            FilterChip(selected = editAccessMode == id, onClick = { editAccessMode = id }, label = { Text(title) })
                        }
                    }
                    when (editType) {
                        "VIDEO" -> LocalUploadField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = "Vídeo ou link do YouTube", mimeType = "video/*")
                        "AUDIO" -> LocalUploadField(value = editAudioUrl, onValueChange = { editAudioUrl = it }, label = "Áudio da aula", mimeType = "audio/*")
                        else -> GlassTextField(value = editTextContent, onValueChange = { editTextContent = it }, label = { Text("Texto da aula") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 10)
                    }
                    LocalUploadField(
                        value = editStudyPdfUrl,
                        onValueChange = { editStudyPdfUrl = it },
                        label = "Conteúdo para estudo — PDF ou link do Drive",
                        mimeType = "application/pdf"
                    )
                    LocalUploadField(
                        value = editStudyDocxUrl,
                        onValueChange = { editStudyDocxUrl = it },
                        label = "Conteúdo para estudo — Word .docx",
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val courseIdx = ibrCoursesState.indexOfFirst { it.id == editingCourse!!.id }
                    if (courseIdx != -1) {
                        val course = ibrCoursesState[courseIdx]
                        val chapterIdx = course.chapters.indexOfFirst { it.id == editingChapter!!.id }
                        if (chapterIdx != -1) {
                            val updatedChapters = course.chapters.toMutableList()
                            val isYt = editType == "VIDEO" && isYoutubeUrl(editVideoUrl)
                            val ytId = if (editType == "VIDEO") extractYouTubeVideoId(editVideoUrl) ?: "" else ""
                            updatedChapters[chapterIdx] = editingChapter!!.copy(
                                title = editTitle,
                                description = editDescription,
                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,
                                type = editType,
                                videoUrl = if (editType == "VIDEO") editVideoUrl else "",
                                audioUrl = if (editType == "AUDIO") editAudioUrl else "",
                                textContent = if (editType == "TEXT") editTextContent else "",
                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                accessMode = editAccessMode,
                                isYoutube = isYt,
                                youtubeId = ytId
                            )
                            val updatedCourse = course.copy(chapters = updatedChapters)
                            ibrCoursesState[courseIdx] = updatedCourse
                            saveIbrCourseSilently(updatedCourse)
                        }
                    }
                    editingChapter = null
                    editingCourse = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingChapter = null
                    editingCourse = null
                }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIbrCertificatesSection() {
    val context = LocalContext.current
    var uploadCertificateForUser by remember { mutableStateOf<MemberRequest?>(null) }
    var uploadUrl by remember { mutableStateOf("") }
    var emailingMemberId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val totalIbrCourses = ibrCoursesState.count { it.chapters.isNotEmpty() }
    val totalIbrLessons = ibrCoursesState.sumOf { it.chapters.size }
    val progressByMember = rememberIbrProgressByMember()
    val eligibleUsers = memberRequestsState.filter { member ->
        member.isIbr && isIbrCompleteFor(progressByMember[member.id].orEmpty())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Certificados IBR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Alunos que concluíram 100% do IBR ($totalIbrCourses módulos).", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (eligibleUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum aluno atingiu 100% de conclusão.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(eligibleUsers) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(member.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(member.email.ifEmpty { "Sem e-mail" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (member.ibrCertificateUrl.isNotEmpty() || member.ibrCertificateStoragePath.isNotEmpty()) {
                                    Icon(Icons.Default.Verified, contentDescription = "Certificado Enviado", tint = Color(0xFF4CAF50))
                                } else {
                                    Icon(Icons.Default.PendingActions, contentDescription = "Pendente", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            val memberProgress = progressByMember[member.id].orEmpty()
                            val completedLessons = memberProgress.count { it.isCompleted }
                            val completedCourses = completedIbrCourseCountFor(memberProgress)
                            val memberBadge = biblicalBadgeForId(member.equippedBadgeId)
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Nível ${memberBadge.level ?: 1}: ${memberBadge.name}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "$completedCourses/$totalIbrCourses cursos concluídos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text("$completedLessons/$totalIbrLessons aulas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (member.ibrCertificateUrl.isNotEmpty() || member.ibrCertificateStoragePath.isNotEmpty()) {
                                Text("Certificado enviado:", style = MaterialTheme.typography.labelSmall)
                                Text(member.ibrCertificateUrl.ifBlank { member.ibrCertificateStoragePath }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            val hasCertificate = member.ibrCertificateStoragePath.isNotBlank() || member.ibrCertificateUrl.startsWith("http://") || member.ibrCertificateUrl.startsWith("https://")
                            Button(onClick = {
                                uploadCertificateForUser = member
                                uploadUrl = member.ibrCertificateStoragePath.ifBlank { member.ibrCertificateUrl }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (!hasCertificate) "Fazer Upload do Certificado" else "Alterar Certificado")
                            }

                            if (hasCertificate) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val recipient = member.email.trim()
                                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipient).matches()) {
                                            Toast.makeText(context, "Informe um e-mail válido no perfil deste membro antes de enviar.", Toast.LENGTH_LONG).show()
                                        } else if (emailingMemberId == null) {
                                            emailingMemberId = member.id
                                            scope.launch {
                                                try {
                                                    val emailIntent = if (member.ibrCertificateStoragePath.isNotBlank()) {
                                                        val certificateFile = StorageManager.downloadStorageFileToCache(
                                                            context = context,
                                                            bucket = "church-documents",
                                                            storagePath = member.ibrCertificateStoragePath,
                                                            targetUid = member.id
                                                        )
                                                        CertificateEmailShare.createIntent(context, recipient, certificateFile)
                                                    } else {
                                                        CertificateEmailShare.createLinkIntent(recipient, member.ibrCertificateUrl)
                                                    }
                                                    if (emailIntent.resolveActivity(context.packageManager) == null) {
                                                        Toast.makeText(context, "Nenhum aplicativo de e-mail disponível neste aparelho.", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        context.startActivity(Intent.createChooser(emailIntent, "Enviar certificado por e-mail"))
                                                        Toast.makeText(context, "E-mail preparado. Toque em Enviar no aplicativo de e-mail.", Toast.LENGTH_LONG).show()
                                                    }
                                                } catch (error: Exception) {
                                                    Toast.makeText(context, "Não foi possível preparar o certificado: ${error.message ?: "verifique sua conexão"}", Toast.LENGTH_LONG).show()
                                                } finally {
                                                    emailingMemberId = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = emailingMemberId == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (emailingMemberId == member.id) "Preparando certificado..." else "Enviar certificado por e-mail")
                                }
                                if (member.email.isBlank()) {
                                    Text(
                                        "Informe o e-mail no perfil do membro para habilitar o envio.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (uploadCertificateForUser != null) {
        AlertDialog(
            onDismissRequest = { uploadCertificateForUser = null },
            title = { Text("Certificado para ${uploadCertificateForUser!!.name}") },
            text = {
                Column {
                    LocalUploadField(
                        value = uploadUrl,
                        onValueChange = { uploadUrl = it },
                        label = "Upload de PDF",
                        mimeType = "application/pdf",
                        targetUid = uploadCertificateForUser!!.id
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val storagePath = uploadUrl.takeIf { it.startsWith("church-documents/") }.orEmpty()
                    val legacyUrl = uploadUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty()
                    val updatedMember = uploadCertificateForUser!!.copy(
                        ibrCertificateUrl = if (storagePath.isNotBlank()) "" else legacyUrl,
                        ibrCertificateStoragePath = storagePath
                    )
                    val index = memberRequestsState.indexOfFirst { it.id == updatedMember.id }
                    if (index != -1) {
                        memberRequestsState[index] = updatedMember
                        MemberManager.saveToFirestore(context, updatedMember, 
                            onSuccess = { 
                                android.widget.Toast.makeText(context, "Certificado salvo!", android.widget.Toast.LENGTH_SHORT).show() 
                            }
                        )
                    }
                    uploadCertificateForUser = null
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { uploadCertificateForUser = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
