package com.aistudio.micrhema

import android.content.Context

/** Referência persistente para arquivos da Loja XP armazenados no Supabase. */
data class XpShopAssetRef(
    val type: String,
    val bucket: String,
    val storagePath: String
)

private const val XP_ASSET_SCHEME = "micrhema-xp://"

fun buildXpShopAssetRef(type: String, upload: StorageUploadResult): String {
    val safeType = type.lowercase().trim().ifBlank { "file" }
    return "$XP_ASSET_SCHEME$safeType/${upload.bucket}/${upload.storagePath}"
}

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
    val ref = parseXpShopAssetRef(value) ?: return value
    return StorageManager.getSignedUrl(
        bucket = ref.bucket,
        storagePath = ref.storagePath,
        targetUid = "admin",
        context = context
    )
}

fun xpShopAssetLabel(value: String): String = when (parseXpShopAssetRef(value)?.type) {
    "image" -> "Imagem"
    "video" -> "Vídeo"
    "audio" -> "Áudio"
    "emblem" -> "Emblema PNG"
    "pdf" -> "PDF"
    else -> "Arquivo"
}
