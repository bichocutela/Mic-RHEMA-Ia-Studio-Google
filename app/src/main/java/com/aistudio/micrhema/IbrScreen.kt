package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IbrMainScreen(
    onNavigateToCourse: (String) -> Unit
) {
    val loggedInMember = loggedInMemberState.value
    if (loggedInMember == null) {
        LoginScreen(onLoginSuccess = {})
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Plataforma de Ensino IBR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                val context = LocalContext.current
                IconButton(onClick = { MemberManager.setLoggedInMember(context, null) }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                }
            }

            if (!loggedInMember.isIbr) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Você não está matriculado no IBR. Procure a secretaria para mais informações.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                if (ibrCoursesState.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum módulo disponível.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                } else {
                    val courses = ibrCoursesState.toList()
                    val totalModules = courses.size
                    
                    var completedModules = 0
                    var totalTime = 0
                    
                    courses.forEach { course ->
                        var allCompleted = true
                        course.chapters.forEach { chapter ->
                            val p = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                            if (p == null || !p.isCompleted) {
                                allCompleted = false
                            } else {
                                totalTime += chapter.durationMinutes
                            }
                        }
                        if (allCompleted && course.chapters.isNotEmpty()) completedModules++
                    }
                    
                    val progressPercent = if (totalModules > 0) completedModules.toFloat() / totalModules else 0f
                    val allLessons = courses.flatMap { course -> course.chapters.map { chapter -> course to chapter } }
                    val nextLesson = allLessons.firstOrNull { (course, chapter) ->
                        val progress = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                        progress != null && progress.lastPositionSeconds > 0 && !progress.isCompleted
                    } ?: allLessons.firstOrNull { (course, chapter) ->
                        ibrProgressState.none { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }
                    }
                    val localContext = LocalContext.current
                    
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            IbrProgressCard(
                                progress = progressPercent,
                                completedModules = completedModules,
                                remainingModules = totalModules - completedModules,
                                totalTime = totalTime,
                                certificateUrl = loggedInMember.ibrCertificateUrl
                            )
                        }
                        if (nextLesson != null) {
                            item {
                                IbrContinueStudyingCard(
                                    course = nextLesson.first,
                                    chapter = nextLesson.second,
                                    onClick = { onNavigateToCourse(nextLesson.first.id) }
                                )
                            }
                        }
                        
                        var previousCourseCompleted = true
                        
                        courses.forEachIndexed { index, course ->
                            val currentIsLocked = if (index == 0) false else !previousCourseCompleted
                            item {
                                IbrModuleCard(
                                    course = course,
                                    isLocked = currentIsLocked,
                                    onClick = { 
                                        if (currentIsLocked) {
                                            android.widget.Toast.makeText(localContext, "Conclua o módulo anterior para desbloquear este.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            onNavigateToCourse(course.id) 
                                        }
                                    }
                                )
                            }
                            
                            // check if this course is completed
                            var cClasses = 0
                            course.chapters.forEach { chapter ->
                                val p = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                                if (p != null && p.isCompleted) cClasses++
                            }
                            previousCourseCompleted = (cClasses == course.chapters.size && course.chapters.isNotEmpty())
                        }
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
    certificateUrl: String
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Progresso Geral", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${(animatedProgress * 100).toInt()}% Concluído", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Módulos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("$completedModules concl. / $remainingModules rest.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tempo de Estudo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("${totalTime / 60}h ${totalTime % 60}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (progress >= 1.0f) {
                Button(
                    onClick = {
                        if (certificateUrl.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(certificateUrl))
                            context.startActivity(intent)
                        } else {
                            android.widget.Toast.makeText(context, "Seu certificado está sendo gerado pela secretaria. Aguarde!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Certificado")
                }
            } else {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Certificado Bloqueado")
                }
            }
        }
    }
}

