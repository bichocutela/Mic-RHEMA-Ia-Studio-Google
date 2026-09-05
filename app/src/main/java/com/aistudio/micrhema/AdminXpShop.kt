package com.aistudio.micrhema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun xpShopSlug(value: String): String = value
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .take(70)

private fun isoDateOnly(value: String): String = value.takeIf { it.length >= 10 }?.take(10).orEmpty()
private fun startOfXpDate(value: String): String = value.trim().takeIf { it.isNotBlank() }?.let { "${it}T00:00:00-03:00" }.orEmpty()
private fun endOfXpDate(value: String): String = value.trim().takeIf { it.isNotBlank() }?.let { "${it}T23:59:59-03:00" }.orEmpty()

@Composable
fun AdminXpShopScreen() {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var catalog by remember { mutableStateOf<List<AdminXpShopItem>>(emptyList()) }
    var redemptions by remember { mutableStateOf<List<AdminXpRedemption>>(emptyList()) }
    var redemptionFilter by remember { mutableStateOf("todos") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var editorItem by remember { mutableStateOf<AdminXpShopItem?>(null) }
    var showNewEditor by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<Pair<AdminXpRedemption, String>?>(null) }

    suspend fun loadCatalog() {
        loading = true
        error = ""
        runCatching { XpShopAdminClient.loadCatalog() }
            .onSuccess { catalog = it }
            .onFailure { error = it.message ?: "Não foi possível carregar as recompensas." }
        loading = false
    }

    suspend fun loadRedemptions() {
        loading = true
        error = ""
        runCatching { XpShopAdminClient.loadRedemptions(redemptionFilter) }
            .onSuccess { redemptions = it }
            .onFailure { error = it.message ?: "Não foi possível carregar os resgates." }
        loading = false
    }

    LaunchedEffect(selectedTab, redemptionFilter) {
        if (selectedTab == 0) loadCatalog() else loadRedemptions()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Loja XP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Cadastre recompensas, controle estoque e acompanhe entregas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp, divider = {}) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recompensas") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Resgates") })
        }

        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (selectedTab == 0) {
            Button(
                onClick = { showNewEditor = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text("Nova recompensa")
            }

            if (loading && catalog.isEmpty()) {
                Text("Carregando recompensas...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                catalog.forEach { item ->
                    AdminXpRewardCard(item = item, onEdit = { editorItem = item })
                }
                if (!loading && catalog.isEmpty()) {
                    Text("Nenhuma recompensa cadastrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("todos" to "Todos", "pendente" to "Pendentes", "entregue" to "Entregues", "cancelado" to "Cancelados").forEach { (value, label) ->
                    FilterChip(
                        selected = redemptionFilter == value,
                        onClick = { redemptionFilter = value },
                        label = { Text(label) }
                    )
                }
            }

            if (loading && redemptions.isEmpty()) {
                Text("Carregando resgates...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                redemptions.forEach { redemption ->
                    AdminXpRedemptionCard(
                        redemption = redemption,
                        onDeliver = { pendingStatusChange = redemption to "entregue" },
                        onCancel = { pendingStatusChange = redemption to "cancelado" }
                    )
                }
                if (!loading && redemptions.isEmpty()) {
                    Text("Nenhum resgate nesta categoria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    val itemToEdit = editorItem
    if (showNewEditor || itemToEdit != null) {
        AdminXpRewardEditor(
            initial = itemToEdit,
            onDismiss = {
                showNewEditor = false
                editorItem = null
            },
            onSave = { draft ->
                scope.launch {
                    loading = true
                    error = ""
                    runCatching { XpShopAdminClient.saveItem(draft) }
                        .onSuccess {
                            showNewEditor = false
                            editorItem = null
                            catalog = XpShopAdminClient.loadCatalog()
                        }
                        .onFailure { error = it.message ?: "Não foi possível salvar a recompensa." }
                    loading = false
                }
            }
        )
    }

    pendingStatusChange?.let { (redemption, newStatus) ->
        val cancelling = newStatus == "cancelado"
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = { Text(if (cancelling) "Cancelar resgate?" else "Marcar como entregue?") },
            text = {
                Text(
                    if (cancelling)
                        "${redemption.itemName}: o saldo de ${redemption.cost} XP será devolvido ao membro e o estoque será restaurado quando houver estoque limitado."
                    else
                        "Confirme que ${redemption.itemName} foi entregue ao membro antes de continuar."
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingStatusChange = null
                    scope.launch {
                        loading = true
                        error = ""
                        runCatching { XpShopAdminClient.updateRedemptionStatus(redemption.id, newStatus) }
                            .onSuccess { redemptions = XpShopAdminClient.loadRedemptions(redemptionFilter) }
                            .onFailure { error = it.message ?: "Não foi possível atualizar o resgate." }
                        loading = false
                    }
                }) {
                    Text(if (cancelling) "Cancelar e estornar" else "Confirmar entrega")
                }
            },
            dismissButton = { TextButton(onClick = { pendingStatusChange = null }) { Text("Voltar") } }
        )
    }
}

@Composable
private fun AdminXpRewardCard(item: AdminXpShopItem, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("${item.category} • ${if (item.kind == "physical") "Física" else if (item.kind == "profile") "Perfil" else "Digital"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${item.cost} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(5.dp))
                Text(if (item.stock == null) "Estoque ilimitado" else "Estoque: ${item.stock}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(if (item.active) "Ativa" else "Inativa", style = MaterialTheme.typography.labelMedium, color = if (item.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val period = listOfNotNull(
                isoDateOnly(item.availableFrom).takeIf { it.isNotBlank() }?.let { "de $it" },
                isoDateOnly(item.availableUntil).takeIf { it.isNotBlank() }?.let { "até $it" }
            ).joinToString(" ")
            if (period.isNotBlank()) Text("Disponibilidade: $period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Editar recompensa")
            }
        }
    }
}

@Composable
private fun AdminXpRedemptionCard(
    redemption: AdminXpRedemption,
    onDeliver: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(7.dp))
                Text(redemption.itemName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("${redemption.cost} XP", fontWeight = FontWeight.Bold)
            }
            Text(redemption.memberName.ifBlank { "Membro ${redemption.memberId.take(10)}" }, style = MaterialTheme.typography.bodyMedium)
            Text("Código: ${redemption.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Status: ${xpRedemptionStatusLabel(redemption.status)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (redemption.createdAt.isNotBlank()) Text(redemption.createdAt.replace('T', ' ').take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (redemption.status == "pendente") {
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDeliver, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Entregue")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                }
            }
        }
    }
}

