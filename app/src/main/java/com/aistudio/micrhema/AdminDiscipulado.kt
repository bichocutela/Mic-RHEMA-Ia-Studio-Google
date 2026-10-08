package com.aistudio.micrhema

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val discipuladoTimeZone = ZoneId.of("America/Fortaleza")
private fun formatDiscipuladoSchedule(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        .withZone(discipuladoTimeZone).format(Instant.ofEpochMilli(epochMillis))

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

        detectedMime == "application/msword" || extension == "doc" ->
            DiscipuladoLocalFile(displayName.ifBlank { "material.doc" }, "application/msword", "word")
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
    var adminMaterials by remember { mutableStateOf<List<DiscipuladoPdf>>(emptyList()) }
    var scheduledMaterials by remember { mutableStateOf<List<DiscipuladoPdf>>(emptyList()) }
    var editingMaterial by remember { mutableStateOf<DiscipuladoPdf?>(null) }
    var schedulePublication by remember { mutableStateOf(false) }
    var keepHidden by remember { mutableStateOf(false) }
    var scheduledPublishAt by remember { mutableStateOf(0L) }
    var pendingDelete by remember { mutableStateOf<DiscipuladoPdf?>(null) }
    var actionInFlight by remember { mutableStateOf<String?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val registration = Firebase.firestore.collection("discipulado_pdfs")
            .addSnapshotListener { snapshot, error ->
                listError = if (error != null) "Não foi possível carregar os estudos. Tente novamente." else null
                if (snapshot != null) {
                    adminMaterials = snapshot.documents.mapNotNull { document ->
                        runCatching { document.toObject(DiscipuladoPdf::class.java)?.also { it.id = document.id } }.getOrNull()
                    }.sortedWith(compareBy<DiscipuladoPdf> { it.order }.thenByDescending { it.createdAt })
                }
            }
        val schedules = Firebase.firestore.collection("discipulado_schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) listError = "Não foi possível carregar os agendamentos. Tente novamente."
                if (snapshot != null) scheduledMaterials = snapshot.documents.mapNotNull { document ->
                    runCatching { document.toObject(DiscipuladoPdf::class.java)?.also { it.id = document.id } }.getOrNull()
                }
            }
        onDispose { registration.remove(); schedules.remove() }
    }

    fun resetForm() {
        selectedUri = null
        selectedFile = null
        driveUrl = ""
        title = ""
        subtitle = ""
        description = ""
        category = "Estudos bíblicos"
        uploadProgress = 0f
        editingMaterial = null
        schedulePublication = false
        keepHidden = false
        scheduledPublishAt = 0L
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

    val materials = (adminMaterials + scheduledMaterials).distinctBy { it.id }
        .sortedWith(compareBy<DiscipuladoPdf> { it.order }.thenByDescending { it.createdAt })

    fun changeVisibility(material: DiscipuladoPdf, publish: Boolean) {
        if (actionInFlight != null) return
        actionInFlight = material.id
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val updated = material.copy(
                    isPublished = publish,
                    scheduledPublishAt = 0L,
                    releaseNotificationPending = publish,
                    releaseNotificationState = if (publish) "pending" else "",
                    releaseNotificationLeaseUntil = "",
                    publishedAt = if (publish) now else material.publishedAt
                )
                val db = Firebase.firestore
                db.batch()
                    .set(db.collection("discipulado_pdfs").document(material.id), updated, com.google.firebase.firestore.SetOptions.merge())
                    .delete(db.collection("discipulado_schedules").document(material.id))
                    .commit().await()
                if (publish) scope.launch { wakeDiscipuladoPublisher() }
                android.widget.Toast.makeText(
                    context,
                    if (publish) "Estudo publicado para todos." else "Estudo ocultado para os usuários.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (error: Exception) {
                android.util.Log.w("AdminDiscipulado", "Falha ao alterar publicação", error)
                android.widget.Toast.makeText(context, "Não foi possível alterar o estudo. Tente novamente.", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                actionInFlight = null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AdminActionHeader(
            title = "Estudos de Discipulado",
            subtitle = "Organize os estudos e publique agora ou escolha uma data e horário.",
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
        listError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (materials.isEmpty()) {
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
                items(materials, key = { it.id }) { material ->
                    val isWord = material.fileType.equals("word", ignoreCase = true) ||
                        material.fileType.equals("docx", ignoreCase = true)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    material.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    when {
                                        material.scheduledPublishAt > 0 && !material.isPublished -> "Agendado: ${formatDiscipuladoSchedule(material.scheduledPublishAt)}"
                                        material.isPublished -> "Publicado"
                                        else -> "Oculto"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
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
                            IconButton(enabled = actionInFlight == null, onClick = {
                                resetForm()
                                editingMaterial = material
                                title = material.title
                                subtitle = material.subtitle
                                description = material.description
                                category = material.category
                                driveUrl = material.fileUrl
                                schedulePublication = !material.isPublished && material.scheduledPublishAt > 0
                                keepHidden = !material.isPublished && material.scheduledPublishAt <= 0
                                scheduledPublishAt = material.scheduledPublishAt
                                showDialog = true
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar estudo e agendamento") }
                            IconButton(enabled = actionInFlight == null, onClick = { pendingDelete = material }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir estudo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (material.isPublished) {
                                OutlinedButton(
                                    enabled = actionInFlight == null,
                                    onClick = { changeVisibility(material, false) }
                                ) { Text("Ocultar") }
                            } else {
                                Button(
                                    enabled = actionInFlight == null,
                                    onClick = { changeVisibility(material, true) }
                                ) { Text("Publicar agora") }
                            }
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
            title = { Text(if (editingMaterial == null) "Publicar estudo" else "Editar estudo") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = !isUploading,
                        onClick = {
                            documentPicker.launch(
                                arrayOf(
                                    DISCIPULADO_PDF_MIME,
                                    DISCIPULADO_DOCX_MIME,
                                    "application/msword"
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
                    Text("Disponibilização", fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = !isUploading) {
                        schedulePublication = false; keepHidden = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !schedulePublication && !keepHidden, enabled = !isUploading, onClick = {
                            schedulePublication = false; keepHidden = false
                        })
                        Text("Publicar agora")
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = !isUploading) {
                        schedulePublication = true; keepHidden = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = schedulePublication, enabled = !isUploading, onClick = {
                            schedulePublication = true; keepHidden = false
                        })
                        Text("Agendar publicação")
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = !isUploading) {
                        schedulePublication = false; keepHidden = true
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = keepHidden, enabled = !isUploading, onClick = {
                            schedulePublication = false; keepHidden = true
                        })
                        Text("Manter oculto")
                    }
                    if (schedulePublication) {
                        OutlinedButton(enabled = !isUploading, modifier = Modifier.fillMaxWidth(), onClick = {
                            val initial = Instant.ofEpochMilli(scheduledPublishAt.takeIf { it > 0 } ?: System.currentTimeMillis()).atZone(discipuladoTimeZone)
                            android.app.DatePickerDialog(context, { _, year, month, day ->
                                android.app.TimePickerDialog(context, { _, hour, minute ->
                                    scheduledPublishAt = java.time.LocalDateTime.of(year, month + 1, day, hour, minute).atZone(discipuladoTimeZone).toInstant().toEpochMilli()
                                }, if (scheduledPublishAt > 0) initial.hour else 12, if (scheduledPublishAt > 0) initial.minute else 0, true).show()
                            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                        }) { Text(if (scheduledPublishAt > 0) formatDiscipuladoSchedule(scheduledPublishAt) else "Escolher data e horário") }
                        Text("Horário de Fortaleza (Brasília). O estudo será liberado automaticamente, mesmo com o aplicativo fechado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                val hasSource = selectedUri != null || driveUrl.isNotBlank()
                Button(
                    enabled = !isUploading && hasSource && title.isNotBlank(),
                    onClick = {
                        if (schedulePublication && scheduledPublishAt <= System.currentTimeMillis()) {
                            android.widget.Toast.makeText(context, "Escolha uma data e horário futuros.", android.widget.Toast.LENGTH_LONG).show()
                            return@Button
                        }
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
                                } else if (editingMaterial != null && driveUrl.trim() == editingMaterial?.fileUrl) {
                                    materialType = editingMaterial!!.fileType
                                    storagePath = editingMaterial!!.storagePath
                                    fileUrl = editingMaterial!!.fileUrl
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

                                val publishNow = !schedulePublication && !keepHidden
                                val shouldNotify = publishNow &&
                                    (editingMaterial?.isPublished != true || editingMaterial?.releaseNotificationPending == true)
                                val item = DiscipuladoPdf(
                                    id = editingMaterial?.id ?: java.util.UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    subtitle = subtitle.trim(),
                                    description = description.trim(),
                                    category = category.trim().ifBlank { "Estudos bíblicos" },
                                    coverUrl = editingMaterial?.coverUrl.orEmpty(),
                                    pageCount = editingMaterial?.pageCount ?: 0,
                                    storagePath = storagePath,
                                    fileUrl = fileUrl,
                                    fileType = materialType,
                                    order = editingMaterial?.order ?: ((materials.maxOfOrNull { it.order } ?: -1) + 1),
                                    isPublished = publishNow,
                                    scheduledPublishAt = if (schedulePublication) scheduledPublishAt else 0L,
                                    releaseNotificationPending = shouldNotify,
                                    releaseNotificationState = if (shouldNotify) "pending" else if (publishNow) editingMaterial?.releaseNotificationState.orEmpty() else "",
                                    releaseNotificationLeaseUntil = "",
                                    publishedAt = if (publishNow && editingMaterial?.isPublished != true) System.currentTimeMillis() else editingMaterial?.publishedAt ?: 0L,
                                    createdAt = editingMaterial?.createdAt ?: System.currentTimeMillis()
                                )
                                val db = Firebase.firestore
                                val target = if (schedulePublication) "discipulado_schedules" else "discipulado_pdfs"
                                val other = if (schedulePublication) "discipulado_pdfs" else "discipulado_schedules"
                                db.batch().set(db.collection(target).document(item.id), item, com.google.firebase.firestore.SetOptions.merge())
                                    .delete(db.collection(other).document(item.id)).commit().await()
                                if (item.releaseNotificationPending) {
                                    scope.launch { wakeDiscipuladoPublisher() }
                                }
                                // O listener do Firestore já atualiza a lista, inclusive antes
                                // do await retornar. Inserir aqui repete a chave da LazyColumn.
                                android.widget.Toast.makeText(
                                    context,
                                    if (schedulePublication) "Estudo agendado para ${formatDiscipuladoSchedule(scheduledPublishAt)}."
                                    else if (keepHidden) "Estudo salvo como oculto." else "Estudo publicado.",
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
                        Text(if (schedulePublication) "Agendar" else if (keepHidden) "Salvar oculto" else if (editingMaterial != null) "Salvar" else "Publicar")
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

    pendingDelete?.let { material ->
        AlertDialog(
            onDismissRequest = { if (actionInFlight == null) pendingDelete = null },
            title = { Text("Excluir estudo?") },
            text = { Text("O estudo \"${material.title}\" será removido da biblioteca e dos agendamentos. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(enabled = actionInFlight == null, onClick = {
                    actionInFlight = material.id
                    scope.launch {
                        try {
                            val db = Firebase.firestore
                            db.batch()
                                .delete(db.collection("discipulado_pdfs").document(material.id))
                                .delete(db.collection("discipulado_schedules").document(material.id))
                                .commit().await()
                            pendingDelete = null
                            android.widget.Toast.makeText(context, "Estudo excluído.", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (error: Exception) {
                            android.util.Log.w("AdminDiscipulado", "Falha ao excluir", error)
                            android.widget.Toast.makeText(context, "Não foi possível excluir. Tente novamente.", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            actionInFlight = null
                        }
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = actionInFlight == null, onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
