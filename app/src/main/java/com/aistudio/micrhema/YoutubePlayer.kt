package com.aistudio.micrhema

fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val cleanUrl = url.trim()
    val regex = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?|shorts|live)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(cleanUrl)
    return matchResult?.groupValues?.get(1) ?: if (!cleanUrl.contains("http") && !cleanUrl.contains("/") && cleanUrl.length >= 11) cleanUrl else null
}

fun isYoutubeUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val cleanUrl = url.trim().lowercase()
    return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") || extractYouTubeVideoId(cleanUrl) != null
}

fun getYoutubeThumbnailUrl(url: String): String? {
    val id = extractYouTubeVideoId(url)
    return if (id != null) "https://img.youtube.com/vi/$id/maxresdefault.jpg" else null
}
