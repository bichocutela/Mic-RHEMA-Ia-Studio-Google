package com.aistudio.micrhema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BollsBibleScreen(
    book: String?,
    chapter: Int?,
    versionCode: String?,
    onBack: () -> Unit,
    onOpenChapter: (String, Int, String) -> Unit,
    onOpenComparison: (String, Int, Int) -> Unit
) {
    val currentBook = book?.takeIf { chapterCounts.containsKey(it) } ?: "Gênesis"
    val currentChapter = chapter?.takeIf { it > 0 } ?: 1
    val currentVersion = BollsBibleCatalog.normalize(versionCode)
    val version = BollsBibleCatalog.translation(currentVersion)
    val maxChapter = chapterCounts[currentBook] ?: 1

    var verses by remember(currentBook, currentChapter, currentVersion) {
        mutableStateOf<List<BibleVerse>>(emptyList())
    }
    var isLoading by remember(currentBook, currentChapter, currentVersion) { mutableStateOf(true) }
    var errorMessage by remember(currentBook, currentChapter, currentVersion) { mutableStateOf<String?>(null) }
    var reloadKey by remember(currentBook, currentChapter, currentVersion) { mutableIntStateOf(0) }
    var showVersionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentBook, currentChapter, currentVersion, reloadKey) {
        isLoading = true
        errorMessage = null
        verses = BollsBibleApi.getChapter(currentBook, currentChapter, currentVersion)
        if (verses.isEmpty()) {
            errorMessage = "Não foi possível carregar este capítulo. Verifique sua conexão e tente novamente."
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$currentBook $currentChapter",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = version.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onOpenComparison(currentBook, currentChapter, 1) }) {
                        Text("Comparar", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showVersionDialog = true }) {
                        Text(version.code, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> RhemaLoadingIndicator(message = "Carregando a Bíblia…")
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { reloadKey++ },
                            modifier = Modifier.padding(top = 20.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Tentar novamente", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(verses, key = { it.verse }) { verseItem ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = verseItem.verse.toString(),
                                        modifier = Modifier.width(30.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = verseItem.text,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 27.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentChapter > 1) {
                                        onOpenChapter(currentBook, currentChapter - 1, currentVersion)
                                    }
                                },
                                enabled = currentChapter > 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Text("Anterior", modifier = Modifier.padding(start = 6.dp))
                            }
                            Text(
                                text = "$currentChapter de $maxChapter",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    if (currentChapter < maxChapter) {
                                        onOpenChapter(currentBook, currentChapter + 1, currentVersion)
                                    }
                                },
                                enabled = currentChapter < maxChapter
                            ) {
                                Text("Próximo", modifier = Modifier.padding(end = 6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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
                    BollsBibleCatalog.translations.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option.code == currentVersion,
                                onClick = {
                                    showVersionDialog = false
                                    onOpenChapter(currentBook, currentChapter, option.code)
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(option.code, fontWeight = FontWeight.Bold)
                                Text(
                                    option.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
