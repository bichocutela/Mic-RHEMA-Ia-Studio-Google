package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class IbrContentWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!NotificationHelper.isIbrMember(context)) return Result.success()
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val courses = FirebaseFirestore.getInstance()
                .collection("ibr_courses")
                .get()
                .await()
                .documents
                .map { document ->
                    val chapters = document.get("chapters") as? List<*> ?: emptyList<Any>()
                    Triple(document.id, document.getString("title") ?: "Novo módulo", chapters.size)
                }
                .sortedBy { it.first }

            if (courses.isEmpty()) return Result.success()

            val signature = courses.joinToString("|") { "${it.first}:${it.third}" }
            val totalLessons = courses.sumOf { it.third }
            val courseCount = courses.size
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            val previousSignature = prefs.getString("ibr_content_signature", null)
            val previousCourseCount = prefs.getInt("ibr_course_count", -1)
            val previousLessonCount = prefs.getInt("ibr_total_lessons", -1)

            if (previousSignature == null || previousCourseCount < 0 || previousLessonCount < 0) {
                saveBaseline(prefs, signature, courseCount, totalLessons)
                return Result.success()
            }
            if (previousSignature == signature) return Result.success()

            // Curso novo já recebe o aviso específico "Novo curso no IBR" via FCM.
            // Este worker avisa apenas quando aulas/módulos crescem dentro do catálogo existente,
            // evitando duas notificações pelo mesmo cadastro.
            val hasNewLessonsInExistingCourses = courseCount == previousCourseCount && totalLessons > previousLessonCount
            if (hasNewLessonsInExistingCourses) {
                val addedLessons = totalLessons - previousLessonCount
                NotificationHelper.showNotification(
                    context = context,
                    title = if (addedLessons == 1) "Nova aula no IBR" else "Novas aulas no IBR",
                    message = if (addedLessons == 1)
                        "Uma nova aula foi adicionada aos seus cursos do IBR."
                    else
                        "$addedLessons novas aulas foram adicionadas aos seus cursos do IBR.",
                    category = NotificationHelper.Category.IBR_CONTENT,
                    respectPreferences = true,
                    destinationRoute = "ibr"
                )
            }

            saveBaseline(prefs, signature, courseCount, totalLessons)
            Result.success()
        } catch (e: Exception) {
            Log.e("IbrContentWorker", "Falha ao verificar novos conteúdos IBR", e)
            Result.retry()
        }
    }

    private fun saveBaseline(
        prefs: android.content.SharedPreferences,
        signature: String,
        courseCount: Int,
        lessonCount: Int
    ) {
        prefs.edit()
            .putString("ibr_content_signature", signature)
            .putInt("ibr_course_count", courseCount)
            .putInt("ibr_total_lessons", lessonCount)
            .apply()
    }
}
