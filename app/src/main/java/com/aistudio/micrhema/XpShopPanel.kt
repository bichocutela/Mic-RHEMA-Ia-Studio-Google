package com.aistudio.micrhema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun XpShopPanel(member: MemberRequest, xpUnlocked: Boolean) {
    val scope = rememberCoroutineScope()
    val account = xpAccountState.value?.takeIf { it.memberId == member.id }
    val items = xpShopItemsState.value
        ?.takeIf { it.memberId == member.id }
        ?.items
        .orEmpty()
    val redemptions = xpRedemptionsState.value
        ?.takeIf { it.memberId == member.id }
        ?.redemptions
        .orEmpty()
    var loading by remember(member.id) { mutableStateOf(false) }
    var error by remember(member.id) { mutableStateOf("") }
    var redeeming by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<XpShopItem?>(null) }
    var successRedemption by remember { mutableStateOf<XpRedemption?>(null) }

    suspend fun refresh() {
        loading = true
        error = ""
        runCatching {
            XpShopClient.loadCatalog(member)
            XpShopClient.loadRedemptions(member)
        }.onFailure {
            error = it.message ?: "Não foi possível carregar a Loja XP."
            xpShopErrorState.value = error
        }
        loading = false
    }

    LaunchedEffect(member.id, xpUnlocked) {
        if (xpUnlocked) refresh()
    }

    if (!xpUnlocked) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("Loja XP bloqueada", fontWeight = FontWeight.Bold)
                    Text(
                        "A Loja XP é liberada no Nível 8 — Semente da Fé. O XP extra do Quiz continua sendo acumulado até lá.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Loja XP", fontWeight = FontWeight.Bold)
                    Text(
                        "Troque somente o Saldo XP. Seu XP Total e seu nível nunca diminuem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${account?.balance ?: 0} XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (error.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { scope.launch { refresh() } }) { Text("Tentar novamente") }
                }
            }
        }

        if (loading && items.isEmpty()) {
            Text("Carregando recompensas...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items.groupBy { it.category.ifBlank { "Recompensas" } }.forEach { (category, categoryItems) ->
            Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            categoryItems.forEach { item ->
                val redeemedCount = redemptions.count { it.itemId == item.id && it.status != "cancelado" }
                val limitReached = redeemedCount >= item.limitPerMember
                val soldOut = item.stock != null && item.stock <= 0
                val enoughBalance = (account?.balance ?: 0) >= item.cost
                XpShopItemCard(
                    item = item,
                    redeemedCount = redeemedCount,
                    limitReached = limitReached,
                    soldOut = soldOut,
                    enoughBalance = enoughBalance,
                    onRedeem = { selectedItem = item }
                )
            }
        }

        if (items.isEmpty() && !loading && error.isBlank()) {
            Text("Nenhuma recompensa disponível no momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (redemptions.isNotEmpty()) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(7.dp))
                Text("Meus resgates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            redemptions.take(8).forEach { redemption -> XpRedemptionRow(redemption) }
        }
    }

    selectedItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!redeeming) selectedItem = null },
            icon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
            title = { Text("Resgatar ${item.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.description)
                    Text("Custo: ${item.cost} XP", fontWeight = FontWeight.Bold)
                    Text("Seu XP Total e seu nível não serão alterados. Apenas o Saldo XP será reduzido.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    enabled = !redeeming && (account?.balance ?: 0) >= item.cost,
                    onClick = {
                        redeeming = true
                        error = ""
                        scope.launch {
                            runCatching { XpShopClient.redeem(member, item) }
                                .onSuccess { result ->
                                    successRedemption = result.redemption
                                    selectedItem = null
                                    runCatching { XpShopClient.loadCatalog(member) }
                                    runCatching { XpShopClient.loadRedemptions(member) }
                                    runCatching { XpEngineClient.loadHistoryNow(member, 100) }
                                }
                                .onFailure { failure ->
                                    error = failure.message ?: "Não foi possível concluir o resgate."
                                    xpShopErrorState.value = error
                                }
                            redeeming = false
                        }
                    }
                ) {
                    Text(if (redeeming) "Resgatando..." else "Resgatar por ${item.cost} XP")
                }
            },
            dismissButton = { TextButton(enabled = !redeeming, onClick = { selectedItem = null }) { Text("Cancelar") } }
        )
    }

    successRedemption?.let { redemption ->
        AlertDialog(
            onDismissRequest = { successRedemption = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Resgate realizado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(redemption.itemName, fontWeight = FontWeight.Bold)
                    if (redemption.code.isNotBlank()) Text("Código: ${redemption.code}")
                    Text(
                        if (redemption.status == "pendente") "Aguardando entrega pelo administrador." else "Recompensa liberada para sua conta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { successRedemption = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun XpShopItemCard(
    item: XpShopItem,
    redeemedCount: Int,
    limitReached: Boolean,
    soldOut: Boolean,
    enoughBalance: Boolean,
    onRedeem: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(13.dp))
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    if (item.description.isNotBlank()) {
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text("${item.cost} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(5.dp))
                Text(
                    when {
                        item.stock == null -> "Disponível"
                        item.stock == 1 -> "Resta 1"
                        else -> "Restam ${item.stock}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (item.kind == "physical") "Física" else "Digital",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (redeemedCount > 0) {
                Text(
                    "Resgates: ${redeemedCount.coerceAtMost(item.limitPerMember)}/${item.limitPerMember}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                limitReached -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item.limitPerMember == 1) "Já resgatado" else "Limite de resgates atingido")
                }
                soldOut -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Esgotado") }
                !enoughBalance -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Saldo insuficiente") }
                else -> Button(onClick = onRedeem, modifier = Modifier.fillMaxWidth()) { Text("Resgatar") }
            }
        }
    }
}

@Composable
private fun XpRedemptionRow(redemption: XpRedemption) {
    val statusLabel = when (redemption.status) {
        "pendente" -> "Aguardando entrega"
        "entregue" -> "Entregue"
        "cancelado" -> "Cancelado"
        else -> "Liberado"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(redemption.itemName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("-${redemption.cost} XP", style = MaterialTheme.typography.labelLarge)
            }
            Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (redemption.code.isNotBlank()) Text("Código ${redemption.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
