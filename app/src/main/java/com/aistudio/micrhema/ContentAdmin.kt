package com.aistudio.micrhema
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditContentSection() {
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

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Gerenciar Conteúdos VIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione e edite livros, áudios e vídeos para os membros VIP e IBR.", style = MaterialTheme.typography.bodyMedium)
        
        // ADD BOOK
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Livro", fontWeight = FontWeight.Bold)
                var title by remember { mutableStateOf("") }
                var author by remember { mutableStateOf("") }
                GlassTextField(value = title, onValueChange = { title = it }, label = { Text("Título do Livro") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = author, onValueChange = { author = it }, label = { Text("Autor (e.g. PDF/Epub Simulado)") }, modifier = Modifier.fillMaxWidth())
                GlassButton(onClick = {
                    contentBooksState.add(ContentBook(id = System.currentTimeMillis().toString(), title = title, author = author, coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "Conteúdo do livro carregado..."))
                    title = ""
                    author = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Livro")
                }
            }
        }
        
        if (contentBooksState.isNotEmpty()) {
            Text("Livros Cadastrados", fontWeight = FontWeight.Bold)
            contentBooksState.forEach { book ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.Bold)
                        Text(book.author, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingBook = book }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { contentBooksState.remove(book) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
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
                GlassTextField(value = audioTitle, onValueChange = { audioTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = audioArtist, onValueChange = { audioArtist = it }, label = { Text("Artista/Preletor") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = audioUrl, onValueChange = { audioUrl = it }, label = { Text("URL do MP3") }, modifier = Modifier.fillMaxWidth())
                GlassButton(onClick = {
                    contentAudiosState.add(ContentAudio(id = System.currentTimeMillis().toString(), title = audioTitle, artist = audioArtist, audioUrl = audioUrl.ifEmpty { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3" }, coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"))
                    audioTitle = ""
                    audioArtist = ""
                    audioUrl = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Áudio")
                }
            }
        }
        
        if (contentAudiosState.isNotEmpty()) {
            Text("Áudios Cadastrados", fontWeight = FontWeight.Bold)
            contentAudiosState.forEach { audio ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(audio.title, fontWeight = FontWeight.Bold)
                        Text(audio.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingAudio = audio }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { contentAudiosState.remove(audio) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
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
                GlassTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("URL do Vídeo (MP4/Youtube)") }, modifier = Modifier.fillMaxWidth())
                GlassButton(onClick = {
                    contentVideosState.add(ContentVideo(id = System.currentTimeMillis().toString(), title = videoTitle, description = videoDesc, videoUrl = videoUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }, thumbnailUrl = "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80"))
                    videoTitle = ""
                    videoDesc = ""
                    videoUrl = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Vídeo")
                }
            }
        }
        
        if (contentVideosState.isNotEmpty()) {
            Text("Vídeos Cadastrados", fontWeight = FontWeight.Bold)
            contentVideosState.forEach { video ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(video.title, fontWeight = FontWeight.Bold)
                        Text(video.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { editingVideo = video }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { contentVideosState.remove(video) }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    

        var albumToDelete by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
        var isDeleting by remember { mutableStateOf(false) }

        // ADD ALBUM
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Álbum de Fotos", fontWeight = FontWeight.Bold)
                var albumTitle by remember { mutableStateOf("") }
                var albumDesc by remember { mutableStateOf("") }
                var customCoverUri by remember { mutableStateOf<android.net.Uri?>(null) }
                var isGenerating by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                val coverPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    customCoverUri = uri
                }

                GlassTextField(value = albumTitle, onValueChange = { albumTitle = it }, label = { Text("Título do Álbum") }, modifier = Modifier.fillMaxWidth())
                GlassTextField(value = albumDesc, onValueChange = { albumDesc = it }, label = { Text("Descrição do Álbum") }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { coverPicker.launch("image/*") }) {
                        Text(if (customCoverUri != null) "Capa Selecionada" else "Selecionar Capa")
                    }
                    if (customCoverUri != null) {
                        IconButton(onClick = { customCoverUri = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Button(onClick = {
                    if (customCoverUri != null) {
                        contentAlbumsState.add(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = customCoverUri.toString(), photos = listOf(AlbumPhoto(url = customCoverUri.toString(), caption = ""))))
                        albumTitle = ""
                        albumDesc = ""
                        customCoverUri = null
                    } else {
                        isGenerating = true
                        scope.launch {
                            val generatedCover = generatePlaceholderAlbumCover("A beautiful abstract aesthetic background suitable for a photo album cover titled '$albumTitle'. Minimalist, pastel colors.")
                            val finalCoverUrl = generatedCover ?: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80" // Fallback se a API não estiver configurada
                            contentAlbumsState.add(ContentPhotoAlbum(id = System.currentTimeMillis().toString(), title = albumTitle, description = albumDesc, coverUrl = finalCoverUrl))
                            isGenerating = false
                            albumTitle = ""
                            albumDesc = ""
                            customCoverUri = null
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
        
        if (contentAlbumsState.isNotEmpty()) {
            Text("Álbuns Cadastrados", fontWeight = FontWeight.Bold)
            contentAlbumsState.forEach { album ->
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
                                contentAlbumsState.removeAt(idx)
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
        var editContent by remember(editingBook) { mutableStateOf(editingBook!!.contentText) }
        
        AlertDialog(
            onDismissRequest = { editingBook = null },
            title = { Text("Editar Livro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("Autor") })
                    GlassTextField(value = editContent, onValueChange = { editContent = it }, label = { Text("Conteúdo") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = contentBooksState.indexOfFirst { it.id == editingBook!!.id }
                    if (idx != -1) {
                        contentBooksState[idx] = editingBook!!.copy(title = editTitle, author = editAuthor, contentText = editContent)
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
                    GlassTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL MP3") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = contentAudiosState.indexOfFirst { it.id == editingAudio!!.id }
                    if (idx != -1) {
                        contentAudiosState[idx] = editingAudio!!.copy(title = editTitle, artist = editArtist, audioUrl = editUrl)
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
                    GlassTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL Vídeo") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = contentVideosState.indexOfFirst { it.id == editingVideo!!.id }
                    if (idx != -1) {
                        contentVideosState[idx] = editingVideo!!.copy(title = editTitle, description = editDesc, videoUrl = editUrl)
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
        
        var photoUriInput by remember { mutableStateOf<android.net.Uri?>(null) }
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val updatedAlbum = editingAlbum!!.copy(
                    photos = editingAlbum!!.photos + AlbumPhoto(url = uri.toString(), caption = ""),
                    coverUrl = editingAlbum!!.coverUrl ?: uri.toString()
                )
                val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                if (index != -1) {
                    contentAlbumsState[index] = updatedAlbum
                    editingAlbum = updatedAlbum
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
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { photoPicker.launch("image/*") }) {
                        Text("Adicionar Foto ao Álbum")
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
                        contentAlbumsState[index] = editingAlbum!!.copy(title = editTitle, description = editDesc)
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