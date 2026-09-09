package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DistinctiveCatalog {
    val items = mutableStateOf<List<AdminProfileCosmetic>>(emptyList())
    private val mutex = Mutex()
    private var lastRefresh = 0L
    suspend fun refresh() = mutex.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastRefresh != 0L && now - lastRefresh < 60_000) return@withLock
        val loaded = XpShopAdminClient.publicCosmetics()
        withContext(Dispatchers.Main) { items.value = loaded }
        lastRefresh = now
    }
}

@Composable
fun DistinctiveImage(item: AdminProfileCosmetic, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url by produceState<String?>(null, item.imageRef) {
        value = runCatching { resolveXpShopAssetUrl(context, item.imageRef) }.getOrNull()
    }
    coil.compose.AsyncImage(model = url, contentDescription = item.name,
        contentScale = ContentScale.Fit, modifier = modifier)
}

@Composable
fun ProfileDistinctives(badgeId: String, memberId: String?, preview: List<AdminProfileCosmetic>? = null) {
    val context = LocalContext.current
    XpRewardManager.revision.value
    val selected = preview ?: DistinctiveCatalog.items.value.filter { item ->
        item.active && item.kind == "distintivo" &&
            if (item.purchasable) memberId != null && XpRewardManager.isActive(context, "cosmetic:${item.id}", memberId) &&
                (item.emblemIds.isEmpty() || badgeId in item.emblemIds)
            else badgeId in item.emblemIds
    }

    // Distintivos são acessórios externos: ficam numa coluna à direita do emblema,
    // sem cobrir a arte, o avatar ou a moldura. Se houver vários, reduzem de tamanho
    // para permanecer dentro da área disponível.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (selected.isEmpty()) return@BoxWithConstraints
        val side = minOf(maxWidth, maxHeight)
        val iconSize = side * when {
            selected.size <= 2 -> .19f
            selected.size <= 4 -> .16f
            else -> .13f
        }
        val gap = side * .018f
        val visible = selected.take(6)
        val totalHeight = iconSize * visible.size + gap * (visible.size - 1).coerceAtLeast(0)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = iconSize * .56f)
                .height(totalHeight),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            visible.forEach { item ->
                DistinctiveImage(item, Modifier.size(iconSize))
            }
        }
    }
}
