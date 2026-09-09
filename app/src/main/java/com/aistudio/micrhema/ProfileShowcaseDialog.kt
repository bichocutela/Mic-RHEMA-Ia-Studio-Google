package com.aistudio.micrhema

import androidx.compose.foundation.clickable
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
    val frames = activeProfileCosmeticsForMember(context, "moldura", badge.id, member.id)
    val availableIds = distinctives.map { it.id }

    LaunchedEffect(member.id, availableIds) {
        runCatching { DistinctiveCatalog.refresh() }
        DistinctiveHighlightsStore.load(context.applicationContext, member.id, availableIds)
    }

    val primaryDistinctive = DistinctiveHighlightsStore.primary(member.id, distinctives)
    val featuredIds = DistinctiveHighlightsStore.ids(member.id)
    val byId = distinctives.associateBy { it.id }
    val ordered = (featuredIds.mapNotNull(byId::get) + distinctives.filterNot { it.id in featuredIds }).distinctBy { it.id }

    if (!openDistinctivesDirectly) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(member.name.ifBlank { "Perfil" }) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        BiblicalAvatarWithBadge(
                            avatar = avatar,
                            badge = badge,
                            ownerMemberId = member.id,
                            previewPrimaryDistinctive = null,
                            previewReaderBadge = false,
                            previewDistinctives = emptyList(),
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Prévia ampliada do perfil"
                        )
                        frames.firstOrNull { it.id != XpRewardManager.PROMISE_FRAME }?.let { frame ->
                            DistinctiveImage(frame, Modifier.fillMaxSize())
                        }
                        primaryDistinctive?.let { PrimaryDistinctiveOverlay(it) }
                    }

                    if (ordered.isNotEmpty()) {
                        Text(
                            "Distintivos",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(ordered, key = { it.id }) { item ->
                                Column(
                                    modifier = Modifier.width(62.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    DistinctiveImage(item, Modifier.size(48.dp))
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (distinctives.isNotEmpty()) {
                            TextButton(onClick = { showAll = true }) {
                                Text("Escolher destaques", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
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
