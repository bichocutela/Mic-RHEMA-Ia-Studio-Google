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
     * Identifies the file type (PDF, image, audio, video) from a given URL.
     */
    suspend fun identifyFileType(url: String): FileType = withContext(Dispatchers.IO) {
        if (isYoutubeUrl(url)) return@withContext FileType.VIDEO
        try {
            val directLink = getDirectDownloadLink(url)
            if (!directLink.startsWith("http")) return@withContext FileType.UNKNOWN
            
            val connection = URL(directLink).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            val contentType = connection.contentType?.lowercase() ?: ""
            connection.disconnect()
            
            return@withContext when {
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
        IMAGE, AUDIO, VIDEO, PDF, UNKNOWN
    }
}
