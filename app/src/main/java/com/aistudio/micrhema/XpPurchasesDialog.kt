package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun XpPurchasesDialog(
    member: MemberRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember(member.id) { mutableStateOf(true) }
    var error by remember(member.id) { mutableStateOf("") }
    var previewItem by remember { mutableStateOf<XpShopItem?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val catalog = xpShopItemsState.value?.takeIf { it.memberId == member.id }?.items.orEmpty()
    val redemptions = xpRedemptionsState.value?.takeIf { it.memberId == member.id }?.redemptions.orEmpty()
    val entitlements = xpEntitlementsState.value?.takeIf { it.memberId == member.id }?.entitlements.orEmpty()

    LaunchedEffect(member.id, refreshKey) {
        loading = true
        error = ""
        runCatching {
            XpShopClient.loadCatalog(member)
            XpShopClient.loadRedemptions(member, context)
        }.onFailure { error = it.message ?: "Não foi possível carregar suas compras XP." }
        loading = false
    }

    val purchasedIds = remember(redemptions, entitlements) {
        buildSet {
            redemptions.filter { it.status != "cancelado" }.forEach { add(it.itemId) }
            entitlements.forEach { add(it.itemId) }
        }
    }
    val purchasedItems = remember(catalog, purchasedIds) {
        catalog.filter { it.id in purchasedIds }
    }
    val missingPurchases = remember(redemptions, entitlements, catalog) {
        val knownIds = catalog.map { it.id }.toSet()
        buildList {
            redemptions.filter { it.status != "cancelado" && it.itemId !in knownIds }
                .distinctBy { it.itemId }
                .forEach { redemption ->
                    add(
                        XpShopItem(
                            id = redemption.itemId,
                            name = redemption.itemName,
                            description = "Recompensa adquirida na Loja XP.",
                            cost = redemption.cost,
                            category = "Compras",
                            kind = entitlements.firstOrNull { it.itemId == redemption.itemId }?.kind ?: "digital",
                            imageUrl = "",
                            stock = null,
                            limitPerMember = 1,
                            active = true
                        )
                    )
                }
            entitlements.filter { it.itemId !in knownIds && redemptions.none { redemption -> redemption.itemId == it.itemId && redemption.status != "cancelado" } }
                .distinctBy { it.itemId }
                .forEach { entitlement ->
                    add(
                        XpShopItem(
                            id = entitlement.itemId,
                            name = entitlement.itemName,
                            description = "Recompensa adquirida na Loja XP.",
                            cost = 0,
                            category = "Compras",
                            kind = entitlement.kind,
                            imageUrl = "",
                            stock = null,
                            limitPerMember = 1,
                            active = true
                        )
                    )
                }
        }
    }
    val allPurchased = purchasedItems + missingPurchases

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minhas Compras XP") },
        text = {
            when {
                loading && allPurchased.isEmpty() -> Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error.isNotBlank() && allPurchased.isEmpty() -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error, textAlign = TextAlign.Center)
                    TextButton(onClick = { refreshKey++ }) { Text("Tentar novamente") }
                }

                allPurchased.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Você ainda não comprou recompensas na Loja XP.", textAlign = TextAlign.Center)
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allPurchased, key = { it.id }) { item ->
                        XpPurchasedItemCard(
                            member = member,
                            item = item,
                            onPreview = { previewItem = item }
                        )
                    }
                    if (error.isNotBlank()) {
                        item {
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )

    previewItem?.let { item ->
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "Prévia de ${item.name}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(18.dp))
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text(item.description.ifBlank { "Recompensa adquirida na Loja XP." })
                }
            },
            confirmButton = { TextButton(onClick = { previewItem = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun XpPurchasedItemCard(
    member: MemberRequest,
    item: XpShopItem,
    onPreview: () -> Unit
) {
    val context = LocalContext.current
    var localToggle by remember(item.id) { mutableIntStateOf(0) }
    val isActivatable = item.id == XpRewardManager.GOLD_PLUS_THEME ||
        item.id == XpRewardManager.PROMISE_FRAME ||
        item.id == XpRewardManager.READER_BADGE
    val isActive = when (item.id) {
        XpRewardManager.GOLD_PLUS_THEME -> currentSettingsState.value.accentColor == AccentColor.GOLD
        XpRewardManager.PROMISE_FRAME,
        XpRewardManager.READER_BADGE -> XpRewardManager.isActive(context, item.id, member.id)
        else -> false
    }
    localToggle

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text(
                        item.category.ifBlank { if (item.kind == "physical") "Recompensa física" else "Recompensa digital" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Ativo", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Prévia")
                }
                if (isActivatable) {
                    Button(
                        onClick = {
                            when (item.id) {
                                XpRewardManager.GOLD_PLUS_THEME -> XpRewardManager.activateGoldenPlusTheme(context, member.id)
                                else -> XpRewardManager.setActive(context, member.id, item.id, !isActive)
                            }
                            localToggle++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                item.id == XpRewardManager.GOLD_PLUS_THEME && isActive -> "Ativo"
                                item.id == XpRewardManager.GOLD_PLUS_THEME -> "Ativar"
                                isActive -> "Desativar"
                                else -> "Ativar"
                            }
                        )
                    }
                }
            }
        }
    }
}
