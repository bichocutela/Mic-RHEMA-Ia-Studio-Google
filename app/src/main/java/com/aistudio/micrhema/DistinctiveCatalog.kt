package com.aistudio.micrhema

import android.content.Context
import androidx.compose.foundation.clickable
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
    val items = mutableStateOf(builtinProfileCosmetics())
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
    if (isBuiltinCosmetic(item.id) && item.imageRef.isBlank()) {
        BuiltinCosmeticImage(item, modifier)
        return
    }
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
    memberId: String?,
    includeInactiveOwned: Boolean = false
): List<AdminProfileCosmetic> = DistinctiveCatalog.items.value.filter { item ->
    (item.active || isBuiltinCosmetic(item.id)) && item.kind == kind &&
        if (item.purchasable) {
            memberId != null && (if (includeInactiveOwned) XpRewardManager.isOwned(context, cosmeticRewardId(item), memberId) else XpRewardManager.isActive(context, cosmeticRewardId(item), memberId)) &&
                (item.emblemIds.isEmpty() || badgeId in item.emblemIds)
        } else {
            item.emblemIds.isEmpty() || badgeId in item.emblemIds
        }
}

object DistinctiveHighlightsStore {
    private const val PREFS = "micrhema_distinctive_highlights"
    private val values = mutableStateMapOf<String, List<String>>()
    private val effectValues = mutableStateMapOf<String, String>()
    private val frameValues = mutableStateMapOf<String, String>()
    private val primaryValues = mutableStateMapOf<String, String>()
    private val loaded = mutableSetOf<String>()
    private val mutex = Mutex()

    fun ids(memberId: String): List<String> = values[memberId].orEmpty()

    fun primary(memberId: String, available: List<AdminProfileCosmetic>): AdminProfileCosmetic? {
        val id = primaryValues[memberId] ?: XpRewardManager.READER_BADGE
        return available.firstOrNull { it.id == id }
    }

    fun frame(memberId: String, available: List<AdminProfileCosmetic>): AdminProfileCosmetic? {
        val id = frameValues[memberId]
        return if (id != null) available.firstOrNull { it.id == id }
        else available.firstOrNull { it.id != XpRewardManager.PROMISE_FRAME } ?: available.firstOrNull()
    }

    fun effects(memberId: String?, available: List<AdminLightEffect>): List<AdminLightEffect> {
        val id = effectValues[memberId] ?: return available.filterNot { it.purchasable }
        return available.filter { it.id == id }.take(1)
    }

