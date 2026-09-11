from pathlib import Path
import sys

ROOT = Path('.')
VIP = ROOT / 'app/src/main/java/com/aistudio/micrhema/VipAdmin.kt'
DATA = ROOT / 'app/src/main/java/com/aistudio/micrhema/Data.kt'
HELPER = ROOT / 'app/src/main/java/com/aistudio/micrhema/IbrAdminSimpleHelpers.kt'
SCREEN = ROOT / 'app/src/main/java/com/aistudio/micrhema/IbrScreen.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'{label} not found')
    return text.replace(old, new, 1)


def block1():
    helper = HELPER.read_text(encoding='utf-8')
    helper = replace_once(
        helper,
        'import androidx.compose.runtime.mutableStateMapOf\n',
        'import androidx.compose.runtime.mutableStateListOf\nimport androidx.compose.runtime.mutableStateMapOf\n',
        'helper import'
    )
    marker = '''fun saveIbrCourseSilently(item: IbrCourse) {
    if (BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("ibr_courses").document(item.id).set(item)
    }
}
'''
    category_code = marker + '''

private val defaultIbrCourseCategories = listOf("Teologia", "História Bíblica", "Vida Cristã")
val ibrCourseCategoriesState = mutableStateListOf<String>().apply { addAll(defaultIbrCourseCategories) }

fun loadIbrCourseCategories() {
    val localCategories = (defaultIbrCourseCategories + ibrCoursesState.map { it.theme })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
    ibrCourseCategoriesState.clear()
    ibrCourseCategoriesState.addAll(localCategories)
    if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
    Firebase.firestore.collection("settings").document("ibr").get()
        .addOnSuccessListener { document ->
            val saved = (document.get("courseThemes") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()
                .filter { it.isNotBlank() }
            val merged = (defaultIbrCourseCategories + saved + ibrCoursesState.map { it.theme })
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
            ibrCourseCategoriesState.clear()
            ibrCourseCategoriesState.addAll(merged)
        }
}

fun saveIbrCourseCategory(name: String) {
    val category = name.trim()
    if (category.isBlank()) return
    val merged = (ibrCourseCategoriesState + category)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }
    ibrCourseCategoriesState.clear()
    ibrCourseCategoriesState.addAll(merged)
    if (BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("settings").document("ibr").set(
            mapOf("courseThemes" to merged),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }
}
'''
    helper = replace_once(helper, marker, category_code, 'helper category marker')
    HELPER.write_text(helper, encoding='utf-8')

    vip = VIP.read_text(encoding='utf-8')
    vip = replace_once(vip, '''    var courseImageUrl by remember { mutableStateOf("") }
    var showCreateCourseForm by remember { mutableStateOf(false) }
''', '''    var courseImageUrl by remember { mutableStateOf("") }
    var showNewCourseCategory by remember { mutableStateOf(false) }
    var newCourseCategory by remember { mutableStateOf("") }
    var showCreateCourseForm by remember { mutableStateOf(false) }
''', 'course states')
    vip = replace_once(vip, '''    val courseThemes = listOf("Todos") + ibrCoursesState.map { it.theme }.filter { it.isNotBlank() }.distinct().sorted()
''', '''    val courseThemes = listOf("Todos") + (ibrCourseCategoriesState + ibrCoursesState.map { it.theme })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }
''', 'course themes')
    vip = replace_once(vip, '''    LaunchedEffect(ibrCoursesState.size) {
''', '''    LaunchedEffect(Unit) {
        loadIbrCourseCategories()
    }

    LaunchedEffect(ibrCoursesState.size) {
''', 'load categories')
    vip = replace_once(vip, '''                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("Teologia", "História Bíblica", "Vida Cristã")
                            themes.forEach { theme ->
                                FilterChip(
                                    selected = courseTheme == theme,
                                    onClick = { courseTheme = theme },
                                    label = { Text(theme) }
                                )
                            }
                        }
''', '''                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ibrCourseCategoriesState.forEach { theme ->
                                FilterChip(
                                    selected = courseTheme == theme,
                                    onClick = { courseTheme = theme },
                                    label = { Text(theme) }
                                )
                            }
                            AssistChip(onClick = { showNewCourseCategory = !showNewCourseCategory }, label = { Text("+") })
                        }
                        if (showNewCourseCategory) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextField(
                                    value = newCourseCategory,
                                    onValueChange = { newCourseCategory = it },
                                    label = { Text("Nova categoria") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val name = newCourseCategory.trim()
                                    if (name.isNotBlank()) {
                                        saveIbrCourseCategory(name)
                                        courseTheme = name
                                        newCourseCategory = ""
                                        showNewCourseCategory = false
                                    }
                                }) { Text("Salvar") }
                            }
                        }
''', 'create category ui')
    vip = replace_once(vip, '''        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }
        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }
''', '''        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }
        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }
        var showNewEditCategory by remember(editingCourse) { mutableStateOf(false) }
        var newEditCategory by remember(editingCourse) { mutableStateOf("") }
''', 'edit category state')
    vip = replace_once(vip, '''                    GlassTextField(value = editTheme, onValueChange = { editTheme = it }, label = { Text("Tema") })
                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")
''', '''                    Text("Tema / Categoria", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (ibrCourseCategoriesState + listOf(editTheme)).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach { theme ->
                            FilterChip(selected = editTheme == theme, onClick = { editTheme = theme }, label = { Text(theme) })
                        }
                        AssistChip(onClick = { showNewEditCategory = !showNewEditCategory }, label = { Text("+") })
                    }
                    if (showNewEditCategory) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlassTextField(value = newEditCategory, onValueChange = { newEditCategory = it }, label = { Text("Nova categoria") }, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val name = newEditCategory.trim()
                                if (name.isNotBlank()) {
                                    saveIbrCourseCategory(name)
                                    editTheme = name
                                    newEditCategory = ""
                                    showNewEditCategory = false
                                }
                            }) { Text("Salvar") }
                        }
                    }
                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")
''', 'edit category ui')
    VIP.write_text(vip, encoding='utf-8')


