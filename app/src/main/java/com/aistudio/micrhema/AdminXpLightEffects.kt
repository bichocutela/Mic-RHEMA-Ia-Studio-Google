package com.aistudio.micrhema

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminXpLightEffectsSection() {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<AdminLightEffect>>(emptyList()) }
    var badges by remember { mutableStateOf<List<AdminCustomBadge>>(emptyList()) }
    var editor by remember { mutableStateOf<AdminLightEffect?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Pair<AdminLightEffect, Boolean>?>(null) }
    var attach by remember { mutableStateOf<AdminLightEffect?>(null) }
    var deleting by remember { mutableStateOf<AdminLightEffect?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching {
                val loadedEffects = XpLightEffectsAdminClient.load()
                val loadedBadges = XpShopAdminClient.loadBadges()
                loadedEffects to loadedBadges
            }.onSuccess { (effects, loadedBadges) ->
                items = effects
                badges = loadedBadges
            }.onFailure {
                error = it.message ?: "Não foi possível sincronizar os efeitos de luz."
            }
            loading = false
        }
    }

    fun saveRemote(item: AdminLightEffect, onDone: () -> Unit = {}) {
        if (saving) return
        saving = true
        error = ""
        scope.launch {
            runCatching { XpLightEffectsAdminClient.save(item) }
                .onSuccess { saved ->
                    items = (items.filterNot { it.id == saved.id } + saved).sortedBy { it.name.lowercase() }
                    onDone()
                }
                .onFailure { error = it.message ?: "Não foi possível salvar o efeito no servidor." }
            saving = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(enabled = !saving, onClick = { showNew = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Adicionar efeito")
            }
            OutlinedButton(enabled = !loading && !saving, onClick = { refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
            }
        }

        Text(
            "Efeitos animados do avatar sincronizados com o Supabase. Cada efeito pode ser vendido, gratuito para todos ou vinculado a emblemas.",
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
            emptyContent = { if (!loading) Text("Nenhum efeito de luz cadastrado.") }
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text("${toneLabel(item.tone)} • ${if (item.active) "Ativo" else "Inativo"}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        when {
                            item.purchasable -> "Venda na Loja XP • ${item.xpCost} XP"
                            item.freeForAll -> "Gratuito p/ Todos"
                            item.emblemIds.isNotEmpty() -> "Vinculado a ${item.emblemIds.size} emblema(s)"
                            else -> "Sem liberação configurada"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { preview = item to false }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Prévia")
                        }
                        OutlinedButton(onClick = { preview = item to true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Testar")
                        }
                    }
                    if (!item.freeForAll) {
                        OutlinedButton(onClick = { attach = item }, modifier = Modifier.fillMaxWidth()) { Text("Ajustar emblemas") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { editor = item }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Editar")
                        }
                        OutlinedButton(onClick = { deleting = item }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Excluir")
                        }
                    }
                }
            }
        }
    }

    if (showNew || editor != null) {
        val editingItem = editor
        LightEffectEditor(initial = editingItem, badges = badges, onDismiss = { showNew = false; editor = null }) { saved ->
            saveRemote(saved) {
                showNew = false
                editor = null
            }
        }
    }

    preview?.let { (item, testMode) ->
        LightEffectPreviewDialog(item, testMode) { preview = null }
    }

    attach?.let { item ->
        LightEffectEmblemDialog(item, badges, onDismiss = { attach = null }) { ids ->
            saveRemote(item.copy(emblemIds = ids)) { attach = null }
        }
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleting = null },
            title = { Text("Excluir efeito?") },
            text = { Text("O efeito ${item.name} será removido do servidor e deixará de aparecer para os demais administradores.") },
            confirmButton = {
                Button(enabled = !saving, onClick = {
                    if (saving) return@Button
                    saving = true
                    error = ""
                    scope.launch {
                        runCatching { XpLightEffectsAdminClient.delete(item.id) }
                            .onSuccess {
                                items = items.filterNot { it.id == item.id }
                                deleting = null
                            }
                            .onFailure { error = it.message ?: "Não foi possível excluir o efeito." }
                        saving = false
                    }
                }) { Text(if (saving) "Excluindo…" else "Excluir") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun LightEffectEditor(
    initial: AdminLightEffect?,
    badges: List<AdminCustomBadge>,
    onDismiss: () -> Unit,
    onSave: (AdminLightEffect) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var effectType by remember(initial?.id) { mutableStateOf(initial?.effectType ?: "orbit") }
    var tone by remember(initial?.id) { mutableStateOf(initial?.tone ?: "medio") }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.colorHex ?: "#FFD54F") }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var releaseMode by remember(initial?.id) {
        mutableStateOf(
            when {
                initial?.purchasable == true -> "shop"
                initial?.freeForAll == true -> "free"
                else -> "emblem"
            }
        )
    }
    var cost by remember(initial?.id) { mutableStateOf(initial?.xpCost?.takeIf { it > 0 }?.toString().orEmpty()) }
    var emblems by remember(initial?.id) { mutableStateOf(initial?.emblemIds?.toSet() ?: emptySet<String>()) }
    var error by remember(initial?.id) { mutableStateOf("") }

    val purchasable = releaseMode == "shop"
    val freeForAll = releaseMode == "free"
    val choices = remember(badges) {
        (badges.map { it.id to it.name } + currentProfileEmblemBadges().map { it.id to it.name }).distinctBy { it.first }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Adicionar efeito" else "Editar efeito") },
        text = {
            Column(Modifier.heightIn(max = 610.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it; error = "" }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(effectType, { effectType = it }, label = { Text("Tipo do efeito") }, supportingText = { Text("Ex.: orbit, stars, flame, pulse, rays") }, modifier = Modifier.fillMaxWidth())
                Text("Tonalidade", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("suave" to "Suave", "medio" to "Médio", "forte" to "Luz forte").forEach { (key, label) ->
                        FilterChip(selected = tone == key, onClick = { tone = key }, label = { Text(label) })
                    }
                }
                OutlinedTextField(colorHex, { colorHex = it.uppercase() }, label = { Text("Cor (#RRGGBB)") }, modifier = Modifier.fillMaxWidth())

                Text("Forma de liberação", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = releaseMode == "shop",
                        onClick = { releaseMode = "shop"; error = "" },
                        label = { Text("Venda na Loja XP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilterChip(
                        selected = releaseMode == "free",
                        onClick = { releaseMode = "free"; error = "" },
                        label = { Text("Gratuito p/ Todos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilterChip(
                        selected = releaseMode == "emblem",
                        onClick = { releaseMode = "emblem"; error = "" },
                        label = { Text("Vinculado a emblema / ADM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    when (releaseMode) {
                        "shop" -> "Só recebe quem comprar na Loja XP."
                        "free" -> "Qualquer usuário recebe acesso, inclusive contas novas, sem XP e sem missão."
                        else -> "O efeito fica disponível somente nos emblemas selecionados abaixo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (purchasable) {
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it.filter(Char::isDigit); error = "" },
                        label = { Text("Valor em XP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!freeForAll) {
                    Text("Aplicar nos emblemas", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (purchasable) "Opcional: sem vínculo, a compra funciona em qualquer emblema." else "Selecione pelo menos um emblema para esta forma de liberação.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    choices.forEach { (badgeId, badgeName) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = badgeId in emblems,
                                onCheckedChange = { checked -> emblems = if (checked) emblems + badgeId else emblems - badgeId }
                            )
                            Text(badgeName, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ativo", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(active, { active = it })
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedCost = cost.toIntOrNull() ?: 0
                when {
                    name.isBlank() -> error = "Informe o nome do efeito."
                    effectType.isBlank() -> error = "Informe o tipo do efeito."
                    !Regex("#[0-9A-Fa-f]{6}").matches(colorHex) -> error = "Use uma cor no formato #RRGGBB."
                    purchasable && parsedCost <= 0 -> error = "Informe um valor maior que zero."
                    releaseMode == "emblem" && emblems.isEmpty() -> error = "Selecione pelo menos um emblema para liberar este efeito."
                    else -> onSave(AdminLightEffect(
                        id = initial?.id ?: "light_${System.currentTimeMillis()}",
                        name = name.trim(),
                        description = description.trim(),
                        effectType = effectType.trim(),
                        tone = tone,
                        colorHex = colorHex.uppercase(),
                        purchasable = purchasable,
                        xpCost = if (purchasable) parsedCost else 0,
                        emblemIds = if (freeForAll) emptyList() else emblems.toList(),
                        active = active,
                        freeForAll = freeForAll
                    ))
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun LightEffectEmblemDialog(item: AdminLightEffect, badges: List<AdminCustomBadge>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var selected by remember(item.id, badges) { mutableStateOf(item.emblemIds.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar ao emblema") },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val choices = (badges.map { it.id to it.name } + currentProfileEmblemBadges().map { it.id to it.name }).distinctBy { it.first }
                choices.forEach { (badgeId, badgeName) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = badgeId in selected, onCheckedChange = { checked -> selected = if (checked) selected + badgeId else selected - badgeId })
                        Text(badgeName, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(selected.toList()) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun LightEffectPreviewDialog(item: AdminLightEffect, testMode: Boolean, onDismiss: () -> Unit) {
    val color = remember(item.colorHex) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }.getOrDefault(Color(0xFFFFD54F))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (testMode) "Testar • ${item.name}" else "Prévia • ${item.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BiblicalAvatarWithBadge(
                    avatar = biblicalAvatarForId(loggedInMemberState.value?.avatarId ?: DEFAULT_BIBLICAL_AVATAR_ID),
                    badge = currentProfileEmblemBadges().firstOrNull { it.id in item.emblemIds }
                        ?: biblicalBadgeForId(loggedInMemberState.value?.equippedBadgeId ?: DEFAULT_BIBLICAL_BADGE_ID),
                    modifier = Modifier.size(220.dp),
                    previewPromiseFrame = false,
                    previewReaderBadge = false,
                    previewLightEffects = listOf(item)
                )
                Text(toneLabel(item.tone), color = color, fontWeight = FontWeight.Bold)
                Text(
                    if (testMode) "Modo de teste ativo: este é o efeito animado real que será usado no avatar." else item.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Fechar") } }
    )
}

private fun toneLabel(tone: String): String = when (tone) {
    "suave" -> "Suave"
    "forte" -> "Luz forte"
    else -> "Médio"
}
