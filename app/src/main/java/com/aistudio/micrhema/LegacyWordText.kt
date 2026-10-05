package com.aistudio.micrhema

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.poi.hwpf.extractor.WordExtractor

/** Text-only reader for Word 97–2003; no macros or embedded objects are executed. */
object LegacyWordText {
    fun read(file: File): String = file.inputStream().use { input ->
        WordExtractor(input).use { extractor ->
            extractor.paragraphText.joinToString("\n") { WordExtractor.stripFields(it) }
                .replace('\r', '\n')
                .replace('\u0007', '\t')
                .replace('\u000B', '\n')
                .replace('\u000C', '\n')
                .replace(Regex("[\\x00-\\x08\\x0E-\\x1F]"), "")
                .trim()
                .also { require(it.isNotBlank()) { "O documento Word não contém texto legível." } }
        }
    }

    /** Existing storage accepts DOCX. Preserve the text of legacy uploads in that format. */
    fun convertToDocx(source: File, destination: File) {
        val text = read(source)
        fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val paragraphs = text.split('\n').joinToString("") {
            "<w:p><w:r><w:t xml:space=\"preserve\">${xml(it)}</w:t></w:r></w:p>"
        }
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            entry("word/document.xml", """<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paragraphs<w:sectPr/></w:body></w:document>""")
        }
    }
}
