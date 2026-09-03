package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
    var pendingDelete by remember { mutableStateOf<BibleNews?>(null) }

    val newsList = currentResolvedBibleNews()
    var searchQuery by remember { mutableStateOf("") }
    var intensityFilter by remember { mutableStateOf<Int?>(null) }
    val filteredNews = newsList.filter { news ->
        val searchable = listOf(
            news.title,
            news.summary,
            news.content,
            news.book,
            news.category,
            news.tags.joinToString(" ")
        ).joinToString(" ")
        val matchesQuery = searchQuery.isBlank() || searchable.contains(searchQuery.trim(), ignoreCase = true)
        val matchesIntensity = intensityFilter == null || news.intensity == intensityFilter
        matchesQuery && matchesIntensity
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Avisos / Notícias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingNews = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar notícias") },
            label = { Text("Buscar por título, conteúdo, livro ou categoria") }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = intensityFilter == null, onClick = { intensityFilter = null }, label = { Text("Todas") })
            (1..4).forEach { level ->
                FilterChip(selected = intensityFilter == level, onClick = { intensityFilter = level }, label = { Text("Nível $level") })
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${filteredNews.size} notícia(s) encontrada(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Escolha uma notícia para uma notificação diária por volta das 12h. O Android pode atrasar alguns minutos por economia de bateria. Selecionar outra substitui a atual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dailyNewsNotificationIdState.value != null) {
            TextButton(
                onClick = {
                    clearDailyNewsNotification(
                        onSuccess = { android.widget.Toast.makeText(context, "Notícia diária desativada", android.widget.Toast.LENGTH_SHORT).show() },
                        onFailure = { error -> android.widget.Toast.makeText(context, "Não foi possível desativar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show() }
                    )
                }
            ) {
                Text("Remover seleção diária", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (filteredNews.isEmpty()) {
            AdminEmptyState("Nenhuma notícia encontrada", "Tente outro título, livro, categoria ou nível.")
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredNews, key = { it.id }) { news ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editingNews = news; showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(news.title, fontWeight = FontWeight.Bold)
                            Text("${news.book} ${news.chapter}:${news.verse}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                AdminStatusChip("Nível ${news.intensity}", positive = news.intensity < 4)
                                if (news.category.isNotBlank()) Text(news.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(
                                onClick = {
                                    selectDailyBibleNewsSafely(
                                        news,
                                        onSuccess = { android.widget.Toast.makeText(context, "Notícia selecionada para a notificação diária", android.widget.Toast.LENGTH_SHORT).show() },
                                        onFailure = { error -> android.widget.Toast.makeText(context, "Não foi possível selecionar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show() }
                                    )
                                }
                            ) {
                                Text(
                                    if (dailyNewsNotificationIdState.value == news.id) "Selecionada para a notificação diária" else "Notificar por volta das 12h",
                                    color = if (dailyNewsNotificationIdState.value == news.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(onClick = { pendingDelete = news }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir notícia", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { news ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir notícia?") },
            text = { Text("A notícia será ocultada para todos e não reaparecerá pelo catálogo interno do aplicativo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        hideBibleNewsSafely(
                            news,
                            onSuccess = {
                                pendingDelete = null
                                android.widget.Toast.makeText(context, "Notícia excluída", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                android.widget.Toast.makeText(context, "Não foi possível excluir: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }

    if (showDialog) {
        var title by remember(editingNews?.id) { mutableStateOf(editingNews?.title ?: "") }
        var summary by remember(editingNews?.id) { mutableStateOf(editingNews?.summary ?: "") }
        var storyKey by remember(editingNews?.id) { mutableStateOf(editingNews?.storyKey ?: "") }
        var content by remember(editingNews?.id) { mutableStateOf(editingNews?.content ?: "") }
        var book by remember(editingNews?.id) { mutableStateOf(editingNews?.book ?: "") }
        var chapter by remember(editingNews?.id) { mutableStateOf(editingNews?.chapter?.takeIf { it > 0 }?.toString() ?: "") }
        var verse by remember(editingNews?.id) { mutableStateOf(editingNews?.verse?.takeIf { it > 0 }?.toString() ?: "") }
        var imageUrl by remember(editingNews?.id) { mutableStateOf(editingNews?.imageUrl ?: "") }
        var category by remember(editingNews?.id) { mutableStateOf(editingNews?.category?.takeIf { it.isNotBlank() } ?: "Para refletir") }
        var intensity by remember(editingNews?.id) { mutableStateOf(editingNews?.intensity?.takeIf { it in 1..4 }?.toString() ?: "1") }
        var tagsText by remember(editingNews?.id) { mutableStateOf(editingNews?.tags?.joinToString(", ") ?: "") }
        var contentWarning by remember(editingNews?.id) { mutableStateOf(editingNews?.contentWarning ?: "") }
        var isFeatured by remember(editingNews?.id) { mutableStateOf(editingNews?.featured ?: false) }
        var isUploading by remember { mutableStateOf(false) }
        var isSaving by remember { mutableStateOf(false) }
        var uploadProgress by remember { mutableFloatStateOf(0f) }
        var validationError by remember { mutableStateOf("") }
        var bookExpanded by remember { mutableStateOf(false) }
        var categoryExpanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val allowedCategories = BibleNewsEditorial.categories.filterNot { it == "Tudo" || it == "Mais recentes" }

        val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                isUploading = true
                scope.launch {
                    try {
                        uploadProgress = 0f
                        val uploadedUrl = StorageHelper.uploadFile(context, uri, "news", mimeTypeHint = "image/*") { progress ->
                            uploadProgress = progress
                        }
                        if (uploadedUrl.isBlank()) throw IllegalStateException("O Supabase não retornou a URL da notícia.")
                        imageUrl = uploadedUrl
                    } catch (error: Exception) {
                        android.widget.Toast.makeText(context, "Upload da imagem não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                    } finally {
                        isUploading = false
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isUploading && !isSaving) showDialog = false },
            title = { Text(if (editingNews == null) "Nova Notícia Bíblica" else "Editar Notícia Bíblica") },
            text = {
                Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it; validationError = "" }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Resumo para o card") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = storyKey, onValueChange = { storyKey = it }, label = { Text("Chave da história") }, supportingText = { Text("Use a mesma chave apenas para a mesma história; exemplo: jose-vendido") }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = bookExpanded, onExpandedChange = { bookExpanded = !bookExpanded }) {
                        OutlinedTextField(
                            value = book,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Livro") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bookExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = bookExpanded, onDismissRequest = { bookExpanded = false }) {
                            chapterCounts.keys.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        book = option
                                        chapter = ""
                                        verse = ""
                                        validationError = ""
                                        bookExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = chapter, onValueChange = { chapter = it.filter(Char::isDigit); validationError = "" }, label = { Text("Capítulo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = verse, onValueChange = { verse = it.filter(Char::isDigit); validationError = "" }, label = { Text("Versículo inicial") }, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL da Imagem") }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria editorial") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            allowedCategories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { category = option; categoryExpanded = false }
                                )
                            }
                        }
                    }

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
                        enabled = !isUploading && !isSaving
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviando ${((uploadProgress * 100).toInt()).coerceIn(0, 100)}%")
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Upload")
                            Spacer(Modifier.width(4.dp))
                            Text("Ou Enviar Imagem")
                        }
                    }

                    OutlinedTextField(value = content, onValueChange = { content = it; validationError = "" }, label = { Text("Conteúdo") }, modifier = Modifier.fillMaxWidth().height(160.dp))
                    if (validationError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(validationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isUploading && !isSaving,
                    onClick = {
                        val canonicalBook = chapterCounts.keys.firstOrNull { it == book }
                        val chapterNumber = chapter.toIntOrNull()
                        val verseNumber = verse.toIntOrNull()
                        val maxChapter = canonicalBook?.let { chapterCounts[it] }
                        validationError = when {
                            title.isBlank() -> "Informe o título da notícia."
                            content.isBlank() -> "Informe o conteúdo da notícia."
                            canonicalBook == null -> "Selecione um livro bíblico válido."
                            chapterNumber == null || maxChapter == null || chapterNumber !in 1..maxChapter -> "Informe um capítulo válido para $canonicalBook."
                            verseNumber == null || verseNumber < 1 -> "Informe o versículo inicial."
                            category !in allowedCategories -> "Selecione uma categoria editorial válida."
                            intensity.toIntOrNull() !in 1..4 -> "A intensidade deve estar entre 1 e 4."
                            else -> ""
                        }
                        if (validationError.isNotBlank()) return@TextButton

                        val reservedIds = (newsList.map { it.id } + hiddenBibleNewsIdsState)
                            .filter { it in 1 until 100_000 }
                        val newId = editingNews?.id ?: ((reservedIds.maxOrNull() ?: 0) + 1)
                        val newNews = BibleNews(
                            id = newId,
                            title = title.trim(),
                            content = content.trim(),
                            summary = summary.trim(),
                            book = canonicalBook!!,
                            chapter = chapterNumber!!,
                            verse = verseNumber!!,
                            imageUrl = convertGoogleDriveUrl(imageUrl.trim()),
                            category = category,
                            intensity = intensity.toInt(),
                            tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            contentWarning = contentWarning.trim(),
                            featured = isFeatured,
                            publishedAt = editingNews?.publishedAt?.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            storyKey = storyKey.trim()
                        )

                        isSaving = true
                        saveBibleNewsSafely(
                            newNews,
                            onSuccess = {
                                val idx = bibleNewsState.indexOfFirst { it.id == newId }
                                if (idx >= 0) bibleNewsState[idx] = BibleNewsEditorial.decorate(newNews)
                                else bibleNewsState.add(0, BibleNewsEditorial.decorate(newNews))
                                isSaving = false
                                showDialog = false
                                android.widget.Toast.makeText(context, "Notícia salva", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                isSaving = false
                                validationError = "Não foi possível salvar: ${error.message ?: "verifique sua conexão"}"
                            }
                        )
                    }
                ) { Text(if (isSaving) "Salvando…" else "Salvar") }
            },
            dismissButton = { TextButton(enabled = !isUploading && !isSaving, onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}
