from pathlib import Path
import re
import sys

VIP_PATH = Path("app/src/main/java/com/aistudio/micrhema/VipAdmin.kt")
HELPER_PATH = Path("app/src/main/java/com/aistudio/micrhema/IbrAdminSimpleHelpers.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Não encontrei: {label}")
    return text.replace(old, new, 1)


def block1() -> None:
    text = VIP_PATH.read_text(encoding="utf-8")
    helper = '''package com.aistudio.micrhema

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/** Salva alterações administrativas sem disparar uma notificação de novo conteúdo. */
fun saveIbrCourseSilently(item: IbrCourse) {
    if (BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("ibr_courses").document(item.id).set(item)
    }
}

fun completedIbrCourseCountFor(progress: List<IbrProgress>, courses: List<IbrCourse> = ibrCoursesState): Int {
    return courses.count { course ->
        course.chapters.isNotEmpty() && course.chapters.all { chapter ->
            progress.any { item -> item.courseId == course.id && item.chapterId == chapter.id && item.isCompleted }
        }
    }
}

fun isIbrCompleteFor(progress: List<IbrProgress>, courses: List<IbrCourse> = ibrCoursesState): Boolean {
    val validCourses = courses.filter { it.chapters.isNotEmpty() }
    return validCourses.isNotEmpty() && completedIbrCourseCountFor(progress, validCourses) == validCourses.size
}

@Composable
fun rememberIbrProgressByMember(): Map<String, List<IbrProgress>> {
    val memberIds = memberRequestsState.filter { it.isIbr }.map { it.id }.filter { it.isNotBlank() }.distinct().sorted()
    val progressByMember = remember { mutableStateMapOf<String, List<IbrProgress>>() }
    LaunchedEffect(memberIds.joinToString("|")) {
        val activeIds = memberIds.toSet()
        progressByMember.keys.toList().filterNot { it in activeIds }.forEach { progressByMember.remove(it) }
        memberIds.forEach { memberId ->
            val progress = runCatching {
                Firebase.firestore.collection("users").document(memberId).collection("ibrProgress")
                    .get().await().documents.mapNotNull { document ->
                        runCatching { document.toObject(IbrProgress::class.java) }.getOrNull()
                    }
            }.getOrElse { emptyList() }
            progressByMember[memberId] = progress
        }
    }
    return progressByMember
}
'''
    HELPER_PATH.write_text(helper, encoding="utf-8")

    marker = "fun EditVipIbrSection() {"
    if marker not in text:
        raise SystemExit("EditVipIbrSection não encontrado")
    head, body = text.split(marker, 1)
    section = marker + body

    section = replace_once(
        section,
        '    var courseDescription by remember { mutableStateOf("") }\n    \n\n    // Add Chapter Form States',
        '    var courseDescription by remember { mutableStateOf("") }\n    var courseImageUrl by remember { mutableStateOf("") }\n    \n\n    // Add Chapter Form States',
        "estado da capa",
    )
    section = replace_once(
        section,
        '    var editingCourse by remember { mutableStateOf<IbrCourse?>(null) }\n    var editingChapter by remember { mutableStateOf<IbrChapter?>(null) }',
        '    var editingCourse by remember { mutableStateOf<IbrCourse?>(null) }\n    var editingChapter by remember { mutableStateOf<IbrChapter?>(null) }\n    var courseToDelete by remember { mutableStateOf<IbrCourse?>(null) }\n    var chapterToDelete by remember { mutableStateOf<Pair<IbrCourse, IbrChapter>?>(null) }',
        "confirmação de exclusão",
    )
    section = replace_once(
        section,
        '        .sortedBy { it.title.lowercase() }\n    LazyColumn(',
        '        .sortedBy { it.title.lowercase() }\n\n    LaunchedEffect(ibrCoursesState.size) {\n        if (selectedCourseForChapter == null || ibrCoursesState.none { it.id == selectedCourseForChapter?.id }) {\n            selectedCourseForChapter = ibrCoursesState.firstOrNull()\n        }\n    }\n\n    LazyColumn(',
        "seleção segura de curso",
    )
    section = replace_once(
        section,
        '''                    GlassTextField(
                        value = courseDescription,
                        onValueChange = { courseDescription = it },
                        label = { Text("Descrição Curta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )

                    // Theme selector chips''',
        '''                    GlassTextField(
                        value = courseDescription,
                        onValueChange = { courseDescription = it },
                        label = { Text("Descrição Curta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )
                    LocalUploadField(
                        value = courseImageUrl,
                        onValueChange = { courseImageUrl = it },
                        label = "Capa do curso (opcional)",
                        mimeType = "image/*"
                    )

                    // Theme selector chips''',
        "capa do curso",
    )
    section = replace_once(section, '                                    imageUrl = "",', '                                    imageUrl = courseImageUrl.trim(),', "salvar capa")
    section = replace_once(
        section,
        '                                courseTitle = ""\n                                courseDescription = ""',
        '                                courseTitle = ""\n                                courseDescription = ""\n                                courseImageUrl = ""',
        "limpar capa",
    )
    section = replace_once(
        section,
        '''        // Initialize selected course if empty
        if (selectedCourseForChapter == null && ibrCoursesState.isNotEmpty()) {
            selectedCourseForChapter = ibrCoursesState.first()
        }

''',
        "",
        "inicialização durante composição",
    )

    pattern = re.compile(
        r'''                        BoxWithConstraints\(modifier = Modifier\.fillMaxWidth\(\)\) \{.*?                        LocalUploadField\(
                            value = studyPdfUrl,''',
        re.S,
    )
    replacement = '''                        Text("Tipo da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("VIDEO" to "Vídeo", "AUDIO" to "Áudio", "TEXT" to "Texto").forEach { (id, title) ->
                                FilterChip(
                                    selected = chapterType == id,
                                    onClick = {
                                        chapterType = id
                                        if (id != "VIDEO") isYoutube = false
                                    },
                                    label = { Text(title) }
                                )
                            }
                        }

                        GlassTextField(
                            value = chapterDuration,
                            onValueChange = { chapterDuration = it },
                            label = { Text("Duração (minutos)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        when (chapterType) {
                            "VIDEO" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vídeo do YouTube", style = MaterialTheme.typography.labelMedium)
                                    Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                                }
                                if (isYoutube) {
                                    GlassTextField(
                                        value = videoUrl,
                                        onValueChange = { videoUrl = it },
                                        label = { Text("Link do YouTube") },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("https://youtube.com/watch?v=...") }
                                    )
                                } else {
                                    LocalUploadField(
                                        value = videoUrl,
                                        onValueChange = { videoUrl = it },
                                        label = "Vídeo da aula",
                                        mimeType = "video/*"
                                    )
                                }
                            }
                            "AUDIO" -> LocalUploadField(
                                value = audioUrl,
                                onValueChange = { audioUrl = it },
                                label = "Áudio da aula",
                                mimeType = "audio/*"
                            )
                            else -> GlassTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                label = { Text("Texto da aula") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                maxLines = 10
                            )
                        }

                        LocalUploadField(
                            value = studyPdfUrl,'''
    section, count = pattern.subn(replacement, section, count=1)
    if count != 1:
        raise SystemExit("formulário antigo de mídia da aula não encontrado")

    section = replace_once(
        section,
        '                                    val detectedYoutubeId = extractYouTubeVideoId(videoUrl).orEmpty()\n                                    val detectedYoutube = isYoutube || detectedYoutubeId.isNotBlank() || isYoutubeUrl(videoUrl)',
        '                                    val detectedYoutubeId = if (chapterType == "VIDEO") extractYouTubeVideoId(videoUrl).orEmpty() else ""\n                                    val detectedYoutube = chapterType == "VIDEO" && (isYoutube || detectedYoutubeId.isNotBlank() || isYoutubeUrl(videoUrl))',
        "detecção youtube",
    )
    section = replace_once(
        section,
        '                                        videoUrl = videoUrl,\n                                        audioUrl = audioUrl,\n                                        textContent = textContent,',
        '                                        videoUrl = if (chapterType == "VIDEO") videoUrl else "",\n                                        audioUrl = if (chapterType == "AUDIO") audioUrl else "",\n                                        textContent = if (chapterType == "TEXT") textContent else "",',
        "campos por tipo",
    )
    section = replace_once(
        section,
        '                                    audioUrl = ""\n                                    studyPdfUrl = ""',
        '                                    audioUrl = ""\n                                    textContent = ""\n                                    chapterType = "VIDEO"\n                                    studyPdfUrl = ""',
        "limpar aula",
    )
    section = replace_once(section, '                                IconButton(onClick = { removeIbrCourse(course) }) {', '                                IconButton(onClick = { courseToDelete = course }) {', "confirmar exclusão curso")
    section = replace_once(
        section,
        '''                                        IconButton(
                                            onClick = {
                                                val updatedChapters = course.chapters.toMutableList().apply { remove(ch) }
                                                val updatedCourse = course.copy(chapters = updatedChapters)
                                                val index = ibrCoursesState.indexOf(course)
                                                if (index != -1) {
                                                    addIbrCourse(updatedCourse)
                                                }
                                            },''',
        '''                                        IconButton(
                                            onClick = { chapterToDelete = course to ch },''',
        "confirmar exclusão aula",
    )
    section = replace_once(
        section,
        '        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }',
        '        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }\n        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }',
        "editar capa",
    )
    section = replace_once(
        section,
        '                    GlassTextField(value = editTheme, onValueChange = { editTheme = it }, label = { Text("Tema") })',
        '                    GlassTextField(value = editTheme, onValueChange = { editTheme = it }, label = { Text("Tema") })\n                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")',
        "campo editar capa",
    )
    section = replace_once(
        section,
        '                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme)\n                        addIbrCourse(updated)',
        '                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme, imageUrl = editImageUrl.trim())\n                        ibrCoursesState[idx] = updated\n                        saveIbrCourseSilently(updated)',
        "salvar edição curso",
    )
    section = replace_once(
        section,
        '        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }',
        '        var editType by remember(editingChapter) { mutableStateOf(editingChapter!!.type.ifBlank { "VIDEO" }.uppercase()) }\n        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }\n        var editAudioUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.audioUrl) }\n        var editTextContent by remember(editingChapter) { mutableStateOf(editingChapter!!.textContent) }',
        "estado editar tipo aula",
    )
    section = replace_once(
        section,
        '                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })\n                    GlassTextField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = { Text("URL Vídeo") })',
        '''                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })
                    Text("Tipo da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("VIDEO" to "Vídeo", "AUDIO" to "Áudio", "TEXT" to "Texto").forEach { (id, title) ->
                            FilterChip(selected = editType == id, onClick = { editType = id }, label = { Text(title) })
                        }
                    }
                    when (editType) {
                        "VIDEO" -> LocalUploadField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = "Vídeo ou link do YouTube", mimeType = "video/*")
                        "AUDIO" -> LocalUploadField(value = editAudioUrl, onValueChange = { editAudioUrl = it }, label = "Áudio da aula", mimeType = "audio/*")
                        else -> GlassTextField(value = editTextContent, onValueChange = { editTextContent = it }, label = { Text("Texto da aula") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 10)
                    }''',
        "editar conteúdo por tipo",
    )
    section = replace_once(
        section,
        '''                            val isYt = isYoutubeUrl(editVideoUrl)
                            val ytId = extractYouTubeVideoId(editVideoUrl) ?: ""
                            updatedChapters[chapterIdx] = editingChapter!!.copy(
                                title = editTitle,
                                description = editDescription,
                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,
                                videoUrl = editVideoUrl,
                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                isYoutube = isYt,
                                youtubeId = ytId
                            )
                            ibrCoursesState[courseIdx] = course.copy(chapters = updatedChapters)''',
        '''                            val isYt = editType == "VIDEO" && isYoutubeUrl(editVideoUrl)
                            val ytId = if (editType == "VIDEO") extractYouTubeVideoId(editVideoUrl) ?: "" else ""
                            updatedChapters[chapterIdx] = editingChapter!!.copy(
                                title = editTitle,
                                description = editDescription,
                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,
                                type = editType,
                                videoUrl = if (editType == "VIDEO") editVideoUrl else "",
                                audioUrl = if (editType == "AUDIO") editAudioUrl else "",
                                textContent = if (editType == "TEXT") editTextContent else "",
                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                isYoutube = isYt,
                                youtubeId = ytId
                            )
                            val updatedCourse = course.copy(chapters = updatedChapters)
                            ibrCoursesState[courseIdx] = updatedCourse
                            saveIbrCourseSilently(updatedCourse)''',
        "persistir edição aula",
    )
    dialogs = '''    if (courseToDelete != null) {
        val target = courseToDelete!!
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Excluir curso?") },
            text = { Text("O curso '${target.title}' e suas aulas serão removidos do IBR.") },
            confirmButton = {
                TextButton(onClick = {
                    removeIbrCourse(target)
                    courseToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (chapterToDelete != null) {
        val (targetCourse, targetChapter) = chapterToDelete!!
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Excluir aula?") },
            text = { Text("A aula '${targetChapter.title}' será removida deste curso.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = targetCourse.copy(chapters = targetCourse.chapters.filterNot { it.id == targetChapter.id })
                    val index = ibrCoursesState.indexOfFirst { it.id == targetCourse.id }
                    if (index >= 0) ibrCoursesState[index] = updated
                    saveIbrCourseSilently(updated)
                    chapterToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { chapterToDelete = null }) { Text("Cancelar") } }
        )
    }

'''
    section = replace_once(section, '    if (editingCourse != null && editingChapter == null) {', dialogs + '    if (editingCourse != null && editingChapter == null) {', "diálogos")
    VIP_PATH.write_text(head + section, encoding="utf-8")


