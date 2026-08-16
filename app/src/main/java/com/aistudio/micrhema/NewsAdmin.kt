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
    
    val newsList = BibleNewsEditorial.withEditorialCatalog(
        if (bibleNewsState.isEmpty()) BibleNewsData.newsList else bibleNewsState.toList()
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Avisos / Notícias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingNews = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Escolha uma notícia para ser enviada uma vez por dia ao meio-dia. Selecionar outra substitui a atual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dailyNewsNotificationIdState.value != null) {
            TextButton(
                onClick = {
                    clearDailyNewsNotification(
                        onSuccess = { android.widget.Toast.makeText(context, "Notícia do meio-dia desativada", android.widget.Toast.LENGTH_SHORT).show() },
                        onFailure = { error -> android.widget.Toast.makeText(context, "Não foi possível desativar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show() }
                    )
                }
            ) {
                Text("Remover seleção das 12h", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
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
                            TextButton(
                                onClick = {
                                    selectDailyNewsNotification(
                                        news,
                                        onSuccess = { android.widget.Toast.makeText(context, "Notícia selecionada para as 12h", android.widget.Toast.LENGTH_SHORT).show() },
                                        onFailure = { error -> android.widget.Toast.makeText(context, "Não foi possível selecionar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show() }
                                    )
                                }
                            ) {
                                Text(
                                    if (dailyNewsNotificationIdState.value == news.id) "Selecionada para as 12h" else "Notificar às 12h",
                                    color = if (dailyNewsNotificationIdState.value == news.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
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
        var summary by remember { mutableStateOf(editingNews?.summary ?: "") }
        var storyKey by remember { mutableStateOf(editingNews?.storyKey ?: "") }
        var content by remember { mutableStateOf(editingNews?.content ?: "") }
        var book by remember { mutableStateOf(editingNews?.book ?: "") }
        var chapter by remember { mutableStateOf(editingNews?.chapter?.toString() ?: "") }
        var verse by remember { mutableStateOf(editingNews?.verse?.toString() ?: "") }
        var imageUrl by remember { mutableStateOf(editingNews?.imageUrl ?: "") }
        var category by remember { mutableStateOf(editingNews?.category?.takeIf { it.isNotBlank() } ?: "Para refletir") }
        var intensity by remember { mutableStateOf(editingNews?.intensity?.takeIf { it in 1..4 }?.toString() ?: "1") }
        var tagsText by remember { mutableStateOf(editingNews?.tags?.joinToString(", ") ?: "") }
        var contentWarning by remember { mutableStateOf(editingNews?.contentWarning ?: "") }
        var isFeatured by remember { mutableStateOf(editingNews?.featured ?: false) }
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
                    val uploadedUrl = StorageHelper.uploadFile(context, uri, "news") { progress ->
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
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Resumo para o card") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = storyKey, onValueChange = { storyKey = it }, label = { Text("Chave da história") }, supportingText = { Text("Use a mesma chave apenas para a mesma história; exemplo: jose-vendido") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = book, onValueChange = { book = it }, label = { Text("Livro") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Capítulo (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = verse, onValueChange = { verse = it }, label = { Text("Versículo (Opcional)") }, modifier = Modifier.fillMaxWidth())
                    
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL da Imagem") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria editorial") }, supportingText = { Text("Ex.: Para refletir, Milagres e sinais, Poder e justiça") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = intensity, onValueChange = { intensity = it.filter(Char::isDigit).take(1) }, label = { Text("Intensidade narrativa (1 a 4)") }, supportingText = { Text("1: reflexão  •  2: conflitos  •  3: consequências  •  4: reviravoltas") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags separadas por vírgula") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contentWarning, onValueChange = { contentWarning = it }, label = { Text("Aviso de conteúdo (opcional)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                        Text("Destacar na Home")
                    }
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
                            summary = summary.trim(),
                            book = book,
                            chapter = chapter.toIntOrNull() ?: 0,
                            verse = verse.toIntOrNull() ?: 0,
                            imageUrl = convertGoogleDriveUrl(imageUrl),
                            category = category.ifBlank { "Para refletir" },
                            intensity = intensity.toIntOrNull()?.coerceIn(1, 4) ?: 1,
                            tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            contentWarning = contentWarning.trim(),
                            featured = isFeatured,
                            publishedAt = editingNews?.publishedAt?.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            storyKey = storyKey.trim()
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
