from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Trecho esperado não encontrado em {path}: {old[:180]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1) Modelo persistente: PDF opcional por aula, compatível com documentos antigos.
data = "app/src/main/java/com/aistudio/micrhema/Data.kt"
replace_once(
    data,
    '''    var textContent: String = "", // For TEXT type\n    var isYoutube: Boolean = false,\n    var youtubeId: String = "" // if Youtube link\n)''',
    '''    var textContent: String = "", // For TEXT type\n    var studyPdfUrl: String = "", // PDF opcional para conteúdo de estudo\n    var isYoutube: Boolean = false,\n    var youtubeId: String = "" // if Youtube link\n)''',
)

# 2) Aula IBR: mostra material abaixo de "Marcar como concluída" apenas quando existir PDF.
ibr = "app/src/main/java/com/aistudio/micrhema/IbrScreen.kt"
replace_once(
    ibr,
    '''            Button(\n                onClick = {\n                    markIbrChapterCompleted(context, course, chapter)\n                    isCompleted = true\n                },\n                enabled = !isCompleted,\n                modifier = Modifier.fillMaxWidth(),\n                colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary),\n                shape = RoundedCornerShape(12.dp)\n            ) {\n                Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check, contentDescription = null)\n                Spacer(Modifier.width(8.dp))\n                Text(if (isCompleted) "Aula concluída" else "Marcar como concluída")\n            }''',
    '''            Button(\n                onClick = {\n                    markIbrChapterCompleted(context, course, chapter)\n                    isCompleted = true\n                },\n                enabled = !isCompleted,\n                modifier = Modifier.fillMaxWidth(),\n                colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary),\n                shape = RoundedCornerShape(12.dp)\n            ) {\n                Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check, contentDescription = null)\n                Spacer(Modifier.width(8.dp))\n                Text(if (isCompleted) "Aula concluída" else "Marcar como concluída")\n            }\n\n            if (chapter.studyPdfUrl.isNotBlank()) {\n                Spacer(Modifier.height(4.dp))\n                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)\n                Spacer(Modifier.height(4.dp))\n                Text(\n                    "Conteúdos para estudo",\n                    style = MaterialTheme.typography.titleLarge,\n                    fontWeight = FontWeight.Bold,\n                    color = MaterialTheme.colorScheme.onBackground\n                )\n                Card(\n                    modifier = Modifier.fillMaxWidth(),\n                    shape = RoundedCornerShape(16.dp),\n                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)\n                ) {\n                    Row(\n                        modifier = Modifier.fillMaxWidth().padding(16.dp),\n                        verticalAlignment = Alignment.CenterVertically,\n                        horizontalArrangement = Arrangement.spacedBy(12.dp)\n                    ) {\n                        Surface(\n                            shape = RoundedCornerShape(12.dp),\n                            color = MaterialTheme.colorScheme.primaryContainer\n                        ) {\n                            Icon(\n                                Icons.Default.PictureAsPdf,\n                                contentDescription = null,\n                                tint = MaterialTheme.colorScheme.onPrimaryContainer,\n                                modifier = Modifier.padding(12.dp).size(28.dp)\n                            )\n                        }\n                        Column(modifier = Modifier.weight(1f)) {\n                            Text("Material complementar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)\n                            Text("PDF disponível para esta aula", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n                        }\n                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)\n                    }\n                    OutlinedButton(\n                        onClick = {\n                            StudyMaterialDownload.enqueuePdf(\n                                context = context,\n                                sourceUrl = chapter.studyPdfUrl,\n                                title = chapter.title\n                            )\n                        },\n                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),\n                        shape = RoundedCornerShape(12.dp)\n                    ) {\n                        Icon(Icons.Default.Download, contentDescription = null)\n                        Spacer(Modifier.width(8.dp))\n                        Text("Baixar PDF")\n                    }\n                }\n            }''',
)

