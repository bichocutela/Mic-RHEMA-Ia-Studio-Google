package com.aistudio.micrhema

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class AdminLightEffect(
    val id: String,
    val name: String,
    val description: String,
    val effectType: String,
    val tone: String,
    val colorHex: String,
    val purchasable: Boolean = false,
    val xpCost: Int = 0,
    val emblemIds: List<String> = emptyList(),
    val active: Boolean = true
)

private val defaultLightEffects = listOf(
    AdminLightEffect("aura_promessa", "Aura da Promessa", "Feixe dourado e partículas contornando o avatar.", "orbit", "medio", "#FFD54F"),
    AdminLightEffect("ceu_estrelado", "Céu Estrelado", "Estrelas cintilantes suaves ao redor do perfil.", "stars", "suave", "#69A7FF"),
    AdminLightEffect("chama_espirito", "Chama do Espírito", "Chamas luminosas animadas ao redor do avatar.", "flame", "medio", "#FF6A1A"),
    AdminLightEffect("gloria_divina", "Glória Divina", "Aura pulsante com expansão de luz.", "pulse", "medio", "#A66BFF"),
    AdminLightEffect("vida_abundante", "Vida Abundante", "Partículas verdes e energia natural.", "particles", "suave", "#73E36B"),
    AdminLightEffect("luz_celestial", "Luz Celestial", "Halo branco-dourado com brilho respirando.", "halo", "medio", "#FFF1B0"),
    AdminLightEffect("espirito_fogo", "Espírito de Fogo", "Anel de fogo intenso com faíscas rápidas.", "fire_ring", "forte", "#FF3B1F"),
    AdminLightEffect("raios_gloria", "Raios de Glória", "Raios luminosos surgem atrás do avatar.", "rays", "forte", "#FFC928"),
    AdminLightEffect("poeira_dourada", "Poeira Dourada", "Poeira cintilante sobe lentamente.", "dust", "suave", "#F4CF68"),
    AdminLightEffect("halo_divino", "Halo Divino", "Círculo luminoso fino girando lentamente.", "halo_orbit", "suave", "#FFF5CF"),
    AdminLightEffect("energia_azul", "Energia Azul", "Arcos elétricos azuis ao redor do avatar.", "electric", "forte", "#3C8DFF"),
    AdminLightEffect("aura_esmeralda", "Aura Esmeralda", "Brilho verde profundo com partículas.", "aura", "medio", "#34D399"),
    AdminLightEffect("chamas_roxas", "Chamas Roxas", "Chamas violetas com brilho mágico.", "violet_flame", "forte", "#A855F7"),
    AdminLightEffect("arco_alianca", "Arco-Íris da Aliança", "Gradiente luminoso multicolorido em movimento.", "rainbow_orbit", "medio", "#FFFFFF"),
    AdminLightEffect("luz_espirito", "Luz do Espírito", "Pulso branco suave com pequenas fagulhas.", "spirit_light", "suave", "#FFFFFF")
)

private object AdminLightEffectStore {
    private const val PREF = "micrhema_admin_light_effects"
    private const val KEY = "items"

    fun load(context: Context): List<AdminLightEffect> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null) ?: return defaultLightEffects
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val emblems = o.optJSONArray("emblemIds") ?: JSONArray()
                    add(AdminLightEffect(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        description = o.optString("description"),
                        effectType = o.optString("effectType", "orbit"),
                        tone = o.optString("tone", "medio"),
                        colorHex = o.optString("colorHex", "#FFD54F"),
                        purchasable = o.optBoolean("purchasable", false),
                        xpCost = o.optInt("xpCost", 0),
                        emblemIds = buildList { for (j in 0 until emblems.length()) add(emblems.optString(j)) }.filter { it.isNotBlank() },
                        active = o.optBoolean("active", true)
                    ))
                }
            }
        }.getOrElse { defaultLightEffects }
    }

    fun save(context: Context, items: List<AdminLightEffect>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("description", item.description)
                put("effectType", item.effectType); put("tone", item.tone); put("colorHex", item.colorHex)
                put("purchasable", item.purchasable); put("xpCost", item.xpCost); put("active", item.active)
                put("emblemIds", JSONArray(item.emblemIds))
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}

