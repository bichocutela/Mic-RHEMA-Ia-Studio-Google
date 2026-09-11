from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}\n--- needle ---\n{old[:500]}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_between(path: str, start: str, end: str, replacement: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    start_index = text.find(start)
    if start_index < 0:
        raise SystemExit(f"{path}: start marker not found: {start}")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise SystemExit(f"{path}: end marker not found: {end}")
    file.write_text(text[:start_index] + replacement + text[end_index:], encoding="utf-8")


# 1) Persist DOCX together with the existing IBR chapter model.
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Data.kt",
    '''    var textContent: String = "", // For TEXT type
    var studyPdfUrl: String = "", // PDF opcional para conteúdo de estudo
    var isYoutube: Boolean = false,
''',
    '''    var textContent: String = "", // For TEXT type
    var studyPdfUrl: String = "", // PDF opcional para conteúdo de estudo
    var studyDocxUrl: String = "", // Word .docx opcional para conteúdo de estudo
    var isYoutube: Boolean = false,
'''
)

# 2) Allow Android's existing media uploader to carry DOCX without changing the certificate bucket.
replace_once(
    "app/src/main/java/com/aistudio/micrhema/StorageManager.kt",
    '''    private const val MEDIA_BUCKET = "media-assets"
    private const val STORAGE_GATEWAY_FUNCTION = "storage-gateway"
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
''',
    '''    private const val MEDIA_BUCKET = "media-assets"
    private const val STORAGE_GATEWAY_FUNCTION = "storage-gateway"
    private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/StorageManager.kt",
    '''        "image/webp" -> ".webp"
        "application/pdf" -> ".pdf"
        else -> ""
''',
    '''        "image/webp" -> ".webp"
        "application/pdf" -> ".pdf"
        DOCX_MIME -> ".docx"
        else -> ""
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/StorageManager.kt",
    '''                "image/jpeg", "image/png", "image/webp",
                "application/pdf",
                "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/aac",
''',
    '''                "image/jpeg", "image/png", "image/webp",
                "application/pdf", DOCX_MIME,
                "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/aac",
'''
)

# 3) The shared storage gateway now accepts DOCX for media-assets too (additive only).
replace_once(
    "supabase/functions/storage-gateway/index.ts",
    '''const SIGNED_URL_TTL_SECONDS = 15 * 60;
const ADMIN_PASSWORD = Deno.env.get("RHEMA_ADMIN_PASSWORD") || "igreja10";

const BUCKET_RULES = {
''',
    '''const SIGNED_URL_TTL_SECONDS = 15 * 60;
const ADMIN_PASSWORD = Deno.env.get("RHEMA_ADMIN_PASSWORD") || "igreja10";
const DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

const BUCKET_RULES = {
'''
)
replace_once(
    "supabase/functions/storage-gateway/index.ts",
    '''          "image/jpeg", "image/png", "image/webp",
          "application/pdf",
          "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/aac",
''',
    '''          "image/jpeg", "image/png", "image/webp",
          "application/pdf", DOCX_MIME,
          "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/aac",
'''
)
replace_once(
    "supabase/functions/storage-gateway/index.ts",
    '''  if (mimeType === "image/webp") return "webp";
  if (mimeType === "application/pdf") return "pdf";
  if (mimeType === "audio/mpeg" || mimeType === "audio/mp3") return "mp3";
''',
    '''  if (mimeType === "image/webp") return "webp";
  if (mimeType === "application/pdf") return "pdf";
  if (mimeType === DOCX_MIME) return "docx";
  if (mimeType === "audio/mpeg" || mimeType === "audio/mp3") return "mp3";
'''
)

# 4) Generalize the study-material downloader while preserving the old PDF API.
study_path = Path("app/src/main/java/com/aistudio/micrhema/StudyMaterialDownload.kt")
study_text = study_path.read_text(encoding="utf-8")
if "import android.content.Intent\n" not in study_text:
    study_text = study_text.replace("import android.content.Context\n", "import android.content.Context\nimport android.content.Intent\n", 1)
start = study_text.find("object StudyMaterialDownload {")
if start < 0:
    raise SystemExit("StudyMaterialDownload.kt: object marker not found")