    suspend fun clear(context: Context, memberId: String, kind: String) = mutex.withLock {
        require(memberId.isNotBlank()) { "Entre novamente no seu perfil." }
        val (field, key) = when (kind) {
            "distintivo" -> "primaryDistinctiveId" to "primary:$memberId"
            "moldura" -> "selectedProfileFrameId" to "frame:$memberId"
            "efeito" -> "selectedProfileEffectId" to "effect:$memberId"
            else -> error("Tipo de item inválido.")
        }
        FirebaseFirestore.getInstance().collection("users").document(memberId)
            .set(mapOf(field to "", "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, "").apply()
        withContext(Dispatchers.Main.immediate) {
            when (kind) {
                "distintivo" -> primaryValues[memberId] = ""
                "moldura" -> frameValues[memberId] = ""
                "efeito" -> effectValues[memberId] = ""
            }
        }
    }

    suspend fun chooseEffect(context: Context, memberId: String, item: AdminLightEffect, badgeId: String) = mutex.withLock {
        require(memberId.isNotBlank()) { "Entre novamente no seu perfil." }
        require(availableProfileLightEffects(context, memberId, badgeId).any { it.id == item.id }) {
            "Este efeito não está mais disponível para seu perfil."
        }
        FirebaseFirestore.getInstance().collection("users").document(memberId)
            .set(mapOf("selectedProfileEffectId" to item.id, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("effect:$memberId", item.id).apply()
        withContext(Dispatchers.Main.immediate) { effectValues[memberId] = item.id }
    }

    suspend fun choose(context: Context, memberId: String, item: AdminProfileCosmetic, badgeId: String) = mutex.withLock {
        require(memberId.isNotBlank()) { "Entre novamente no seu perfil." }
        require(item.kind == "moldura" || item.kind == "distintivo") { "Tipo de item inválido." }
        require(activeProfileCosmeticsForMember(context, item.kind, badgeId, memberId, true).any { it.id == item.id }) {
            "Este item não está mais disponível para seu perfil."
        }
        val field = if (item.kind == "moldura") "selectedProfileFrameId" else "primaryDistinctiveId"
        FirebaseFirestore.getInstance().collection("users").document(memberId)
            .set(mapOf(field to item.id, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        if (item.purchasable) XpRewardManager.setActive(context, memberId, cosmeticRewardId(item), true)
        val key = if (item.kind == "moldura") "frame:$memberId" else "primary:$memberId"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, item.id).apply()
        withContext(Dispatchers.Main.immediate) {
            if (item.kind == "moldura") frameValues[memberId] = item.id else primaryValues[memberId] = item.id
        }
    }

    suspend fun load(context: Context, memberId: String, availableIds: List<String>) = mutex.withLock {
        if (memberId.isBlank()) return@withLock
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var ids = prefs.getString(memberId, null)?.split('|')
            ?.filter { it.isNotBlank() }?.distinct()?.take(4) ?: availableIds.take(4)
        var primary = prefs.getString("primary:$memberId", null)
        var frame = prefs.getString("frame:$memberId", null)
        var effect = prefs.getString("effect:$memberId", null)
        if (memberId !in loaded) {
            try {
                val snap = FirebaseFirestore.getInstance().collection("users").document(memberId).get().await()
                (snap.get("featuredDistinctiveIds") as? List<*>)?.let { remote ->
                    ids = remote.filterIsInstance<String>().filter { it.isNotBlank() }.distinct().take(4)
                }
                if (snap.contains("primaryDistinctiveId")) primary = snap.getString("primaryDistinctiveId").orEmpty()
                if (snap.contains("selectedProfileFrameId")) frame = snap.getString("selectedProfileFrameId").orEmpty()
                if (snap.contains("selectedProfileEffectId")) effect = snap.getString("selectedProfileEffectId").orEmpty()
                loaded += memberId
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the cached choice and retry on the next load.
            }
        }
        prefs.edit().putString(memberId, ids.joinToString("|"))
            .apply {
                if (primary != null) putString("primary:$memberId", primary)
                if (frame != null) putString("frame:$memberId", frame)
                if (effect != null) putString("effect:$memberId", effect)
            }.apply()
        withContext(Dispatchers.Main.immediate) {
            values[memberId] = ids
            primary?.let { primaryValues[memberId] = it }
            frame?.let { frameValues[memberId] = it }
            effect?.let { effectValues[memberId] = it }
        }
    }

    suspend fun save(context: Context, memberId: String, ids: List<String>, primaryId: String) = mutex.withLock {
        require(memberId.isNotBlank()) { "Entre novamente no seu perfil." }
        val normalized = ids.filter { it.isNotBlank() }.distinct().take(4)
        val payload = mapOf<String, Any>(
            "featuredDistinctiveIds" to normalized,
            "primaryDistinctiveId" to primaryId,
            "updatedAt" to System.currentTimeMillis()
        )
        // The member owns users/{memberId}; acessos_pendentes is admin-only.
        FirebaseFirestore.getInstance().collection("users").document(memberId)
            .set(payload, SetOptions.merge()).await()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(memberId, normalized.joinToString("|"))
            .putString("primary:$memberId", primaryId).apply()
        withContext(Dispatchers.Main.immediate) {
            values[memberId] = normalized
            primaryValues[memberId] = primaryId
            loaded += memberId
        }
    }
}

@Composable
fun ProfileDistinctives(badgeId: String, memberId: String?, preview: List<AdminProfileCosmetic>? = null) {
    val context = LocalContext.current
    XpRewardManager.revision.value

    val loggedMember = loggedInMemberState.value
    val resolvedMember = when {
        preview != null -> null
        !memberId.isNullOrBlank() -> loggedMember?.takeIf { it.id == memberId }
        loggedMember?.equippedBadgeId == badgeId -> loggedMember
        else -> null
    }
    val resolvedMemberId = resolvedMember?.id ?: memberId
    var showProfile by remember(resolvedMemberId, badgeId) { mutableStateOf(false) }

    val selected = preview?.filter { it.kind == "distintivo" } ?: activeProfileCosmeticsForMember(context, "distintivo", badgeId, resolvedMemberId)
    val activeFrame = if (preview == null && !resolvedMemberId.isNullOrBlank()) {
        DistinctiveHighlightsStore.frame(resolvedMemberId, activeProfileCosmeticsForMember(context, "moldura", badgeId, resolvedMemberId))
    } else preview?.firstOrNull { it.kind == "moldura" }
    val availableIds = selected.map { it.id }

    LaunchedEffect(resolvedMemberId, availableIds) {
        if (!resolvedMemberId.isNullOrBlank() && preview == null) {
            DistinctiveHighlightsStore.load(context.applicationContext, resolvedMemberId, availableIds)
        }
    }

    val visible = if (preview != null || resolvedMemberId.isNullOrBlank()) {
        selected.take(4)
    } else {
        val featured = DistinctiveHighlightsStore.ids(resolvedMemberId)
        val byId = selected.associateBy { it.id }
        featured.mapNotNull(byId::get).take(4)
    }

    // Esta camada ocupa o mesmo espaço do avatar. A moldura ativa acompanha
    // o conjunto completo; os quatro distintivos escolhidos ficam somente no
    // perfil atual/Home/Drawer. Pré-visualizações de outros emblemas podem
    // passar preview vazio para permanecerem limpas.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .then(
                if (resolvedMember != null && preview == null) {
                    Modifier.clickable { showProfile = true }
                } else Modifier
            )
    ) {
        activeFrame?.takeUnless { it.id == XpRewardManager.PROMISE_FRAME }?.let { frame ->
            DistinctiveImage(frame, Modifier.fillMaxSize())
        }

        if (visible.isNotEmpty()) {
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

    if (showProfile && resolvedMember != null) {
        MemberProfileShowcaseDialog(
            member = resolvedMember,
            badge = biblicalBadgeForId(badgeId),
            onDismiss = { showProfile = false }
        )
    }
}
