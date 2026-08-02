package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNewsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<BibleNews?>(null) }
    
    val newsList = if (bibleNewsState.isEmpty()) BibleNewsData.newsList else bibleNewsState

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Avisos / Notícias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingNews = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(newsList) { news ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editingNews = news; showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(news.title, fontWeight = FontWeight.Bold)
                            Text(news.book, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { 
                            bibleNewsState.remove(news)
                            removeBibleNews(news)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        var title by remember { mutableStateOf(editingNews?.title ?: "") }
        var content by remember { mutableStateOf(editingNews?.content ?: "") }
        var book by remember { mutableStateOf(editingNews?.book ?: "") }
        var chapter by remember { mutableStateOf(editingNews?.chapter?.toString() ?: "") }
        var verse by remember { mutableStateOf(editingNews?.verse?.toString() ?: "") }
        var imageUrl by remember { mutableStateOf(editingNews?.imageUrl ?: "") }
        var isUploading by remember { mutableStateOf(false) }
        var uploadProgress by remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()
        
        val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                isUploading = true
                scope.launch {
                    uploadProgress = 0f
                    val uploadedUrl = StorageManager.uploadFile(context, uri, "news") { progress ->
                        uploadProgress = progress
                    }
                    if (uploadedUrl.isNotEmpty()) {
                        imageUrl = uploadedUrl
                    }
                    isUploading = false
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingNews == null) "Novo Aviso" else "Editar Aviso") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = book, onValueChange = { book = it }, label = { Text("Categoria / Livro") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Capítulo (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = verse, onValueChange = { verse = it }, label = { Text("Versículo (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL da Imagem") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Upload")
                            Spacer(Modifier.width(4.dp))
                            Text("Ou Enviar Imagem")
                        }
                    }
                    
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Conteúdo") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotEmpty()) {
                        val newId = editingNews?.id ?: (newsList.maxOfOrNull { it.id } ?: 0) + 1
                        val newNews = BibleNews(
                            id = newId,
                            title = title,
                            content = content,
                            book = book,
                            chapter = chapter.toIntOrNull() ?: 0,
                            verse = verse.toIntOrNull() ?: 0,
                            imageUrl = imageUrl
                        )
                        
                        if (editingNews != null) {
                            val idx = bibleNewsState.indexOfFirst { it.id == newId }
                            if (idx >= 0) {
                                bibleNewsState[idx] = newNews
                            }
                        } else {
                            bibleNewsState.add(0, newNews)
                        }
                        
                        addBibleNews(newNews)
                        showDialog = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}
