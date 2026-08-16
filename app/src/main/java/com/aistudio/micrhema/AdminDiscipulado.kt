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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun EditDiscipuladoSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Estudos bíblicos") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            title = ""
            subtitle = ""
            description = ""
            category = "Estudos bíblicos"
            showDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Estudos de Discipulado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Publique PDFs para todos os usuários do aplicativo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { pdfPicker.launch("application/pdf") }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar PDF")
                Spacer(Modifier.size(6.dp))
                Text("Novo PDF")
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isUploading) {
            LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Enviando PDF com segurança…", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }
        if (discipuladoPdfsState.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text("Nenhum estudo publicado ainda", fontWeight = FontWeight.Bold)
                    Text("Adicione o primeiro material em PDF para liberar a biblioteca pública.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(discipuladoPdfsState, key = { it.id }) { pdf ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pdf.title, fontWeight = FontWeight.Bold)
                                Text(pdf.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                if (pdf.description.isNotBlank()) {
                                    Text(
                                        pdf.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = {
                                Firebase.firestore.collection("discipulado_pdfs").document(pdf.id).delete()
                                discipuladoPdfsState.removeIf { it.id == pdf.id }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir PDF", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) { showDialog = false; selectedUri = null } },
            title = { Text("Publicar estudo em PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do estudo *") },
                        placeholder = { Text("Ex.: Fundamentos da Fé") },
                        supportingText = { Text("O título será exibido na biblioteca pública.") },
                        singleLine = true,
                        isError = title.isBlank()
                    )
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtítulo") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") }, singleLine = true)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição do estudo") },
                        placeholder = { Text("Explique brevemente o que o aluno encontrará neste PDF.") },
                        supportingText = { Text("A descrição é opcional e aparecerá no cartão do estudo.") },
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploading && selectedUri != null && title.isNotBlank(),
                    onClick = {
                        val uri = selectedUri
                        if (uri != null) {
                            isUploading = true
                            scope.launch {
                            try {
                                val uploaded = StorageManager.uploadChurchDocument(
                                    context = context,
                                    uri = uri,
                                    uid = "admin",
                                    onProgress = { uploadProgress = it },
                                    mimeTypeHint = "application/pdf"
                                )
                                val item = DiscipuladoPdf(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    subtitle = subtitle.trim(),
                                    description = description.trim(),
                                    category = category.trim().ifBlank { "Estudos bíblicos" },
                                    storagePath = uploaded.storagePath,
                                    fileUrl = uploaded.signedUrl,
                                    order = discipuladoPdfsState.size,
                                    isPublished = true
                                )
                                Firebase.firestore.collection("discipulado_pdfs").document(item.id).set(item)
                                discipuladoPdfsState.add(item)
                                android.widget.Toast.makeText(context, "Estudo publicado para todos os usuários.", android.widget.Toast.LENGTH_SHORT).show()
                                showDialog = false
                                selectedUri = null
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Não foi possível publicar o PDF: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                                uploadProgress = 0f
                            }
                            }
                        }
                    }
                ) { if (isUploading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Publicar") }
            },
            dismissButton = { TextButton(enabled = !isUploading, onClick = { showDialog = false; selectedUri = null }) { Text("Cancelar") } }
        )
    }
}
