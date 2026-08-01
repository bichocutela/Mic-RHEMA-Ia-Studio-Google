package com.aistudio.micrhema

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloader {
    suspend fun downloadFile(
        context: Context,
        url: String,
        fileName: String,
        directoryName: String = "audio_downloads",
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val fileLength = connection.contentLength
            val directory = File(context.filesDir, directoryName)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val outputFile = File(directory, fileName)
            
            // Se já baixou e o tamanho for igual (ou aproximado, assumindo que > 0 é suficiente)
            if (outputFile.exists() && outputFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    onProgress(100f)
                }
                return@withContext outputFile
            }

            val input = connection.inputStream
            val output = FileOutputStream(outputFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            var lastProgress = 0f

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = (total * 100f / fileLength)
                    if (progress - lastProgress >= 1f) {
                        lastProgress = progress
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()
            
            withContext(Dispatchers.Main) {
                onProgress(100f)
            }
            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    fun getLocalFile(context: Context, fileName: String, directoryName: String = "audio_downloads"): File? {
        val directory = File(context.filesDir, directoryName)
        val file = File(directory, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }
}
