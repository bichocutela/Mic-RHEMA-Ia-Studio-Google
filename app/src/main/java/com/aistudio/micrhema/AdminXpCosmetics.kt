package com.aistudio.micrhema

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminXpCosmeticsSection(kind: String) {
    val scope = rememberCoroutineScope()
    val label = if (kind == "moldura") "Moldura" else "Distintivo"
    val plural = if (kind == "moldura") "Molduras" else "Distintivos"
    var items by remember(kind) { mutableStateOf<List<AdminProfileCosmetic>>(emptyList()) }
    var loading by remember(kind) { mutableStateOf(false) }
    var error by remember(kind) { mutableStateOf("") }
    var editing by remember(kind) { mutableStateOf<AdminProfileCosmetic?>(null) }
    var showEditor by remember(kind) { mutableStateOf(false) }

    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching { XpShopAdminClient.loadCosmetics(kind) }
                .onSuccess { items = it }
                .onFailure { error = it.message ?: "Não foi possível carregar $plural." }
            loading = false
        }
    }

    LaunchedEffect(kind) { refresh() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { editing = null; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Novo $label")
        }
        Text(
            "$plural em PNG transparente, com nome, descrição e desafio verificável pelo sistema.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (loading && items.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        AdminPagedList(
            items = items,
            modifier = Modifier.weight(1f),
            key = { it.id },
            emptyContent = {
                if (!loading) Text("Nenhum ${label.lowercase()} personalizado criado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text(if (item.active) "Ativo" else "Inativo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
                    Text("Desafio: ${item.challenge}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { editing = item; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar $label")
                    }
                }
            }
        }
    }

    if (showEditor) {
        AdminXpCosmeticEditor(
            kind = kind,
            initial = editing,
            onDismiss = { showEditor = false; editing = null },
            onSaved = {
                showEditor = false
                editing = null
                refresh()
            }
        )
    }
}

@Composable
private fun AdminXpCosmeticEditor(
    kind: String,
    initial: AdminProfileCosmetic?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val label = if (kind == "moldura") "Moldura" else "Distintivo"
    val assetType = if (kind == "moldura") "frame" else "badge"
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var challenge by remember(initial?.id) { mutableStateOf(initial?.challenge.orEmpty()) }
    var difficulty by remember(initial?.id) { mutableStateOf("easy") }
    var generated by remember(initial?.id) { mutableStateOf<GeneratedBadgeChallenge?>(null) }
    var imageRef by remember(initial?.id) { mutableStateOf(initial?.imageRef.orEmpty()) }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var uploading by remember(initial?.id) { mutableStateOf(false) }
    var loadingChallenge by remember(initial?.id) { mutableStateOf(false) }
    var saving by remember(initial?.id) { mutableStateOf(false) }
    var progress by remember(initial?.id) { mutableStateOf(0f) }
    var error by remember(initial?.id) { mutableStateOf("") }

    fun requestChallenge(level: String = difficulty) {
        if (loadingChallenge || saving) return
        difficulty = level
        loadingChallenge = true
        error = ""
        scope.launch {
            runCatching { XpShopAdminClient.generateBadgeChallenge(level, generated?.text.orEmpty()) }
                .onSuccess { generated = it; challenge = it.text }
                .onFailure { error = it.message ?: "Não foi possível buscar outro desafio." }
            loadingChallenge = false
        }
    }

    LaunchedEffect(initial?.id) {
        if (initial == null && challenge.isBlank()) requestChallenge("easy")
    }

    fun upload(uri: Uri?) {
        if (uri == null || uploading) return
        uploading = true
        progress = 0f
        error = ""
        scope.launch {
            runCatching {
                StorageManager.uploadMediaAsset(
                    context = context,
                    uri = uri,
                    uid = StorageManager.resolveStorageTargetUid(context),
                    onProgress = { progress = it },
                    mimeTypeHint = "image/png"
                )
            }.onSuccess { result -> imageRef = buildXpShopAssetRef(assetType, result) }
                .onFailure { error = it.message ?: "Não foi possível enviar o PNG." }
            uploading = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { upload(it) }
    val busy = uploading || loadingChallenge || saving

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (initial == null) "Novo $label" else "Editar $label") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 610.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = "" }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Text("Dificuldade do desafio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach { (key, text) ->
                        FilterChip(
                            selected = difficulty == key,
                            onClick = { requestChallenge(key) },
                            enabled = !busy,
                            label = { Text(text) }
                        )
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Desafio", fontWeight = FontWeight.SemiBold)
                        Text(challenge.ifBlank { "Buscando desafio…" })
                    }
                }
                OutlinedButton(enabled = !busy, onClick = { requestChallenge(difficulty) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Buscar outro desafio")
                }

                OutlinedButton(enabled = !busy, onClick = { launcher.launch("image/png") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (imageRef.isBlank()) "Enviar PNG transparente" else "Trocar PNG")
                }
                if (uploading) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                } else if (imageRef.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PNG enviado", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Disponível", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = active, onCheckedChange = { active = it }, enabled = !busy)
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = {
            Button(enabled = !busy, onClick = {
                when {
                    name.isBlank() -> error = "Informe o nome."
                    challenge.isBlank() -> error = "Escolha um desafio."
                    imageRef.isBlank() -> error = "Envie o PNG."
                    else -> {
                        saving = true
                        scope.launch {
                            runCatching {
                                XpShopAdminClient.saveCosmetic(
                                    AdminProfileCosmetic(
                                        id = initial?.id.orEmpty(),
                                        kind = kind,
                                        name = name.trim(),
                                        description = description.trim(),
                                        challenge = challenge.trim(),
                                        imageRef = imageRef,
                                        active = active
                                    )
                                )
                            }.onSuccess { onSaved() }
                                .onFailure { error = it.message ?: "Não foi possível salvar $label." }
                            saving = false
                        }
                    }
                }
            }) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(7.dp))
                }
                Text(if (saving) "Salvando…" else "Salvar")
            }
        }
    )
}
