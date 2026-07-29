package com.aistudio.micrhema

fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null
    val cleanUrl = url.trim()
    val lowerUrl = cleanUrl.lowercase()
    return when {
        lowerUrl.contains("v=") -> cleanUrl.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("/").substringBefore("#")
        lowerUrl.contains("youtu.be/") -> cleanUrl.substringAfter("youtu.be/", "youtu.be/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTU.BE/") else it }
        lowerUrl.contains("youtube.com/shorts/") -> cleanUrl.substringAfter("youtube.com/shorts/", "youtube.com/shorts/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/SHORTS/") else it }
        lowerUrl.contains("youtube.com/live/") -> cleanUrl.substringAfter("youtube.com/live/", "youtube.com/live/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/LIVE/") else it }
        lowerUrl.contains("youtube.com/embed/") -> cleanUrl.substringAfter("youtube.com/embed/", "youtube.com/embed/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#").let { if (it.lowercase() == lowerUrl) cleanUrl.substringAfter("YOUTUBE.COM/EMBED/") else it }
        !lowerUrl.contains("http") && !lowerUrl.contains("/") && cleanUrl.length >= 8 -> cleanUrl
        else -> null
    }
}

fun isYoutubeUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val cleanUrl = url.trim().lowercase()
    return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") || extractYoutubeId(cleanUrl) != null
}
