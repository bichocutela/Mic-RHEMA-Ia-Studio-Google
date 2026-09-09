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
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminXpBadgesSection() {
    val scope = rememberCoroutineScope()
    var badges by remember { mutableStateOf<List<AdminCustomBadge>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminCustomBadge?>(null) }

    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching { XpShopAdminClient.loadBadges() }
                .onSuccess { badges = it }
                .onFailure { error = it.message ?: "Não foi possível carregar os emblemas." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { editing = null; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Novo emblema")
        }

        Text(
            "Crie quantos emblemas quiser. Os normais continuam automaticamente como 23, 24, 25…; emblemas especiais ficam fora da numeração.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (loading && badges.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            badges.forEach { badge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (badge.special) badge.name else "Emblema ${badge.sequenceNo ?: "?"} · ${badge.name}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(if (badge.special) "Especial" else "Catálogo numerado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(if (badge.active) "Ativo" else "Inativo", style = MaterialTheme.typography.labelMedium)
                        }
                        if (badge.description.isNotBlank()) Text(badge.description, style = MaterialTheme.typography.bodySmall)
                        Text("Desafio: ${badge.challenge}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { editing = badge; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Editar emblema")
                        }
                    }
                }
            }
            if (!loading && badges.isEmpty()) {
                Text("Nenhum emblema personalizado criado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showEditor) {
        AdminXpBadgeEditor(
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
private fun AdminXpBadgeEditor(
    initial: AdminCustomBadge?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var challenge by remember(initial?.id) { mutableStateOf(initial?.challenge.orEmpty()) }
    var challengeDifficulty by remember(initial?.id) { mutableStateOf("easy") }
    var generatedChallenge by remember(initial?.id) { mutableStateOf<GeneratedBadgeChallenge?>(null) }
    var loadingChallenge by remember(initial?.id) { mutableStateOf(false) }
    var imageRef by remember(initial?.id) { mutableStateOf(initial?.imageRef.orEmpty()) }
    var special by remember(initial?.id) { mutableStateOf(initial?.special ?: false) }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var uploading by remember(initial?.id) { mutableStateOf(false) }
    var saving by remember(initial?.id) { mutableStateOf(false) }
    var progress by remember(initial?.id) { mutableStateOf(0f) }
    var error by remember(initial?.id) { mutableStateOf("") }

    fun requestChallenge(difficulty: String = challengeDifficulty, excludeCurrent: Boolean = true) {
        if (loadingChallenge || saving) return
        challengeDifficulty = difficulty
        loadingChallenge = true
        error = ""
        scope.launch {
            runCatching {
                XpShopAdminClient.generateBadgeChallenge(
                    difficulty = difficulty,
                    exclude = if (excludeCurrent) generatedChallenge?.text.orEmpty() else ""
                )
            }.onSuccess { generated ->
                generatedChallenge = generated
            }.onFailure {
                error = it.message ?: "Não foi possível buscar um desafio agora."
            }
            loadingChallenge = false
        }
    }

    LaunchedEffect(initial?.id) {
        if (initial == null && challenge.isBlank()) requestChallenge(challengeDifficulty, excludeCurrent = false)
    }

    fun upload(uri: Uri?) {
        if (uri == null || uploading) return
        uploading = true
        progress = 0f
        error = ""
        scope.launch {
            runCatching {
                val targetUid = StorageManager.resolveStorageTargetUid(context)
                StorageManager.uploadMediaAsset(
                    context = context,
                    uri = uri,
                    uid = targetUid,
                    onProgress = { progress = it },
                    mimeTypeHint = "image/png"
                )
            }.onSuccess { result ->
                imageRef = buildXpShopAssetRef("emblem", result)
            }.onFailure { error = it.message ?: "Não foi possível enviar o PNG." }
            uploading = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { upload(it) }
    val busy = uploading || saving || loadingChallenge
    val candidateAccepted = generatedChallenge?.text?.let { it == challenge } == true

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Default.MilitaryTech, contentDescription = null) },
        title = { Text(if (initial == null) "Novo emblema" else "Editar emblema") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = "" }, label = { Text("Nome do emblema") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Text("Dificuldade do desafio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach { (key, label) ->
                        FilterChip(
                            selected = challengeDifficulty == key,
                            onClick = { if (challengeDifficulty != key) requestChallenge(key, excludeCurrent = false) },
                            enabled = !busy,
                            label = { Text(label) }
                        )
                    }
                }

                generatedChallenge?.let { generated ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Sugestão de desafio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(generated.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Detecção automática pronta · meta ${generated.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    challenge = generated.text
                                    error = ""
                                },
                                enabled = !busy && !candidateAccepted,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (candidateAccepted) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(7.dp))
                                }
                                Text(if (candidateAccepted) "Desafio escolhido" else "Usar este desafio")
                            }
                        }
                    }
                } ?: Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (loadingChallenge) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (loadingChallenge) "Buscando um desafio verificável…" else "Escolha uma dificuldade para buscar um desafio.")
                    }
                }

                OutlinedButton(
                    enabled = !busy,
                    onClick = { requestChallenge(challengeDifficulty, excludeCurrent = true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Buscar outro desafio")
                }

                if (challenge.isNotBlank()) {
                    Text(
                        "Desafio que será salvo: $challenge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Os desafios são escolhidos apenas entre atividades que o MIC Rhema consegue verificar no servidor. Assim o desbloqueio não depende de interpretação de texto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(enabled = !busy, onClick = { launcher.launch("image/png") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (imageRef.isBlank()) "Enviar emblema PNG transparente" else "Trocar emblema PNG")
                }
                if (uploading) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("Enviando… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                } else if (imageRef.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PNG enviado", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Emblema especial", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (special) "Não usa número de nível." else "Receberá automaticamente o próximo número a partir do 23.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = special, onCheckedChange = { special = it }, enabled = !busy)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Disponível no catálogo", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = active, onCheckedChange = { active = it }, enabled = !busy)
                }

                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(enabled = !busy, onClick = {
                when {
                    name.isBlank() -> error = "Informe o nome do emblema."
                    challenge.isBlank() -> error = "Escolha e confirme um desafio para o emblema."
                    imageRef.isBlank() -> error = "Envie o arquivo PNG do emblema."
                    else -> {
                        saving = true
                        error = ""
                        scope.launch {
                            runCatching {
                                XpShopAdminClient.saveBadge(
                                    AdminCustomBadge(
                                        id = initial?.id.orEmpty(),
                                        sequenceNo = initial?.sequenceNo,
                                        name = name.trim(),
                                        description = description.trim(),
                                        challenge = challenge.trim(),
                                        imageRef = imageRef,
                                        special = special,
                                        active = active
                                    )
                                )
                            }.onSuccess {
                                RemoteBadgeEngineClient.refreshCatalog(force = true)
                                onSaved()
                            }.onFailure { error = it.message ?: "Não foi possível salvar o emblema." }
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
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } }
    )
}
