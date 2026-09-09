package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val XP_SHOP_PAGE_SIZE = 10
private const val XP_SHOP_ALL_CATEGORIES = "Todos"

private fun xpShopCategory(item: XpShopItem): String = item.category.ifBlank { "Recompensas" }

private fun xpShopCategoryPriority(category: String): Int = when (category.lowercase()) {
    "efeitos de luz" -> 0
    "distintivos" -> 1
    "molduras" -> 2
    "emblemas" -> 3
    "temas" -> 4
    "personalização", "personalizacao" -> 5
    "digitais", "digital" -> 6
    "físicos", "fisicos", "físico", "fisico" -> 7
    else -> 20
}

@Composable
fun XpShopPanel(member: MemberRequest, xpUnlocked: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = xpAccountState.value?.takeIf { it.memberId == member.id }
    val items = xpShopItemsState.value?.takeIf { it.memberId == member.id }?.items.orEmpty()
    val redemptions = xpRedemptionsState.value?.takeIf { it.memberId == member.id }?.redemptions.orEmpty()
    val lightEffects = XpLightEffectsAdminClient.catalog.value
    var loading by remember(member.id) { mutableStateOf(false) }
    var error by remember(member.id) { mutableStateOf("") }
    var redeeming by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<XpShopItem?>(null) }
    var successRedemption by remember { mutableStateOf<XpRedemption?>(null) }
    var showDistinctives by remember(member.id) { mutableStateOf(false) }
    var selectedCategory by remember(member.id) { mutableStateOf(XP_SHOP_ALL_CATEGORIES) }
    var currentPage by remember(member.id) { mutableIntStateOf(0) }

    val categories = remember(items) {
        items.map(::xpShopCategory)
            .distinct()
            .sortedWith(compareBy<String> { xpShopCategoryPriority(it) }.thenBy { it.lowercase() })
    }
    val filteredItems = remember(items, selectedCategory) {
        items.asSequence()
            .filter { selectedCategory == XP_SHOP_ALL_CATEGORIES || xpShopCategory(it) == selectedCategory }
            .sortedWith(
                compareBy<XpShopItem> { xpShopCategoryPriority(xpShopCategory(it)) }
                    .thenBy { xpShopCategory(it).lowercase() }
                    .thenBy { it.cost }
                    .thenBy { it.name.lowercase() }
            )
            .toList()
    }
    val totalPages = maxOf(1, (filteredItems.size + XP_SHOP_PAGE_SIZE - 1) / XP_SHOP_PAGE_SIZE)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = remember(filteredItems, safePage) {
        filteredItems.drop(safePage * XP_SHOP_PAGE_SIZE).take(XP_SHOP_PAGE_SIZE)
    }

    LaunchedEffect(categories) {
        if (selectedCategory != XP_SHOP_ALL_CATEGORIES && selectedCategory !in categories) {
            selectedCategory = XP_SHOP_ALL_CATEGORIES
            currentPage = 0
        }
    }
    LaunchedEffect(totalPages, currentPage) {
        if (currentPage != safePage) currentPage = safePage
    }

    suspend fun synchronizedMember(): MemberRequest =
        XpSessionSynchronizer.synchronize(context.applicationContext, member)

    suspend fun refresh() {
        loading = true
        error = ""
        runCatching {
            val activeMember = synchronizedMember()
            runCatching { XpLightEffectsAdminClient.refreshPublicCatalog() }
            XpShopClient.loadCatalog(activeMember)
            XpShopClient.loadRedemptions(activeMember, context)
        }.onFailure {
            error = it.message ?: "Não foi possível carregar a Loja XP."
            xpShopErrorState.value = error
        }
        loading = false
    }

    LaunchedEffect(member.id, xpUnlocked) { if (xpUnlocked) refresh() }

    if (!xpUnlocked) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("Loja XP bloqueada", fontWeight = FontWeight.Bold)
                    Text("A Loja XP é liberada no Nível 8 — Semente da Fé. O XP extra do Quiz continua sendo acumulado até lá.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Loja XP", fontWeight = FontWeight.Bold)
                    Text("Troque somente o Saldo XP. Seu XP Total e seu nível nunca diminuem.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${account?.balance ?: 0} XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (error.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { scope.launch { refresh() } }) { Text("Tentar novamente") }
                }
            }
        }
        if (loading && items.isEmpty()) Text("Carregando recompensas...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (items.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            ) {
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Categorias", fontWeight = FontWeight.Bold)
                            Text("${items.size} recompensas organizadas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("10 por página", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        item(key = XP_SHOP_ALL_CATEGORIES) {
                            FilterChip(
                                selected = selectedCategory == XP_SHOP_ALL_CATEGORIES,
                                onClick = {
                                    selectedCategory = XP_SHOP_ALL_CATEGORIES
                                    currentPage = 0
                                },
                                label = { Text("Todos (${items.size})") },
                                leadingIcon = if (selectedCategory == XP_SHOP_ALL_CATEGORIES) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                        lazyItems(categories, key = { it }) { category ->
                            val count = items.count { xpShopCategory(it) == category }
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    currentPage = 0
                                },
                                label = { Text("$category ($count)") },
                                leadingIcon = if (selectedCategory == category) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedCategory == XP_SHOP_ALL_CATEGORIES) "Todas as recompensas" else selectedCategory,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${filteredItems.size} item${if (filteredItems.size == 1) "" else "s"} • Página ${safePage + 1} de $totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }

            pageItems.groupBy(::xpShopCategory).forEach { (category, categoryItems) ->
                val categoryTotal = filteredItems.count { xpShopCategory(it) == category }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (category.lowercase()) {
                                "efeitos de luz" -> Icons.Default.Star
                                "físicos", "fisicos", "físico", "fisico" -> Icons.Default.Inventory2
                                "digitais", "digital" -> Icons.Default.InsertDriveFile
                                else -> Icons.Default.AccountCircle
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(7.dp))
                        Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("$categoryTotal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                categoryItems.forEach { item ->
                    val redeemedCount = redemptions.count { it.itemId == item.id && it.status != "cancelado" }
                    val limitReached = redeemedCount >= item.limitPerMember
                    val soldOut = item.stock != null && item.stock <= 0
                    val enoughBalance = (account?.balance ?: 0) >= item.cost
                    val hasAsset = item.kind != "physical" && item.imageUrl.isNotBlank()
                    val cosmetic = DistinctiveCatalog.items.value.firstOrNull { cosmeticRewardId(it) == item.id }
                    val isLightEffect = lightEffects.any { it.id == item.id } || category.equals("Efeitos de Luz", ignoreCase = true)
                    val isDistinctive = item.kind == "profile" &&
                        (cosmetic?.kind == "distintivo" ||
                            (cosmetic == null && item.category.equals("Distintivos", ignoreCase = true)))
                    val ownedLabel = when {
                        isLightEffect -> "Escolher efeito no perfil"
                        item.id == XpRewardManager.GOLD_PLUS_THEME -> if (currentSettingsState.value.accentColor == AccentColor.GOLD) "Dourado Plus ativo" else "Ativar Dourado Plus"
                        item.id == XpRewardManager.PROMISE_FRAME -> "Moldura ativa no avatar"
                        item.id == XpRewardManager.READER_BADGE -> "Escolher destaque principal"
                        hasAsset -> "Abrir recompensa"
                        else -> "Já resgatado"
                    }
                    val onOwnedAction: (() -> Unit)? = when {
                        item.id == XpRewardManager.GOLD_PLUS_THEME && currentSettingsState.value.accentColor != AccentColor.GOLD -> ({ XpRewardManager.activateGoldenPlusTheme(context, member.id) })
                        isDistinctive || isLightEffect -> ({ showDistinctives = true })
                        hasAsset -> ({ openXpShopAsset(scope, context, item.imageUrl) { message -> error = message } })
                        else -> null
                    }
                    XpShopItemCard(item, redeemedCount, limitReached, soldOut, enoughBalance, ownedLabel, onOwnedAction) { selectedItem = item }
                }
            }

            if (totalPages > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentPage = (safePage - 1).coerceAtLeast(0) },
                            enabled = safePage > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(3.dp))
                            Text("Anterior")
                        }
                        Text("${safePage + 1}/$totalPages", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = { currentPage = (safePage + 1).coerceAtMost(totalPages - 1) },
                            enabled = safePage < totalPages - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Próxima")
                            Spacer(Modifier.size(3.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (items.isEmpty() && !loading && error.isBlank()) Text("Nenhuma recompensa disponível no momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (redemptions.isNotEmpty()) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(7.dp))
                Column {
                    Text("Meus resgates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Últimos ${minOf(8, redemptions.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            redemptions.take(8).forEach { redemption -> XpRedemptionRow(redemption) }
        }
    }

    if (showDistinctives) {
        MemberProfileShowcaseDialog(
            member = loggedInMemberState.value?.takeIf { it.id == member.id } ?: member,
            openDistinctivesDirectly = false,
            onDismiss = { showDistinctives = false }
        )
    }

    selectedItem?.let { item ->
        val lightEffect = lightEffects.firstOrNull { it.id == item.id }
        AlertDialog(
            onDismissRequest = { if (!redeeming) selectedItem = null },
            icon = { Icon(if (lightEffect != null) Icons.Default.Star else Icons.Default.CardGiftcard, contentDescription = null) },
            title = { Text("Prévia de ${item.name}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        lightEffect != null -> {
                            Text(
                                "Veja o efeito funcionando no avatar antes de comprar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            XpLightEffectPurchasePreview(member, lightEffect)
                        }
                        item.kind == "profile" && item.imageUrl.isNotBlank() -> {
                            Text(
                                "Veja como este item ficará no seu perfil antes de confirmar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            XpProfilePurchasePreview(member, item)
                        }
                        item.imageUrl.isNotBlank() -> XpShopAssetPreview(item)
                    }
                    if (item.description.isNotBlank()) Text(item.description, modifier = Modifier.fillMaxWidth())
                    Text("Custo: ${item.cost} XP", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Seu XP Total e seu nível não serão alterados. Apenas o Saldo XP será reduzido.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(enabled = !redeeming && (account?.balance ?: 0) >= item.cost, onClick = {
                    redeeming = true; error = ""
                    scope.launch {
                        runCatching {
                            val activeMember = synchronizedMember()
                            XpShopClient.redeem(activeMember, item, context)
                        }
                            .onSuccess { result ->
                                successRedemption = result.redemption; selectedItem = null
                                val activeMember = loggedInMemberState.value?.takeIf { it.id == member.id } ?: member
                                runCatching { XpShopClient.loadCatalog(activeMember) }
                                runCatching { XpShopClient.loadRedemptions(activeMember, context) }
                                runCatching { XpEngineClient.loadHistoryNow(activeMember, 100) }
                            }
                            .onFailure { failure -> error = failure.message ?: "Não foi possível concluir o resgate."; xpShopErrorState.value = error }
                        redeeming = false
                    }
                }) { Text(if (redeeming) "Comprando..." else "Confirmar compra por ${item.cost} XP") }
            },
            dismissButton = { TextButton(enabled = !redeeming, onClick = { selectedItem = null }) { Text("Cancelar") } }
        )
    }

    successRedemption?.let { redemption ->
        AlertDialog(
            onDismissRequest = { successRedemption = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Resgate realizado") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(redemption.itemName, fontWeight = FontWeight.Bold); if (redemption.code.isNotBlank()) Text("Código: ${redemption.code}"); Text(if (redemption.status == "pendente") "Aguardando entrega pelo administrador." else "Recompensa vinculada à sua conta e pronta para uso.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { TextButton(onClick = { successRedemption = null }) { Text("OK") } }
        )
    }
}

private fun openXpShopAsset(scope: CoroutineScope, context: android.content.Context, raw: String, onError: (String) -> Unit) {
    scope.launch {
        runCatching { resolveXpShopAssetUrl(context, raw) }
            .onSuccess { url ->
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    .onFailure { onError("Não há aplicativo disponível para abrir esta recompensa.") }
            }
            .onFailure { onError(it.message ?: "Não foi possível abrir a recompensa.") }
    }
}

@Composable
private fun XpLightEffectPurchasePreview(member: MemberRequest, effect: AdminLightEffect) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Box(
            modifier = Modifier.size(230.dp).padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            BiblicalAvatarWithBadge(
                avatar = biblicalAvatarForId(member.avatarId),
                badge = biblicalBadgeForId(member.equippedBadgeId),
                ownerMemberId = member.id,
                previewLightEffects = listOf(effect),
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Prévia do efeito ${effect.name}"
            )
        }
    }
}

@Composable
private fun XpProfilePurchasePreview(member: MemberRequest, item: XpShopItem) {
    val context = LocalContext.current
    val ref = remember(item.imageUrl) { parseXpShopAssetRef(item.imageUrl) }
    var resolvedUrl by remember(item.imageUrl) { mutableStateOf(if (ref == null) item.imageUrl else "") }
    LaunchedEffect(item.imageUrl) {
        resolvedUrl = runCatching { resolveXpShopAssetUrl(context, item.imageUrl) }.getOrDefault(item.imageUrl)
    }

    val lower = "${item.category} ${item.name} ${item.id}".lowercase()
    val isFrame = "moldura" in lower || "frame" in lower
    val isEmblem = "emblema" in lower || "emblem" in lower

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Box(
            modifier = Modifier.size(230.dp).padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            BiblicalAvatarWithBadge(
                avatar = biblicalAvatarForId(member.avatarId),
                badge = biblicalBadgeForId(member.equippedBadgeId),
                ownerMemberId = member.id,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Prévia do perfil com ${item.name}"
            )
            if (resolvedUrl.isNotBlank()) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = when {
                        isFrame || isEmblem -> Modifier.fillMaxSize()
                        else -> Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-2).dp)
                            .size(48.dp)
                    }
                )
            }
        }
    }
}

