package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.*

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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = minOf(maxWidth, maxHeight)
        val iconSize = side * if (selected.size > 5) .17f else .20f
        selected.forEachIndexed { index, item ->
            val angle = PI / 2 + index * 2 * PI / selected.size.coerceAtLeast(1)
            DistinctiveImage(item, Modifier.offset(
                x = maxWidth / 2 + side * (.40f * cos(angle).toFloat()) - iconSize / 2,
                y = maxHeight / 2 + side * (.40f * sin(angle).toFloat()) - iconSize / 2
            ).size(iconSize))
        }
    }
}