@Composable
fun AdminXpLightEffectsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(AdminLightEffectStore.load(context)) }
    var badges by remember { mutableStateOf<List<AdminCustomBadge>>(emptyList()) }
    var editor by remember { mutableStateOf<AdminLightEffect?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Pair<AdminLightEffect, Boolean>?>(null) }
    var pricing by remember { mutableStateOf<AdminLightEffect?>(null) }
    var attach by remember { mutableStateOf<AdminLightEffect?>(null) }
    var deleting by remember { mutableStateOf<AdminLightEffect?>(null) }

    LaunchedEffect(Unit) {
        runCatching { XpShopAdminClient.loadBadges() }.onSuccess { badges = it }
    }

    fun persist(newItems: List<AdminLightEffect>) {
        items = newItems
        AdminLightEffectStore.save(context, newItems)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Adicionar efeito")
        }
        Text(
            "Efeitos animados do avatar. Tonalidade: Suave, Médio ou Luz forte.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AdminPagedList(
            items = items,
            modifier = Modifier.weight(1f),
            key = { it.id },
            emptyContent = { Text("Nenhum efeito de luz cadastrado.") }
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
                        if (item.purchasable) Text("${item.xpCost} XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
                    if (item.emblemIds.isNotEmpty()) Text("Emblemas: ${item.emblemIds.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { preview = item to false }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Prévia")
                        }
                        OutlinedButton(onClick = { preview = item to true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Testar")
                        }
                    }
                    OutlinedButton(onClick = { attach = item }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar ao emblema") }
                    OutlinedButton(onClick = { pricing = item }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LocalMall, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp))
                        Text(if (item.purchasable) "Editar cobrança na Loja XP" else "Cobrar na Loja XP")
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
        LightEffectEditor(initial = editor, onDismiss = { showNew = false; editor = null }) { saved ->
            val next = if (editor == null) items + saved else items.map { if (it.id == editor!!.id) saved else it }
            persist(next)
            showNew = false
            editor = null
        }
    }

    preview?.let { (item, testMode) ->
        LightEffectPreviewDialog(item, testMode) { preview = null }
    }

    pricing?.let { item ->
        LightEffectPriceDialog(item, onDismiss = { pricing = null }) { enabled, cost ->
            persist(items.map { if (it.id == item.id) it.copy(purchasable = enabled, xpCost = if (enabled) cost else 0) else it })
            pricing = null
        }
    }

    attach?.let { item ->
        LightEffectEmblemDialog(item, badges, onDismiss = { attach = null }) { ids ->
            persist(items.map { if (it.id == item.id) it.copy(emblemIds = ids) else it })
            attach = null
        }
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir efeito?") },
            text = { Text("O efeito ${item.name} será removido desta coleção administrativa.") },
            confirmButton = { Button(onClick = { persist(items.filterNot { it.id == item.id }); deleting = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun LightEffectEditor(initial: AdminLightEffect?, onDismiss: () -> Unit, onSave: (AdminLightEffect) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var effectType by remember(initial?.id) { mutableStateOf(initial?.effectType ?: "orbit") }
    var tone by remember(initial?.id) { mutableStateOf(initial?.tone ?: "medio") }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.colorHex ?: "#FFD54F") }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var error by remember(initial?.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Adicionar efeito" else "Editar efeito") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ativo", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(active, { active = it })
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.isBlank() -> error = "Informe o nome do efeito."
                    effectType.isBlank() -> error = "Informe o tipo do efeito."
                    !Regex("#[0-9A-Fa-f]{6}").matches(colorHex) -> error = "Use uma cor no formato #RRGGBB."
                    else -> onSave(AdminLightEffect(
                        id = initial?.id ?: "light_${System.currentTimeMillis()}",
                        name = name.trim(), description = description.trim(), effectType = effectType.trim(), tone = tone,
                        colorHex = colorHex.uppercase(), purchasable = initial?.purchasable ?: false,
                        xpCost = initial?.xpCost ?: 0, emblemIds = initial?.emblemIds ?: emptyList(), active = active
                    ))
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun LightEffectPriceDialog(item: AdminLightEffect, onDismiss: () -> Unit, onSave: (Boolean, Int) -> Unit) {
    var enabled by remember(item.id) { mutableStateOf(item.purchasable) }
    var cost by remember(item.id) { mutableStateOf(item.xpCost.takeIf { it > 0 }?.toString().orEmpty()) }
    var error by remember(item.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar na Loja XP") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Disponível para compra", modifier = Modifier.weight(1f))
                    Switch(enabled, { enabled = it })
                }
                if (enabled) OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter(Char::isDigit); error = "" },
                    label = { Text("Valor em XP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = {
            val parsed = cost.toIntOrNull() ?: 0
            if (enabled && parsed <= 0) error = "Informe um valor maior que zero." else onSave(enabled, parsed)
        }) { Text("Salvar") } },
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
                if (badges.isEmpty()) Text("Nenhum emblema disponível ou não foi possível carregar o catálogo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                badges.forEach { badge ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = badge.id in selected, onCheckedChange = { checked -> selected = if (checked) selected + badge.id else selected - badge.id })
                        Text(badge.name, modifier = Modifier.weight(1f))
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
    val infinite = rememberInfiniteTransition(label = "lightEffect")
    val pulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (item.tone == "forte") 650 else 1200), RepeatMode.Reverse),
        label = "pulse"
    )
    val color = remember(item.colorHex) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }.getOrDefault(Color(0xFFFFD54F))
    }
    val width = when (item.tone) { "suave" -> 3.dp; "forte" -> 8.dp; else -> 5.dp }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (testMode) "Testar • ${item.name}" else "Prévia • ${item.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(pulse)
                            .alpha(if (item.tone == "suave") 0.65f else 0.9f)
                            .border(width, color, CircleShape)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(138.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Text("AVATAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(toneLabel(item.tone), color = color, fontWeight = FontWeight.Bold)
                Text(if (testMode) "Modo de teste ativo: visualize o comportamento contínuo antes de publicar." else item.description, style = MaterialTheme.typography.bodySmall)
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