def block2():
    data = DATA.read_text(encoding='utf-8')
    data = replace_once(data, '''    var studyDocxUrl: String = "", // Word .docx opcional para conteúdo de estudo
    var isYoutube: Boolean = false,
''', '''    var studyDocxUrl: String = "", // Word .docx opcional para conteúdo de estudo
    var accessMode: String = "FREE", // FREE, AFTER_PREVIOUS, MANUAL_LOCKED
    var isYoutube: Boolean = false,
''', 'chapter model')
    data = replace_once(data, '''    var description: String = "",
    var imageUrl: String = "",
    var chapters: List<IbrChapter> = emptyList()
''', '''    var description: String = "",
    var imageUrl: String = "",
    var accessMode: String = "AUTO", // AUTO, UNLOCKED, LOCKED
    var chapters: List<IbrChapter> = emptyList()
''', 'course model')
    DATA.write_text(data, encoding='utf-8')

    helper = HELPER.read_text(encoding='utf-8')
    helper = helper.rstrip() + '''

const val IBR_COURSE_AUTO = "AUTO"
const val IBR_COURSE_UNLOCKED = "UNLOCKED"
const val IBR_COURSE_LOCKED = "LOCKED"
const val IBR_LESSON_FREE = "FREE"
const val IBR_LESSON_AFTER_PREVIOUS = "AFTER_PREVIOUS"
const val IBR_LESSON_MANUAL_LOCKED = "MANUAL_LOCKED"

fun isIbrCourseLocked(course: IbrCourse, automaticLocked: Boolean): Boolean = when (course.accessMode.uppercase()) {
    IBR_COURSE_UNLOCKED -> false
    IBR_COURSE_LOCKED -> true
    else -> automaticLocked
}

fun ibrCourseAccessLabel(mode: String): String = when (mode.uppercase()) {
    IBR_COURSE_UNLOCKED -> "Desbloqueado"
    IBR_COURSE_LOCKED -> "Bloqueado"
    else -> "Automático"
}

fun isIbrChapterUnlocked(course: IbrCourse, chapter: IbrChapter, progress: List<IbrProgress> = ibrProgressState): Boolean {
    return when (chapter.accessMode.uppercase()) {
        IBR_LESSON_MANUAL_LOCKED -> false
        IBR_LESSON_AFTER_PREVIOUS -> {
            val index = course.chapters.indexOfFirst { it.id == chapter.id }
            index <= 0 || progress.any {
                it.courseId == course.id && it.chapterId == course.chapters[index - 1].id && it.isCompleted
            }
        }
        else -> true
    }
}

fun ibrChapterAccessLabel(mode: String): String = when (mode.uppercase()) {
    IBR_LESSON_AFTER_PREVIOUS -> "Após aula anterior"
    IBR_LESSON_MANUAL_LOCKED -> "Bloqueada"
    else -> "Livre"
}
''' + '\n'
    HELPER.write_text(helper, encoding='utf-8')

    vip = VIP.read_text(encoding='utf-8')
    vip = replace_once(vip, '''    var chapterType by remember { mutableStateOf("VIDEO") } // VIDEO, AUDIO, TEXT
    var isYoutube by remember { mutableStateOf(false) }
''', '''    var chapterType by remember { mutableStateOf("VIDEO") } // VIDEO, AUDIO, TEXT
    var chapterAccessMode by remember { mutableStateOf(IBR_LESSON_FREE) }
    var isYoutube by remember { mutableStateOf(false) }
''', 'chapter access state')
    vip = replace_once(vip, '''                        GlassTextField(
                            value = chapterDuration,
                            onValueChange = { chapterDuration = it },
                            label = { Text("Duração (minutos)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        when (chapterType) {
''', '''                        GlassTextField(
                            value = chapterDuration,
                            onValueChange = { chapterDuration = it },
                            label = { Text("Duração (minutos)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Text("Liberação da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(IBR_LESSON_FREE to "Livre", IBR_LESSON_AFTER_PREVIOUS to "Após aula anterior", IBR_LESSON_MANUAL_LOCKED to "Bloqueada").forEach { (id, title) ->
                                FilterChip(selected = chapterAccessMode == id, onClick = { chapterAccessMode = id }, label = { Text(title) })
                            }
                        }
                        Text(
                            when (chapterAccessMode) {
                                IBR_LESSON_AFTER_PREVIOUS -> "Libera automaticamente quando a aula anterior for concluída."
                                IBR_LESSON_MANUAL_LOCKED -> "Fica bloqueada até o ADM mudar esta opção."
                                else -> "Pode ser aberta a qualquer momento dentro do módulo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        when (chapterType) {
''', 'new lesson access ui')
    vip = replace_once(vip, '''                                        studyPdfUrl = studyPdfUrl.trim(),
                                        studyDocxUrl = studyDocxUrl.trim(),
                                        isYoutube = detectedYoutube,
''', '''                                        studyPdfUrl = studyPdfUrl.trim(),
                                        studyDocxUrl = studyDocxUrl.trim(),
                                        accessMode = chapterAccessMode,
                                        isYoutube = detectedYoutube,
''', 'new lesson save')
    vip = replace_once(vip, '''                                    chapterType = "VIDEO"
                                    studyPdfUrl = ""
''', '''                                    chapterType = "VIDEO"
                                    chapterAccessMode = IBR_LESSON_FREE
                                    studyPdfUrl = ""
''', 'new lesson reset')
    vip = replace_once(vip, '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}${if (ch.studyDocxUrl.isNotBlank()) " • Word 📝" else ""}",
''', '''                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else if (ch.audioUrl.isNotEmpty()) "Áudio 🎵" else "Texto 📖"} • ${ibrChapterAccessLabel(ch.accessMode)}${if (ch.studyPdfUrl.isNotBlank()) " • PDF 📄" else ""}${if (ch.studyDocxUrl.isNotBlank()) " • Word 📝" else ""}",
''', 'lesson summary')
    vip = replace_once(vip, '''        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }
        var showNewEditCategory by remember(editingCourse) { mutableStateOf(false) }
