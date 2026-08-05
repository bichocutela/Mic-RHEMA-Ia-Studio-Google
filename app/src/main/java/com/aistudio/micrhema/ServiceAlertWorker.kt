package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ServiceAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.success()

            val db = FirebaseFirestore.getInstance()
            val result = db.collection("cultos_agenda").get().await()

            if (!result.isEmpty) {
                val services = result.documents.mapNotNull {
                    try {
                        it.toObject(ChurchService::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                val calendar = Calendar.getInstance()
                val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val dayMap = mapOf(
                    Calendar.SUNDAY to "Domingo",
                    Calendar.MONDAY to "Segunda",
                    Calendar.TUESDAY to "Terça",
                    Calendar.WEDNESDAY to "Quarta",
                    Calendar.THURSDAY to "Quinta",
                    Calendar.FRIDAY to "Sexta",
                    Calendar.SATURDAY to "Sábado"
                )

                val todayStr = dayMap[todayDayOfWeek] ?: ""
                val tomorrowStr = dayMap[tomorrowDayOfWeek] ?: ""

                val todayService = services.find { it.day.equals(todayStr, ignoreCase = true) || it.day.contains(todayStr, ignoreCase = true) }
                val tomorrowService = services.find { it.day.equals(tomorrowStr, ignoreCase = true) || it.day.contains(tomorrowStr, ignoreCase = true) }

                val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
                val lastNotifiedDate = prefs.getString("last_notified_service_date", null)
                
                // We want to notify once per day. Format YYYY-MM-DD
                val currentCalendar = Calendar.getInstance()
                val todayDateStr = "${currentCalendar.get(Calendar.YEAR)}-${currentCalendar.get(Calendar.MONTH)}-${currentCalendar.get(Calendar.DAY_OF_MONTH)}"

                if (lastNotifiedDate != todayDateStr) {
                    var title = ""
                    var message = ""
                    if (todayService != null) {
                        title = "Hoje tem ${todayService.title}"
                        message = "Não perca nosso encontro às ${todayService.time}!"
                    } else if (tomorrowService != null) {
                        title = "Amanhã é Dia de ${tomorrowService.title}"
                        message = "Prepare-se para o nosso culto às ${tomorrowService.time}!"
                    }

                    if (title.isNotEmpty()) {
                        NotificationHelper.showNotification(
                            context,
                            title,
                            message
                        )
                        prefs.edit().putString("last_notified_service_date", todayDateStr).apply()
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("ServiceAlertWorker", "Error fetching services agenda", e)
            return Result.failure()
        }
    }
}