# 3) ADM IBR: estado e upload opcional de PDF ao criar a aula.
admin = "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt"
replace_once(
    admin,
    '''    var videoUrl by remember { mutableStateOf("") }\n    var audioUrl by remember { mutableStateOf("") }\n    var textContent by remember { mutableStateOf("") }''',
    '''    var videoUrl by remember { mutableStateOf("") }\n    var audioUrl by remember { mutableStateOf("") }\n    var textContent by remember { mutableStateOf("") }\n    var studyPdfUrl by remember { mutableStateOf("") }''',
)

# Insere o campo exatamente antes do botão de adicionar aula, usando o fim do bloco de mídia como âncora.
replace_once(
    admin,
    '''                            LocalUploadField(\n                                value = audioUrl,\n                                onValueChange = { audioUrl = it },\n                                label = "Upload de Áudio (URL ou Arquivo)",\n                                mimeType = "audio/*"\n                            )\n                        }\n\n                        GlassButton(''',
    '''                            LocalUploadField(\n                                value = audioUrl,\n                                onValueChange = { audioUrl = it },\n                                label = "Upload de Áudio (URL ou Arquivo)",\n                                mimeType = "audio/*"\n                            )\n                        }\n\n                        LocalUploadField(\n                            value = studyPdfUrl,\n                            onValueChange = { studyPdfUrl = it },\n                            label = "Conteúdo para estudo — PDF ou link do Drive (opcional)",\n                            mimeType = "application/pdf"\n                        )\n                        Text(\n                            "O PDF aparecerá abaixo da aula para o aluno baixar quando estiver disponível.",\n                            style = MaterialTheme.typography.bodySmall,\n                            color = MaterialTheme.colorScheme.onSurfaceVariant\n                        )\n\n                        GlassButton(''',
)

replace_once(
    admin,
    '''                                        textContent = textContent,\n                                        isYoutube = detectedYoutube,\n                                        youtubeId = detectedYoutubeId''',
    '''                                        textContent = textContent,\n                                        studyPdfUrl = studyPdfUrl.trim(),\n                                        isYoutube = detectedYoutube,\n                                        youtubeId = detectedYoutubeId''',
)

replace_once(
    admin,
    '''                                    videoUrl = ""\n                                    audioUrl = ""\n                                    isYoutube = false''',
    '''                                    videoUrl = ""\n                                    audioUrl = ""\n                                    studyPdfUrl = ""\n                                    isYoutube = false''',
)

# 4) Edição de aula existente: permite anexar/trocar/remover o PDF depois.
replace_once(
    admin,
    '''        var editDuration by remember(editingChapter) { mutableStateOf(editingChapter!!.durationMinutes.toString()) }\n        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }''',
    '''        var editDuration by remember(editingChapter) { mutableStateOf(editingChapter!!.durationMinutes.toString()) }\n        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }\n        var editStudyPdfUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyPdfUrl) }''',
)

replace_once(
    admin,
    '''                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })\n                    GlassTextField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = { Text("URL Vídeo") })''',
    '''                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })\n                    GlassTextField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = { Text("URL Vídeo") })\n                    LocalUploadField(\n                        value = editStudyPdfUrl,\n                        onValueChange = { editStudyPdfUrl = it },\n                        label = "Conteúdo para estudo — PDF ou link do Drive",\n                        mimeType = "application/pdf"\n                    )''',
)

replace_once(
    admin,
    '''                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,\n                                videoUrl = editVideoUrl,\n                                isYoutube = isYt,''',
    '''                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,\n                                videoUrl = editVideoUrl,\n                                studyPdfUrl = editStudyPdfUrl.trim(),\n                                isYoutube = isYt,''',
)

# 5) Indicação visual no ADM quando a aula já possui material complementar.
replace_once(
    admin,
    '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}",''',
    '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}",''',
)

print("Material de estudo PDF do IBR aplicado com sucesso.")