''', '''        var editImageUrl by remember(editingCourse) { mutableStateOf(editingCourse!!.imageUrl) }
        var editCourseAccessMode by remember(editingCourse) { mutableStateOf(editingCourse!!.accessMode.ifBlank { IBR_COURSE_AUTO }.uppercase()) }
        var showNewEditCategory by remember(editingCourse) { mutableStateOf(false) }
''', 'edit course access state')
    vip = replace_once(vip, '''                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")
                }
''', '''                    LocalUploadField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = "Capa do curso (opcional)", mimeType = "image/*")
                    Text("Acesso ao curso", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(IBR_COURSE_AUTO to "Automático", IBR_COURSE_UNLOCKED to "Desbloqueado", IBR_COURSE_LOCKED to "Bloqueado").forEach { (id, title) ->
                            FilterChip(selected = editCourseAccessMode == id, onClick = { editCourseAccessMode = id }, label = { Text(title) })
                        }
                    }
                    Text(
                        when (editCourseAccessMode) {
                            IBR_COURSE_UNLOCKED -> "Pode ser aberto mesmo antes do módulo anterior ser concluído."
                            IBR_COURSE_LOCKED -> "Fica bloqueado até o ADM desbloquear."
                            else -> "Mantém a regra atual: libera após concluir o módulo anterior."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
''', 'edit course access ui')
    vip = replace_once(vip, '''                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme, imageUrl = editImageUrl.trim())
''', '''                        val updated = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme, imageUrl = editImageUrl.trim(), accessMode = editCourseAccessMode)
''', 'edit course access save')
    vip = replace_once(vip, '''        var editStudyDocxUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyDocxUrl) }
        
