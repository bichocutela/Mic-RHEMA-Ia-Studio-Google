package com.aistudio.micrhema

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.micrhema.ui.theme.MICRhemaTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShareImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = when (SettingsManager.getThemeMode(this@ShareImportActivity)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MICRhemaTheme(darkTheme = darkTheme) {
                ShareImportScreen(intent)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ShareImportScreen(sharedIntent: Intent) {
        val scope = rememberCoroutineScope()
        var candidate by remember { mutableStateOf<SharedDocumentCandidate?>(null) }
        var resolving by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var title by remember { mutableStateOf("") }
        var importing by remember { mutableStateOf(false) }
        var progress by remember { mutableFloatStateOf(0f) }
        var importedBook by remember { mutableStateOf<ContentBook?>(null) }
        val adminReady = adminAuthenticatedState.value || loggedInMemberState.value?.isAdmin == true

        LaunchedEffect(sharedIntent) {
            resolving = true
            error = null
            runCatching { SharedDocumentImport.resolve(this@ShareImportActivity, sharedIntent) }
                .onSuccess {
                    candidate = it
                    title = it.suggestedTitle
                }
                .onFailure { error = it.message ?: "Não foi possível receber o arquivo compartilhado." }
            resolving = false
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Importar para o IBR") },
                    navigationIcon = { TextButton(onClick = { finish() }) { Text("Fechar") } }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    resolving -> {
                        Spacer(Modifier.weight(1f))
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        Text("Recebendo o arquivo compartilhado…", modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.weight(1f))
                    }

                    error != null && candidate == null -> {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(42.dp))
                        Text("Não foi possível importar", style = MaterialTheme.typography.titleLarge)
                        Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { finish() }) { Text("Fechar") }
                    }

                    importedBook != null -> {
                        Spacer(Modifier.height(12.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                        Text("Arquivo adicionado ao IBR", style = MaterialTheme.typography.headlineSmall)
                        Text("${importedBook!!.title} foi copiado para o armazenamento do MIC Rhema e publicado no conteúdo do IBR.")
                        Button(
                            onClick = {
                                startActivity(
                                    DocumentReaderActivity.intent(
                                        this@ShareImportActivity,
                                        importedBook!!.bookUrl,
                                        importedBook!!.title,
                                        importedBook!!.type
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir no leitor do MIC Rhema")
                        }
                        OutlinedButton(
                            onClick = {
                                startActivity(
                                    Intent(this@ShareImportActivity, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                )
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Voltar para o MIC Rhema") }
                    }

                    candidate != null -> {
                        val current = candidate!!
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(current.type.label, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(current.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(formatBytes(current.sizeBytes), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    when (current.type) {
                                        SharedDocumentType.PDF -> "Leitor atribuído: PDF interno"
                                        SharedDocumentType.DOCX -> "Leitor atribuído: Word interno"
                                        SharedDocumentType.EPUB -> "Leitor atribuído: EPUB interno"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título para o IBR") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!adminReady) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Acesso administrativo necessário", style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text(
                                        "O arquivo já foi preservado no cache. Entre na Área Administrativa e depois volte para esta tela para publicar.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = {
                                            startActivity(
                                                Intent(this@ShareImportActivity, MainActivity::class.java)
                                                    .putExtra(NotificationHelper.EXTRA_NOTIFICATION_DESTINATION, Screen.Admin.route)
                                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Entrar na Área Administrativa") }
                                }
                            }
                        } else {
                            if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            if (importing) {
                                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                Text("Enviando para o MIC Rhema… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                enabled = !importing && title.isNotBlank(),
                                onClick = {
                                    importing = true
                                    progress = 0f
                                    error = null
                                    scope.launch {
                                        runCatching { importCandidate(current, title.trim()) { progress = it } }
                                            .onSuccess { importedBook = it }
                                            .onFailure { error = it.message ?: "Falha ao publicar o arquivo no IBR." }
                                        importing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Adicionar ao conteúdo do IBR")
                            }
                        }

                        Text(
                            "Compatível com o compartilhamento do Xodo e de outros aplicativos Android. PDF, EPUB e Word .docx são identificados automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private suspend fun importCandidate(
        candidate: SharedDocumentCandidate,
        title: String,
        onProgress: (Float) -> Unit
    ): ContentBook {
        val publicUrl = StorageHelper.uploadFile(
            context = this,
            uri = candidate.localUri,
            path = "uploads",
            mimeTypeHint = candidate.type.mimeType,
            onProgress = onProgress
        )
        require(publicUrl.isNotBlank()) { "O upload terminou sem retornar o endereço do arquivo." }

        val book = ContentBook(
            id = System.currentTimeMillis().toString(),
            title = title.ifBlank { candidate.suggestedTitle },
            author = "Compartilhado pelo Android",
            coverUrl = "",
            contentText = "",
            bookUrl = publicUrl,
            type = candidate.type.contentBookType,
            isApproved = true
        )
        if (BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            Firebase.firestore.collection("vip_books").document(book.id).set(book).await()
        } else {
            throw IllegalStateException("O Firebase do IBR não está configurado nesta versão.")
        }
        withContext(Dispatchers.Main) {
            if (vipBooksState.none { it.id == book.id }) vipBooksState.add(book)
        }
        return book
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes bytes"
    }
}
