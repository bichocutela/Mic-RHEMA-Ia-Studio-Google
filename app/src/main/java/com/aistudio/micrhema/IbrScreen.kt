package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun IbrMainScreen(onNavigateToCourse: (String) -> Unit) {
    val loggedInMember = loggedInMemberState.value
    if (loggedInMember == null) {
        LoginScreen(onLoginSuccess = {})
        return
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Plataforma de Ensino IBR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val context = LocalContext.current
                IconButton(onClick = { MemberManager.setLoggedInMember(context, null) }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                }
            }

            if (!loggedInMember.isIbr) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Você não está matriculado no IBR. Procure a secretaria para mais informações.",
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            if (ibrCoursesState.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum módulo disponível.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
                }
                return@Column
            }

            val courses = ibrCoursesState.toList()
            var completedModules = 0
            var totalTime = 0
            courses.forEach { course ->
                val completed = course.chapters.isNotEmpty() && course.chapters.all { chapter ->
                    val p = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                    if (p?.isCompleted == true) {
                        totalTime += chapter.durationMinutes
                        true
                    } else false
                }
                if (completed) completedModules++
            }
            val progressPercent = if (courses.isEmpty()) 0f else completedModules.toFloat() / courses.size
            val allLessons = courses.flatMap { c -> c.chapters.map { ch -> c to ch } }
            val nextLesson = allLessons.firstOrNull { (c, ch) ->
                ibrProgressState.find { it.courseId == c.id && it.chapterId == ch.id }?.let { it.lastPositionSeconds > 0 && !it.isCompleted } == true
            } ?: allLessons.firstOrNull { (c, ch) ->
                ibrProgressState.none { it.courseId == c.id && it.chapterId == ch.id && it.isCompleted }
            }
            val localContext = LocalContext.current

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    IbrProgressCard(
                        progressPercent,
                        completedModules,
                        courses.size - completedModules,
                        totalTime,
                        loggedInMember.ibrCertificateUrl,
                        loggedInMember.email
                    )
                }
                nextLesson?.let { lesson ->
                    item {
                        IbrContinueStudyingCard(lesson.first, lesson.second) { onNavigateToCourse(lesson.first.id) }
                    }
                }
                var previousCourseCompleted = true
                courses.forEachIndexed { index, course ->
                    val locked = index > 0 && !previousCourseCompleted
                    item {
                        IbrModuleCard(course, locked) {
                            if (locked) android.widget.Toast.makeText(localContext, "Conclua o módulo anterior para desbloquear este.", android.widget.Toast.LENGTH_SHORT).show()
                            else onNavigateToCourse(course.id)
                        }
                    }
                    previousCourseCompleted = course.chapters.isNotEmpty() && course.chapters.all { chapter ->
                        ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }
                    }
                }
            }
        }
    }
}

@Composable
fun IbrProgressCard(
    progress: Float,
    completedModules: Int,
    remainingModules: Int,
    totalTime: Int,
    certificateUrl: String,
    recipientEmail: String
) {
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(1000))
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Progresso Geral", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("${(animatedProgress * 100).toInt()}% Concluído", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Módulos", style = MaterialTheme.typography.labelSmall)
                    Text("$completedModules concl. / $remainingModules rest.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tempo de Estudo", style = MaterialTheme.typography.labelSmall)
                    Text("${totalTime / 60}h ${totalTime % 60}m", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .1f))
            Spacer(Modifier.height(10.dp))
            when {
                progress >= 1f && certificateUrl.isNotBlank() -> {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(certificateUrl))) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver Certificado")
                    }
                    if (recipientEmail.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val emailIntent = CertificateEmailShare.createLinkIntent(recipientEmail.trim(), certificateUrl)
                                if (emailIntent.resolveActivity(context.packageManager) != null) context.startActivity(emailIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar por e-mail")
                        }
                    }
                }
                progress >= 1f -> DisabledIbrCertificateButton("Certificado em processamento", Icons.Default.Schedule)
                else -> DisabledIbrCertificateButton("Certificado Bloqueado", Icons.Default.Lock)
            }
        }
    }
}

