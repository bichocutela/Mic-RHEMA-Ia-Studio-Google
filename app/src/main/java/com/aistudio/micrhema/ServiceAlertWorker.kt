package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ServiceAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val services = FirebaseFirestore.getInstance()
                .collection("cultos_agenda")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    runCatching { document.toObject(ChurchService::class.java) }.getOrNull()
                }
            if (services.isEmpty()) return Result.success()

            val dayMap = mapOf(
                Calendar.SUNDAY to "Domingo",
                Calendar.MONDAY to "Segunda",
                Calendar.TUESDAY to "Terça",
                Calendar.WEDNESDAY to "Quarta",
                Calendar.THURSDAY to "Quinta",
                Calendar.FRIDAY to "Sexta",
                Calendar.SATURDAY to "Sábado"
            )
            val now = Calendar.getInstance()
            val nextService = services.mapNotNull { service ->
                val daysAhead = (0..6).firstOrNull { offset ->
                    val candidate = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    val dayName = dayMap[candidate.get(Calendar.DAY_OF_WEEK)] ?: return@firstOrNull false
                    service.day.contains(dayName, ignoreCase = true)
                } ?: return@mapNotNull null
                val date = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, daysAhead) }
                Triple(daysAhead, date, service)
            }.minWithOrNull(compareBy<Triple<Int, Calendar, ChurchService>> { it.first }.thenBy { it.third.time })
                ?: return Result.success()

            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(nextService.second.time)
            val serviceKey = "${dateKey}:${nextService.third.id}:${nextService.third.title}"
            val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
            if (prefs.getString("last_notified_service_key", null) == serviceKey) return Result.success()

            NotificationHelper.showNotification(
                context = context,
                title = "Próximo culto: ${nextService.third.title}",
                message = "${nextService.third.day} às ${nextService.third.time}. Prepare-se para estar conosco.",
                category = NotificationHelper.Category.NEXT_SERVICE,
                respectPreferences = true
            )
            prefs.edit().putString("last_notified_service_key", serviceKey).apply()
            Result.success()
        } catch (e: Exception) {
            Log.e("ServiceAlertWorker", "Falha ao verificar o próximo culto", e)
            Result.retry()
        }
    }
}
