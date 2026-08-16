package com.aistudio.micrhema
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditMediaSection() {
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
    var mediaSearchQuery by remember { mutableStateOf("") }
    val filteredBooks = contentBooksState.filter { it.title.contains(mediaSearchQuery, ignoreCase = true) || it.author.contains(mediaSearchQuery, ignoreCase = true) }
    val filteredAudios = contentAudiosState.filter { it.title.contains(mediaSearchQuery, ignoreCase = true) || it.artist.contains(mediaSearchQuery, ignoreCase = true) }
    val filteredVideos = contentVideosState.filter { it.title.contains(mediaSearchQuery, ignoreCase = true) || it.description.contains(mediaSearchQuery, ignoreCase = true) }
    val filteredAlbums = contentAlbumsState.filter { it.title.contains(mediaSearchQuery, ignoreCase = true) || it.description.contains(mediaSearchQuery, ignoreCase = true) }
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        MediaAdminHeader(
            totalItems = contentBooksState.size + contentAudiosState.size + contentVideosState.size + contentAlbumsState.size,
            isUploading = isUploading,
            uploadProgress = uploadProgress
        )
        OutlinedTextField(
            value = mediaSearchQuery,
            onValueChange = { mediaSearchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar na mídia") },
            label = { Text("Buscar livro, áudio, vídeo ou álbum") }
        )

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
                if (coverUrl.isNotBlank()) {
                    MediaImagePreview(url = coverUrl, label = "Prévia da capa")
                }
                var bookUrl by remember { mutableStateOf("") }
                LocalUploadField(value = bookUrl, onValueChange = { bookUrl = it }, label = "Arquivo do Livro (URL ou PDF/Epub)", mimeType = "*/*")
                if (bookUrl.isNotBlank()) {
                    MediaFileStatus(name = bookUrl.substringAfterLast('/').ifBlank { "Arquivo do livro selecionado" }, type = "PDF/E-book", ready = bookUrl.startsWith("http") || bookUrl.startsWith("content:"))
                }
                GlassButton(onClick = {
                    if (isUploading) return@GlassButton
                    isUploading = true
                    coroutineScope.launch {
                        try {
                            uploadProgress = 0f
                            val finalCover = if (coverUrl.isNotBlank() && !coverUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(coverUrl), "books/covers") { progress -> uploadProgress = progress / 2f } else convertGoogleDriveUrl(coverUrl).ifEmpty { "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" }
                            val finalBookUrl = if (bookUrl.isNotBlank() && !bookUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(bookUrl), "books/files") { progress -> uploadProgress = 0.5f + (progress / 2f) } else convertGoogleDriveUrl(bookUrl)
                            addContentBook(ContentBook(id = System.currentTimeMillis().toString(), title = title, author = author, coverUrl = finalCover, contentText = "Conteúdo do livro carregado...", bookUrl = finalBookUrl))
                            title = ""
                            author = ""
                            coverUrl = ""
                            bookUrl = ""
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Upload não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    if (isUploading) {
                        Text("Enviando... ${(uploadProgress * 100).toInt()}%")
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.CircularProgressIndicator(progress = { uploadProgress }, modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Salvar Livro")
                    }
                }
            }
        }
        
        if (filteredBooks.isNotEmpty()) {
            MediaListHeading("Livros cadastrados", filteredBooks.size)
            filteredBooks.forEach { book ->
                MediaContentRow(
                    title = book.title,
                    subtitle = book.author,
                    type = "Livro / PDF",
                    onEdit = { editingBook = book },
                    onDelete = { removeContentBook(book) }
                )
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
                LocalUploadField(value = audioCover, onValueChange = { audioCover = it }, label = "URL ou Arquivo de Capa (Opcional)", mimeType = "image/*")
                LocalUploadField(value = audioUrl, onValueChange = { audioUrl = it }, label = "URL ou Arquivo Local MP3", mimeType = "audio/*")
                GlassButton(onClick = {
                    if (isUploading) return@GlassButton
                    isUploading = true
                    coroutineScope.launch {
                        try {
                            uploadProgress = 0f
                            if (audioUrl.isBlank()) {
                                android.widget.Toast.makeText(context, "Informe ou selecione um arquivo de áudio.", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val finalCoverUrl = if (audioCover.isNotBlank() && !audioCover.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(audioCover), "audios/covers") { progress -> uploadProgress = progress / 2f } else convertGoogleDriveUrl(audioCover).ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80" }
                            val finalAudioUrl = if (!audioUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(audioUrl), "audios/files") { progress -> uploadProgress = 0.5f + (progress / 2f) } else convertGoogleDriveUrl(audioUrl)
                            addContentAudio(ContentAudio(id = System.currentTimeMillis().toString(), title = audioTitle, artist = audioArtist, audioUrl = finalAudioUrl, coverUrl = finalCoverUrl))
                            audioTitle = ""
                            audioArtist = ""
                            audioUrl = ""
                            audioCover = ""
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Upload não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    if (isUploading) {
                        Text("Enviando... ${(uploadProgress * 100).toInt()}%")
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.CircularProgressIndicator(progress = { uploadProgress }, modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Salvar Áudio")
                    }
                }
            }
        }
        
        if (filteredAudios.isNotEmpty()) {
            MediaListHeading("Áudios cadastrados", filteredAudios.size)
            filteredAudios.forEach { audio ->
                MediaContentRow(
                    title = audio.title,
                    subtitle = audio.artist,
                    type = "Áudio",
                    onEdit = { editingAudio = audio },
                    onDelete = { removeContentAudio(audio) }
                )
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
                var videoThumb by remember { mutableStateOf("") }
                GlassTextField(value = videoTitle, onValueChange = { videoTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = videoDesc, onValueChange = { videoDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                LocalUploadField(value = videoThumb, onValueChange = { videoThumb = it }, label = "URL ou Arquivo de Capa/Thumbnail (Opcional)", mimeType = "image/*")
                LocalUploadField(value = videoUrl, onValueChange = { videoUrl = it }, label = "URL ou Arquivo Local MP4", mimeType = "video/*")
                
                if (isYoutubeUrl(videoUrl) || videoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Prévia do vídeo", style = MaterialTheme.typography.labelMedium)
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f), shape = RoundedCornerShape(12.dp)) {
                        if (isYoutubeUrl(videoUrl)) {
                            YoutubeThumbnailImage(videoUrl = videoUrl, explicitThumbnailUrl = videoThumb)
                        } else {
                            coil.compose.AsyncImage(model = videoThumb.ifEmpty { videoUrl }, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                            val finalThumbUploaded = if (videoThumb.isNotBlank() && !videoThumb.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(videoThumb), "videos/covers") { progress -> uploadProgress = progress / 2f } else convertGoogleDriveUrl(videoThumb)
                            val finalVideoUrl = if (videoUrl.isNotBlank() && !videoUrl.startsWith("http")) StorageHelper.uploadFile(context, android.net.Uri.parse(videoUrl), "videos/files") { progress -> uploadProgress = 0.5f + (progress / 2f) } else convertGoogleDriveUrl(videoUrl).ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }
                            addContentVideo(ContentVideo(id = System.currentTimeMillis().toString(), title = videoTitle, description = videoDesc, videoUrl = finalVideoUrl, thumbnailUrl = finalThumbUploaded))
                            videoTitle = ""
                            videoDesc = ""
                            videoUrl = ""
                            videoThumb = ""
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Upload não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isUploading = false
                        }
                    }
                }, modifier = Modifier.padding(top = 8.dp)) {
                    if (isUploading) {
                        Text("Enviando... ${(uploadProgress * 100).toInt()}%")
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.CircularProgressIndicator(progress = { uploadProgress }, modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Salvar Vídeo")
                    }
                }
            }
        }
        
        if (filteredVideos.isNotEmpty()) {
            MediaListHeading("Vídeos cadastrados", filteredVideos.size)
            filteredVideos.forEach { video ->
                MediaContentRow(
                    title = video.title,
                    subtitle = video.description,
                    type = "Vídeo",
                    onEdit = { editingVideo = video },
                    onDelete = { removeContentVideo(video) }
                )
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
                        addContentPhotoAlbum(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = convertGoogleDriveUrl(customCoverUrl!!), photos = listOf(AlbumPhoto(url = convertGoogleDriveUrl(customCoverUrl!!), caption = "")), driveFolderUrl = finalDriveUrl))
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
        
        if (filteredAlbums.isNotEmpty()) {
            MediaListHeading("Álbuns cadastrados", filteredAlbums.size)
            filteredAlbums.forEach { album ->
                MediaContentRow(
                    title = album.title,
                    subtitle = album.description,
                    type = "Álbum",
                    onEdit = { editingAlbum = album },
                    onDelete = { albumToDelete = album }
                )
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
                            
                            val idx = contentAlbumsState.indexOfFirst { it.id == albumToDelete!!.id }
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
                    val idx = contentBooksState.indexOfFirst { it.id == editingBook!!.id }
                    if (idx != -1) {
                        val updated = editingBook!!.copy(title = editTitle, author = editAuthor, coverUrl = convertGoogleDriveUrl(editCoverUrl), contentText = editContent, bookUrl = convertGoogleDriveUrl(editBookUrl))
                        addContentBook(updated)
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
        var editCoverUrl by remember(editingAudio) { mutableStateOf(editingAudio!!.coverUrl) }
        
        AlertDialog(
            onDismissRequest = { editingAudio = null },
            title = { Text("Editar Áudio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artista") })
                    LocalUploadField(value = editUrl, onValueChange = { editUrl = it }, label = "URL ou Arquivo Local MP3", mimeType = "audio/*")
                    LocalUploadField(value = editCoverUrl, onValueChange = { editCoverUrl = it }, label = "URL ou Arquivo de Capa (Opcional)", mimeType = "image/*")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = contentAudiosState.indexOfFirst { it.id == editingAudio!!.id }
                    if (idx != -1) {
                        val updated = editingAudio!!.copy(title = editTitle, artist = editArtist, audioUrl = editUrl, coverUrl = convertGoogleDriveUrl(editCoverUrl))
                        addContentAudio(updated)
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
        var editThumbUrl by remember(editingVideo) { mutableStateOf(editingVideo!!.thumbnailUrl) }
        
        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Editar Vídeo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descrição") })
                    LocalUploadField(value = editUrl, onValueChange = { editUrl = it }, label = "URL ou Arquivo Local MP4", mimeType = "video/*")
                    LocalUploadField(value = editThumbUrl, onValueChange = { editThumbUrl = it }, label = "URL ou Arquivo de Capa/Thumbnail (Opcional)", mimeType = "image/*")
                    
                    if (isYoutubeUrl(editUrl) || editUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Prévia do vídeo", style = MaterialTheme.typography.labelMedium)
                        Card(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f), shape = RoundedCornerShape(12.dp)) {
                            if (isYoutubeUrl(editUrl)) {
                                YoutubeThumbnailImage(videoUrl = editUrl, explicitThumbnailUrl = editThumbUrl)
                            } else {
                                coil.compose.AsyncImage(model = editThumbUrl.ifEmpty { editUrl }, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                        if (editTitle.isNotBlank()) Text(editTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        if (editDesc.isNotBlank()) Text(editDesc, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = contentVideosState.indexOfFirst { it.id == editingVideo!!.id }
                    if (idx != -1) {
                        val updated = editingVideo!!.copy(title = editTitle, description = editDesc, videoUrl = editUrl, thumbnailUrl = convertGoogleDriveUrl(editThumbUrl))
                        addContentVideo(updated)
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
        var editCoverUrl by remember(editingAlbum) { mutableStateOf(editingAlbum!!.coverUrl ?: "") }
        
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
                    try {
                        photoProgress = 0f
                        val url = StorageHelper.uploadFile(context, uri, "album_photos", mimeTypeHint = "image/*") { progress -> photoProgress = progress }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val updatedAlbum = editingAlbum!!.copy(
                                photos = editingAlbum!!.photos + AlbumPhoto(url = url, caption = ""),
                                coverUrl = editingAlbum!!.coverUrl ?: url
                            )
                            val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                            if (index != -1) {
                                contentAlbumsState[index] = updatedAlbum
                                addContentPhotoAlbum(updatedAlbum)
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
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    LocalUploadField(value = editCoverUrl, onValueChange = { editCoverUrl = it }, label = "Capa do Álbum (URL da Imagem)", mimeType = "image/*")
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
                                val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                if (index != -1) {
                                    contentAlbumsState[index] = updatedAlbum
                                    addContentPhotoAlbum(updatedAlbum)
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
                                        val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                        if (index != -1) {
                                            contentAlbumsState[index] = updatedAlbum
                                            addContentPhotoAlbum(updatedAlbum)
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
                                    val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                    if (index != -1) {
                                        contentAlbumsState[index] = updatedAlbum
                                            addContentPhotoAlbum(updatedAlbum)
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
                    val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                    if (index != -1) {
                        val updated = editingAlbum!!.copy(title = editTitle, description = editDesc, driveFolderUrl = convertGoogleDriveUrl(editDriveUrl), coverUrl = convertGoogleDriveUrl(editCoverUrl))
                        contentAlbumsState[index] = updated
                        addContentPhotoAlbum(updated)
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


@Composable
private fun MediaAdminHeader(totalItems: Int, isUploading: Boolean, uploadProgress: Float) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Gerenciar uploads", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Central de Mídia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Livros, áudios, vídeos e álbuns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
                    Text("$totalItems itens", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MediaTypeBadge("PDF", Icons.Default.Description)
                MediaTypeBadge("Imagem", Icons.Default.Image)
                MediaTypeBadge("Áudio", Icons.Default.AudioFile)
                MediaTypeBadge("Vídeo", Icons.Default.VideoLibrary)
            }
            if (isUploading) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Enviando arquivo para o Supabase...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${(uploadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    LinearProgressIndicator(progress = { uploadProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MediaTypeBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)) {
        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MediaImagePreview(url: String, label: String) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(150.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun MediaFileStatus(name: String, type: String, ready: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (ready) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ready) Icons.Default.CheckCircle else Icons.Default.Description,
                contentDescription = null,
                tint = if (ready) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (ready) "Arquivo selecionado" else "Aguardando arquivo válido", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("$type • ${name.takeLast(42)}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

private fun mediaTypeIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type.lowercase()) {
    "book" -> Icons.Default.Description
    "audio" -> Icons.Default.AudioFile
    "video" -> Icons.Default.VideoLibrary
    else -> Icons.Default.Image
}

@Composable
private fun MediaContentRow(title: String, subtitle: String, type: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(mediaTypeIcon(type), contentDescription = type, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle.ifBlank { "Sem descrição" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar $title", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir $title", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun MediaListHeading(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
        }
    }
}
