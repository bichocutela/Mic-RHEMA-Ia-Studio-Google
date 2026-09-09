package com.aistudio.micrhema

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

private const val XP_PURCHASES_PAGE_SIZE = 10

private enum class XpOwnedOrigin(val title: String) {
    PURCHASED("Comprados na Loja XP"),
    ADMIN("Recebidos do ADM")
}

private data class XpOwnedDisplayItem(
    val item: XpShopItem,
    val origin: XpOwnedOrigin,
    val categoryLabel: String
)

private fun categoryLabel(item: XpShopItem): String {
    val explicit = item.category.trim()
    if (explicit.isNotBlank() && !explicit.equals("Compras", ignoreCase = true)) return explicit

    return when {
        item.id == XpRewardManager.PROMISE_FRAME -> "Molduras"
        item.id == XpRewardManager.READER_BADGE -> "Distintivos"
        item.id == XpRewardManager.GOLD_PLUS_THEME -> "Temas"
        item.id.startsWith("cosmetic:") -> when {
            item.id.contains("frame", ignoreCase = true) || item.id.contains("moldura", ignoreCase = true) -> "Molduras"
            item.id.contains("light", ignoreCase = true) || item.id.contains("efeito", ignoreCase = true) -> "Efeitos de Luz"
            item.id.contains("badge", ignoreCase = true) || item.id.contains("distintivo", ignoreCase = true) -> "Distintivos"
            else -> "Itens de Perfil"
        }
        item.kind == "physical" -> "Itens Físicos"
        item.kind == "profile" -> "Itens de Perfil"
        item.kind == "digital" -> "Itens Digitais"
        else -> "Outros"
    }
}

