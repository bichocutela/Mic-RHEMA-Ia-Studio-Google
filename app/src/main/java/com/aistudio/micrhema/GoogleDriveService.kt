package com.aistudio.micrhema

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object GoogleDriveService {
    private fun parsed(url: String): URI? = runCatching { URI(url.trim()) }.getOrNull()

    private fun parameters(uri: URI): Map<String, String> = uri.rawQuery.orEmpty()
        .split('&').mapNotNull { part ->
            val pair = part.split('=', limit = 2)
            if (pair.size != 2) null else runCatching {
                URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair[1], "UTF-8")
            }.getOrNull()
        }.toMap()

    private fun isGoogleFile(uri: URI): Boolean =
        uri.scheme?.lowercase() in setOf("http", "https") &&
            uri.host?.lowercase() in setOf("drive.google.com", "docs.google.com")

    fun getDirectDownloadLink(url: String): String {
        val clean = url.trim()
        val uri = parsed(clean) ?: return clean
        if (!isGoogleFile(uri)) return clean
        val params = parameters(uri)
        val fileId = Regex("/d/([a-zA-Z0-9_-]+)").find(uri.path.orEmpty())?.groupValues?.get(1)
            ?: params["id"]?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }
            ?: return clean
        val resourceKey = params["resourcekey"]?.let { "&resourcekey=" + URLEncoder.encode(it, "UTF-8") }.orEmpty()
        // Office compatibility mode serves the original file; it is not a native Google Doc.
        if (uri.host.equals("docs.google.com", true) && uri.path.startsWith("/document/") &&
            !params["rtpof"].equals("true", true)) {
            // Keep explicitly requested exports (PDF, TXT, etc.).
            if (uri.path.endsWith("/export")) return clean
            return "https://docs.google.com/document/d/$fileId/export?format=docx$resourceKey"
        }
        return "https://drive.google.com/uc?export=download&id=$fileId$resourceKey"
    }

    fun documentDownloadCandidates(url: String): List<String> {
        val primary = getDirectDownloadLink(url)
        val uri = parsed(url) ?: return listOf(primary)
        if (!isGoogleFile(uri) || !uri.path.orEmpty().startsWith("/document/")) return listOf(primary)
        val fileId = Regex("/document/d/([a-zA-Z0-9_-]+)").find(uri.path)?.groupValues?.get(1)
            ?: return listOf(primary)
        val resourceKey = parameters(uri)["resourcekey"]?.let { "&resourcekey=" + URLEncoder.encode(it, "UTF-8") }.orEmpty()
        return listOf(primary, "https://drive.google.com/uc?export=download&id=$fileId$resourceKey").distinct()
    }

    suspend fun identifyFileType(url: String): FileType = withContext(Dispatchers.IO) {
        if (isYoutubeUrl(url)) return@withContext FileType.VIDEO
        val uri = parsed(url)
        val path = uri?.path.orEmpty().lowercase()
        when {
            path.endsWith(".epub") -> return@withContext FileType.EPUB
            path.endsWith(".docx") || path.endsWith(".doc") -> return@withContext FileType.WORD
            path.endsWith(".pdf") -> return@withContext FileType.PDF
            uri != null && isGoogleFile(uri) && path.startsWith("/document/") &&
                parameters(uri)["format"] !in setOf("pdf", "txt") -> return@withContext FileType.WORD
        }
        for (candidate in documentDownloadCandidates(url)) {
            val connection = runCatching { URL(candidate).openConnection() as HttpURLConnection }.getOrNull() ?: continue
            try {
                connection.requestMethod = "HEAD"
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.connect()
                if (connection.responseCode !in 200..299) continue
                val contentType = connection.contentType?.lowercase().orEmpty()
                // HTML login/error pages must never inherit "docx" from the requested URL.
                if (contentType.contains("text/html")) continue
                val disposition = connection.getHeaderField("Content-Disposition")?.lowercase().orEmpty()
                val type = when {
                    contentType.contains("epub+zip") || disposition.contains(".epub") -> FileType.EPUB
                    contentType.contains("wordprocessingml") || contentType.contains("msword") ||
                        Regex("\\.docx?(?:[\\s\\\";]|$)").containsMatchIn(disposition) -> FileType.WORD
                    contentType.startsWith("image/") -> FileType.IMAGE
                    contentType.startsWith("audio/") -> FileType.AUDIO
                    contentType.startsWith("video/") -> FileType.VIDEO
                    contentType.contains("pdf") || disposition.contains(".pdf") -> FileType.PDF
                    else -> FileType.UNKNOWN
                }
                if (type != FileType.UNKNOWN) return@withContext type
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
            } finally {
                connection.disconnect()
            }
        }
        FileType.UNKNOWN
    }

    enum class FileType { IMAGE, AUDIO, VIDEO, PDF, EPUB, WORD, UNKNOWN }
}
