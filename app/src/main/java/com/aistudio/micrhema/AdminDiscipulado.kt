package com.aistudio.micrhema

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val DISCIPULADO_PDF_MIME = "application/pdf"
private const val DISCIPULADO_DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

private data class DiscipuladoLocalFile(
    val displayName: String,
    val mimeType: String,
    val fileType: String
)

private fun resolveDiscipuladoLocalFile(
    context: android.content.Context,
    uri: android.net.Uri
): DiscipuladoLocalFile? {
    val displayName = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
        }
    }.getOrDefault("").orEmpty()

    val detectedMime = context.contentResolver.getType(uri).orEmpty().lowercase().substringBefore(';')
    val extension = displayName.substringAfterLast('.', "").lowercase()
    return when {
        detectedMime == DISCIPULADO_PDF_MIME || extension == "pdf" ->
            DiscipuladoLocalFile(displayName.ifBlank { "material.pdf" }, DISCIPULADO_PDF_MIME, "pdf")

        detectedMime == DISCIPULADO_DOCX_MIME || extension == "docx" ->
            DiscipuladoLocalFile(displayName.ifBlank { "material.docx" }, DISCIPULADO_DOCX_MIME, "word")

        else -> null
    }
}

private fun isWebLink(value: String): Boolean {
    val normalized = value.trim()
    return normalized.startsWith("https://", ignoreCase = true) ||
        normalized.startsWith("http://", ignoreCase = true)
}

@Composable
fun EditDiscipuladoSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFile by remember { mutableStateOf<DiscipuladoLocalFile?>(null) }
    var driveUrl by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Estudos bíblicos") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    fun resetForm() {
        selectedUri = null
        selectedFile = null
        driveUrl = ""
        title = ""
        subtitle = ""
        description = ""
        category = "Estudos bíblicos"
        uploadProgress = 0f
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val resolved = resolveDiscipuladoLocalFile(context, uri)
            if (resolved == null) {
                android.widget.Toast.makeText(
                    context,
                    "Selecione um PDF ou arquivo Word no formato DOCX.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                selectedUri = uri
                selectedFile = resolved
                driveUrl = ""
                if (title.isBlank()) {
                    title = resolved.displayName.substringBeforeLast('.').ifBlank { title }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AdminActionHeader(
            title = "Estudos de Discipulado",
            subtitle = "Publique PDFs e arquivos Word para todos os usuários do aplicativo.",
            actionText = "Novo material",
            onAction = {
                resetForm()
                showDialog = true
            }
        )
        Spacer(Modifier.height(16.dp))
        if (isUploading) {
            LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Publicando material com segurança…", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }
        if (discipuladoPdfsState.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Nenhum estudo publicado ainda", fontWeight = FontWeight.Bold)
                    Text(
                        "Adicione o primeiro material em PDF ou Word para liberar a biblioteca pública.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(discipuladoPdfsState, key = { it.id }) { material ->
                    val isWord = material.fileType.equals("word", ignoreCase = true) ||
                        material.fileType.equals("docx", ignoreCase = true)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isWord) Icons.Default.Description else Icons.Default.PictureAsPdf,
                                contentDescription = if (isWord) "Word" else "PDF",
                                tint = if (isWord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(material.title, fontWeight = FontWeight.Bold)
                                Text(
                                    "${if (isWord) "Word" else "PDF"} • ${material.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (material.description.isNotBlank()) {
                                    Text(
                                        material.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = {
                                Firebase.firestore.collection("discipulado_pdfs").document(material.id).delete()
                                discipuladoPdfsState.removeIf { it.id == material.id }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir material",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUploading) {
                    showDialog = false
                    resetForm()
                }
            },
            title = { Text("Publicar material de estudo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = !isUploading,
                        onClick = {
                            documentPicker.launch(
                                arrayOf(
                                    DISCIPULADO_PDF_MIME,
                                    DISCIPULADO_DOCX_MIME
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Selecionar PDF ou Word")
                    }

                    selectedFile?.let { file ->
                        Text(
                            "Arquivo selecionado: ${file.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        "ou",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    OutlinedTextField(
                        value = driveUrl,
                        onValueChange = { value ->
                            driveUrl = value
                            if (value.isNotBlank()) {
                                selectedUri = null
                                selectedFile = null
                            }
                        },
                        label = { Text("Link do Google Drive") },
                        placeholder = { Text("Cole o link de um PDF, DOCX ou Google Docs") },
                        supportingText = {
                            Text("Use um arquivo do aparelho ou um link do Drive. O mesmo cadastro serve para os dois.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do estudo *") },
                        placeholder = { Text("Ex.: Fundamentos da Fé") },
                        supportingText = { Text("O título será exibido na biblioteca pública.") },
                        singleLine = true,
                        isError = title.isBlank()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtítulo") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição do estudo") },
                        placeholder = { Text("Explique brevemente o que o aluno encontrará neste material.") },
                        supportingText = { Text("A descrição é opcional e aparecerá no cartão do estudo.") },
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                val hasSource = selectedUri != null || driveUrl.isNotBlank()
                Button(
                    enabled = !isUploading && hasSource && title.isNotBlank(),
                    onClick = {
                        isUploading = true
                        scope.launch {
                            try {
                                val localUri = selectedUri
                                val localFile = selectedFile

                                val materialType: String
                                val storagePath: String
                                val fileUrl: String

                                if (localUri != null && localFile != null) {
                                    val uploaded = StorageManager.uploadMediaAsset(
                                        context = context,
                                        uri = localUri,
                                        uid = "admin",
                                        onProgress = { uploadProgress = it },
                                        mimeTypeHint = localFile.mimeType
                                    )
                                    materialType = localFile.fileType
                                    storagePath = uploaded.storagePath
                                    fileUrl = uploaded.signedUrl
                                } else {
                                    val link = driveUrl.trim()
                                    if (!isWebLink(link)) {
                                        throw IllegalArgumentException("Informe um link válido do Google Drive.")
                                    }
                                    val detected = GoogleDriveService.identifyFileType(link)
                                    materialType = when (detected) {
                                        GoogleDriveService.FileType.PDF -> "pdf"
                                        GoogleDriveService.FileType.WORD -> "word"
                                        else -> throw IllegalArgumentException(
                                            "O link precisa apontar para um PDF, DOCX ou documento do Google Docs."
                                        )
                                    }
                                    storagePath = ""
                                    fileUrl = link
                                    uploadProgress = 1f
                                }

                                val item = DiscipuladoPdf(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    subtitle = subtitle.trim(),
                                    description = description.trim(),
                                    category = category.trim().ifBlank { "Estudos bíblicos" },
                                    storagePath = storagePath,
                                    fileUrl = fileUrl,
                                    fileType = materialType,
                                    order = discipuladoPdfsState.size,
                                    isPublished = true
                                )
                                Firebase.firestore.collection("discipulado_pdfs").document(item.id).set(item).await()
                                discipuladoPdfsState.add(item)
                                android.widget.Toast.makeText(
                                    context,
                                    "Material publicado para todos os usuários.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                showDialog = false
                                resetForm()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Não foi possível publicar: ${error.message ?: "verifique o arquivo ou link"}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isUploading = false
                                uploadProgress = 0f
                            }
                        }
                    }
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Publicar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isUploading,
                    onClick = {
                        showDialog = false
                        resetForm()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