def block2() -> None:
    text = VIP_PATH.read_text(encoding="utf-8")
    text = replace_once(text, '                EditIbrOverviewSection()', '                EditIbrOverviewSection(onOpenTab = { vipTab = it })', "callback visão geral")
    text = replace_once(
        text,
        '@Composable\nfun EditIbrOverviewSection() {\n    val totalCourses = ibrCoursesState.size\n    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }\n    val totalIbrMembers = memberRequestsState.count { it.isIbr }\n    val certificatesPending = memberRequestsState.count { it.isIbr && it.ibrCertificateUrl.isBlank() && it.ibrCertificateStoragePath.isBlank() }',
        '@Composable\nfun EditIbrOverviewSection(onOpenTab: (String) -> Unit = {}) {\n    val totalCourses = ibrCoursesState.size\n    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }\n    val totalIbrMembers = memberRequestsState.count { it.isIbr }\n    val progressByMember = rememberIbrProgressByMember()\n    val certificatesPending = memberRequestsState.count { member ->\n        member.isIbr && member.ibrCertificateUrl.isBlank() && member.ibrCertificateStoragePath.isBlank() &&\n            isIbrCompleteFor(progressByMember[member.id].orEmpty())\n    }',
        "contagem correta certificados",
    )
    text = replace_once(
        text,
        '                IbrAdminMetricCard("Cursos", totalCourses.toString(), Icons.Default.MenuBook, Modifier.weight(1f))\n                IbrAdminMetricCard("Aulas", totalLessons.toString(), Icons.Default.Class, Modifier.weight(1f))',
        '                IbrAdminMetricCard("Cursos", totalCourses.toString(), Icons.Default.MenuBook, Modifier.weight(1f)) { onOpenTab("cursos") }\n                IbrAdminMetricCard("Aulas", totalLessons.toString(), Icons.Default.Class, Modifier.weight(1f)) { onOpenTab("cursos") }',
        "atalhos cursos",
    )
    text = replace_once(
        text,
        '                IbrAdminMetricCard("Alunos IBR", totalIbrMembers.toString(), Icons.Default.People, Modifier.weight(1f))\n                IbrAdminMetricCard("Certificados", certificatesPending.toString(), Icons.Default.EmojiEvents, Modifier.weight(1f))',
        '                IbrAdminMetricCard("Alunos IBR", totalIbrMembers.toString(), Icons.Default.People, Modifier.weight(1f))\n                IbrAdminMetricCard("Cert. pendentes", certificatesPending.toString(), Icons.Default.EmojiEvents, Modifier.weight(1f)) { onOpenTab("certificados") }',
        "atalho certificados",
    )
    text = replace_once(
        text,
        '''@Composable
private fun IbrAdminMetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {''',
        '''@Composable
private fun IbrAdminMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {''',
        "métrica clicável",
    )
    pattern = re.compile(
        r'''    val totalIbrCourses = ibrCoursesState\.size\n    \n    val eligibleUsers = memberRequestsState\.filter \{ member ->.*?    \}\n\n    Column\(modifier = Modifier\.fillMaxSize\(\)\.padding\(16\.dp\)\) \{''',
        re.S,
    )
    replacement = '''    val totalIbrCourses = ibrCoursesState.count { it.chapters.isNotEmpty() }
    val totalIbrLessons = ibrCoursesState.sumOf { it.chapters.size }
    val progressByMember = rememberIbrProgressByMember()
    val eligibleUsers = memberRequestsState.filter { member ->
        member.isIbr && isIbrCompleteFor(progressByMember[member.id].orEmpty())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {'''
    text, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit("filtro de certificados não encontrado")
    text = replace_once(
        text,
        '''                            val memberBadgeProgress = calculateBadgeProgress(member)
                            val memberBadge = biblicalBadgeForId(member.equippedBadgeId)''',
        '''                            val memberProgress = progressByMember[member.id].orEmpty()
                            val completedLessons = memberProgress.count { it.isCompleted }
                            val completedCourses = completedIbrCourseCountFor(memberProgress)
                            val memberBadge = biblicalBadgeForId(member.equippedBadgeId)''',
        "progresso individual",
    )
    text = replace_once(text, '                                            "${memberBadgeProgress.unlockedIds.size} emblemas desbloqueados • ${memberBadgeProgress.completedIbrCourses} cursos concluídos",', '                                            "$completedCourses/$totalIbrCourses cursos concluídos",', "resumo cursos")
    text = replace_once(text, '                                    Text("${memberBadgeProgress.completedIbrLessons} aulas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)', '                                    Text("$completedLessons/$totalIbrLessons aulas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)', "resumo aulas")
    VIP_PATH.write_text(text, encoding="utf-8")


