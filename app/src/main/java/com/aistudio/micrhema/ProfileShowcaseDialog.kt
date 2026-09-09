package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MemberProfileShowcaseDialog(
    member: MemberRequest,
    avatar: BiblicalAvatar = biblicalAvatarForId(member.avatarId),
    badge: BiblicalBadge = biblicalBadgeForId(member.equippedBadgeId),
    openDistinctivesDirectly: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showAll by remember(openDistinctivesDirectly) { mutableStateOf(openDistinctivesDirectly) }
    XpRewardManager.revision.value
    val distinctives = activeProfileCosmeticsForMember(context, "distintivo", badge.id, member.id)
    val ownedDistinctives = activeProfileCosmeticsForMember(context, "distintivo", badge.id, member.id, true)
    val frames = activeProfileCosmeticsForMember(context, "moldura", badge.id, member.id, true)
    val activeFrames = activeProfileCosmeticsForMember(context, "moldura", badge.id, member.id)
    val selectedFrame = DistinctiveHighlightsStore.frame(member.id, activeFrames)
    var previewDistinctiveId by remember(member.id, badge.id) { mutableStateOf<String?>(null) }
    var previewFrameId by remember(member.id, badge.id) { mutableStateOf<String?>(null) }
    var applying by remember(member.id) { mutableStateOf(false) }
    var applyError by remember(member.id) { mutableStateOf("") }

    fun choose(item: AdminProfileCosmetic) {
        if (applying) return
        applying = true
        applyError = ""
        scope.launch {
            try {
                DistinctiveHighlightsStore.choose(context.applicationContext, member.id, item, badge.id)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                applyError = error.message ?: "Não foi possível ativar. Tente novamente."
            } finally {
                applying = false
            }
        }
    }
    val availableIds = distinctives.map { it.id }

    LaunchedEffect(member.id, availableIds) {
        runCatching { DistinctiveCatalog.refresh() }
        DistinctiveHighlightsStore.load(context.applicationContext, member.id, availableIds)
    }

    val primaryDistinctive = DistinctiveHighlightsStore.primary(member.id, distinctives)
    val featuredIds = DistinctiveHighlightsStore.ids(member.id)
    val byId = ownedDistinctives.associateBy { it.id }
    val ordered = (featuredIds.mapNotNull(byId::get) + ownedDistinctives.filterNot { it.id in featuredIds }).distinctBy { it.id }
    val previewDistinctive = byId[previewDistinctiveId] ?: primaryDistinctive
    val previewFrame = frames.firstOrNull { it.id == previewFrameId } ?: selectedFrame

    if (!openDistinctivesDirectly) {
        AlertDialog(
            onDismissRequest = { if (!applying) onDismiss() },
            title = { Text(member.name.ifBlank { "Perfil" }) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BiblicalAvatarWithBadge(
                        avatar = avatar,
                        badge = badge,
                        ownerMemberId = member.id,
                        previewPrimaryDistinctive = previewDistinctive,
                        previewReaderBadge = false,
                        previewPromiseFrame = false,
                        previewDistinctives = listOfNotNull(previewFrame),
                        modifier = Modifier.size(200.dp),
                        contentDescription = "Prévia do perfil com ${previewDistinctive?.name ?: "nenhum distintivo principal"} e ${previewFrame?.name ?: "nenhuma moldura"}"
                    )
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ordered.isNotEmpty()) {
                            Text("Distintivos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            ProfileCosmeticCarousel(ordered, previewDistinctive?.id, !applying) {
                                previewDistinctiveId = it.id
                                applyError = ""
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(enabled = !applying, onClick = { showAll = true }, modifier = Modifier.weight(1f)) {
                                    Text("Escolher destaques")
                                }
                                val candidate = byId[previewDistinctiveId]
                                if (candidate != null) {
                                    Button(
                                        enabled = !applying && candidate.id != primaryDistinctive?.id,
                                        onClick = { choose(candidate) }, modifier = Modifier.weight(1f)
                                    ) { Text(if (candidate.id == primaryDistinctive?.id) "Em uso" else "Escolher esse") }
                                }
                            }
                        }
                        Text("Molduras", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        if (frames.isEmpty()) {
                            Text("Você ainda não tem molduras disponíveis para este emblema.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            ProfileCosmeticCarousel(frames, previewFrame?.id, !applying) {
                                previewFrameId = it.id
                                applyError = ""
                            }
                            val candidate = frames.firstOrNull { it.id == previewFrameId }
                            if (candidate != null) {
                                Button(
                                    enabled = !applying && candidate.id != selectedFrame?.id,
                                    onClick = { choose(candidate) }, modifier = Modifier.align(Alignment.End)
                                ) { Text(if (candidate.id == selectedFrame?.id) "Em uso" else "Escolher esse") }
                            }
                        }
                        Text("Toque para ver no avatar. Confirme em Escolher esse para ativar.", style = MaterialTheme.typography.bodySmall)
                        if (applying) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (applyError.isNotBlank()) Text(applyError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(enabled = !applying, onClick = onDismiss) { Text("Fechar") } }
        )
    }

    if (showAll) {
        var selected by remember(featuredIds, availableIds, showAll) { mutableStateOf(featuredIds.filter { it in availableIds }.toSet()) }
        var saving by remember { mutableStateOf(false) }
        var primaryId by remember(member.id, primaryDistinctive?.id) { mutableStateOf(primaryDistinctive?.id.orEmpty()) }
        var choosingPrimary by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf("") }
        fun closeDistinctives() {
            if (openDistinctivesDirectly) onDismiss() else showAll = false
        }
        AlertDialog(
            onDismissRequest = { if (!saving) closeDistinctives() },
            title = { Text("Meus distintivos") },
            text = {
                Column(modifier = Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Destaque principal", fontWeight = FontWeight.Bold)
                            Text("Escolha 1 distintivo para aparecer sobre o círculo escuro no seu avatar.", style = MaterialTheme.typography.bodySmall)
                            Box {
                                OutlinedButton(enabled = !saving, onClick = { choosingPrimary = true }, modifier = Modifier.fillMaxWidth()) {
                                    distinctives.firstOrNull { it.id == primaryId }?.let {
                                        DistinctiveImage(it, Modifier.size(32.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(distinctives.firstOrNull { it.id == primaryId }?.name ?: "Escolher distintivo")
                                }
                                DropdownMenu(expanded = choosingPrimary, onDismissRequest = { choosingPrimary = false }, modifier = Modifier.heightIn(max = 260.dp)) {
                                    DropdownMenuItem(text = { Text("Sem destaque principal") }, onClick = { primaryId = ""; choosingPrimary = false })
                                    distinctives.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item.name) },
                                            leadingIcon = { DistinctiveImage(item, Modifier.size(28.dp)) },
                                            onClick = { primaryId = item.id; choosingPrimary = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Escolha até 4 para aparecerem como destaque na Home, no Drawer e no seu perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(distinctives, key = { it.id }) { item ->
                            val checked = item.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !saving && (checked || selected.size < 4)) {
                                        selected = if (checked) selected - item.id else selected + item.id
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DistinctiveImage(item, Modifier.size(44.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(item.name, Modifier.weight(1f))
                                Checkbox(
                                    checked = checked,
                                    enabled = !saving && (checked || selected.size < 4),
                                    onCheckedChange = { value ->
                                        selected = if (value) selected + item.id else selected - item.id
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "${selected.count { it in availableIds }}/4 abaixo do avatar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (saveError.isNotBlank()) Text(saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { closeDistinctives() }) { Text("Cancelar") }
            },
            confirmButton = {
                Button(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            saveError = ""
                            runCatching {
                                DistinctiveHighlightsStore.save(
                                    context.applicationContext, member.id,
                                    selected.filter { it in availableIds },
                                    primaryId.takeIf { it in availableIds }.orEmpty()
                                )
                            }.onSuccess { closeDistinctives() }
                                .onFailure { saveError = "Não foi possível salvar os destaques. Tente novamente." }
                            saving = false
                        }
                    }
                ) { Text(if (saving) "Salvando…" else "Usar em destaque") }
            }
        )
    }
}

@Composable
private fun ProfileCosmeticCarousel(
    items: List<AdminProfileCosmetic>,
    previewId: String?,
    enabled: Boolean,
    onPreview: (AdminProfileCosmetic) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Surface(
                modifier = Modifier.width(86.dp),
                onClick = { onPreview(item) },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                color = if (item.id == previewId) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DistinctiveImage(item, Modifier.size(48.dp))
                    Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
        }
    }
}