@Composable
private fun DisabledIbrCertificateButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
fun IbrContinueStudyingCard(course: IbrCourse, chapter: IbrChapter, onClick: () -> Unit) {
    val chapterType = when (chapter.type) { "VIDEO" -> "Vídeo"; "AUDIO" -> "Áudio"; "TEXT" -> "Leitura"; else -> "Aula" }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("CONTINUAR ESTUDANDO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(chapter.title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)) {
                    Text(chapterType, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text("Abrir aula", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, "Abrir próxima aula", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun IbrModuleCard(course: IbrCourse, isLocked: Boolean, onClick: () -> Unit) {
    val totalClasses = course.chapters.size
    val completedClasses = course.chapters.count { chapter -> ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted } }
    val videos = course.chapters.count { it.type == "VIDEO" }
    val audios = course.chapters.count { it.type == "AUDIO" }
    val texts = course.chapters.count { it.type == "TEXT" }
    val estTime = course.chapters.sumOf { it.durationMinutes }
    val progress = if (totalClasses > 0) completedClasses.toFloat() / totalClasses else 0f

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            if (course.imageUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = course.imageUrl,
                    contentDescription = "Capa do curso ${course.title}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.School, null, modifier = Modifier.size(38.dp)) }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(course.theme.ifEmpty { "Módulo" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                when {
                    isLocked -> Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    progress == 1f -> Text("Concluído", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    progress > 0f -> Text("Em andamento", style = MaterialTheme.typography.labelSmall)
                    else -> Text("Não iniciado", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(course.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isLocked) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .72f)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Conclua o módulo anterior para liberar este conteúdo.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$totalClasses aulas", style = MaterialTheme.typography.labelSmall)
                Text("${estTime / 60}h ${estTime % 60}m", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (videos > 0) Text("🎥 $videos", style = MaterialTheme.typography.labelSmall)
                if (audios > 0) Text("🎧 $audios", style = MaterialTheme.typography.labelSmall)
                if (texts > 0) Text("📖 $texts", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun markIbrChapterCompleted(context: Context, course: IbrCourse, chapter: IbrChapter) {
    val existing = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
    val progress = existing ?: IbrProgress(course.id, chapter.id, 0, chapter.durationMinutes * 60, false)
    progress.isCompleted = true
    if (existing == null) ibrProgressState.add(progress)
    IbrDatabaseHelper(context).saveProgress(progress)
    syncIbrProgressToFirestore(progress)
}

private fun markIbrChapterStarted(context: Context, course: IbrCourse, chapter: IbrChapter) {
    if (ibrProgressState.none { it.courseId == course.id && it.chapterId == chapter.id }) {
        val progress = IbrProgress(course.id, chapter.id, 1, chapter.durationMinutes * 60, false)
        ibrProgressState.add(progress)
        IbrDatabaseHelper(context).saveProgress(progress)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrCourseScreen(courseId: String, onBack: () -> Unit, onNavigateToLesson: (String, String) -> Unit) {
    val context = LocalContext.current
    val course = ibrCoursesState.find { it.id == courseId } ?: run { onBack(); return }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aulas do Módulo", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(course.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            items(course.chapters) { chapter ->
                val isCompleted = ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        markIbrChapterStarted(context, course, chapter)
                        onNavigateToLesson(course.id, chapter.id)
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            val icon = when (chapter.type) { "VIDEO" -> Icons.Default.PlayCircle; "AUDIO" -> Icons.Default.Headphones; "TEXT" -> Icons.Default.MenuBook; else -> Icons.Default.PlayCircle }
                            Icon(icon, null, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chapter.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(chapter.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (isCompleted) "Concluído" else "Pendente", modifier = Modifier.size(23.dp), tint = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrTextScreen(courseId: String, chapterId: String, onBack: () -> Unit) {
    val course = ibrCoursesState.find { it.id == courseId }
    val chapter = course?.chapters?.find { it.id == chapterId }
    if (course == null || chapter == null) { onBack(); return }
    var fontSizeMultiplier by remember { mutableFloatStateOf(1f) }
    var isCompleted by remember { mutableStateOf(ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.title, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                actions = {
                    IconButton(onClick = { if (fontSizeMultiplier > .8f) fontSizeMultiplier -= .1f }) { Icon(Icons.Default.Remove, "Diminuir Fonte") }
                    IconButton(onClick = { if (fontSizeMultiplier < 2f) fontSizeMultiplier += .1f }) { Icon(Icons.Default.Add, "Aumentar Fonte") }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(chapter.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                chapter.textContent.ifEmpty { "Nenhum conteúdo adicionado." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontSizeMultiplier
                )
            )
            Spacer(Modifier.height(20.dp))
            IbrStudyMaterials(chapter)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val newProg = IbrProgress(course.id, chapter.id, 0, chapter.durationMinutes * 60, true)
                    syncIbrProgressToFirestore(newProg)
                    val existing = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                    if (existing != null) existing.isCompleted = true else ibrProgressState.add(newProg)
                    isCompleted = true
                },
                enabled = !isCompleted,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isCompleted) "Leitura concluída" else "Concluir leitura") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrLessonScreen(courseId: String, chapterId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val course = ibrCoursesState.find { it.id == courseId }
    val chapter = course?.chapters?.find { it.id == chapterId }
    if (course == null || chapter == null) { onBack(); return }
    if (chapter.type == "TEXT") { IbrTextScreen(courseId, chapterId, onBack); return }

    var isCompleted by remember { mutableStateOf(ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }?.isCompleted == true) }
    val typeLabel = if (chapter.type == "VIDEO") "Vídeo-aula" else "Áudio-aula"
    val typeIcon = if (chapter.type == "VIDEO") Icons.Default.PlayCircle else Icons.Default.Headphones

    Scaffold(
        topBar = { TopAppBar(title = { Text("Aula IBR", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (chapter.type == "VIDEO") {
                CleanVideoPlayer(
                    videoUrl = chapter.videoUrl.ifBlank { chapter.youtubeId },
                    title = chapter.title,
                    modifier = Modifier.fillMaxWidth(),
                    showTitleBar = false,
                    showExternalButton = true
                )
            } else {
                Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(typeIcon, null, modifier = Modifier.size(58.dp))
                }
            }
            Text(course.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(chapter.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(chapter.description.ifBlank { "Acompanhe esta aula do curso e marque como concluída ao terminar." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(typeIcon, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(typeLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall)
            }
            if (chapter.type != "VIDEO") {
                OutlinedButton(
                    onClick = {
                        GlobalAudioPlayer.playTrack(context, AudioTrack(chapter.id, chapter.title, course.title, chapter.audioUrl, course.imageUrl))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Headphones, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reproduzir áudio")
                }
            }
            Button(
                onClick = { markIbrChapterCompleted(context, course, chapter); isCompleted = true },
                enabled = !isCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isCompleted) "Aula concluída" else "Marcar como concluída")
            }

            IbrStudyMaterials(chapter)
            Spacer(Modifier.height(18.dp))
        }
    }
}


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