study_object = r'''object StudyMaterialDownload {
    private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    fun enqueuePdf(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "pdf", "application/pdf", "PDF")
    }

    fun enqueueDocx(context: Context, sourceUrl: String, title: String) {
        enqueue(context, sourceUrl, title, "docx", DOCX_MIME, "Word")
    }

    fun openDocument(context: Context, sourceUrl: String, label: String) {
        val resolvedUrl = convertGoogleDriveUrl(sourceUrl.trim())
        if (!resolvedUrl.startsWith("http://", ignoreCase = true) &&
            !resolvedUrl.startsWith("https://", ignoreCase = true)
        ) {
            Toast.makeText(context, "O $label não possui um link válido para abrir.", Toast.LENGTH_LONG).show()
            return
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(context, "Não foi possível abrir o $label neste aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    private fun enqueue(
        context: Context,
        sourceUrl: String,
        title: String,
        extension: String,
        mimeType: String,
        label: String
    ) {
        val resolvedUrl = convertGoogleDriveUrl(sourceUrl.trim())
        if (!resolvedUrl.startsWith("http://", ignoreCase = true) &&
            !resolvedUrl.startsWith("https://", ignoreCase = true)
        ) {
            Toast.makeText(context, "O arquivo $label não possui um link válido para download.", Toast.LENGTH_LONG).show()
            return
        }

        val safeBaseName = title
            .trim()
            .ifBlank { "conteudo-para-estudo" }
            .replace(Regex("[^\\p{L}\\p{N}._ -]+"), "")
            .replace(Regex("\\s+"), " ")
            .take(80)
            .trim()
            .ifBlank { "conteudo-para-estudo" }
        val fileName = if (safeBaseName.endsWith(".$extension", ignoreCase = true)) safeBaseName else "$safeBaseName.$extension"

        runCatching {
            val request = DownloadManager.Request(Uri.parse(resolvedUrl))
                .setTitle(fileName)
                .setDescription("Conteúdo para estudo — MIC Rhema")
                .setMimeType(mimeType)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onSuccess {
            Toast.makeText(context, "Download do arquivo $label iniciado.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Não foi possível iniciar o download do arquivo $label.", Toast.LENGTH_LONG).show()
        }
    }
}
'''
study_path.write_text(study_text[:start] + study_object, encoding="utf-8")

# 5) Native student IBR: show Word material alongside PDF for every lesson type.
replace_once(
    "app/src/main/java/com/aistudio/micrhema/IbrScreen.kt",
    '''            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val newProg = IbrProgress(course.id, chapter.id, 0, chapter.durationMinutes * 60, true)
''',
    '''            Spacer(Modifier.height(20.dp))
            IbrStudyMaterials(chapter)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val newProg = IbrProgress(course.id, chapter.id, 0, chapter.durationMinutes * 60, true)
'''
)
old_pdf_block = '''            if (chapter.studyPdfUrl.isNotBlank()) {
                HorizontalDivider()
                Text("Conteúdos para estudo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.padding(9.dp).size(24.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Material complementar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("PDF disponível para esta aula", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Download, null)
                    }
                    OutlinedButton(
                        onClick = { StudyMaterialDownload.enqueuePdf(context, chapter.studyPdfUrl, chapter.title) },
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Baixar PDF")
                    }
                }
            }
'''
replace_once(
    "app/src/main/java/com/aistudio/micrhema/IbrScreen.kt",
    old_pdf_block,
    '''            IbrStudyMaterials(chapter)
'''
)
ibr_screen = Path("app/src/main/java/com/aistudio/micrhema/IbrScreen.kt")
ibr_text = ibr_screen.read_text(encoding="utf-8")
helper_marker = "\n@Composable\nprivate fun IbrStudyMaterials("
if helper_marker not in ibr_text:
    ibr_text += r'''

@Composable
private fun IbrStudyMaterials(chapter: IbrChapter) {
    val context = LocalContext.current
    val hasPdf = chapter.studyPdfUrl.isNotBlank()
    val hasDocx = chapter.studyDocxUrl.isNotBlank()
    if (!hasPdf && !hasDocx) return

    HorizontalDivider()
    Text("Conteúdos para estudo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    if (hasPdf) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.padding(9.dp).size(24.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Material complementar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("PDF disponível para esta aula", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Download, null)
            }
            OutlinedButton(
                onClick = { StudyMaterialDownload.enqueuePdf(context, chapter.studyPdfUrl, chapter.title) },
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Baixar PDF")
            }
        }
    }

    if (hasDocx) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.padding(9.dp).size(24.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Material em Word", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Arquivo .docx disponível para esta aula", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(
                onClick = { StudyMaterialDownload.openDocument(context, chapter.studyDocxUrl, "arquivo Word") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Abrir Word")
            }
            OutlinedButton(
                onClick = { StudyMaterialDownload.enqueueDocx(context, chapter.studyDocxUrl, chapter.title) },
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Baixar DOCX")
            }
        }
    }
}
'''
    ibr_screen.write_text(ibr_text, encoding="utf-8")

