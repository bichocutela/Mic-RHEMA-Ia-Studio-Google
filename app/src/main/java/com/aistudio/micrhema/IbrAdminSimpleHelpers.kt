package com.aistudio.micrhema

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
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

