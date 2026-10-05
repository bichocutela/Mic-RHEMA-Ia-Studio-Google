package com.aistudio.micrhema

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object RemoteDocumentDownload {
    fun download(sourceUrl: String, destination: File): Triple<String, String, String> {
        var lastError: Exception? = null
        for (candidate in GoogleDriveService.documentDownloadCandidates(sourceUrl)) {
            val connection = URL(candidate).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "MIC-Rhema/Android")
                connection.connect()
                require(connection.responseCode in 200..299) { "Não foi possível baixar o documento (HTTP ${connection.responseCode}). Confira a permissão de acesso ao link." }
                require(connection.contentLengthLong <= DocumentFiles.MAX_BYTES) { "O documento ultrapassa 50 MB." }
                connection.inputStream.use { DocumentFiles.copy(it, destination) }
                return Triple(connection.getHeaderField("Content-Disposition").orEmpty(), connection.contentType.orEmpty(), candidate)
            } catch (error: Exception) {
                lastError = error
            } finally {
                connection.disconnect()
            }
        }
        throw lastError ?: IllegalArgumentException("O documento não possui um endereço válido.")
    }
}