''', '''        var editStudyDocxUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.studyDocxUrl) }
        var editAccessMode by remember(editingChapter) { mutableStateOf(editingChapter!!.accessMode.ifBlank { IBR_LESSON_FREE }.uppercase()) }
        
''', 'edit lesson access state')
    vip = replace_once(vip, '''                    when (editType) {
                        "VIDEO" -> LocalUploadField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = "Vídeo ou link do YouTube", mimeType = "video/*")
''', '''                    Text("Liberação da aula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(IBR_LESSON_FREE to "Livre", IBR_LESSON_AFTER_PREVIOUS to "Após anterior", IBR_LESSON_MANUAL_LOCKED to "Bloqueada").forEach { (id, title) ->
                            FilterChip(selected = editAccessMode == id, onClick = { editAccessMode = id }, label = { Text(title) })
                        }
                    }
                    when (editType) {
                        "VIDEO" -> LocalUploadField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = "Vídeo ou link do YouTube", mimeType = "video/*")
''', 'edit lesson access ui')
    vip = replace_once(vip, '''                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                isYoutube = isYt,
''', '''                                studyPdfUrl = editStudyPdfUrl.trim(),
                                studyDocxUrl = editStudyDocxUrl.trim(),
                                accessMode = editAccessMode,
                                isYoutube = isYt,
''', 'edit lesson access save')
    VIP.write_text(vip, encoding='utf-8')


def block3():
    text = SCREEN.read_text(encoding='utf-8')
    text = replace_once(text, '''            val nextLesson = allLessons.firstOrNull { (c, ch) ->
                ibrProgressState.find { it.courseId == c.id && it.chapterId == ch.id }?.let { it.lastPositionSeconds > 0 && !it.isCompleted } == true
            } ?: allLessons.firstOrNull { (c, ch) ->
                ibrProgressState.none { it.courseId == c.id && it.chapterId == ch.id && it.isCompleted }
            }
