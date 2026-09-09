package com.aistudio.micrhema

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    coil.compose.AsyncImage(
        model = url,
        contentDescription = item.name,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
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

    // Os distintivos ficam limpos, sem placa/fundo geométrico. A faixa aparece
    // logo abaixo do emblema; quatro cabem como destaque e, se houver mais,
    // o próprio usuário pode deslizar horizontalmente para revelar o restante.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (selected.isEmpty()) return@BoxWithConstraints
        val side = minOf(maxWidth, maxHeight)
        val iconSize = side * .17f
        val gap = side * .045f
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = iconSize * .92f)
                .width(side * .86f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            selected.forEach { item ->
                DistinctiveImage(item, Modifier.size(iconSize))
            }
        }
    }
}
