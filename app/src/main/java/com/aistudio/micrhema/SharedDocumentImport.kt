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
                sourceUri != null -> {
                    originalName = queryDisplayName(context, sourceUri)
                    if (hintedMime.isBlank() || hintedMime == "application/octet-stream" || hintedMime == "*/*") {
                        hintedMime = context.contentResolver.getType(sourceUri).orEmpty().lowercase().substringBefore(';')
                    }
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        copyLimited(input, temp)
                    } ?: throw IllegalArgumentException("Não foi possível ler o arquivo compartilhado.")
                }

                sharedText.startsWith("http://", true) || sharedText.startsWith("https://", true) -> {
                    val result = downloadRemote(sharedText, temp)
                    originalName = result.first
                    if (hintedMime.isBlank() || hintedMime == "text/plain" || hintedMime == "application/octet-stream" || hintedMime == "*/*") {
                        hintedMime = result.second
                    }
                }

                else -> throw IllegalArgumentException("Compartilhe um arquivo PDF, EPUB, Word (.docx) ou um link direto para um desses arquivos.")
            }

            val detected = detectType(temp, originalName, hintedMime)
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
        val resolved = convertGoogleDriveUrl(sourceUrl)
        val connection = URL(resolved).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "MIC-Rhema/Android")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalArgumentException("Não foi possível baixar o link compartilhado (HTTP ${connection.responseCode}).")
            }
            val announced = connection.contentLengthLong
            if (announced > MAX_BYTES) throw IllegalArgumentException("O arquivo compartilhado excede o limite de 50 MB.")
            connection.inputStream.use { input -> copyLimited(input, destination) }
            val disposition = connection.getHeaderField("Content-Disposition").orEmpty()
            val nameFromDisposition = Regex("(?i)filename\\*?=(?:UTF-8''|\")?([^\";]+)")
                .find(disposition)?.groupValues?.getOrNull(1)?.let { Uri.decode(it.trim().trim('"')) }.orEmpty()
            val urlName = runCatching { Uri.parse(connection.url.toString()).lastPathSegment.orEmpty() }.getOrDefault("")
            return (nameFromDisposition.ifBlank { urlName }) to connection.contentType.orEmpty().lowercase().substringBefore(';')
        } finally {
            connection.disconnect()
        }
    }

    private fun copyLimited(input: java.io.InputStream, destination: File) {
        var total = 0L
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_BYTES) {
                    destination.delete()
                    throw IllegalArgumentException("O arquivo compartilhado excede o limite de 50 MB.")
                }
                output.write(buffer, 0, read)
            }
        }
        if (total <= 0L) {
            destination.delete()
            throw IllegalArgumentException("O arquivo compartilhado está vazio.")
        }
    }

    private fun detectType(file: File, displayName: String, mime: String): SharedDocumentType {
        val lowerName = displayName.lowercase()
        val normalizedMime = mime.lowercase().substringBefore(';')

        if (lowerName.endsWith(".doc") || normalizedMime == "application/msword") {
            throw IllegalArgumentException("O formato Word antigo .doc não é compatível com o leitor interno. Salve ou compartilhe como .docx.")
        }
        if (lowerName.endsWith(".pdf") || normalizedMime == "application/pdf" || hasPdfHeader(file)) return SharedDocumentType.PDF
        if (lowerName.endsWith(".docx") || normalizedMime.contains("wordprocessingml")) return SharedDocumentType.DOCX
        if (lowerName.endsWith(".epub") || normalizedMime.contains("epub+zip")) return SharedDocumentType.EPUB

        if (isZip(file)) {
            runCatching {
                ZipFile(file).use { zip ->
                    if (zip.getEntry("word/document.xml") != null) return SharedDocumentType.DOCX
                    if (zip.getEntry("META-INF/container.xml") != null) return SharedDocumentType.EPUB
                }
            }
        }

        throw IllegalArgumentException("Não foi possível identificar o arquivo. Use PDF, EPUB ou Word no formato .docx.")
    }

    private fun hasPdfHeader(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val bytes = ByteArray(5)
            input.read(bytes) == 5 && String(bytes, Charsets.US_ASCII) == "%PDF-"
        }
    }.getOrDefault(false)

    private fun isZip(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val first = input.read()
            val second = input.read()
            first == 0x50 && second == 0x4B
        }
    }.getOrDefault(false)

    private fun sanitizeBaseName(value: String): String = value
        .replace(Regex("[^\\p{L}\\p{N}._ -]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
