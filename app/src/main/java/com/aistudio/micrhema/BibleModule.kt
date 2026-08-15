package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Download
import android.widget.Toast

import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val chapterCounts = mapOf(
    "Gênesis" to 50, "Êxodo" to 40, "Levítico" to 27, "Números" to 36, "Deuteronômio" to 34,
    "Josué" to 24, "Juízes" to 21, "Rute" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
    "1 Reis" to 22, "2 Reis" to 25, "1 Crônicas" to 29, "2 Crônicas" to 36, "Esdras" to 10,
    "Neemias" to 13, "Ester" to 10, "Jó" to 42, "Salmos" to 150, "Provérbios" to 31,
    "Eclesiastes" to 12, "Cânticos" to 8, "Isaías" to 66, "Jeremias" to 52, "Lamentações" to 5,
    "Ezequiel" to 48, "Daniel" to 12, "Oséias" to 14, "Joel" to 3, "Amós" to 9, "Obadias" to 1,
    "Jonas" to 4, "Miquéias" to 7, "Naum" to 3, "Habacuque" to 3, "Sofonias" to 3, "Ageu" to 2,
    "Zacarias" to 14, "Malaquias" to 4, "Mateus" to 28, "Marcos" to 16, "Lucas" to 24, "João" to 21,
    "Atos" to 28, "Romanos" to 16, "1 Coríntios" to 16, "2 Coríntios" to 13, "Gálatas" to 6,
    "Efésios" to 6, "Filipenses" to 4, "Colossenses" to 4, "1 Tessalonicenses" to 5,
    "2 Tessalonicenses" to 3, "1 Timóteo" to 6, "2 Timóteo" to 4, "Tito" to 3, "Filemom" to 1,
    "Hebreus" to 13, "Tiago" to 5, "1 Pedro" to 5, "2 Pedro" to 3, "1 João" to 5,
    "2 João" to 1, "3 João" to 1, "Judas" to 1, "Apocalipse" to 22
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    initialBook: String? = null,
    initialChapter: Int? = null,
    initialVersion: String? = null,
    onOpenBible: (String, Int, String) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<String?>(initialBook ?: "Gênesis") }
    var selectedChapter by remember { mutableStateOf<Int?>(initialChapter ?: 1) }
    var selectedVersion by remember { mutableStateOf(initialVersion ?: "ARA") }
    var showVersionDialog by remember { mutableStateOf(false) }
    var shouldOpenBible by remember { mutableStateOf(true) }
    
    var showBookSelector by remember { mutableStateOf(false) }
    var selectorStep by remember { mutableStateOf("book") }
    var selectedBookTemp by remember { mutableStateOf<String?>(null) }

    val versions = BollsBibleCatalog.translations.map { it.code to it.name }

    LaunchedEffect(selectedBook, selectedChapter, selectedVersion, shouldOpenBible) {
        if (shouldOpenBible && selectedBook != null && selectedChapter != null) {
            shouldOpenBible = false
            onOpenBible(selectedBook!!, selectedChapter!!, selectedVersion)
        }
    }
    
    LaunchedEffect(selectedBook) {
        if (selectedBook == null) {
            showBookSelector = true
            selectorStep = "book"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top App Bar like YouVersion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Book and Chapter
                TextButton(
                    onClick = { 
                        showBookSelector = true 
                        selectorStep = "book"
                        selectedBookTemp = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = if (selectedBook != null && selectedChapter != null) "$selectedBook $selectedChapter" else "Livros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Selecionar Livro",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Seleção de versão; a escolha abre automaticamente a página no YouVersion.
                TextButton(
                    onClick = { showVersionDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(selectedVersion, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Versão",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Selecione um livro, capítulo ou versão para continuar a leitura…")
            }
        }
        
        // Book Selector Full Screen Overlay
        if (showBookSelector) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        IconButton(onClick = { 
                            if (selectorStep == "chapter") selectorStep = "book" 
                            else showBookSelector = false 
                        }) {
                            Icon(if (selectorStep == "chapter") Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close, contentDescription = "Voltar")
                        }
                        Text(if (selectorStep == "book") "Livros" else selectedBookTemp ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    if (selectorStep == "book") {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(chapterCounts.keys.toList()) { book ->
                                ListItem(
                                    headlineContent = { Text(book, fontWeight = FontWeight.Medium) },
                                    modifier = Modifier.clickable {
                                        selectedBookTemp = book
                                        selectorStep = "chapter"
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }
                        }
                    } else {
                        val maxChaps = chapterCounts[selectedBookTemp] ?: 50
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items((1..maxChaps).toList()) { chapter ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.aspectRatio(1f).clickable {
                                        selectedBook = selectedBookTemp
                                        selectedChapter = chapter
                                        shouldOpenBible = true
                                        showBookSelector = false
                                        selectorStep = "book"
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(chapter.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                title = { Text("Versões da Bíblia") },
                text = {
                    Column {
                        versions.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVersion = code
                                        shouldOpenBible = true
                                        showVersionDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedVersion == code,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
}

