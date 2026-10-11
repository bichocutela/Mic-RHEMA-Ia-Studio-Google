package com.aistudio.micrhema

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PendingNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val category = runCatching { NotificationHelper.Category.valueOf(inputData.getString("category").orEmpty()) }.getOrNull() ?: return Result.success()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        val data: Map<String, String> = Gson().fromJson(inputData.getString("data") ?: "{}", mapType)
        NotificationHelper.showNotification(
            applicationContext, inputData.getString("title").orEmpty(), inputData.getString("message").orEmpty(), category,
            inputData.getBoolean("respect", true), inputData.getString("route"), inputData.getString("documentId"), data
        )
        return Result.success()
    }
}
