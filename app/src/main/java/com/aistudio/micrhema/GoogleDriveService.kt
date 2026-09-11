package com.aistudio.micrhema

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object GoogleDriveService {

    /**
     * Converts a Google Drive share link into a direct download link.
     */
    fun getDirectDownloadLink(url: String): String {
        try {
            // Google Docs nativo: exporta diretamente como Word (.docx).
            if (url.contains("docs.google.com/document/")) {
                val docIdRegex = Regex("/document/d/([a-zA-Z0-9_-]+)")
                val docMatch = docIdRegex.find(url)
                if (docMatch != null && docMatch.groupValues.size > 1) {
                    return "https://docs.google.com/document/d/${docMatch.groupValues[1]}/export?format=docx"
                }
            }
            if (url.contains("drive.google.com") || url.contains("docs.google.com")) {
                val fileIdRegex = Regex("/d/([a-zA-Z0-9_-]+)")
                val match = fileIdRegex.find(url)
                if (match != null && match.groupValues.size > 1) {
                    return "https://drive.google.com/uc?export=download&id=${match.groupValues[1]}"
                }
                val idRegex = Regex("id=([a-zA-Z0-9_-]+)")
                val idMatch = idRegex.find(url)
                if (idMatch != null && idMatch.groupValues.size > 1) {
                    return "https://drive.google.com/uc?export=download&id=${idMatch.groupValues[1]}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return url
    }

    /**
     * Identifies the file type (PDF, EPUB, Word, image, audio, video) from a given URL.
     */
    suspend fun identifyFileType(url: String): FileType = withContext(Dispatchers.IO) {
        if (isYoutubeUrl(url)) return@withContext FileType.VIDEO

        val rawFingerprint = url.lowercase()
        if (rawFingerprint.substringBefore('?').endsWith(".epub")) return@withContext FileType.EPUB
        if (rawFingerprint.substringBefore('?').endsWith(".docx") || rawFingerprint.contains("format=docx")) return@withContext FileType.WORD
        if (rawFingerprint.substringBefore('?').endsWith(".pdf")) return@withContext FileType.PDF

        try {
            val directLink = getDirectDownloadLink(url)
            if (!directLink.startsWith("http")) return@withContext FileType.UNKNOWN

            val connection = URL(directLink).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()

            val contentType = connection.contentType?.lowercase().orEmpty()
            val contentDisposition = connection.getHeaderField("Content-Disposition")?.lowercase().orEmpty()
            val resolvedUrl = connection.url.toString().lowercase()
            connection.disconnect()

            val fingerprint = "$contentType $contentDisposition $resolvedUrl $rawFingerprint"
            return@withContext when {
                contentType.contains("epub+zip") || fingerprint.contains(".epub") -> FileType.EPUB
                contentType.contains("wordprocessingml") ||
                    contentType.contains("msword") ||
                    fingerprint.contains(".docx") ||
                    fingerprint.contains("format=docx") -> FileType.WORD
                contentType.startsWith("image/") -> FileType.IMAGE
                contentType.startsWith("audio/") -> FileType.AUDIO
                contentType.startsWith("video/") -> FileType.VIDEO
                contentType.contains("pdf") -> FileType.PDF
                else -> FileType.UNKNOWN
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FileType.UNKNOWN
        }
    }

    enum class FileType {
        IMAGE, AUDIO, VIDEO, PDF, EPUB, WORD, UNKNOWN
    }
}
