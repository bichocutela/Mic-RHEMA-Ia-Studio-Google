package com.aistudio.micrhema

import android.content.Context

/** Referência persistente para arquivos da Loja XP armazenados no Supabase. */
data class XpShopAssetRef(
    val type: String,
    val bucket: String,
    val storagePath: String
)

private const val XP_ASSET_SCHEME = "micrhema-xp://"
private const val BUILTIN_EMBLEM_SCHEME = "builtin-emblem://"

fun buildXpShopAssetRef(type: String, upload: StorageUploadResult): String {
    val safeType = type.lowercase().trim().ifBlank { "file" }
    return "$XP_ASSET_SCHEME$safeType/${upload.bucket}/${upload.storagePath}"
}

fun buildBuiltinEmblemRef(id: String): String = "$BUILTIN_EMBLEM_SCHEME${id.trim()}"

fun parseXpShopAssetRef(value: String): XpShopAssetRef? {
    val raw = value.trim()
    if (!raw.startsWith(XP_ASSET_SCHEME, ignoreCase = true)) return null
    val rest = raw.removePrefix(XP_ASSET_SCHEME)
    val parts = rest.split('/').filter { it.isNotBlank() }
    if (parts.size < 3) return null
    return XpShopAssetRef(
        type = parts[0].lowercase(),
        bucket = parts[1],
        storagePath = parts.drop(2).joinToString("/")
    )
}

suspend fun resolveXpShopAssetUrl(context: Context, value: String): String {
    val raw = value.trim()
    if (raw.startsWith(BUILTIN_EMBLEM_SCHEME, ignoreCase = true)) {
        val id = raw.substringAfter(BUILTIN_EMBLEM_SCHEME).trim()
        val level = biblicalLevelBadges.firstOrNull { it.id == id }?.level
            ?: throw IllegalArgumentException("Emblema interno não encontrado.")
        val resourceName = "profile_emblem_level_${level.toString().padStart(2, '0')}"
        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (resourceId == 0) throw IllegalArgumentException("PNG interno do emblema não encontrado.")
        return "android.resource://${context.packageName}/$resourceId"
    }

    val ref = parseXpShopAssetRef(raw) ?: return raw
    return StorageManager.getSignedUrl(
        bucket = ref.bucket,
        storagePath = ref.storagePath,
        targetUid = "admin",
        context = context
    )
}

fun xpShopAssetLabel(value: String): String {
    if (value.trim().startsWith(BUILTIN_EMBLEM_SCHEME, ignoreCase = true)) return "Emblema interno"
    return when (parseXpShopAssetRef(value)?.type) {
        "image" -> "Imagem"
        "video" -> "Vídeo"
        "audio" -> "Áudio"
        "emblem" -> "Emblema PNG"
        "pdf" -> "PDF"
        "frame" -> "Moldura PNG"
        "badge" -> "Distintivo PNG"
        else -> "Arquivo"
    }
}