''', '''            val nextLesson = allLessons.firstOrNull { (c, ch) ->
                isIbrChapterUnlocked(c, ch) &&
                    ibrProgressState.find { it.courseId == c.id && it.chapterId == ch.id }?.let { it.lastPositionSeconds > 0 && !it.isCompleted } == true
            } ?: allLessons.firstOrNull { (c, ch) ->
                isIbrChapterUnlocked(c, ch) &&
                    ibrProgressState.none { it.courseId == c.id && it.chapterId == ch.id && it.isCompleted }
            }
''', 'next lesson')
    text = replace_once(text, '''                courses.forEachIndexed { index, course ->
                    val locked = index > 0 && !previousCourseCompleted
''', '''                courses.forEachIndexed { index, course ->
                    val automaticLocked = index > 0 && !previousCourseCompleted
                    val locked = isIbrCourseLocked(course, automaticLocked)
''', 'course lock')
    text = replace_once(text, '''                            if (locked) android.widget.Toast.makeText(localContext, "Conclua o módulo anterior para desbloquear este.", android.widget.Toast.LENGTH_SHORT).show()
                            else onNavigateToCourse(course.id)
''', '''                            if (locked) {
                                val message = if (course.accessMode.uppercase() == IBR_COURSE_LOCKED) "Este módulo foi bloqueado pelo administrador." else "Conclua o módulo anterior para desbloquear este."
                                android.widget.Toast.makeText(localContext, message, android.widget.Toast.LENGTH_SHORT).show()
                            } else onNavigateToCourse(course.id)
''', 'course toast')
    text = replace_once(text, '''            items(course.chapters) { chapter ->
                val isCompleted = ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        markIbrChapterStarted(context, course, chapter)
                        onNavigateToLesson(course.id, chapter.id)
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
''', '''            items(course.chapters) { chapter ->
                val isCompleted = ibrProgressState.any { it.courseId == course.id && it.chapterId == chapter.id && it.isCompleted }
                val isUnlocked = isIbrChapterUnlocked(course, chapter)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (isUnlocked) {
                            markIbrChapterStarted(context, course, chapter)
                            onNavigateToLesson(course.id, chapter.id)
                        } else {
                            val message = if (chapter.accessMode.uppercase() == IBR_LESSON_AFTER_PREVIOUS) "Conclua a aula anterior para liberar esta aula." else "Esta aula está bloqueada pelo administrador."
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
                ) {
''', 'lesson card')
    text = replace_once(text, '''                            Text("${chapter.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (isCompleted) "Concluído" else "Pendente", modifier = Modifier.size(23.dp), tint = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
''', '''                            Text("${chapter.durationMinutes} min • ${ibrChapterAccessLabel(chapter.accessMode)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (!isUnlocked) {
                                Text(
                                    if (chapter.accessMode.uppercase() == IBR_LESSON_AFTER_PREVIOUS) "Aguardando aula anterior" else "Bloqueada pelo ADM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Icon(
                            if (!isUnlocked) Icons.Default.Lock else if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            if (!isUnlocked) "Bloqueada" else if (isCompleted) "Concluído" else "Pendente",
                            modifier = Modifier.size(23.dp),
                            tint = if (!isUnlocked) MaterialTheme.colorScheme.error else if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = .3f)
                        )
''', 'lesson status')
    SCREEN.write_text(text, encoding='utf-8')


if __name__ == '__main__':
    if len(sys.argv) != 2 or sys.argv[1] not in {'1', '2', '3'}:
        raise SystemExit('usage: patch_ibr_categories_access.py 1|2|3')
    {'1': block1, '2': block2, '3': block3}[sys.argv[1]]()
