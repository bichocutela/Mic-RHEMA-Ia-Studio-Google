package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Baixa o APK da release em arquivo temporário, valida se é um pacote instalável
 * do próprio MIC Rhema e só então entrega o arquivo ao instalador do Android.
 */
class UpdateDownloader(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun updateDirectory(): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun updateFile(version: String): File = File(updateDirectory(), "MIC-Rhema-v$version.apk")

    fun downloadUpdate(url: String, version: String): Flow<DownloadState> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO)
        val target = updateFile(version)
        val temporary = File(target.parentFile, "${target.name}.part")

        scope.launch {
            try {
                temporary.delete()
                target.delete()
                trySend(DownloadState.Downloading(0))

                val response = client.newCall(Request.Builder().url(url).build()).execute()
                response.use { httpResponse ->
                    if (!httpResponse.isSuccessful) {
                        throw IOException("O servidor não disponibilizou o APK (código ${httpResponse.code}).")
                    }
                    val body = httpResponse.body ?: throw IOException("O servidor retornou um arquivo vazio.")

                    val totalBytes = body.contentLength()
                    body.byteStream().use { input ->
                        FileOutputStream(temporary).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloadedBytes = 0L
                            var lastProgress = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                if (totalBytes > 0L) {
                                    val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        trySend(DownloadState.Downloading(progress))
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                }

                val validationError = validateApk(temporary)
                if (validationError != null) {
                    temporary.delete()
                    trySend(DownloadState.Error(validationError))
                    close()
                    return@launch
                }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
                trySend(DownloadState.Downloaded)
            } catch (error: Exception) {
                temporary.delete()
                trySend(DownloadState.Error("Não foi possível baixar a atualização: ${error.message ?: "verifique a conexão"}"))
            } finally {
                close()
            }
        }

        awaitClose { scope.cancel() }
    }

    private fun validateApk(file: File): String? {
        if (!file.exists() || file.length() < 1024L) {
            return "O arquivo baixado está incompleto. Tente novamente."
        }
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            ?: return "O arquivo baixado não é um APK válido. Tente novamente."
        if (packageInfo.packageName != context.packageName) {
            return "O arquivo baixado não pertence ao MIC Rhema. Tente novamente."
        }
        return null
    }

    fun installApk(version: String): Boolean {
        val file = updateFile(version)
        val validationError = validateApk(file)
        if (validationError != null) return false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
