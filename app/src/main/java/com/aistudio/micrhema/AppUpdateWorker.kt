package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // ADM logado recebe o aviso imediato da Release via FCM. Não repetimos o mesmo
        // fluxo no verificador silencioso usado pelos usuários comuns.
        if (loggedInMemberState.value?.isAdmin == true) return Result.success()

        return when (val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)) {
            is UpdateResult.Success -> {
                val info = result.info
                if (!info.updateAvailable) return Result.success()

                val prefs = applicationContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                val now = System.currentTimeMillis()
                val lastNotifiedAt = prefs.getLong(KEY_LAST_NOTIFIED_AT, 0L)
                val elapsed = now - lastNotifiedAt

                // O WorkManager consulta a versão a cada 12h, mas um usuário comum recebe
                // no máximo um aviso a cada 24h. Se várias versões surgirem nesse intervalo,
                // a próxima consulta elegível mostra apenas a release mais recente do GitHub.
                if (lastNotifiedAt > 0L && elapsed < MIN_NOTIFICATION_INTERVAL_MS) {
                    Log.d("AppUpdateWorker", "Atualização ${info.latestVersion} encontrada silenciosamente; aviso diário ainda em espera.")
                    return Result.success()
                }

                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = "Tem atualização nova!",
                    message = "O MIC Rhema ${info.latestVersion} já está disponível.",
                    category = NotificationHelper.Category.CONTENT_UPDATES,
                    respectPreferences = true,
                    destinationRoute = Screen.About.route
                )
                prefs.edit()
                    .putString(KEY_LAST_NOTIFIED_VERSION, info.latestVersion)
                    .putLong(KEY_LAST_NOTIFIED_AT, now)
                    .apply()
                Result.success()
            }
            is UpdateResult.Error -> Result.retry()
        }
    }

    companion object {
        private const val PREFS_NAME = "micrhema_update_notifications"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val KEY_LAST_NOTIFIED_AT = "last_notified_at"
        private val MIN_NOTIFICATION_INTERVAL_MS = TimeUnit.HOURS.toMillis(24)
    }
}
