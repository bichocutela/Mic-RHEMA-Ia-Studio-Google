package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
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
import com.aistudio.micrhema.ui.theme.GoldPlusPreviewTheme

private fun downloadXpDigitalReward(context: Context, item: XpShopItem) {
    if (item.imageUrl.isBlank()) return
    runCatching {
        val uri = Uri.parse(item.imageUrl)
        val rawExtension = uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.substringBefore('?')
            ?.lowercase()
            .orEmpty()
        val extension = rawExtension.takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") } ?: "jpg"
        val safeName = item.name
            .trim()
            .replace(Regex("[^A-Za-z0-9À-ÿ _-]"), "")
            .replace(Regex("\\s+"), "_")
            .ifBlank { "MIC_Rhema_XP" }
        val fileName = "$safeName.$extension"
        val request = DownloadManager.Request(uri)
            .setTitle(item.name)
            .setDescription("Recompensa digital da Loja XP")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download iniciado. Veja a pasta Downloads.", Toast.LENGTH_LONG).show()
    }.onFailure {
        Toast.makeText(context, "Não foi possível iniciar o download.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun XpPurchasesDialog(
    member: MemberRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
                            onPreview = { previewItem = item },
                            onDownload = { downloadXpDigitalReward(context, item) }
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
        val previewAvatar = biblicalAvatarForId(member.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID })
        val previewBadge = biblicalBadgeForId(member.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID })
        val downloadable = item.kind == "digital" && item.imageUrl.isNotBlank()
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text("Prévia — ${item.name}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (item.id) {
                        XpRewardManager.PROMISE_FRAME -> {
                            Text("Assim a Moldura Luz da Promessa ficará no seu avatar.", style = MaterialTheme.typography.bodySmall)
                            BiblicalAvatarWithBadge(
                                avatar = previewAvatar,
                                badge = previewBadge,
                                modifier = Modifier.size(260.dp),
                                previewPromiseFrame = true
                            )
                        }
                        XpRewardManager.READER_BADGE -> {
                            Text("Assim o Distintivo Leitor da Palavra ficará no seu avatar.", style = MaterialTheme.typography.bodySmall)
                            BiblicalAvatarWithBadge(
                                avatar = previewAvatar,
                                badge = previewBadge,
                                modifier = Modifier.size(260.dp),
                                previewReaderBadge = true
                            )
                        }
                        XpRewardManager.GOLD_PLUS_THEME -> {
                            GoldPlusPreviewTheme {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Dourado Plus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Prévia fiel do tema", fontWeight = FontWeight.Bold)
                                                Text("Superfícies, botões, destaques e contraste usam exatamente as cores do Dourado Plus.", style = MaterialTheme.typography.bodySmall)
                                                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Botão Dourado Plus") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            if (item.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = "Prévia de ${item.name}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(18.dp))
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
                        }
                    }
                    Text(item.description.ifBlank { "Recompensa adquirida na Loja XP." })
                }
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) { Text("Fechar") }
            },
            dismissButton = if (downloadable) {
                {
                    TextButton(onClick = { downloadXpDigitalReward(context, item) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Baixar")
                    }
                }
            } else null
        )
    }
}

@Composable
private fun XpPurchasedItemCard(
    member: MemberRequest,
    item: XpShopItem,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    var localToggle by remember(item.id) { mutableIntStateOf(0) }
    val isActivatable = item.id == XpRewardManager.GOLD_PLUS_THEME ||
        item.id == XpRewardManager.PROMISE_FRAME ||
        item.id == XpRewardManager.READER_BADGE
    localToggle
    val isActive = XpRewardManager.isActive(context, item.id, member.id)
    val downloadable = item.kind == "digital" && item.imageUrl.isNotBlank()

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
                            XpRewardManager.setActive(context, member.id, item.id, !isActive)
                            localToggle++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isActive) "Desativar" else "Ativar")
                    }
                }
            }
            if (downloadable) {
                OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Baixar no celular")
                }
            }
        }
    }
}
