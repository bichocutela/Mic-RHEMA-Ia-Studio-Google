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
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showAll by remember { mutableStateOf(false) }
    XpRewardManager.revision.value
    val distinctives = activeProfileCosmeticsForMember(context, "distintivo", badge.id, member.id)
    val frames = activeProfileCosmeticsForMember(context, "moldura", badge.id, member.id)
    val availableIds = distinctives.map { it.id }

    LaunchedEffect(member.id, availableIds) {
        DistinctiveCatalog.refresh()
        DistinctiveHighlightsStore.load(context.applicationContext, member.id, availableIds)
    }

    val featuredIds = DistinctiveHighlightsStore.ids(member.id)
    val byId = distinctives.associateBy { it.id }
    val ordered = (featuredIds.mapNotNull(byId::get) + distinctives.filterNot { it.id in featuredIds }).distinctBy { it.id }

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
                        ownerMemberId = null,
                        previewDistinctives = emptyList(),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Prévia ampliada do perfil"
                    )
                    frames.firstOrNull()?.let { frame ->
                        DistinctiveImage(frame, Modifier.fillMaxSize())
                    }
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
                    if (distinctives.size > 4) {
                        TextButton(onClick = { showAll = true }) {
                            Text("Ver todos", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )

    if (showAll) {
        var selected by remember(featuredIds, showAll) { mutableStateOf(featuredIds.toSet()) }
        var saving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!saving) showAll = false },
            title = { Text("Meus distintivos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Escolha até 4 para aparecerem como destaque na Home, no Drawer e no seu perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(distinctives, key = { it.id }) { item ->
                            val checked = item.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = checked || selected.size < 4) {
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
                                    enabled = checked || selected.size < 4,
                                    onCheckedChange = { value ->
                                        selected = if (value) selected + item.id else selected - item.id
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "${selected.size}/4 selecionados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showAll = false }) { Text("Cancelar") }
            },
            confirmButton = {
                Button(
                    enabled = !saving && selected.isNotEmpty(),
                    onClick = {
                        saving = true
                        scope.launch {
                            DistinctiveHighlightsStore.save(context.applicationContext, member.id, selected.toList())
                            saving = false
                            showAll = false
                        }
                    }
                ) { Text(if (saving) "Salvando…" else "Usar em destaque") }
            }
        )
    }
}
