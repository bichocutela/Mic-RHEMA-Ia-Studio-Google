package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Cold-start links wait for the catalog and keep the same access rules as the course list. */
@Composable
internal fun NotificationIbrDestination(
    courseId: String,
    chapterId: String? = null,
    onBack: () -> Unit,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToLesson: (String, String) -> Unit
) {
    val member = loggedInMemberState.value
    val administrator = adminAuthenticatedState.value || member?.isAdmin == true
    if (!administrator && member?.isIbr != true) {
        IbrMainScreen(onNavigateToCourse = onNavigateToCourse)
        return
    }
    val courses = ibrCoursesState.toList()
    val index = courses.indexOfFirst { it.id == courseId }
    val course = courses.getOrNull(index)
    val chapter = course?.chapters?.find { it.id == chapterId }
    var waited by remember(courseId, chapterId) { mutableStateOf(false) }
    LaunchedEffect(courseId, chapterId) { delay(15000); waited = true }
    if (course == null || (chapterId != null && chapter == null)) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (!waited) CircularProgressIndicator()
            Text(if (waited) "Este conteúdo não está disponível no momento." else "Carregando conteúdo do IBR…", modifier = Modifier.padding(16.dp))
            TextButton(onClick = onBack) { Text("Voltar") }
        }
        return
    }
    val previous = courses.getOrNull(index - 1)
    val previousCompleted = previous == null || (previous.chapters.isNotEmpty() && previous.chapters.all { lesson ->
        ibrProgressState.any { it.courseId == previous.id && it.chapterId == lesson.id && it.isCompleted }
    })
    if (!administrator && (isIbrCourseLocked(course, !previousCompleted) || (chapter != null && !isIbrChapterUnlocked(course, chapter)))) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Este conteúdo ainda está bloqueado para você.")
            if (!isIbrCourseLocked(course, !previousCompleted)) TextButton(onClick = { onNavigateToCourse(courseId) }) { Text("Ver módulo") }
            TextButton(onClick = onBack) { Text("Voltar") }
        }
        return
    }
    if (chapterId == null) IbrCourseScreen(courseId, onBack, onNavigateToLesson)
    else IbrLessonScreen(courseId, chapterId, onBack)
}