@Composable
fun IbrContinueStudyingCard(course: IbrCourse, chapter: IbrChapter, onClick: () -> Unit) {
    val chapterType = when (chapter.type) {
        "VIDEO" -> "Vídeo"
        "AUDIO" -> "Áudio"
        "TEXT" -> "Leitura"
        else -> "Aula"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text("CONTINUAR ESTUDANDO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(chapter.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f), maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
                    Text(chapterType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f))
                Spacer(Modifier.weight(1f))
                Text("Abrir aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Icon(Icons.Default.ChevronRight, contentDescription = "Abrir próxima aula", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun IbrModuleCard(course: IbrCourse, isLocked: Boolean, onClick: () -> Unit) {
    var completedClasses = 0
    val totalClasses = course.chapters.size
    var videos = 0
    var audios = 0
    var texts = 0
    var estTime = 0
    
    course.chapters.forEach { chapter ->
        val p = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
        if (p != null && p.isCompleted) {
            completedClasses++
        }
        when (chapter.type) {
            "VIDEO" -> videos++
            "AUDIO" -> audios++
            "TEXT" -> texts++
        }
        estTime += chapter.durationMinutes
    }
    
    val progress = if (totalClasses > 0) completedClasses.toFloat() / totalClasses else 0f
    
    val status = when {
        progress == 1f -> "Concluído"
        progress > 0f -> "Em andamento"
        else -> "Não iniciado"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (course.imageUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = course.imageUrl,
                    contentDescription = "Capa do curso ${course.title}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(course.theme.ifEmpty { "Módulo" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (isLocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                } else if (progress == 1f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Concluído", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                } else {
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Conclua o módulo anterior para liberar este conteúdo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$totalClasses Aulas", style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${estTime / 60}h ${estTime % 60}m", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (videos > 0) Text("🎥 $videos Vídeos", style = MaterialTheme.typography.bodySmall)
                if (audios > 0) Text("🎧 $audios Áudios", style = MaterialTheme.typography.bodySmall)
                if (texts > 0) Text("📖 $texts Textos", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = if (progress == 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
}

private fun markIbrChapterStarted(context: Context, course: IbrCourse, chapter: IbrChapter) {
    val existing = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
    if (existing == null) {
        val progress = IbrProgress(
            courseId = course.id,
            chapterId = chapter.id,
            lastPositionSeconds = 1,
            totalDurationSeconds = chapter.durationMinutes * 60,
            isCompleted = false
        )
        ibrProgressState.add(progress)
        IbrDatabaseHelper(context).saveProgress(progress)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrCourseScreen(
    courseId: String,
    onBack: () -> Unit,
    onNavigateToLesson: (String, String) -> Unit
) {
    val context = LocalContext.current
    val course = ibrCoursesState.find { it.id == courseId }
    if (course == null) {
        onBack()
        return
    }

    var showCompletionAnim by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aulas do Módulo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                items(course.chapters) { chapter ->
                    val progress = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                    val isCompleted = progress?.isCompleted == true

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            markIbrChapterStarted(context, course, chapter)
                            onNavigateToLesson(course.id, chapter.id)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when(chapter.type) {
                                    "VIDEO" -> Icons.Default.PlayCircle
                                    "AUDIO" -> Icons.Default.Headphones
                                    "TEXT" -> Icons.Default.MenuBook
                                    else -> Icons.Default.PlayCircle
                                }
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chapter.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(chapter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCompleted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Concluído", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                            } else {
                                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Pendente", tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
            
            if (showCompletionAnim) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    showCompletionAnim = false
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aula Concluída!", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrTextScreen(
    courseId: String,
    chapterId: String,
    onBack: () -> Unit
) {
    val course = ibrCoursesState.find { it.id == courseId }
    val chapter = course?.chapters?.find { it.id == chapterId }
    
    if (course == null || chapter == null) {
        onBack()
        return
    }

    var fontSizeMultiplier by remember { mutableFloatStateOf(1f) }
    
    var isCompleted by remember {
        mutableStateOf(ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { if (fontSizeMultiplier > 0.8f) fontSizeMultiplier -= 0.1f }) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuir Fonte")
                    }
                    IconButton(onClick = { if (fontSizeMultiplier < 2.0f) fontSizeMultiplier += 0.1f }) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar Fonte")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(chapter.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = chapter.textContent.ifEmpty { "Nenhum conteúdo adicionado." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontSizeMultiplier
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(48.dp))
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
            ) {
                Text(if (isCompleted) "Leitura concluída" else "Concluir leitura")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbrLessonScreen(
    courseId: String,
    chapterId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val course = ibrCoursesState.find { it.id == courseId }
    val chapter = course?.chapters?.find { it.id == chapterId }
    if (course == null || chapter == null) {
        onBack()
        return
    }
    if (chapter.type == "TEXT") {
        IbrTextScreen(courseId = courseId, chapterId = chapterId, onBack = onBack)
        return
    }

    val existingProgress = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
    var isCompleted by remember { mutableStateOf(existingProgress?.isCompleted == true) }
    val typeLabel = if (chapter.type == "VIDEO") "Vídeo-aula" else "Áudio-aula"
    val typeIcon = if (chapter.type == "VIDEO") Icons.Default.PlayCircle else Icons.Default.Headphones

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aula IBR", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(74.dp))
            }
            Text(course.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(chapter.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(chapter.description.ifBlank { "Acompanhe esta aula do curso e marque como concluída ao terminar." }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(typeIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text(typeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (chapter.type == "VIDEO") {
                Button(
                    onClick = {
                        val url = if (chapter.isYoutube && chapter.videoUrl.isNotEmpty() && !chapter.videoUrl.startsWith("http")) "https://www.youtube.com/watch?v=${chapter.videoUrl}" else chapter.videoUrl
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        catch (_: Exception) { android.widget.Toast.makeText(context, "Não foi possível abrir o vídeo.", android.widget.Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir vídeo")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val track = AudioTrack(
                            id = chapter.id,
                            title = chapter.title,
                            subtitle = course.title,
                            audioUrl = chapter.audioUrl,
                            coverUrl = course.imageUrl
                        )
                        GlobalAudioPlayer.playTrack(context, track)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reproduzir áudio")
                }
            }
            Button(
                onClick = {
                    markIbrChapterCompleted(context, course, chapter)
                    isCompleted = true
                },
                enabled = !isCompleted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isCompleted) "Aula concluída" else "Marcar como concluída")
            }
        }
    }
}
