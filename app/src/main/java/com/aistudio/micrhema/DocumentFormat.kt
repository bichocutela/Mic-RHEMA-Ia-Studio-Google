package com.aistudio.micrhema

import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile

/** File bytes decide the format; an HTTP header or filename alone is not enough. */
enum class DocumentFormat { PDF, DOC, DOCX, EPUB }

object DocumentFiles {
    const val MAX_BYTES = 50L * 1024L * 1024L

    fun detect(file: File): DocumentFormat {
        require(file.length() in 1..MAX_BYTES) { "O documento está vazio ou ultrapassa 50 MB." }
        val header = file.inputStream().use { input ->
            val bytes = ByteArray(8)
            val count = input.read(bytes)
            bytes.take(count.coerceAtLeast(0)).toByteArray()
        }
        if (header.size >= 5 && String(header, 0, 5, Charsets.US_ASCII) == "%PDF-") return DocumentFormat.PDF
        if (header.contentEquals(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()))) return DocumentFormat.DOC
        if (header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
            ZipFile(file).use { zip ->
                if (zip.getEntry("word/document.xml") != null) return DocumentFormat.DOCX
                if (zip.getEntry("META-INF/container.xml") != null) return DocumentFormat.EPUB
            }
        }
        throw IllegalArgumentException("O endereço não retornou um PDF, Word ou EPUB válido. Confira o arquivo e a permissão de acesso ao link.")
    }

    /** Publish a cache entry only after the entire transfer and validation succeed. */
    fun copy(input: InputStream, destination: File, validate: (File) -> Unit = { detect(it); Unit }) {
        destination.parentFile?.mkdirs()
        val staging = File.createTempFile("document_", ".part", destination.parentFile)
        try {
            var total = 0L
            FileOutputStream(staging).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    total += read
                    require(total <= MAX_BYTES) { "O documento ultrapassa 50 MB." }
                    output.write(buffer, 0, read)
                }
            }
            require(total > 0L) { "O arquivo recebido está vazio." }
            validate(staging)
            check(staging.renameTo(destination)) { "Não foi possível salvar o documento para leitura." }
        } finally {
            staging.delete()
        }
    }
}
