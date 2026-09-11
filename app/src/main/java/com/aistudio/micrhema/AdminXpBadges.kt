package com.aistudio.micrhema

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun nativeAdminBadges(): List<AdminCustomBadge> =
    (biblicalLevelBadges + simpleBiblicalBadges).map { badge ->
        AdminCustomBadge(
            id = badge.id,
            sequenceNo = badge.level,
            name = badge.name,
            description = badge.description,
            challenge = badge.requirement,
            imageRef = buildBuiltinEmblemRef(badge.id),
            special = false,
            active = true
        )
    }

@Composable
private fun AdminBadgeArtwork(
    badge: AdminCustomBadge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedUrl by produceState<String?>(initialValue = null, badge.imageRef) {
        value = runCatching {
            resolveXpShopAssetUrl(context.applicationContext, badge.imageRef)
        }.getOrNull()
    }
    val imageModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Surface(
        modifier = imageModifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!resolvedUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = resolvedUrl,
                    contentDescription = "Imagem do emblema ${badge.name}",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize(0.46f)
                )
            }
        }
    }
}

@Composable
fun AdminXpBadgesSection() {
    val scope = rememberCoroutineScope()
    var badges by remember { mutableStateOf<List<AdminCustomBadge>>(nativeAdminBadges()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AdminCustomBadge?>(null) }
    var preview by remember { mutableStateOf<AdminCustomBadge?>(null) }
    var savingId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching { XpShopAdminClient.loadBadges() }
                .onSuccess { remote ->
                    val remoteById = remote.associateBy { it.id }
                    val mergedNative = nativeAdminBadges().map { remoteById[it.id] ?: it }
                    val nativeIds = mergedNative.map { it.id }.toSet()
                    val extras = remote.filterNot { it.id in nativeIds }
                    badges = (mergedNative + extras).sortedWith(
                        compareBy<AdminCustomBadge> { it.special }
                            .thenBy { it.sequenceNo ?: Int.MAX_VALUE }
                            .thenBy { it.name.lowercase() }
                    )
                }
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
            "Todos os emblemas do app aparecem aqui para edição. Toque na imagem para ampliar. Os níveis 1–22 e as conquistas nativas preservam a arte atual até você trocar o PNG; novos emblemas continuam em 23, 24, 25… ou podem ser especiais.",
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

        AdminPagedList(
            items = badges,
            modifier = Modifier.weight(1f),
            key = { it.id },
            emptyContent = {
                if (!loading) Text("Nenhum emblema disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        ) { badge ->
            val nativeAchievement = isNativeAchievementBadgeId(badge.id)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdminBadgeArtwork(
                            badge = badge,
                            modifier = Modifier.size(58.dp),
                            onClick = { preview = badge }
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    badge.special -> badge.name
                                    badge.sequenceNo != null -> "Emblema ${badge.sequenceNo} · ${badge.name}"
                                    else -> badge.name
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (badge.active) "Ativo" else "Inativo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                when {
                                    badge.special -> "Especial"
                                    nativeAchievement -> "Conquista nativa do app"
                                    (badge.sequenceNo ?: 0) in 1..22 -> "Emblema nativo do app"
                                    else -> "Catálogo numerado"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = badge.active,
                            onCheckedChange = { enabled ->
                                savingId = badge.id
                                error = ""
                                scope.launch {
                                    runCatching {
                                        XpShopAdminClient.saveBadge(badge.copy(active = enabled))
                                    }.onSuccess { saved ->
                                        badges = badges.map { if (it.id == saved.id) saved else it }
                                        RemoteBadgeEngineClient.refreshCatalog(force = true)
                                    }.onFailure {
                                        error = it.message ?: "Não foi possível alterar a disponibilidade do emblema."
                                    }
                                    savingId = null
                                }
                            },
                            enabled = savingId == null
                        )
                    }
                    if (badge.description.isNotBlank()) Text(badge.description, style = MaterialTheme.typography.bodySmall)
                    Text("Desafio: ${badge.challenge}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { editing = badge; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar emblema")
                    }
                }
            }
        }
    }

    preview?.let { badge ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(badge.name) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminBadgeArtwork(
                        badge = badge,
                        modifier = Modifier.size(280.dp)
                    )
                    Text(
                        "Prévia da arte em proporção original, sem cortar nem esticar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { preview = null }) { Text("Fechar") }
            }
        )
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
    val isNativeLevel = (initial?.sequenceNo ?: 0) in 1..22 && initial?.special != true
    val isNativeAchievement = initial?.id?.let(::isNativeAchievementBadgeId) == true
    val isNative = isNativeLevel || isNativeAchievement
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
            }.onSuccess { generatedChallenge = it }
                .onFailure { error = it.message ?: "Não foi possível buscar um desafio agora." }
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
            }.onSuccess { result -> imageRef = buildXpShopAssetRef("emblem", result) }
                .onFailure { error = it.message ?: "Não foi possível enviar o PNG." }
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
                if (isNative) {
                    Text(
                        if (isNativeAchievement) {
                            "Conquista nativa do app. A arte original continua sendo usada até você enviar outro PNG."
                        } else {
                            "Emblema nativo · nível ${initial?.sequenceNo}. A arte original continua sendo usada até você enviar outro PNG."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(value = name, onValueChange = { name = it; error = "" }, label = { Text("Nome do emblema") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                Text("Dificuldade do desafio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                AdminSafeHorizontalRow {
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
                            Text("Detecção automática pronta · meta ${generated.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { challenge = generated.text; error = "" },
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
                }

                OutlinedButton(enabled = !busy, onClick = { requestChallenge(challengeDifficulty, excludeCurrent = true) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Buscar outro desafio")
                }

                if (challenge.isNotBlank()) {
                    Text("Desafio que será salvo: $challenge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(enabled = !busy, onClick = { launcher.launch("image/png") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (imageRef.startsWith("builtin-emblem://")) "Trocar PNG do emblema" else if (imageRef.isBlank()) "Enviar emblema PNG transparente" else "Trocar emblema PNG")
                }
                if (uploading) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("Enviando… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                } else if (imageRef.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (imageRef.startsWith("builtin-emblem://")) "PNG original do app preservado" else "PNG enviado",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Emblema especial", fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                isNativeAchievement -> "Conquistas nativas não recebem número de nível."
                                isNativeLevel -> "Emblemas nativos mantêm a numeração original."
                                special -> "Não usa número de nível."
                                else -> "Receberá automaticamente o próximo número a partir do 23."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = special, onCheckedChange = { special = it }, enabled = !busy && !isNative)
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
                    challenge.isBlank() -> error = "Escolha um desafio para o emblema."
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
                                        special = if (isNative) false else special,
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