private fun xpRedemptionStatusLabel(status: String): String = when (status) {
    "pendente" -> "Aguardando entrega"
    "entregue" -> "Entregue"
    "cancelado" -> "Cancelado / XP estornado"
    "liberado" -> "Liberado"
    else -> status
}

@Composable
private fun AdminXpRewardEditor(
    initial: AdminXpShopItem?,
    onDismiss: () -> Unit,
    onSave: (AdminXpShopItem) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var cost by remember(initial?.id) { mutableStateOf(initial?.cost?.toString().orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "Personalização") }
    var kind by remember(initial?.id) { mutableStateOf(initial?.kind ?: "digital") }
    var imageUrl by remember(initial?.id) { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var stock by remember(initial?.id) { mutableStateOf(initial?.stock?.toString().orEmpty()) }
    var limit by remember(initial?.id) { mutableStateOf(initial?.limitPerMember?.toString() ?: "1") }
    var startDate by remember(initial?.id) { mutableStateOf(isoDateOnly(initial?.availableFrom.orEmpty())) }
    var endDate by remember(initial?.id) { mutableStateOf(isoDateOnly(initial?.availableUntil.orEmpty())) }
    var active by remember(initial?.id) { mutableStateOf(initial?.active ?: true) }
    var localError by remember(initial?.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova recompensa" else "Editar recompensa") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it; localError = "" }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cost, onValueChange = { cost = it.filter(Char::isDigit) }, label = { Text("Preço em XP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = kind == "digital", onClick = { kind = "digital" }, label = { Text("Digital") })
                    FilterChip(selected = kind == "profile", onClick = { kind = "profile" }, label = { Text("Perfil") })
                    FilterChip(selected = kind == "physical", onClick = { kind = "physical" }, label = { Text("Física") })
                }
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL da imagem") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = stock, onValueChange = { stock = it.filter(Char::isDigit) }, label = { Text("Estoque (vazio = ilimitado)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = limit, onValueChange = { limit = it.filter(Char::isDigit) }, label = { Text("Limite por usuário") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Data inicial (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("Data final (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recompensa ativa", fontWeight = FontWeight.SemiBold)
                        Text("Quando inativa, some da Loja XP.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (localError.isNotBlank()) Text(localError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedCost = cost.toIntOrNull() ?: 0
                val parsedLimit = limit.toIntOrNull() ?: 0
                val parsedStock = stock.takeIf { it.isNotBlank() }?.toIntOrNull()
                when {
                    name.isBlank() -> localError = "Informe o nome da recompensa."
                    parsedCost <= 0 -> localError = "Informe um preço em XP maior que zero."
                    parsedLimit <= 0 -> localError = "O limite por usuário deve ser pelo menos 1."
                    stock.isNotBlank() && parsedStock == null -> localError = "Estoque inválido."
                    startDate.isNotBlank() && !Regex("\\d{4}-\\d{2}-\\d{2}").matches(startDate) -> localError = "Use AAAA-MM-DD na data inicial."
                    endDate.isNotBlank() && !Regex("\\d{4}-\\d{2}-\\d{2}").matches(endDate) -> localError = "Use AAAA-MM-DD na data final."
                    else -> onSave(
                        AdminXpShopItem(
                            id = initial?.id ?: xpShopSlug(name),
                            name = name.trim(),
                            description = description.trim(),
                            cost = parsedCost,
                            category = category.trim().ifBlank { "Recompensas" },
                            kind = kind,
                            imageUrl = imageUrl.trim(),
                            stock = parsedStock,
                            limitPerMember = parsedLimit,
                            active = active,
                            availableFrom = startOfXpDate(startDate),
                            availableUntil = endOfXpDate(endDate)
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
