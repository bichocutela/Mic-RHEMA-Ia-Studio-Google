package com.aistudio.micrhema

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class DocumentFilesTest {
    private fun withFile(block: (File) -> Unit) {
        val file = File.createTempFile("document-test", ".bin")
        try { block(file) } finally { file.delete() }
    }

    @Test fun htmlDisguisedAsWordIsRejected() = withFile { file ->
        file.writeText("<html>Sign in</html>")
        assertThrows(IllegalArgumentException::class.java) { DocumentFiles.detect(file) }
    }

    @Test fun zipContentsDecideWordOrEpub() = withFile { file ->
        for ((entry, expected) in listOf("word/document.xml" to DocumentFormat.DOCX, "META-INF/container.xml" to DocumentFormat.EPUB)) {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry(entry))
                zip.write("<xml/>".toByteArray())
                zip.closeEntry()
            }
            assertEquals(expected, DocumentFiles.detect(file))
        }
    }

    @Test fun unrelatedZipIsRejected() = withFile { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("unrelated.txt"))
            zip.write("hello".toByteArray())
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { DocumentFiles.detect(file) }
    }

    @Test fun failedTransferDoesNotReplaceCachedFile() = withFile { file ->
        file.writeText("previous cache")
        val failing = object : InputStream() {
            var calls = 0
            override fun read(): Int {
                if (calls++ > 5) throw java.io.IOException("Connection interrupted")
                return 'P'.code
            }
        }
        assertThrows(java.io.IOException::class.java) { DocumentFiles.copy(failing, file) }
        assertEquals("previous cache", file.readText())
        assertTrue(file.parentFile.listFiles().orEmpty().none { it.name.startsWith("document_") && it.extension == "part" })
    }

    @Test fun invalidDownloadDoesNotCreateCache() = withFile { file ->
        file.delete()
        assertThrows(IllegalArgumentException::class.java) {
            DocumentFiles.copy("<html>Access denied</html>".byteInputStream(), file)
        }
        assertFalse(file.exists())
    }

    @Test fun pdfIsDetectedFromBytes() = withFile { file ->
        DocumentFiles.copy("%PDF-1.7\nexample".byteInputStream(), file)
        assertEquals(DocumentFormat.PDF, DocumentFiles.detect(file))
    }

    @Test fun legacyWordIsReadAndConvertedToValidDocx() = withFile { source ->
        javaClass.getResourceAsStream("/fixtures/SampleDoc.doc")!!.use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        assertEquals(DocumentFormat.DOC, DocumentFiles.detect(source))
        val text = LegacyWordText.read(source)
        assertTrue(text.contains("I am a test document"))
        assertTrue(text.contains("This is page two"))
        withFile { converted ->
            LegacyWordText.convertToDocx(source, converted)
            assertEquals(DocumentFormat.DOCX, DocumentFiles.detect(converted))
            java.util.zip.ZipFile(converted).use { zip ->
                zip.getInputStream(zip.getEntry("word/document.xml")).use { input ->
                    val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
                    assertTrue(xml.documentElement.textContent.contains("I am a test document"))
                    assertTrue(xml.documentElement.textContent.contains("This is page two"))
                }
            }
        }
    }

    @Test fun nonWordOleFileDoesNotYieldText() = withFile { file ->
        org.apache.poi.poifs.filesystem.POIFSFileSystem().use { fs ->
            fs.root.createDocument("Workbook", "not Word".byteInputStream())
            file.outputStream().use { fs.writeFilesystem(it) }
        }
        assertThrows(Exception::class.java) { LegacyWordText.read(file) }
    }
}
