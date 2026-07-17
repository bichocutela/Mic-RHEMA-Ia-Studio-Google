package com.aistudio.micrhema

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class DevotionalReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Ensure data is loaded if running in background without activity
        if (devotionalsState.isEmpty()) {
            loadDevotionalsFromJson(context)
        }
        
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayDevotional = if (devotionalsState.isNotEmpty()) {
            devotionalsState[dayOfYear % devotionalsState.size]
        } else {
            null
        }
        
        val title = "Devocional Diário: ${todayDevotional?.title ?: "Nova Palavra"}"
        val message = todayDevotional?.verse ?: "Tempo para o seu devocional de hoje! Venha se fortalecer com a Palavra."
        
        NotificationHelper.showNotification(
            context = context,
            title = title,
            message = message
        )
        return Result.success()
    }
}