# 6) Native IBR admin: responsive tabs + DOCX controls + compact rows on small phones.
new_edit_vip_section = r'''@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVipSection() {
    var vipTab by remember { mutableStateOf("overview") } // overview, midia, cursos or certificados
    val tabs = listOf(
        "overview" to "Visão geral",
        "midia" to "Conteúdo IBR",
        "cursos" to "Módulos IBR",
        "certificados" to "Certificados IBR"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val compact = maxWidth < 600.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.chunked(2).forEach { rowTabs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTabs.forEach { (id, title) ->
                                FilterChip(
                                    selected = vipTab == id,
                                    onClick = { vipTab = id },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            title,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                )
                            }
                            if (rowTabs.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { (id, title) ->
                        FilterChip(
                            selected = vipTab == id,
                            onClick = { vipTab = id },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    title,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (vipTab == "overview") {
                EditIbrOverviewSection()
            } else if (vipTab == "midia") {
                EditVipContentSection()
            } else if (vipTab == "cursos") {
                EditVipIbrSection()
            } else {
                EditIbrCertificatesSection()
            }
        }
    }
}

'''
replace_between(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun EditVipSection()",
    "@Composable\nfun EditIbrOverviewSection()",
    new_edit_vip_section
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''    var textContent by remember { mutableStateOf("") }
    var studyPdfUrl by remember { mutableStateOf("") }
    
    var courseSearch by remember { mutableStateOf("") }
''',
    '''    var textContent by remember { mutableStateOf("") }
    var studyPdfUrl by remember { mutableStateOf("") }
    var studyDocxUrl by remember { mutableStateOf("") }
    
    var courseSearch by remember { mutableStateOf("") }
'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val themes = listOf("Teologia", "História Bíblica", "Vida Cristã")
''',
    '''                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("Teologia", "História Bíblica", "Vida Cristã")
'''
)

old_duration = '''                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextField(
                                value = chapterDuration,
                                onValueChange = { chapterDuration = it },
                                label = { Text("Duração (Minutos)") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("É YouTube?", style = MaterialTheme.typography.labelMedium)
                                Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                            }
                        }
'''
new_duration = '''                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth < 430.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassTextField(
                                        value = chapterDuration,
                                        onValueChange = { chapterDuration = it },
                                        label = { Text("Duração (Minutos)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("É YouTube?", style = MaterialTheme.typography.labelMedium)
                                        Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassTextField(
                                        value = chapterDuration,
                                        onValueChange = { chapterDuration = it },
                                        label = { Text("Duração (Minutos)") },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(0.8f)
                                    ) {
                                        Text("É YouTube?", style = MaterialTheme.typography.labelMedium)
                                        Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                                    }
                                }
                            }
                        }
'''
replace_once("app/src/main/java/com/aistudio/micrhema/VipAdmin.kt", old_duration, new_duration)

old_pdf_admin = '''                        LocalUploadField(
                            value = studyPdfUrl,
                            onValueChange = { studyPdfUrl = it },
                            label = "Conteúdo para estudo — PDF ou link do Drive (opcional)",
                            mimeType = "application/pdf"
                        )
                        Text(
                            "O PDF aparecerá abaixo da aula para o aluno baixar quando estiver disponível.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
'''
new_pdf_admin = '''                        LocalUploadField(
                            value = studyPdfUrl,
                            onValueChange = { studyPdfUrl = it },
                            label = "Conteúdo para estudo — PDF ou link do Drive (opcional)",
                            mimeType = "application/pdf"
                        )
                        LocalUploadField(
                            value = studyDocxUrl,
                            onValueChange = { studyDocxUrl = it },
                            label = "Conteúdo para estudo — Word .docx (opcional)",
                            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                        Text(
                            "PDF e Word aparecerão abaixo da aula para o aluno abrir ou baixar quando estiverem disponíveis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
'''
replace_once("app/src/main/java/com/aistudio/micrhema/VipAdmin.kt", old_pdf_admin, new_pdf_admin)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                                        textContent = textContent,
                                        studyPdfUrl = studyPdfUrl.trim(),
                                        isYoutube = detectedYoutube,
''',
    '''                                        textContent = textContent,
                                        studyPdfUrl = studyPdfUrl.trim(),
                                        studyDocxUrl = studyDocxUrl.trim(),
                                        isYoutube = detectedYoutube,
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                                    audioUrl = ""
                                    studyPdfUrl = ""
                                    isYoutube = false
''',
    '''                                    audioUrl = ""
                                    studyPdfUrl = ""
                                    studyDocxUrl = ""
                                    isYoutube = false
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}",
''',
    '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}${if (ch.studyDocxUrl.isNotBlank()) " • Word 📝" else ""}",
'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                            Column {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
''',
    '''                            Column(modifier = Modifier.weight(1f)) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Column {
                                            Text(ch.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
''',
    '''                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                ch.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }
        var editStudyPdfUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyPdfUrl) }
        
        AlertDialog(
''',
    '''        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }
        var editStudyPdfUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyPdfUrl) }
        var editStudyDocxUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyDocxUrl) }
        
        AlertDialog(
'''
)

# Limit only the chapter edit dialog body so it scrolls instead of overflowing on short/small displays.
vip_path = Path("app/src/main/java/com/aistudio/micrhema/VipAdmin.kt")
vip_text = vip_path.read_text(encoding="utf-8")
chapter_edit_marker = "    if (editingCourse != null && editingChapter != null) {"
chapter_index = vip_text.find(chapter_edit_marker)
if chapter_index < 0:
    raise SystemExit("VipAdmin.kt: chapter edit block not found")
column_needle = '''            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
'''
column_index = vip_text.find(column_needle, chapter_index)
if column_index < 0:
    raise SystemExit("VipAdmin.kt: chapter edit dialog column not found")
column_replacement = '''            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).imePadding().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
'''
vip_text = vip_text[:column_index] + column_replacement + vip_text[column_index + len(column_needle):]
vip_path.write_text(vip_text, encoding="utf-8")

replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                    LocalUploadField(
                        value = editStudyPdfUrl,
                        onValueChange = { editStudyPdfUrl = it },
                        label = "Conteúdo para estudo — PDF ou link do Drive",
                        mimeType = "application/pdf"
                    )
''',
    '''                    LocalUploadField(
                        value = editStudyPdfUrl,
                        onValueChange = { editStudyPdfUrl = it },
                        label = "Conteúdo para estudo — PDF ou link do Drive",
                        mimeType = "application/pdf"
                    )
                    LocalUploadField(
                        value = editStudyDocxUrl,
                        onValueChange = { editStudyDocxUrl = it },
                        label = "Conteúdo para estudo — Word .docx",
                        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt",
    '''                                videoUrl = editVideoUrl,
                                studyPdfUrl = editStudyPdfUrl.trim(),
                                isYoutube = isYt,
''',
    '''                                videoUrl = editVideoUrl,
                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                isYoutube = isYt,
'''
)

# 7) Keep the main admin toolbar stable when text size is large or the phone is narrow.
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''                        Text("Painel Administrativo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
''',
    '''                        Text(
                            "Painel Administrativo",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
'''
)

print("Android IBR DOCX + responsive admin patch applied successfully.")
