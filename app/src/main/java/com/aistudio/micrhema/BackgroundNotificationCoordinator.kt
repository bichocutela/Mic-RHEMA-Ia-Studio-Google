package com.aistudio.micrhema

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Fonte única dos agendamentos locais de notificação.
 *
 * Importante: este coordenador não depende de uma tela Compose estar aberta.
 * Ele é chamado pela Application, por mudanças nas Configurações e pelo receiver
 * de reinicialização/alteração de horário. Assim o WorkManager continua cuidando
 * dos avisos quando o app sai da tela ou o processo é encerrado normalmente.
 */
object BackgroundNotificationCoordinator {
    private const val PREFS = "micrhema_notification_schedule"
    private const val KEY_SCHEMA = "schema"
    private const val SCHEMA = 4

    private const val WORK_DEVOTIONAL = "DailyDevotionalReminderV3"
    private const val WORK_NEWS = "MiddayNewsWorkerV3"
    private const val WORK_SERVICE_PLANNER = "ServiceAlertPlannerV3"
    private const val WORK_SERVICE_BOOTSTRAP = "ServiceAlertPlannerBootstrapV3"
    private const val WORK_MEDIA = "MediaUpdateWorker"
    private const val WORK_IBR = "IbrContentWorker"
    private const val WORK_APP_UPDATE = "AppUpdateWorkerV2"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        NotificationHelper.createNotificationChannel(appContext)
        NotificationHelper.ensureMessagingReady(appContext)
        reconcile(appContext)
    }

    fun reanchorAfterClockChange(context: Context) {
        val appContext = context.applicationContext
        val wm = WorkManager.getInstance(appContext)
        wm.cancelUniqueWork(WORK_DEVOTIONAL)
        wm.cancelUniqueWork(WORK_NEWS)
        wm.cancelUniqueWork(WORK_SERVICE_PLANNER)
        wm.cancelUniqueWork(WORK_SERVICE_BOOTSTRAP)
        ServiceAlertWorker.cancelScheduledServiceMoments(appContext, wm)
        reconcile(appContext)
    }

    fun reconcile(context: Context, adminEnabledOverride: Boolean? = null) {
        val appContext = context.applicationContext
        val wm = WorkManager.getInstance(appContext)
        migrateLegacySchedulesIfNeeded(appContext, wm)

        val adminEnabled = adminEnabledOverride ?: appContext
            .getSharedPreferences("micrhema_admin_settings", Context.MODE_PRIVATE)
            .getBoolean("notificationsEnabled", true)
        val settings = UserSettingsManager.getStoredSettings(appContext)
        val globallyEnabled = adminEnabled && settings.notificationsEnabled

        reconcileDailyDevotional(wm, globallyEnabled && settings.notifDailyDevotional)
        reconcileDailyNews(wm, globallyEnabled && settings.notifDailyNews)
        reconcileServiceAlerts(appContext, wm, globallyEnabled && settings.notifNextService)
        reconcileMedia(wm, globallyEnabled && settings.notifNewMedia)
        reconcileIbr(wm, globallyEnabled && settings.notifIbrContent && NotificationHelper.isIbrMember(appContext))
        scheduleAppUpdate(wm)
    }

    private fun reconcileDailyDevotional(wm: WorkManager, enabled: Boolean) {
        if (!enabled) {
            wm.cancelUniqueWork(WORK_DEVOTIONAL)
            return
        }
        val request = PeriodicWorkRequestBuilder<DevotionalReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(8, 0), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_DEVOTIONAL, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun reconcileDailyNews(wm: WorkManager, enabled: Boolean) {
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NEWS)
            return
        }
        val request = PeriodicWorkRequestBuilder<MiddayNewsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(12, 0), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NEWS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun reconcileServiceAlerts(context: Context, wm: WorkManager, enabled: Boolean) {
        if (!enabled) {
            wm.cancelUniqueWork(WORK_SERVICE_PLANNER)
            wm.cancelUniqueWork(WORK_SERVICE_BOOTSTRAP)
            ServiceAlertWorker.cancelScheduledServiceMoments(context, wm)
            return
        }

        val periodic = PeriodicWorkRequestBuilder<ServiceAlertWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_SERVICE_PLANNER, ExistingPeriodicWorkPolicy.KEEP, periodic)

        val bootstrap = OneTimeWorkRequestBuilder<ServiceAlertWorker>()
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniqueWork(WORK_SERVICE_BOOTSTRAP, ExistingWorkPolicy.REPLACE, bootstrap)
    }

    private fun reconcileMedia(wm: WorkManager, enabled: Boolean) {
        if (!enabled) {
            wm.cancelUniqueWork(WORK_MEDIA)
            return
        }
        val request = PeriodicWorkRequestBuilder<MediaUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_MEDIA, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun reconcileIbr(wm: WorkManager, enabled: Boolean) {
        if (!enabled) {
            wm.cancelUniqueWork(WORK_IBR)
            return
        }
        val request = PeriodicWorkRequestBuilder<IbrContentWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_IBR, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun scheduleAppUpdate(wm: WorkManager) {
        // A consulta de versão acontece em segundo plano, sem incomodar o usuário.
        // O primeiro ciclo só roda depois de 12h e então permanece nessa cadência.
        val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_APP_UPDATE, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun migrateLegacySchedulesIfNeeded(context: Context, wm: WorkManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_SCHEMA, 0) == SCHEMA) return

        listOf(
            "DailyDevotionalReminder",
            "MiddayNewsWorker",
            "ServiceAlertWorker",
            "DevotionalSyncWorker",
            "AppUpdateWorker"
        ).forEach(wm::cancelUniqueWork)
        ServiceAlertWorker.cancelScheduledServiceMoments(context, wm)
        prefs.edit().putInt(KEY_SCHEMA, SCHEMA).apply()
    }

    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (due.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
