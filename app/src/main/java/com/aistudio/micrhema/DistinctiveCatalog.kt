package com.aistudio.micrhema

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
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

fun activeProfileCosmeticsForMember(
    context: Context,
    kind: String,
    badgeId: String,
    memberId: String?
): List<AdminProfileCosmetic> = DistinctiveCatalog.items.value.filter { item ->
    item.active && item.kind == kind &&
        if (item.purchasable) {
            memberId != null && XpRewardManager.isActive(context, "cosmetic:${item.id}", memberId) &&
                (item.emblemIds.isEmpty() || badgeId in item.emblemIds)
        } else {
            item.emblemIds.isEmpty() || badgeId in item.emblemIds
        }
}

object DistinctiveHighlightsStore {
    private const val PREFS = "micrhema_distinctive_highlights"
    private val values = mutableStateMapOf<String, List<String>>()
    private val loaded = mutableSetOf<String>()

    fun ids(memberId: String): List<String> = values[memberId].orEmpty()

    suspend fun load(context: Context, memberId: String, availableIds: List<String>) {
        if (memberId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var ids = prefs.getString(memberId, null)
            ?.split('|')
            ?.filter { it.isNotBlank() && it in availableIds }
            ?.distinct()
            ?.take(4)
            .orEmpty()

        if (memberId !in loaded) {
            runCatching {
                val snap = FirebaseFirestore.getInstance().collection("users").document(memberId).get().await()
                val remote = (snap.get("featuredDistinctiveIds") as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    ?.filter { it in availableIds }
                    ?.distinct()
                    ?.take(4)
                    .orEmpty()
                if (remote.isNotEmpty()) ids = remote
            }
            loaded += memberId
        }

        if (ids.isEmpty()) ids = availableIds.take(4)
        prefs.edit().putString(memberId, ids.joinToString("|")).apply()
        withContext(Dispatchers.Main.immediate) { values[memberId] = ids }
    }

    suspend fun save(context: Context, memberId: String, ids: List<String>) {
        if (memberId.isBlank()) return
        val normalized = ids.filter { it.isNotBlank() }.distinct().take(4)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(memberId, normalized.joinToString("|")).apply()
        withContext(Dispatchers.Main.immediate) { values[memberId] = normalized }

        val payload = mapOf<String, Any>(
            "featuredDistinctiveIds" to normalized,
            "updatedAt" to System.currentTimeMillis()
        )
        runCatching {
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            batch.set(db.collection("users").document(memberId), payload, SetOptions.merge())
            batch.set(db.collection("acessos_pendentes").document(memberId), payload, SetOptions.merge())
            batch.commit().await()
        }
    }
}

@Composable
fun ProfileDistinctives(badgeId: String, memberId: String?, preview: List<AdminProfileCosmetic>? = null) {
    val context = LocalContext.current
    XpRewardManager.revision.value
    val selected = preview ?: activeProfileCosmeticsForMember(context, "distintivo", badgeId, memberId)
    val availableIds = selected.map { it.id }

    LaunchedEffect(memberId, availableIds) {
        if (!memberId.isNullOrBlank() && preview == null) {
            DistinctiveHighlightsStore.load(context.applicationContext, memberId, availableIds)
        }
    }

    val visible = if (preview != null || memberId.isNullOrBlank()) {
        selected.take(4)
    } else {
        val featured = DistinctiveHighlightsStore.ids(memberId)
        val byId = selected.associateBy { it.id }
        (featured.mapNotNull(byId::get) + selected.filterNot { it.id in featured }).distinctBy { it.id }.take(4)
    }

    // No avatar, Drawer e Home ficam somente os quatro distintivos escolhidos
    // como destaque. A coleção completa é exibida no balão ampliado do perfil.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (visible.isEmpty()) return@BoxWithConstraints
        val side = minOf(maxWidth, maxHeight)
        val iconSize = side * .17f
        val gap = side * .035f
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = iconSize * .92f)
                .width(side * .88f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visible.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.width(gap))
                DistinctiveImage(item, Modifier.size(iconSize))
            }
        }
    }
}
