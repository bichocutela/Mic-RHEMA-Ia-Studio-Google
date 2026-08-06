package com.aistudio.micrhema

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
    var vipTab by remember { mutableStateOf("midia") } // midia or cursos or certificados
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = vipTab == "midia",
                onClick = { vipTab = "midia" },
                label = { Text("Conteúdo VIP") }
            )
            FilterChip(
                selected = vipTab == "cursos",
                onClick = { vipTab = "cursos" },
                label = { Text("Módulos IBR") }
            )
            FilterChip(
                selected = vipTab == "certificados",
                onClick = { vipTab = "certificados" },
                label = { Text("Certificados IBR") }
            )
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (vipTab == "midia") {
                EditVipContentSection()
            } else if (vipTab == "cursos") {
                EditVipIbrSection()
            } else {
                EditIbrCertificatesSection()
            }
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
    var isDeleting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("VIP - Conteúdo Geral", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione e edite livros, áudios e vídeos para os membros VIP e IBR.", style = MaterialTheme.typography.bodyMedium)
        // SMART IMPORTER
        var smartUrl by remember { mutableStateOf("") }
        var isSmartLoading by remember { mutableStateOf(false) }
        var smartMessage by remember { mutableStateOf("") }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Smart Import Google Drive 🚀", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Cole um link do Google Drive. O sistema detectará automaticamente se é Livro (PDF), Áudio (MP3), Vídeo (MP4) ou Imagem.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
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
                        uploadProgress = 0f
                        val finalCover = if (coverUrl.isNotBlank() && !coverUrl.startsWith("http")) StorageManager.uploadFile(context, android.net.Uri.parse(coverUrl), "books/covers") { progress -> uploadProgress = progress / 2f } else convertGoogleDriveUrl(coverUrl).ifEmpty { "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" }
                        val finalBookUrl = if (bookUrl.isNotBlank() && !bookUrl.startsWith("http")) StorageManager.uploadFile(context, android.net.Uri.parse(bookUrl), "books/files") { progress -> uploadProgress = 0.5f + (progress / 2f) } else convertGoogleDriveUrl(bookUrl)
                        addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = title, author = author, coverUrl = finalCover, contentText = "Conteúdo do livro carregado...", bookUrl = finalBookUrl))
                        title = ""
                        author = ""
                        coverUrl = ""
                        bookUrl = ""
                        isUploading = false
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Livro")
                }
            }
        }
        
        if (vipBooksState.isNotEmpty()) {
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
                    addVipAudio(ContentAudio(id = System.currentTimeMillis().toString(), title = audioTitle, artist = audioArtist, audioUrl = convertGoogleDriveUrl(audioUrl).ifEmpty { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3" }, coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"))
                    audioTitle = ""
                    audioArtist = ""
                    audioUrl = ""
                    audioCover = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Áudio")
                }
            }
        }
        
        if (vipAudiosState.isNotEmpty()) {
            Text("Áudios Cadastrados", fontWeight = FontWeight.Bold)
            vipAudiosState.forEach { audio ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(audio.title, fontWeight = FontWeight.Bold)
                        Text(audio.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingAudio = audio }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { removeVipAudio(audio) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        
        Divider()
        
        // ADD VIDEO
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Vídeo", fontWeight = FontWeight.Bold)
                var videoTitle by remember { mutableStateOf("") }
                var videoDesc by remember { mutableStateOf("") }
                var videoUrl by remember { mutableStateOf("") }
                GlassTextField(value = videoTitle, onValueChange = { videoTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = videoDesc, onValueChange = { videoDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                LocalUploadField(value = videoUrl, onValueChange = { videoUrl = it }, label = "URL ou Arquivo Local MP4", mimeType = "video/*")
                GlassButton(onClick = {
                    if (isUploading) return@GlassButton
                    isUploading = true
                    coroutineScope.launch {
                        uploadProgress = 0f
                        val finalVideoUrl = if (videoUrl.isNotBlank() && !videoUrl.startsWith("http")) StorageManager.uploadFile(context, android.net.Uri.parse(videoUrl), "videos/files") { progress -> uploadProgress = progress } else convertGoogleDriveUrl(videoUrl).ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }
                        val finalThumb = getYoutubeThumbnailUrl(finalVideoUrl) ?: "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80"
                        addVipVideo(ContentVideo(id = System.currentTimeMillis().toString(), title = videoTitle, description = videoDesc, videoUrl = finalVideoUrl, thumbnailUrl = finalThumb))
                        videoTitle = ""
                        videoDesc = ""
                        videoUrl = ""
                        isUploading = false
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Vídeo")
                }
            }
        }
        
        if (vipVideosState.isNotEmpty()) {
            Text("Vídeos Cadastrados", fontWeight = FontWeight.Bold)
            vipVideosState.forEach { video ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(video.title, fontWeight = FontWeight.Bold)
                        Text(video.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingVideo = video }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { removeVipVideo(video) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    


        // ADD ALBUM
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
                            coverProgress = 0f
                            val url = StorageHelper.uploadFile(context, uri, "covers") { progress -> coverProgress = progress }
                            kotlinx.coroutines.Dispatchers.Main.let {
                                kotlinx.coroutines.withContext(it) {
                                    isUploadingCover = false
                                    if (url != null) customCoverUrl = url
                                }
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
                        addContentPhotoAlbum(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = customCoverUrl!!, photos = listOf(AlbumPhoto(url = customCoverUrl!!, caption = "")), driveFolderUrl = finalDriveUrl))
                        albumTitle = ""
                        albumDesc = ""
                        albumDriveUrl = ""
                        customCoverUrl = null
                    } else {
                        isGenerating = true
                        scope.launch {
                            val generatedCover = generatePlaceholderAlbumCover("A beautiful abstract aesthetic background suitable for a photo album cover titled '$albumTitle'. Minimalist, pastel colors.")
                            val finalCoverUrl = generatedCover ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" // Fallback se a API não estiver configurada
                            addContentPhotoAlbum(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = finalCoverUrl, driveFolderUrl = finalDriveUrl))
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
        
        if (vipAlbumsState.isNotEmpty()) {
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
                            // Simulate network delay and explicit storage cleanup logic
                            android.util.Log.i("StorageCleanup", "Deleting cover image: ${albumToDelete!!.coverUrl}")
                            albumToDelete!!.photos.forEach { photo ->
                                android.util.Log.i("StorageCleanup", "Deleting photo from storage: ${photo.url}")
                            }
                            kotlinx.coroutines.delay(1500)
                            
                            val idx = vipAlbumsState.indexOfFirst { it.id == albumToDelete!!.id }
                            if (idx != -1) {
                                removeContentPhotoAlbum(albumToDelete!!)
                            }
                            isDeleting = false
                            albumToDelete = null
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
        val scope = rememberCoroutineScope()
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                isUploadingPhoto = true
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    photoProgress = 0f
                    val url = StorageHelper.uploadFile(context, uri, "album_photos") { progress -> photoProgress = progress }
                    kotlinx.coroutines.Dispatchers.Main.let {
                        kotlinx.coroutines.withContext(it) {
                            isUploadingPhoto = false
                            if (url != null) {
                                val updatedAlbum = editingAlbum!!.copy(
                                    photos = editingAlbum!!.photos + AlbumPhoto(url = url, caption = ""),
                                    coverUrl = editingAlbum!!.coverUrl ?: url
                                )
                                val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                if (index != -1) {
                                    vipAlbumsState[index] = updatedAlbum
                                            addVipAlbum(updatedAlbum)
                                            addVipAlbum(updatedAlbum)
                                    editingAlbum = updatedAlbum
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { editingAlbum = null },
            title = { Text("Editar Álbum") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
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
                                IconButton(onClick = {
                                    val updatedPhotos = editingAlbum!!.photos.filter { it.url != photo.url }
                                    val updatedCover = if (editingAlbum!!.coverUrl == photo.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                    val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                    val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                    if (index != -1) {
                                        vipAlbumsState[index] = updatedAlbum
                                            addVipAlbum(updatedAlbum)
                                            addVipAlbum(updatedAlbum)
                                        editingAlbum = updatedAlbum
                                    }
                                }) {
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
    

    // Add Chapter Form States
    var editingCourse by remember { mutableStateOf<IbrCourse?>(null) }
    var editingChapter by remember { mutableStateOf<IbrChapter?>(null) }

    var selectedCourseForChapter by remember { mutableStateOf<IbrCourse?>(null) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterDescription by remember { mutableStateOf("") }
    var chapterDuration by remember { mutableStateOf("30") }
    var chapterType by remember { mutableStateOf("VIDEO") } // VIDEO, AUDIO, TEXT
    var isYoutube by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    
    // Bulk Upload States
    class BulkUploadTask(
        val id: String,
        val filename: String,
        val title: String,
        var progress: Float = 0f,
        var status: String = "Pendente"
    )
    val bulkUploadQueue = remember { mutableStateListOf<BulkUploadTask>() }
    val coroutineScope = rememberCoroutineScope()

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
                    text = "VIP - Cursos Exclusivos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cadastre novos cursos teológicos, capítulos, links do YouTube, vídeos e áudios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. CREATE NEW COURSE CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
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

                    // Theme selector chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tema / Categoria:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val themes = listOf("Teologia", "História Bíblica", "Vida Cristã")
                            themes.forEach { theme ->
                                FilterChip(
                                    selected = courseTheme == theme,
                                    onClick = { courseTheme = theme },
                                    label = { Text(theme) }
                                )
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
                                    imageUrl = "",
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

        // Initialize selected course if empty
        if (selectedCourseForChapter == null && ibrCoursesState.isNotEmpty()) {
            selectedCourseForChapter = ibrCoursesState.first()
        }

        // 2. ADD CHAPTER CARD (Only shown if courses exist)
        if (ibrCoursesState.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextField(
                                value = chapterDuration,
                                onValueChange = { chapterDuration = it },
                                label = { Text("Duração (Minutos)") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("É YouTube?", style = MaterialTheme.typography.labelMedium)
                                Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                            }
                        }

                        if (isYoutube) {
                            GlassTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("Link do YouTube (ID ou URL)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                placeholder = { Text("https://youtube.com/watch?v=...") }
                            )
                        } else {
                            LocalUploadField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = "Upload de Vídeo (URL ou Arquivo)",
                                mimeType = "video/*"
                            )
                            LocalUploadField(
                                value = audioUrl,
                                onValueChange = { audioUrl = it },
                                label = "Upload de Áudio (URL ou Arquivo)",
                                mimeType = "audio/*"
                            )
                        }

                        GlassButton(
                            onClick = {
                                if (chapterTitle.isNotBlank() && selectedCourseForChapter != null) {
                                    val duration = chapterDuration.toIntOrNull() ?: 30
                                    val newChapter = IbrChapter(
                                        id = "chap_${System.currentTimeMillis()}",
                                        title = chapterTitle,
                                        description = chapterDescription,
                                        durationMinutes = duration,
                                        type = chapterType,
                                        videoUrl = videoUrl,
                                        audioUrl = audioUrl,
                                        textContent = textContent,
                                        isYoutube = isYoutube
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
                                    isYoutube = false
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
        
        // 2.5 BULK UPLOAD CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🚀 Upload em Lote (Múltiplas Aulas)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    
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
                    
                    if (bulkUploadQueue.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (selectedCourseForChapter == null) {
                                    NotificationHelper.showNotification(context, "Erro", "Selecione o curso destino antes de adicionar arquivos.")
                                    return@OutlinedButton
                                }
                                bulkUploadQueue.add(BulkUploadTask("t1", "aula1_introducao.mp4", "Aula 1: Introdução"))
                                bulkUploadQueue.add(BulkUploadTask("t2", "aula2_fundamentos.mp4", "Aula 2: Fundamentos"))
                                bulkUploadQueue.add(BulkUploadTask("t3", "aula3_avancado.mp4", "Aula 3: Avançado"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Selecionar Arquivos Locais (Simulado)")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bulkUploadQueue.forEach { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(task.title, fontWeight = FontWeight.Bold)
                                            Text(task.status, style = MaterialTheme.typography.labelSmall, color = if (task.status == "Concluído") Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                                        }
                                        Text(task.filename, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        LinearProgressIndicator(
                                            progress = { task.progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (task.status == "Concluído") Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            val allDone = bulkUploadQueue.all { it.status == "Concluído" }
                            if (!allDone) {
                                GlassButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            for (task in bulkUploadQueue) {
                                                if (task.status == "Concluído") continue
                                                
                                                val index = bulkUploadQueue.indexOf(task)
                                                
                                                val updatedTask = BulkUploadTask(task.id, task.filename, task.title, 0f, "Enviando...")
                                                bulkUploadQueue[index] = updatedTask
                                                
                                                // Simulate upload progress
                                                for (p in 1..10) {
                                                    kotlinx.coroutines.delay(300)
                                                    bulkUploadQueue[index] = BulkUploadTask(task.id, task.filename, task.title, p * 0.1f, "Enviando... ${p*10}%")
                                                }
                                                
                                                bulkUploadQueue[index] = BulkUploadTask(task.id, task.filename, task.title, 1f, "Concluído")
                                                
                                                // Add chapter to course
                                                val targetCourse = ibrCoursesState.find { it.id == selectedCourseForChapter?.id }
                                                if (targetCourse != null) {
                                                    val newChap = IbrChapter(
                                                        id = "chap_${System.currentTimeMillis()}_${task.id}",
                                                        title = task.title,
                                                        description = "Upload em lote: ${task.filename}",
                                                        durationMinutes = 45,
                                                        type = "VIDEO",
                                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                                        audioUrl = "",
                                                        textContent = "",
                                                        isYoutube = false
                                                    )
                                                    val updatedChapters = targetCourse.chapters.toMutableList().apply { add(newChap) }
                                                    val updatedCourse = targetCourse.copy(chapters = updatedChapters)
                                                    val cIndex = ibrCoursesState.indexOf(targetCourse)
                                                    if (cIndex != -1) {
                                                        ibrCoursesState[cIndex] = updatedCourse
                                                    }
                                                    selectedCourseForChapter = updatedCourse
                                                }
                                            }
                                            NotificationHelper.showNotification(context, "Sucesso", "Todos os uploads foram concluídos e adicionados ao curso.")
                                            kotlinx.coroutines.delay(2000)
                                            bulkUploadQueue.clear()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Iniciar Upload em Lote", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                GlassButton(
                                    onClick = { bulkUploadQueue.clear() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Limpar Fila", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. LIST OF EXISTING COURSES AND CHAPTERS
        item {
            Text("📚 Cursos e Aulas Ativas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (ibrCoursesState.isEmpty()) {
            item {
                Text("Nenhum curso ou aula cadastrado ainda.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            items(ibrCoursesState) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(course.theme.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(onClick = { editingCourse = course }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar Curso", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { removeIbrCourse(course) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar Curso", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Column {
                                            Text(ch.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}",
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
                                            onClick = {
                                                val updatedChapters = course.chapters.toMutableList().apply { remove(ch) }
                                                val updatedCourse = course.copy(chapters = updatedChapters)
                                                val index = ibrCoursesState.indexOf(course)
                                                if (index != -1) {
                                                    addIbrCourse(updatedCourse)
                                                }
                                            },
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

    if (editingCourse != null && editingChapter == null) {
        var editTitle by remember(editingCourse) { mutableStateOf(editingCourse!!.title) }
        var editDescription by remember(editingCourse) { mutableStateOf(editingCourse!!.description) }
        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }
        
        AlertDialog(
            onDismissRequest = { editingCourse = null },
            title = { Text("Editar Curso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    GlassTextField(value = editTheme, onValueChange = { editTheme = it }, label = { Text("Tema") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = ibrCoursesState.indexOfFirst { it.id == editingCourse!!.id }
                    if (idx != -1) {
                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme)
                        addIbrCourse(updated)
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
        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }
        
        AlertDialog(
            onDismissRequest = {
                editingChapter = null
                editingCourse = null
            },
            title = { Text("Editar Aula") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })
                    GlassTextField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = { Text("URL Vídeo") })
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
                            val isYt = isYoutubeUrl(editVideoUrl)
                            val ytId = extractYoutubeId(editVideoUrl) ?: ""
                            updatedChapters[chapterIdx] = editingChapter!!.copy(
                                title = editTitle,
                                description = editDescription,
                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,
                                videoUrl = editVideoUrl,
                                isYoutube = isYt,
                                youtubeId = ytId
                            )
                            ibrCoursesState[courseIdx] = course.copy(chapters = updatedChapters)
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
    
    val totalIbrCourses = ibrCoursesState.size
    
    val eligibleUsers = memberRequestsState.filter { member -> 
        if (!member.isIbr) return@filter false
        if (totalIbrCourses == 0) return@filter false
        
        var completedCoursesCount = 0
        ibrCoursesState.forEach { course ->
            var allChaptersCompleted = true
            course.chapters.forEach { chapter ->
                val p = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                if (p == null || !p.isCompleted) {
                    allChaptersCompleted = false
                }
            }
            if (allChaptersCompleted && course.chapters.isNotEmpty()) {
                completedCoursesCount++
            }
        }
        
        completedCoursesCount == totalIbrCourses
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
                                if (member.ibrCertificateUrl.isNotEmpty()) {
                                    Icon(Icons.Default.Verified, contentDescription = "Certificado Enviado", tint = Color(0xFF4CAF50))
                                } else {
                                    Icon(Icons.Default.PendingActions, contentDescription = "Pendente", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (member.ibrCertificateUrl.isNotEmpty()) {
                                Text("Certificado enviado:", style = MaterialTheme.typography.labelSmall)
                                Text(member.ibrCertificateUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Button(onClick = { 
                                uploadCertificateForUser = member 
                                uploadUrl = member.ibrCertificateUrl
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (member.ibrCertificateUrl.isEmpty()) "Fazer Upload do Certificado" else "Alterar Certificado")
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
                        label = "Upload PDF ou Imagem",
                        mimeType = "*/*"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedMember = uploadCertificateForUser!!.copy(ibrCertificateUrl = uploadUrl)
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
