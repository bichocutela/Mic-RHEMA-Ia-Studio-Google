package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleComparisonScreen(
    initialBook: String?,
    initialChapter: Int?,
    initialVerse: Int?,
    onBack: () -> Unit
) {
    var selectedBook by remember {
        mutableStateOf(initialBook?.takeIf { chapterCounts.containsKey(it) } ?: "Gênesis")
    }
    var selectedChapter by remember { mutableStateOf(initialChapter?.takeIf { it > 0 } ?: 1) }
    var verseInput by remember { mutableStateOf((initialVerse?.takeIf { it > 0 } ?: 1).toString()) }
    var selectedVersions by remember {
        mutableStateOf(
            listOf("ARA", "NVI").filter { code ->
                BollsBibleCatalog.translations.any { it.code == code }
            }
        )
    }
    var comparison by remember { mutableStateOf<List<Pair<BollsBibleCatalog.Translation, BibleVerse?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBookDialog by remember { mutableStateOf(false) }
    var showChapterDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val verse = verseInput.toIntOrNull()
    val selectedVersionKey = selectedVersions.joinToString(",")

    fun compareVerse() {
        if (verse == null || verse < 1 || selectedVersions.size < 2) return
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            comparison = BollsBibleApi.getVerseComparison(
                book = selectedBook,
                chapter = selectedChapter,
                verse = verse,
                versionCodes = selectedVersions
            )
            if (comparison.none { it.second != null }) {
                errorMessage = "Não foi possível carregar esse versículo. Verifique a referência e sua conexão."
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedBook, selectedChapter, verse, selectedVersionKey) {
        compareVerse()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comparar versões", fontWeight = FontWeight.Bold)
                        Text(
                            "$selectedBook $selectedChapter:$verseInput",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Leia o mesmo versículo em diferentes traduções",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showBookDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedBook, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showChapterDialog = true },
                        modifier = Modifier.width(116.dp)
                    ) {
                        Text("Cap. $selectedChapter")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = verseInput,
                        onValueChange = { value ->
                            verseInput = value.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Versículo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(130.dp)
                    )
                    OutlinedButton(
                        onClick = { showVersionDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (selectedVersions.isEmpty()) "Escolher versões" else selectedVersions.joinToString("  •  "),
                            maxLines = 1
                        )
                    }
                }
                if (selectedVersions.size < 2) {
                    Text(
                        "Escolha pelo menos duas versões para comparar.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { compareVerse() },
                    enabled = verse != null && verse > 0 && selectedVersions.size >= 2 && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null)
                    Text("Comparar versículo", modifier = Modifier.padding(start = 8.dp))
                }
            }

            when {
                isLoading -> RhemaLoadingIndicator(message = "Comparando versões…")
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { compareVerse() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comparison, key = { it.first.code }) { (translation, verseItem) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        translation.code,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        translation.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        verseItem?.text ?: "Versículo indisponível nesta versão.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(27f, androidx.compose.ui.unit.TextUnitType.Sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBookDialog) {
        AlertDialog(
            onDismissRequest = { showBookDialog = false },
            title = { Text("Escolher livro") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(chapterCounts.keys.toList()) { book ->
                        TextButton(
                            onClick = {
                                selectedBook = book
                                selectedChapter = 1
                                showBookDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(book, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showChapterDialog) {
        AlertDialog(
            onDismissRequest = { showChapterDialog = false },
            title = { Text("Escolher capítulo de $selectedBook") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((1..(chapterCounts[selectedBook] ?: 1)).toList()) { chapterItem ->
                        OutlinedButton(
                            onClick = {
                                selectedChapter = chapterItem
                                showChapterDialog = false
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(chapterItem.toString())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Escolher versões") },
            text = {
                Column {
                    Text(
                        "Selecione duas ou mais versões.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 390.dp)) {
                        items(BollsBibleCatalog.translations) { translation ->
                            val checked = selectedVersions.contains(translation.code)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVersions = if (checked) {
                                            selectedVersions.filterNot { it == translation.code }
                                        } else {
                                            selectedVersions + translation.code
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(translation.code, fontWeight = FontWeight.Bold)
                                    Text(
                                        translation.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (checked) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selecionada",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Concluir")
                }
            }
        )
    }
}
