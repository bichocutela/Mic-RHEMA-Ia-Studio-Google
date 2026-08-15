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
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            val previousSignature = prefs.getString("ibr_content_signature", null)
            if (previousSignature == null) {
                prefs.edit().putString("ibr_content_signature", signature).apply()
                return Result.success()
            }
            if (previousSignature == signature) return Result.success()

            val totalLessons = courses.sumOf { it.third }
            NotificationHelper.showNotification(
                context = context,
                title = "Novos conteúdos do IBR",
                message = "Há ${courses.size} módulo(s) e $totalLessons aula(s) disponíveis para você.",
                category = NotificationHelper.Category.IBR_CONTENT,
                respectPreferences = true
            )
            prefs.edit().putString("ibr_content_signature", signature).apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("IbrContentWorker", "Falha ao verificar novos conteúdos IBR", e)
            Result.retry()
        }
    }
}
