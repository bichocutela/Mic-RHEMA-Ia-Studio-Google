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
        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val devJson = prefs.getString("devotionalsState", null)
        val gson = com.google.gson.Gson()
        
        val devotionals = if (devJson != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<Devotional>>() {}.type
            gson.fromJson<List<Devotional>>(devJson, type)
        } else {
            emptyList()
        }
        
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val todayDevotional = if (devotionals.isNotEmpty()) {
            devotionals[dayOfYear % devotionals.size]
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
