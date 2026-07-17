package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditContentSection() {
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Gerenciar Conteúdos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione livros, áudios e vídeos para os membros.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Livro", fontWeight = FontWeight.Bold)
                var title by remember { mutableStateOf("") }
                var author by remember { mutableStateOf("") }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título do Livro") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor (e.g. PDF/Epub Simulado)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    contentBooksState.add(ContentBook(id = System.currentTimeMillis().toString(), title = title, author = author, coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "Conteúdo do livro carregado..."))
                    title = ""
                    author = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Livro")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Áudio", fontWeight = FontWeight.Bold)
                var audioTitle by remember { mutableStateOf("") }
                var audioArtist by remember { mutableStateOf("") }
                var audioUrl by remember { mutableStateOf("") }
                OutlinedTextField(value = audioTitle, onValueChange = { audioTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = audioArtist, onValueChange = { audioArtist = it }, label = { Text("Artista/Preletor") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = audioUrl, onValueChange = { audioUrl = it }, label = { Text("URL do MP3") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    contentAudiosState.add(ContentAudio(id = System.currentTimeMillis().toString(), title = audioTitle, artist = audioArtist, audioUrl = audioUrl.ifEmpty { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3" }, coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"))
                    audioTitle = ""
                    audioArtist = ""
                    audioUrl = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Áudio")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adicionar Vídeo", fontWeight = FontWeight.Bold)
                var videoTitle by remember { mutableStateOf("") }
                var videoDesc by remember { mutableStateOf("") }
                var videoUrl by remember { mutableStateOf("") }
                OutlinedTextField(value = videoTitle, onValueChange = { videoTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoDesc, onValueChange = { videoDesc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("URL do Vídeo (MP4/Youtube)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    contentVideosState.add(ContentVideo(id = System.currentTimeMillis().toString(), title = videoTitle, description = videoDesc, videoUrl = videoUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }, thumbnailUrl = "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80"))
                    videoTitle = ""
                    videoDesc = ""
                    videoUrl = ""
                }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Salvar Vídeo")
                }
            }
        }
    }
}