@Composable
private fun XpShopAssetPreview(item: XpShopItem) {
    val context = LocalContext.current
    val ref = remember(item.imageUrl) { parseXpShopAssetRef(item.imageUrl) }
    var resolvedUrl by remember(item.imageUrl) { mutableStateOf(if (ref == null) item.imageUrl else "") }
    var showFullPreview by remember(item.id, item.imageUrl) { mutableStateOf(false) }

    LaunchedEffect(item.imageUrl) {
        if (ref != null && ref.type in setOf("image", "emblem")) {
            resolvedUrl = runCatching { resolveXpShopAssetUrl(context, item.imageUrl) }.getOrDefault("")
        }
    }

    when {
        item.imageUrl.isBlank() -> Unit
        ref == null || ref.type in setOf("image", "emblem") -> {
            if (resolvedUrl.isNotBlank()) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = "Abrir imagem completa de ${item.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { showFullPreview = true }
                )
            }
        }
        else -> {
            Surface(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        when (ref.type) {
                            "video" -> Icons.Default.VideoFile
                            "audio" -> Icons.Default.AudioFile
                            "pdf" -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(xpShopAssetLabel(item.imageUrl), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showFullPreview && resolvedUrl.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showFullPreview = false },
            title = { Text(item.name, fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 520.dp)
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullPreview = false }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun XpLightEffectCardPreview(effect: AdminLightEffect) {
    val effectColor = remember(effect.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(effect.colorHex)) }.getOrDefault(Color(0xFFFFD76A))
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(92.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = effectColor, modifier = Modifier.size(38.dp))
            Spacer(Modifier.size(10.dp))
            Column {
                Text(effect.name, fontWeight = FontWeight.Bold)
                Text(
                    "Efeito ${effect.tone.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} • prévia animada ao comprar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun XpShopItemCard(item: XpShopItem, redeemedCount: Int, limitReached: Boolean, soldOut: Boolean, enoughBalance: Boolean, ownedLabel: String, onOwnedAction: (() -> Unit)?, onRedeem: () -> Unit) {
    val lightEffect = XpLightEffectsAdminClient.catalog.value.firstOrNull { it.id == item.id }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lightEffect != null) XpLightEffectCardPreview(lightEffect) else XpShopAssetPreview(item)
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.Bold); if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.size(8.dp)); Text("${item.cost} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.size(5.dp))
                Text(when { item.stock == null -> "Disponível"; item.stock == 1 -> "Resta 1"; else -> "Restam ${item.stock}" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f)); Text(if (lightEffect != null) "Efeito" else if (item.kind == "physical") "Física" else if (item.kind == "profile") "Perfil" else "Digital", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (redeemedCount > 0) Text("Resgates: ${redeemedCount.coerceAtMost(item.limitPerMember)}/${item.limitPerMember}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                limitReached && onOwnedAction != null -> Button(onClick = onOwnedAction, modifier = Modifier.fillMaxWidth()) { Text(ownedLabel) }
                limitReached -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(ownedLabel) }
                soldOut -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Esgotado") }
                !enoughBalance -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Saldo insuficiente") }
                else -> Button(onClick = onRedeem, modifier = Modifier.fillMaxWidth()) { Text("Resgatar") }
            }
        }
    }
}

@Composable
private fun XpRedemptionRow(redemption: XpRedemption) {
    val statusLabel = when (redemption.status) { "pendente" -> "Aguardando entrega"; "entregue" -> "Entregue"; "cancelado" -> "Cancelado / XP estornado"; else -> "Liberado" }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(redemption.itemName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(if (redemption.status == "cancelado") "+${redemption.cost} XP estornado" else "-${redemption.cost} XP", style = MaterialTheme.typography.labelLarge)
            }
            Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (redemption.code.isNotBlank()) Text("Código ${redemption.code}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
