package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Formatos compactados que o MIC Rhema renderiza sem depender de outro aplicativo. */
enum class RichDocumentKind { DOCX, EPUB }

private data class PreparedRichDocument(
    val html: String,
    val baseUrl: String,
    val cacheKey: String
)

@Composable
fun RichDocumentReader(
    sourceUrl: String,
    title: String,
    kind: RichDocumentKind,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    var prepared by remember(sourceUrl, kind) { mutableStateOf<PreparedRichDocument?>(null) }
    var loading by remember(sourceUrl, kind) { mutableStateOf(true) }
    var error by remember(sourceUrl, kind) { mutableStateOf<String?>(null) }

    LaunchedEffect(sourceUrl, kind, background, foreground, primary) {
        loading = true
        error = null
        prepared = null
        runCatching {
            withContext(Dispatchers.IO) {
                RichDocumentParser.prepare(
                    context = context,
                    sourceUrl = sourceUrl,
                    title = title,
                    kind = kind,
                    backgroundCss = cssColor(background.toArgb()),
                    foregroundCss = cssColor(foreground.toArgb()),
                    primaryCss = cssColor(primary.toArgb())
                )
            }
        }.onSuccess { prepared = it }
            .onFailure { throwable ->
                error = throwable.message ?: "Não foi possível preparar o documento para leitura."
            }
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(if (kind == RichDocumentKind.DOCX) "Preparando documento Word…" else "Preparando livro EPUB…")
            }

            error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { StudyMaterialDownload.openExternalDocument(context, sourceUrl) }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir em outro aplicativo")
                }
            }

            prepared != null -> RichDocumentWebView(
                prepared = prepared!!,
                sourceUrl = sourceUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RichDocumentWebView(
    prepared: PreparedRichDocument,
    sourceUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rich_document_reader", Context.MODE_PRIVATE) }
    val positionKey = remember(sourceUrl) { "scroll_${sourceUrl.hashCode()}" }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.loadsImagesAutomatically = true
                settings.textZoom = 100
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url ?: return false
                        val scheme = target.scheme.orEmpty().lowercase()
                        if (scheme == "http" || scheme == "https" || scheme == "mailto") {
                            runCatching {
                                viewContext.startActivity(Intent(Intent.ACTION_VIEW, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val savedY = prefs.getInt(positionKey, 0)
                        if (savedY > 0) view?.post { view.scrollTo(0, savedY) }
                    }
                }
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    prefs.edit().putInt(positionKey, scrollY.coerceAtLeast(0)).apply()
                    val range = ((contentHeight * scale).toInt() - height).coerceAtLeast(1)
                    XpMediaClient.recordBook(viewContext, sourceUrl, (scrollY.toFloat() / range.toFloat()).coerceIn(0f, 1f))
                }
            }
        },
        update = { webView ->
            if (webView.tag != prepared.cacheKey) {
                webView.tag = prepared.cacheKey
                webView.loadDataWithBaseURL(
                    prepared.baseUrl,
                    prepared.html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

private fun cssColor(argb: Int): String = String.format("#%06X", 0xFFFFFF and argb)

private object RichDocumentParser {
    private const val MAX_PACKAGE_BYTES = 50L * 1024L * 1024L
    private const val MAX_UNPACKED_BYTES = 120L * 1024L * 1024L
    private const val MAX_ZIP_ENTRIES = 5000

    fun prepare(
        context: Context,
        sourceUrl: String,
        title: String,
        kind: RichDocumentKind,
        backgroundCss: String,
        foregroundCss: String,
        primaryCss: String
    ): PreparedRichDocument {
        if (sourceUrl.isBlank()) throw IllegalArgumentException("O documento não possui um endereço válido.")
        val cacheId = "${kind.name.lowercase()}_${sourceUrl.hashCode().toUInt().toString(16)}"
        val extension = if (kind == RichDocumentKind.DOCX) "docx" else "epub"
        val packageFile = File(context.cacheDir, "reader_$cacheId.$extension")
        if (!packageFile.exists() || packageFile.length() <= 0L) {
            copySourceToFile(context, sourceUrl, packageFile)
        }
        if (packageFile.length() > MAX_PACKAGE_BYTES) {
            packageFile.delete()
            throw IllegalArgumentException("O arquivo excede o limite de 50 MB do leitor interno.")
        }

        val unpacked = File(context.cacheDir, "reader_unpack_$cacheId")
        if (!unpacked.exists() || unpacked.listFiles().isNullOrEmpty()) {
            unpacked.deleteRecursively()
            unpacked.mkdirs()
            unpackZip(packageFile, unpacked)
        }

        val body = when (kind) {
            RichDocumentKind.DOCX -> renderDocx(unpacked)
            RichDocumentKind.EPUB -> renderEpub(unpacked)
        }
        if (body.isBlank()) throw IllegalArgumentException("O documento não contém texto compatível com o leitor interno.")

        val html = wrapHtml(
            title = title,
            body = body,
            backgroundCss = backgroundCss,
            foregroundCss = foregroundCss,
            primaryCss = primaryCss
        )
        return PreparedRichDocument(
            html = html,
            baseUrl = unpacked.toURI().toString(),
            cacheKey = "$cacheId:${packageFile.length()}:$backgroundCss:$foregroundCss"
        )
    }

    private fun copySourceToFile(context: Context, sourceUrl: String, destination: File) {
        destination.parentFile?.mkdirs()
        if (sourceUrl.startsWith("http://", true) || sourceUrl.startsWith("https://", true)) {
            val resolved = convertGoogleDriveUrl(sourceUrl)
            val connection = URL(resolved).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw IllegalArgumentException("O servidor respondeu ${connection.responseCode} ao baixar o documento.")
                }
                val announced = connection.contentLengthLong
                if (announced > MAX_PACKAGE_BYTES) throw IllegalArgumentException("O arquivo excede o limite de 50 MB.")
                connection.inputStream.use { input -> copyLimited(input, destination, MAX_PACKAGE_BYTES) }
            } finally {
                connection.disconnect()
            }
        } else {
            val uri = Uri.parse(sourceUrl)
            context.contentResolver.openInputStream(uri)?.use { input ->
                copyLimited(input, destination, MAX_PACKAGE_BYTES)
            } ?: throw IllegalArgumentException("Não foi possível ler o documento selecionado.")
        }
    }

    private fun copyLimited(input: java.io.InputStream, destination: File, maxBytes: Long) {
        var total = 0L
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) {
                    destination.delete()
                    throw IllegalArgumentException("O arquivo excede o limite de 50 MB.")
                }
                output.write(buffer, 0, read)
            }
        }
        if (total <= 0L) {
            destination.delete()
            throw IllegalArgumentException("O arquivo recebido está vazio.")
        }
    }

    private fun unpackZip(packageFile: File, destination: File) {
        val root = destination.canonicalFile
        var total = 0L
        var entries = 0
        ZipInputStream(packageFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                if (entries > MAX_ZIP_ENTRIES) throw IllegalArgumentException("O documento possui arquivos internos demais.")
                val target = File(root, entry.name).canonicalFile
                if (!target.path.startsWith(root.path + File.separator)) {
                    throw IllegalArgumentException("O documento contém um caminho interno inválido.")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > MAX_UNPACKED_BYTES) {
                                throw IllegalArgumentException("O documento expandido excede o limite permitido.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun renderDocx(root: File): String {
        val documentFile = File(root, "word/document.xml")
        if (!documentFile.exists()) throw IllegalArgumentException("Este arquivo não parece ser um DOCX válido.")
        val rels = parseDocxRelationships(File(root, "word/_rels/document.xml.rels"))
        val document = parseXml(documentFile)
        val body = document.getElementsByTagNameNS("*", "body").item(0)
            ?: throw IllegalArgumentException("O DOCX não possui corpo de texto.")
        val html = StringBuilder()
        for (child in childNodes(body)) {
            when (tag(child)) {
                "p" -> html.append(renderDocxParagraph(child, root, rels))
                "tbl" -> html.append(renderDocxTable(child, root, rels))
            }
        }
        return html.toString()
    }

    private data class DocxRelationship(val target: String, val external: Boolean)

    private fun parseDocxRelationships(file: File): Map<String, DocxRelationship> {
        if (!file.exists()) return emptyMap()
        val document = parseXml(file)
        val result = linkedMapOf<String, DocxRelationship>()
        val relationships = document.getElementsByTagNameNS("*", "Relationship")
        for (i in 0 until relationships.length) {
            val element = relationships.item(i) as? Element ?: continue
            val id = element.getAttribute("Id").trim()
            val target = element.getAttribute("Target").trim()
            if (id.isNotBlank() && target.isNotBlank()) {
                result[id] = DocxRelationship(target, element.getAttribute("TargetMode").equals("External", true))
            }
        }
        return result
    }

    private fun renderDocxParagraph(node: Node, root: File, rels: Map<String, DocxRelationship>): String {
        val properties = childNodes(node).firstOrNull { tag(it) == "pPr" }
        val styleName = properties?.let { firstDescendant(it, "pStyle") }?.let { attributeLocal(it, "val") }.orEmpty()
        val numbered = properties?.let { firstDescendant(it, "numPr") } != null
        val alignment = properties?.let { firstDescendant(it, "jc") }?.let { attributeLocal(it, "val") }.orEmpty()
        val alignStyle = when (alignment.lowercase()) {
            "center" -> " style=\"text-align:center\""
            "right", "end" -> " style=\"text-align:right\""
            "both", "justify" -> " style=\"text-align:justify\""
            else -> ""
        }
        val content = buildString {
            if (numbered) append("<span class=\"bullet\">•</span>")
            childNodes(node).filter { tag(it) != "pPr" }.forEach { append(renderDocxInline(it, root, rels)) }
        }.ifBlank { "&nbsp;" }

        val normalized = styleName.lowercase()
        val heading = when {
            normalized.contains("heading1") || normalized.contains("título1") || normalized.contains("titulo1") -> "h1"
            normalized.contains("heading2") || normalized.contains("título2") || normalized.contains("titulo2") -> "h2"
            normalized.contains("heading3") || normalized.contains("título3") || normalized.contains("titulo3") -> "h3"
            normalized == "title" || normalized == "título" || normalized == "titulo" -> "h1"
            else -> "p"
        }
        return "<$heading$alignStyle>$content</$heading>"
    }

    private fun renderDocxInline(node: Node, root: File, rels: Map<String, DocxRelationship>): String = when (tag(node)) {
        "r" -> {
            val props = childNodes(node).firstOrNull { tag(it) == "rPr" }
            val bold = props?.let { firstDescendant(it, "b") } != null
            val italic = props?.let { firstDescendant(it, "i") } != null
            val underline = props?.let { firstDescendant(it, "u") } != null
            var content = buildString {
                childNodes(node).filter { tag(it) != "rPr" }.forEach { child ->
                    when (tag(child)) {
                        "t" -> append(escapeHtml(child.textContent.orEmpty()))
                        "tab" -> append("&emsp;")
                        "br", "cr" -> append("<br>")
                        "drawing", "pict", "object" -> append(renderDocxImage(child, root, rels))
                        else -> append(renderDocxInline(child, root, rels))
                    }
                }
            }
            if (underline) content = "<u>$content</u>"
            if (italic) content = "<em>$content</em>"
            if (bold) content = "<strong>$content</strong>"
            content
        }
        "hyperlink" -> {
            val relId = attributeLocal(node, "id")
            val target = rels[relId]
            val inner = childNodes(node).joinToString("") { renderDocxInline(it, root, rels) }
            if (target?.external == true && target.target.startsWith("http")) {
                "<a href=\"${escapeAttribute(target.target)}\">$inner</a>"
            } else inner
        }
        "t" -> escapeHtml(node.textContent.orEmpty())
        "tab" -> "&emsp;"
        "br", "cr" -> "<br>"
        "drawing", "pict", "object" -> renderDocxImage(node, root, rels)
        else -> childNodes(node).joinToString("") { renderDocxInline(it, root, rels) }
    }

    private fun renderDocxImage(node: Node, root: File, rels: Map<String, DocxRelationship>): String {
        val blip = firstDescendant(node, "blip") ?: firstDescendant(node, "imagedata") ?: return ""
        val relId = attributeLocal(blip, "embed").ifBlank { attributeLocal(blip, "id") }
        val relation = rels[relId] ?: return ""
        if (relation.external) return ""
        val base = File(root, "word").canonicalFile
        val target = File(base, relation.target).canonicalFile
        if (!target.path.startsWith(root.canonicalPath + File.separator) || !target.exists()) return ""
        val relative = target.relativeTo(root).path.replace(File.separatorChar, '/')
        return "<img src=\"${escapeAttribute(relative)}\" alt=\"Imagem do documento\">"
    }

    private fun renderDocxTable(node: Node, root: File, rels: Map<String, DocxRelationship>): String {
        val rows = childNodes(node).filter { tag(it) == "tr" }
        if (rows.isEmpty()) return ""
        return buildString {
            append("<div class=\"table-wrap\"><table>")
            rows.forEach { row ->
                append("<tr>")
                childNodes(row).filter { tag(it) == "tc" }.forEach { cell ->
                    append("<td>")
                    childNodes(cell).filter { tag(it) == "p" || tag(it) == "tbl" }.forEach { content ->
                        if (tag(content) == "p") append(renderDocxParagraph(content, root, rels))
                        else append(renderDocxTable(content, root, rels))
                    }
                    append("</td>")
                }
                append("</tr>")
            }
            append("</table></div>")
        }
    }

    private fun renderEpub(root: File): String {
        val container = File(root, "META-INF/container.xml")
        if (!container.exists()) throw IllegalArgumentException("Este arquivo não parece ser um EPUB válido.")
        val containerDoc = parseXml(container)
        val rootfile = containerDoc.getElementsByTagNameNS("*", "rootfile").item(0) as? Element
            ?: throw IllegalArgumentException("O EPUB não informa o arquivo principal.")
        val opfPath = rootfile.getAttribute("full-path").trim()
        if (opfPath.isBlank()) throw IllegalArgumentException("O EPUB possui uma estrutura incompleta.")
        val opf = File(root, opfPath).canonicalFile
        if (!opf.path.startsWith(root.canonicalPath + File.separator) || !opf.exists()) {
            throw IllegalArgumentException("O arquivo principal do EPUB não foi encontrado.")
        }

        val opfDoc = parseXml(opf)
        val manifest = linkedMapOf<String, Pair<String, String>>()
        val items = opfDoc.getElementsByTagNameNS("*", "item")
        for (i in 0 until items.length) {
            val item = items.item(i) as? Element ?: continue
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            val mediaType = item.getAttribute("media-type")
            if (id.isNotBlank() && href.isNotBlank()) manifest[id] = href to mediaType
        }

        val spineIds = mutableListOf<String>()
        val itemRefs = opfDoc.getElementsByTagNameNS("*", "itemref")
        for (i in 0 until itemRefs.length) {
            val ref = itemRefs.item(i) as? Element ?: continue
            ref.getAttribute("idref").takeIf { it.isNotBlank() }?.let(spineIds::add)
        }

        val readingOrder = spineIds.mapNotNull { manifest[it]?.first }
            .ifEmpty {
                manifest.values.filter { (_, type) -> type.contains("xhtml") || type.contains("html") }.map { it.first }
            }
        if (readingOrder.isEmpty()) throw IllegalArgumentException("O EPUB não possui capítulos legíveis.")

        val opfDir = opf.parentFile ?: root
        val sections = mutableListOf<String>()
        val stylesheets = linkedSetOf<String>()
        readingOrder.forEach { href ->
            val chapter = File(opfDir, Uri.decode(href.substringBefore('#'))).canonicalFile
            if (!chapter.path.startsWith(root.canonicalPath + File.separator) || !chapter.exists() || !chapter.isFile) return@forEach
            var raw = runCatching { chapter.readText(Charsets.UTF_8) }.getOrNull() ?: return@forEach
            raw = raw.replace(Regex("(?is)<script\\b[^>]*>.*?</script>"), "")
                .replace(Regex("(?is)<iframe\\b[^>]*>.*?</iframe>"), "")
                .replace(Regex("(?is)<object\\b[^>]*>.*?</object>"), "")
                .replace(Regex("(?is)\\son[a-z]+\\s*=\\s*[\"'][^\"']*[\"']"), "")

            Regex("(?is)<link\\b[^>]*>").findAll(raw)
                .map { it.value }
                .filter { it.contains("stylesheet", ignoreCase = true) }
                .map { rewriteRelativeAttributes(it, chapter.parentFile ?: opfDir, root) }
                .forEach(stylesheets::add)

            val body = Regex("(?is)<body\\b[^>]*>(.*?)</body>").find(raw)?.groupValues?.getOrNull(1)
                ?: raw.substringAfter("?>", raw)
            val rewritten = rewriteRelativeAttributes(body, chapter.parentFile ?: opfDir, root)
            if (rewritten.isNotBlank()) sections += "<article class=\"epub-chapter\">$rewritten</article>"
        }
        if (sections.isEmpty()) throw IllegalArgumentException("Nenhum capítulo do EPUB pôde ser aberto.")
        return stylesheets.joinToString("") + sections.joinToString("<hr class=\"chapter-break\">")
    }

    private fun rewriteRelativeAttributes(html: String, baseDir: File, root: File): String {
        val regex = Regex("(?i)\\b(src|href)\\s*=\\s*[\"']([^\"']+)[\"']")
        return regex.replace(html) { match ->
            val attribute = match.groupValues[1]
            val raw = match.groupValues[2].trim()
            if (raw.isBlank() || raw.startsWith("#") || raw.startsWith("data:", true) || raw.startsWith("http://", true) || raw.startsWith("https://", true) || raw.startsWith("mailto:", true)) {
                match.value
            } else if (raw.startsWith("javascript:", true)) {
                "$attribute=\"#\""
            } else {
                val pathPart = raw.substringBefore('#').substringBefore('?')
                val suffix = raw.removePrefix(pathPart)
                val target = runCatching { File(baseDir, Uri.decode(pathPart)).canonicalFile }.getOrNull()
                if (target == null || !target.path.startsWith(root.canonicalPath + File.separator)) {
                    "$attribute=\"#\""
                } else {
                    "$attribute=\"${escapeAttribute(Uri.fromFile(target).toString() + suffix)}\""
                }
            }
        }
    }

    private fun wrapHtml(title: String, body: String, backgroundCss: String, foregroundCss: String, primaryCss: String): String = """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=4, user-scalable=yes">
          <style>
            :root { color-scheme: light dark; }
            * { box-sizing: border-box; }
            html, body { margin: 0; padding: 0; background: $backgroundCss; color: $foregroundCss; }
            body { font-family: sans-serif; font-size: 18px; line-height: 1.62; padding: 20px 18px 48px; overflow-wrap: anywhere; }
            h1, h2, h3 { line-height: 1.25; margin: 1.25em 0 .55em; color: $foregroundCss; }
            h1 { font-size: 1.55em; } h2 { font-size: 1.32em; } h3 { font-size: 1.16em; }
            p { margin: .65em 0; }
            a { color: $primaryCss; }
            img, svg { max-width: 100% !important; height: auto !important; display: block; margin: 14px auto; }
            .bullet { display: inline-block; width: 1.25em; color: $primaryCss; font-weight: 700; }
            .table-wrap { overflow-x: auto; width: 100%; margin: 14px 0; }
            table { width: 100%; border-collapse: collapse; font-size: .95em; }
            th, td { border: 1px solid color-mix(in srgb, $foregroundCss 28%, transparent); padding: 8px; vertical-align: top; }
            td p { margin: .25em 0; }
            .chapter-break { border: 0; border-top: 1px solid color-mix(in srgb, $foregroundCss 18%, transparent); margin: 28px 0; }
            .epub-chapter { max-width: 860px; margin: 0 auto; }
          </style>
          <title>${escapeHtml(title)}</title>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()

    private fun parseXml(file: File): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { isXIncludeAware = false }
            isExpandEntityReferences = false
        }
        return file.inputStream().buffered().use { factory.newDocumentBuilder().parse(it) }
    }

    private fun childNodes(node: Node): List<Node> = buildList {
        val children = node.childNodes
        for (i in 0 until children.length) add(children.item(i))
    }

    private fun tag(node: Node): String = node.localName ?: node.nodeName.substringAfter(':')

    private fun firstDescendant(node: Node, wanted: String): Node? {
        childNodes(node).forEach { child ->
            if (tag(child) == wanted) return child
            firstDescendant(child, wanted)?.let { return it }
        }
        return null
    }

    private fun attributeLocal(node: Node, wanted: String): String {
        val attrs = node.attributes ?: return ""
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val name = attr.localName ?: attr.nodeName.substringAfter(':')
            if (name == wanted) return attr.nodeValue.orEmpty()
        }
        return ""
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun escapeAttribute(value: String): String = escapeHtml(value).replace("'", "&#39;")
}
