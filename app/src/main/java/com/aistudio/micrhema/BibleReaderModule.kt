package com.aistudio.micrhema
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderModal(
    onDismiss: () -> Unit,
    initialReference: String = ""
) {
    var searchQuery by remember { mutableStateOf(initialReference.substringBefore(" ").trim()) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }
    
    var selectedVersion by remember { mutableStateOf("NTLH") }
    var showVersionDialog by remember { mutableStateOf(false) }

    var verses by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val versions = listOf(
        "ARA" to "Almeida Revista e Atualizada",
        "ARC" to "Almeida Revista e Corrigida",
        "NVI" to "Nova Versão Internacional",
        "NTLH" to "Nova Tradução na Linguagem de Hoje"
    )
    val books = listOf("Gênesis", "Êxodo", "Levítico", "Números", "Deuteronômio", "Josué", "Juízes", "Rute", "1 Samuel", "2 Samuel", "1 Reis", "2 Reis", "1 Crônicas", "2 Crônicas", "Esdras", "Neemias", "Ester", "Jó", "Salmos", "Provérbios", "Eclesiastes", "Cânticos", "Isaías", "Jeremias", "Lamentações", "Ezequiel", "Daniel", "Oséias", "Joel", "Amós", "Obadias", "Jonas", "Miquéias", "Naum", "Habacuque", "Sofonias", "Ageu", "Zacarias", "Malaquias",
    "Mateus", "Marcos", "Lucas", "João", "Atos", "Romanos", "1 Coríntios", "2 Coríntios", "Gálatas", "Efésios", "Filipenses", "Colossenses", "1 Tessalonicenses", "2 Tessalonicenses", "1 Timóteo", "2 Timóteo", "Tito", "Filemom", "Hebreus", "Tiago", "1 Pedro", "2 Pedro", "1 João", "2 João", "3 João", "Judas", "Apocalipse")

    LaunchedEffect(initialReference) {
        if (initialReference.isNotBlank()) {
            val parts = initialReference.split(" ")
            if (parts.size >= 2) {
                var bookPart = ""
                var refPart = ""
                if (parts[0].all { it.isDigit() }) {
                    bookPart = "${parts[0]} ${parts[1]}"
                    refPart = parts.getOrNull(2) ?: ""
                } else {
                    bookPart = parts[0]
                    refPart = parts[1]
                }
                
                val chapterPart = refPart.substringBefore(":")
                val ch = chapterPart.toIntOrNull()
                
                if (books.any { it.equals(bookPart, ignoreCase = true) }) {
                    selectedBook = books.first { it.equals(bookPart, ignoreCase = true) }
                    if (ch != null) {
                        selectedChapter = ch
                    }
                }
            }
        }
    }

    fun fetchChapter() {
        if (selectedBook != null && selectedChapter != null) {
            isLoading = true
            coroutineScope.launch {
                verses = BibleFetcher.getChapter(context, selectedBook!!, selectedChapter!!, selectedVersion)
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedChapter, selectedVersion) {
        if (selectedChapter != null) {
            fetchChapter()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bíblia Sagrada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showVersionDialog = true }) {
                        Text(selectedVersion, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Settings, contentDescription = "Versão", modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar livro...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedBook == null) {
                LazyColumn {
                    items(books.filter { it.contains(searchQuery, ignoreCase = true) }) { book ->
                        ListItem(
                            headlineContent = { Text(book) },
                            modifier = Modifier.clickable {
                                selectedBook = book
                            }
                        )
                        HorizontalDivider()
                    }
                }
            } else if (selectedChapter == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedBook = null }) {
                        Text("Voltar aos Livros")
                    }
                    Text(" > $selectedBook", fontWeight = FontWeight.Bold)
                }
                LazyColumn {
                    val maxChaps = if (selectedBook == "Salmos") 150 else 50
                    items((1..maxChaps).toList()) { chapter ->
                        ListItem(
                            headlineContent = { Text("Capítulo $chapter") },
                            modifier = Modifier.clickable {
                                selectedChapter = chapter
                            }
                        )
                        HorizontalDivider()
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedChapter = null }) {
                        Text("Voltar aos Capítulos")
                    }
                    Text(" > $selectedBook $selectedChapter", fontWeight = FontWeight.Bold)
                }
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (verses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Capítulo não encontrado ou erro de conexão.", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        LazyColumn {
                            items(verses) { verse ->
                                Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                                    Text(
                                        "${verse.verse}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        verse.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Versão da Bíblia") },
            text = {
                Column {
                    versions.forEach { (code, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedVersion = code
                                showVersionDialog = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVersion == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(code, fontWeight = FontWeight.Bold)
                                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