def block3() -> None:
    text = VIP_PATH.read_text(encoding="utf-8")
    text = replace_once(text, '        "midia" to "Conteúdo IBR",\n        "cursos" to "Módulos IBR",\n        "certificados" to "Certificados IBR"', '        "midia" to "Conteúdo",\n        "cursos" to "Módulos",\n        "certificados" to "Certificados"', "nomes curtos")

    start_marker = '        BoxWithConstraints(\n            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)\n        ) {'
    start = text.index(start_marker)
    end_marker = '\n\n        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {'
    end = text.index(end_marker, start)
    new_tabs = '''        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { (id, title) ->
                FilterChip(
                    selected = vipTab == id,
                    onClick = { vipTab = id },
                    label = { Text(title, maxLines = 1) }
                )
            }
        }'''
    text = text[:start] + new_tabs + text[end:]

    text = replace_once(
        text,
        '    var isDeleting by remember { mutableStateOf(false) }\n\n    Column(modifier = Modifier.padding(16.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {',
        '    var isDeleting by remember { mutableStateOf(false) }\n    var contentFilter by remember { mutableStateOf("Todos") }\n    var addContentType by remember { mutableStateOf<String?>(null) }\n\n    Column(modifier = Modifier.padding(16.dp).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {',
        "estados conteúdo",
    )
    text = replace_once(
        text,
        '        Text("Adicione e edite livros, áudios e vídeos para os alunos do Instituto Bíblico Rhema.", style = MaterialTheme.typography.bodyMedium)\n        // SMART IMPORTER',
        '''        Text("Adicione e edite os materiais dos alunos sem sair desta tela.", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Livros", "Áudios", "Vídeos", "Fotos").forEach { option ->
                FilterChip(selected = contentFilter == option, onClick = { contentFilter = option }, label = { Text(option) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Livros" to "+ Livro", "Áudios" to "+ Áudio", "Vídeos" to "+ Vídeo", "Fotos" to "+ Álbum").forEach { (type, label) ->
                AssistChip(onClick = { addContentType = if (addContentType == type) null else type }, label = { Text(label) })
            }
        }
        // SMART IMPORTER''',
        "filtros conteúdo",
    )
    text = replace_once(text, 'PDF, Word (DOCX), Áudio (MP3), Vídeo (MP4) ou Imagem.', 'PDF, EPUB, Word (DOCX), Áudio (MP3), Vídeo (MP4) ou Imagem.', "texto EPUB")
    text = replace_once(
        text,
        '                                GoogleDriveService.FileType.WORD -> {',
        '''                                GoogleDriveService.FileType.EPUB -> {
                                    smartMessage = "Livro EPUB detectado e adicionado!"
                                    addVipBook(ContentBook(id = System.currentTimeMillis().toString(), title = "Novo Livro EPUB Importado", author = "Documento EPUB", coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", contentText = "", bookUrl = GoogleDriveService.getDirectDownloadLink(smartUrl), type = "epub"))
                                }
                                GoogleDriveService.FileType.WORD -> {''',
        "smart import EPUB",
    )

    text = replace_once(text, '        // ADD BOOK\n        Card(modifier = Modifier.fillMaxWidth()) {', '        // ADD BOOK\n        if (addContentType == "Livros") {\n        Card(modifier = Modifier.fillMaxWidth()) {', "abrir livro")
    text = replace_once(text, '        }\n        \n        if (vipBooksState.isNotEmpty()) {', '        }\n        }\n        \n        if ((contentFilter == "Todos" || contentFilter == "Livros") && vipBooksState.isNotEmpty()) {', "fechar livro")
    text = replace_once(text, '        // ADD AUDIO\n        Card(modifier = Modifier.fillMaxWidth()) {', '        // ADD AUDIO\n        if (addContentType == "Áudios") {\n        Card(modifier = Modifier.fillMaxWidth()) {', "abrir áudio")
    text = replace_once(text, '        }\n        \n        if (vipAudiosState.isNotEmpty()) {', '        }\n        }\n        \n        if ((contentFilter == "Todos" || contentFilter == "Áudios") && vipAudiosState.isNotEmpty()) {', "fechar áudio")
    text = replace_once(text, '        // ADD VIDEO\n        Card(modifier = Modifier.fillMaxWidth()) {', '        // ADD VIDEO\n        if (addContentType == "Vídeos") {\n        Card(modifier = Modifier.fillMaxWidth()) {', "abrir vídeo")
    text = replace_once(text, '        }\n        \n        if (vipVideosState.isNotEmpty()) {', '        }\n        }\n        \n        if ((contentFilter == "Todos" || contentFilter == "Vídeos") && vipVideosState.isNotEmpty()) {', "fechar vídeo")
    text = replace_once(text, '        // ADD ALBUM\n        Card(modifier = Modifier.fillMaxWidth()) {', '        // ADD ALBUM\n        if (addContentType == "Fotos") {\n        Card(modifier = Modifier.fillMaxWidth()) {', "abrir álbum")
    text = replace_once(text, '        }\n        \n        if (vipAlbumsState.isNotEmpty()) {', '        }\n        }\n        \n        if ((contentFilter == "Todos" || contentFilter == "Fotos") && vipAlbumsState.isNotEmpty()) {', "fechar álbum")

    marker = "fun EditVipIbrSection() {"
    head, section = text.split(marker, 1)
    section = marker + section
    section = replace_once(section, '    var courseImageUrl by remember { mutableStateOf("") }', '    var courseImageUrl by remember { mutableStateOf("") }\n    var showCreateCourseForm by remember { mutableStateOf(false) }\n    var showAddChapterForm by remember { mutableStateOf(false) }\n    var expandedCourseIds by remember { mutableStateOf(setOf<String>()) }', "estados compactos")
    section = replace_once(section, '                    text = "Cadastre novos cursos teológicos, capítulos, links do YouTube, vídeos e áudios",', '                    text = "Cursos e aulas do IBR, organizados de forma simples.",', "subtítulo")
    section = replace_once(
        section,
        '''                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. CREATE NEW COURSE CARD''',
        '''                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showCreateCourseForm = !showCreateCourseForm; if (showCreateCourseForm) showAddChapterForm = false },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (showCreateCourseForm) "Fechar" else "+ Curso") }
                    Button(
                        onClick = { showAddChapterForm = !showAddChapterForm; if (showAddChapterForm) showCreateCourseForm = false },
                        modifier = Modifier.weight(1f),
                        enabled = ibrCoursesState.isNotEmpty()
                    ) { Text(if (showAddChapterForm) "Fechar" else "+ Aula") }
                }
            }
        }

        // 1. CREATE NEW COURSE CARD''',
        "botões rápidos",
    )
    section = replace_once(section, '        item {\n            Card(\n                modifier = Modifier.fillMaxWidth(),\n                shape = RoundedCornerShape(32.dp),', '        if (showCreateCourseForm) {\n        item {\n            Card(\n                modifier = Modifier.fillMaxWidth(),\n                shape = RoundedCornerShape(18.dp),', "formulário curso")
    section = replace_once(section, '                }\n            }\n        }\n\n        // 2. ADD CHAPTER CARD', '                }\n            }\n        }\n        }\n\n        // 2. ADD CHAPTER CARD', "fechar formulário curso")
    section = replace_once(section, '        if (ibrCoursesState.isNotEmpty()) {', '        if (ibrCoursesState.isNotEmpty() && showAddChapterForm) {', "formulário aula")
    section = replace_once(section, '                    shape = RoundedCornerShape(32.dp),\n                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)', '                    shape = RoundedCornerShape(18.dp),\n                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)', "shape aula")
    section = replace_once(section, '            items(visibleCourses) { course ->\n                Card(', '            items(visibleCourses) { course ->\n                val expanded = expandedCourseIds.contains(course.id)\n                Card(', "expandido")
    section = replace_once(section, '                    shape = RoundedCornerShape(32.dp),', '                    shape = RoundedCornerShape(18.dp),', "shape curso")
    section = replace_once(
        section,
        '                            Row {\n                                IconButton(onClick = { editingCourse = course }) {',
        '''                            Row {
                                IconButton(onClick = {
                                    expandedCourseIds = if (expanded) expandedCourseIds - course.id else expandedCourseIds + course.id
                                }) {
                                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Recolher" else "Abrir")
                                }
                                IconButton(onClick = { editingCourse = course }) {''',
        "botão expandir",
    )
    section = replace_once(section, '                                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)\n                            }', '                                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)\n                                Text("${course.chapters.size} aula(s) • ${course.chapters.sumOf { it.durationMinutes }} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n                            }', "resumo curso")
    section = replace_once(section, '                        Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)\n                        \n                        Divider(modifier = Modifier.padding(vertical = 8.dp))', '                        if (expanded) {\n                        Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)\n                        \n                        Divider(modifier = Modifier.padding(vertical = 8.dp))', "abrir detalhes")
    needle = '''                            }
                        }
                    }
                }
            }
        }

    }'''
    replacement = '''                            }
                        }
                        }
                    }
                }
            }
        }

    }'''
    if needle not in section:
        raise SystemExit("fechamento dos detalhes não encontrado")
    section = section.replace(needle, replacement, 1)
    section = section.replace('                                courseImageUrl = ""', '                                courseImageUrl = ""\n                                showCreateCourseForm = false', 1)
    section = section.replace('                                    isYoutube = false', '                                    isYoutube = false\n                                    showAddChapterForm = false', 1)

    VIP_PATH.write_text(head + section, encoding="utf-8")


if __name__ == "__main__":
    block = sys.argv[1] if len(sys.argv) > 1 else ""
    if block == "1":
        block1()
    elif block == "2":
        block2()
    elif block == "3":
        block3()
    else:
        raise SystemExit("Use 1, 2 ou 3")