private fun categoryOrder(label: String): Int = when (label.lowercase()) {
    "distintivos" -> 0
    "molduras" -> 1
    "efeitos de luz" -> 2
    "emblemas" -> 3
    "temas" -> 4
    "certificados" -> 5
    "itens de perfil" -> 6
    "itens digitais" -> 7
    "itens físicos" -> 8
    else -> 20
}

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
        }.onFailure { error = it.message ?: "Não foi possível carregar seus itens XP." }
        loading = false
    }

    val purchaseIds = remember(redemptions) {
        redemptions.filter { it.status != "cancelado" }.mapTo(linkedSetOf()) { it.itemId }
    }
    val entitlementIds = remember(entitlements) { entitlements.mapTo(linkedSetOf()) { it.itemId } }
    val ownedIds = remember(purchaseIds, entitlementIds) {
        linkedSetOf<String>().apply {
            addAll(purchaseIds)
            addAll(entitlementIds)
        }
    }

    val catalogById = remember(catalog) { catalog.associateBy { it.id } }
    val redemptionByItem = remember(redemptions) {
        redemptions.filter { it.status != "cancelado" }.associateBy { it.itemId }
    }
    val entitlementByItem = remember(entitlements) { entitlements.associateBy { it.itemId } }

    val ownedItems = remember(ownedIds, catalogById, redemptionByItem, entitlementByItem, purchaseIds) {
        ownedIds.mapNotNull { itemId ->
            val item = catalogById[itemId] ?: redemptionByItem[itemId]?.let { redemption ->
                XpShopItem(
                    id = redemption.itemId,
                    name = redemption.itemName,
                    description = "Recompensa adquirida na Loja XP.",
                    cost = redemption.cost,
                    category = "Compras",
                    kind = entitlementByItem[itemId]?.kind ?: "digital",
                    imageUrl = "",
                    stock = null,
                    limitPerMember = 1,
                    active = true
                )
            } ?: entitlementByItem[itemId]?.let { entitlement ->
                XpShopItem(
                    id = entitlement.itemId,
                    name = entitlement.itemName,
                    description = "Recompensa liberada para você pelo administrador.",
                    cost = 0,
                    category = "",
                    kind = entitlement.kind,
                    imageUrl = "",
                    stock = null,
                    limitPerMember = 1,
                    active = true
                )
            } ?: return@mapNotNull null

            XpOwnedDisplayItem(
                item = item,
                origin = if (itemId in purchaseIds) XpOwnedOrigin.PURCHASED else XpOwnedOrigin.ADMIN,
                categoryLabel = categoryLabel(item)
            )
        }.sortedWith(
            compareBy<XpOwnedDisplayItem> { it.origin.ordinal }
                .thenBy { categoryOrder(it.categoryLabel) }
                .thenBy { it.categoryLabel.lowercase() }
                .thenBy { it.item.name.lowercase() }
        )
    }

    val pages = remember(ownedItems) {
        ownedItems.chunked(XP_PURCHASES_PAGE_SIZE).ifEmpty { listOf(emptyList()) }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(member.id) { pagerState.scrollToPage(0) }
    LaunchedEffect(pages.size) {
        if (pagerState.currentPage > pages.lastIndex) pagerState.scrollToPage(pages.lastIndex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Meus Itens XP")
                Text(
                    "Compras e recompensas recebidas, organizadas por categoria.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            when {
                loading && ownedItems.isEmpty() -> Box(
                    Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error.isNotBlank() && ownedItems.isEmpty() -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(error, textAlign = TextAlign.Center)
                    TextButton(onClick = { refreshKey++ }) { Text("Tentar novamente") }
                }

                ownedItems.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(38.dp))
                    Text("Você ainda não possui itens da Loja XP nem recompensas liberadas pelo ADM.", textAlign = TextAlign.Center)
                }

                else -> Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        beyondViewportPageCount = 0,
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        val pageItems = pages.getOrElse(page) { emptyList() }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            XpOwnedOrigin.entries.forEach { origin ->
                                val originItems = pageItems.filter { it.origin == origin }
                                if (originItems.isNotEmpty()) {
                                    item(key = "origin:${origin.name}:$page") {
                                        Text(
                                            origin.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                                        )
                                    }
                                    originItems.groupBy { it.categoryLabel }.forEach { (category, entries) ->
                                        item(key = "category:${origin.name}:$category:$page") {
                                            Text(
                                                category,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        items(entries, key = { "${origin.name}:${it.item.id}" }) { owned ->
                                            XpPurchasedItemCard(
                                                member = member,
                                                item = owned.item,
                                                onPreview = { previewItem = owned.item },
                                                onDownload = { downloadXpDigitalReward(context, owned.item) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (error.isNotBlank()) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                if (ownedItems.isNotEmpty()) {
                    Text(
                        "Página ${pagerState.currentPage + 1} de ${pages.size} · Até 10 itens",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            enabled = pagerState.currentPage > 0 && !pagerState.isScrollInProgress,
                            onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } }
                        ) { Text("Anterior") }
                        TextButton(
                            enabled = pagerState.currentPage < pages.lastIndex && !pagerState.isScrollInProgress,
                            onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pages.lastIndex)) } }
                        ) { Text("Avançar") }
                    }
                    if (pages.size > 1) {
                        Text(
                            "Deslize para os lados para trocar de página.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fechar") }
            }
        }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (item.id) {
                        XpRewardManager.PROMISE_FRAME -> {
                            Text("Assim a Moldura Luz da Promessa ficará no seu avatar.", style = MaterialTheme.typography.bodySmall)
                            BiblicalAvatarWithBadge(
                                avatar = previewAvatar,
                                badge = previewBadge,
                                modifier = Modifier.size(240.dp),
                                previewPromiseFrame = true
                            )
                        }
                        XpRewardManager.READER_BADGE -> {
                            Text("Assim o Distintivo Leitor da Palavra ficará no seu avatar.", style = MaterialTheme.typography.bodySmall)
                            BiblicalAvatarWithBadge(
                                avatar = previewAvatar,
                                badge = previewBadge,
                                modifier = Modifier.size(240.dp),
                                previewReaderBadge = true
                            )
                        }
                        XpRewardManager.GOLD_PLUS_THEME -> {
                            GoldPlusPreviewTheme {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Column(
                                        Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Dourado Plus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(16.dp))
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(130.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    Text(item.description.ifBlank { "Recompensa disponível na sua conta XP." })
                }
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) { Text("Fechar") }
            },
            dismissButton = if (downloadable) {
                {
                    TextButton(onClick = { downloadXpDigitalReward(context, item) }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
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
        item.id == XpRewardManager.READER_BADGE || item.id.startsWith("cosmetic:")
    localToggle
    val isActive = XpRewardManager.isActive(context, item.id, member.id)
    val downloadable = item.kind == "digital" && item.imageUrl.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text(
                        categoryLabel(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Ativo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Prévia")
                }
                if (isActivatable) {
                    Button(
                        onClick = {
                            XpRewardManager.setActive(context, member.id, item.id, !isActive)
                            localToggle++
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(if (isActive) "Desativar" else "Ativar")
                    }
                }
            }
            if (downloadable) {
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Baixar no celular")
                }
            }
        }
    }
}
