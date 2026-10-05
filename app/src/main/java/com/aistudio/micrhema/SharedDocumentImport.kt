package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

enum class SharedDocumentType(
    val contentBookType: String,
    val mimeType: String,
    val extension: String,
    val label: String
) {
    PDF("pdf", "application/pdf", "pdf", "PDF"),
    EPUB("epub", "application/epub+zip", "epub", "EPUB"),
    DOC("word", "application/msword", "doc", "Word antigo"),
    DOCX("word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", "Word")
}

data class SharedDocumentCandidate(
    val localUri: Uri,
    val displayName: String,
    val suggestedTitle: String,
    val type: SharedDocumentType,
    val sizeBytes: Long
)

/**
 * Materializa o compartilhamento imediatamente no cache do MIC Rhema.
 * Assim o arquivo continua disponível mesmo que o app de origem (ex.: Xodo)
 * revogue a permissão temporária do content:// depois que a tela for aberta.
 */
object SharedDocumentImport {
    private const val MAX_BYTES = 50L * 1024L * 1024L

    suspend fun resolve(context: Context, intent: Intent): SharedDocumentCandidate = withContext(Dispatchers.IO) {
        val streamUri = firstStreamUri(intent)
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val sourceUri = streamUri ?: intent.data

        val temp = File.createTempFile("shared_ibr_", ".tmp", context.cacheDir)
        var originalName = ""
        var hintedMime = intent.type.orEmpty().lowercase().substringBefore(';')

        try {
            when {
                sourceUri != null && sourceUri.scheme !in setOf("http", "https") -> {
                    originalName = queryDisplayName(context, sourceUri)
                    if (hintedMime.isBlank() || hintedMime == "application/octet-stream" || hintedMime == "*/*") {
                        hintedMime = context.contentResolver.getType(sourceUri).orEmpty().lowercase().substringBefore(';')
                    }
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        copyLimited(input, temp)
                    } ?: throw IllegalArgumentException("Não foi possível ler o arquivo compartilhado.")
                }

                sourceUri?.scheme in setOf("http", "https") ||
                    sharedText.startsWith("http://", true) || sharedText.startsWith("https://", true) -> {
                    val result = downloadRemote(sourceUri?.toString() ?: sharedText, temp)
                    originalName = result.first
                    if (hintedMime.isBlank() || hintedMime == "text/plain" || hintedMime == "application/octet-stream" || hintedMime == "*/*") {
                        hintedMime = result.second
                    }
                }

                else -> throw IllegalArgumentException("Compartilhe um arquivo PDF, EPUB, Word (.doc ou .docx) ou um link direto para um desses arquivos.")
            }

            val detected = detectType(temp)
            val safeBase = sanitizeBaseName(originalName.substringBeforeLast('.', originalName))
                .ifBlank { "material-ibr-${System.currentTimeMillis()}" }
            val finalFile = File(context.cacheDir, "${safeBase.take(70)}_${System.currentTimeMillis()}.${detected.extension}")
            if (!temp.renameTo(finalFile)) {
                temp.inputStream().use { input -> FileOutputStream(finalFile).use { output -> input.copyTo(output) } }
                temp.delete()
            }
            val localUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                finalFile
            )
            SharedDocumentCandidate(
                localUri = localUri,
                displayName = if (originalName.isBlank()) finalFile.name else originalName,
                suggestedTitle = originalName.substringBeforeLast('.', originalName).trim().ifBlank { "Material ${detected.label}" },
                type = detected,
                sizeBytes = finalFile.length()
            )
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun firstStreamUri(intent: Intent): Uri? {
        val direct = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        if (direct != null) return direct
        val list = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (!list.isNullOrEmpty()) return list.first()
        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) return clip.getItemAt(0).uri
        return null
    }

    private fun queryDisplayName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index).orEmpty() else ""
        }.orEmpty()
    }.getOrDefault("")

    private fun downloadRemote(sourceUrl: String, destination: File): Pair<String, String> {
        val (disposition, mime) = RemoteDocumentDownload.download(sourceUrl, destination)
            val name = Regex("(?i)filename\\*?=(?:UTF-8''|\")?([^\";]+)")
            .find(disposition)?.groupValues?.getOrNull(1)?.let { Uri.decode(it.trim().trim('"')) }.orEmpty()
        return name to mime.lowercase().substringBefore(';')
    }

    private fun copyLimited(input: java.io.InputStream, destination: File) {
        DocumentFiles.copy(input, destination)
    }

    private fun detectType(file: File): SharedDocumentType = when (DocumentFiles.detect(file)) {
        DocumentFormat.PDF -> SharedDocumentType.PDF
        DocumentFormat.DOCX -> SharedDocumentType.DOCX
        DocumentFormat.EPUB -> SharedDocumentType.EPUB
        DocumentFormat.DOC -> {
            // Other OLE documents (Excel, encrypted files) must not be accepted as Word.
            LegacyWordText.read(file)
            SharedDocumentType.DOC
        }
    }

    private fun sanitizeBaseName(value: String): String = value
        .replace(Regex("[^\\p{L}\\p{N}._ -]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
