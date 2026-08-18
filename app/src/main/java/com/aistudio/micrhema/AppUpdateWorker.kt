package com.aistudio.micrhema

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)) {
            is UpdateResult.Success -> {
                val info = result.info
                if (info.updateAvailable) {
                    val prefs = applicationContext.getSharedPreferences(
                        "micrhema_update_notifications",
                        Context.MODE_PRIVATE
                    )
                    val lastNotifiedVersion = prefs.getString("last_notified_version", "")

                    if (lastNotifiedVersion != info.latestVersion) {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            title = "Tem atualização nova!",
                            message = "O MIC Rhema ${info.latestVersion} já está disponível.",
                            category = NotificationHelper.Category.CONTENT_UPDATES,
                            respectPreferences = true,
                            destinationRoute = Screen.About.route
                        )
                        prefs.edit()
                            .putString("last_notified_version", info.latestVersion)
                            .apply()
                    }
                }
                Result.success()
            }
            is UpdateResult.Error -> Result.retry()
        }
    }
}
