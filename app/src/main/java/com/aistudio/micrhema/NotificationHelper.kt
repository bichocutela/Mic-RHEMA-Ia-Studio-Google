package com.aistudio.micrhema

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "micrhema_notifications"
    private const val CHANNEL_NAME = "Notificações MIC Rhema"
    private const val CHANNEL_DESCRIPTION = "Alertas de novos devocionais, eventos e notícias"
    const val EXTRA_NOTIFICATION_DESTINATION = "notification_destination"
    private var notificationId = 100

    fun scheduleDailyReminder(context: Context) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DevotionalReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyDevotionalReminder",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    fun scheduleServiceAlert(context: Context) {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<ServiceAlertWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ServiceAlertWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun scheduleDevotionalSync(context: Context) {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<DevotionalSyncWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DevotionalSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun scheduleDailyNews(context: Context) {
        val now = Calendar.getInstance()
        val nextNoon = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = (nextNoon.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        val request = PeriodicWorkRequestBuilder<MiddayNewsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MiddayNewsWorker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleMediaSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<MediaUpdateWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MediaUpdateWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleIbrContentSync(context: Context) {
        val request = androidx.work.PeriodicWorkRequestBuilder<IbrContentWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "IbrContentWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleAppUpdateCheck(context: Context) {
        val request = androidx.work.PeriodicWorkRequestBuilder<AppUpdateWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AppUpdateWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun applyAdminNotificationPolicy(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            scheduleDailyReminder(context)
            scheduleDevotionalSync(context)
            scheduleServiceAlert(context)
            scheduleDailyNews(context)
            scheduleMediaSync(context)
            scheduleIbrContentSync(context)
            scheduleAppUpdateCheck(context)
        } else {
            workManager.cancelUniqueWork("DailyDevotionalReminder")
            workManager.cancelUniqueWork("DevotionalSyncWorker")
            workManager.cancelUniqueWork("ServiceAlertWorker")
            workManager.cancelUniqueWork("MiddayNewsWorker")
            workManager.cancelUniqueWork("MediaUpdateWorker")
            workManager.cancelUniqueWork("IbrContentWorker")
            workManager.cancelUniqueWork("AppUpdateWorker")
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Reconfirma os tópicos sempre que o app abre. onNewToken continua sendo
     * usado, mas não dependemos mais exclusivamente dele para receber push.
     */
    fun ensureMessagingReady(context: Context) {
        createNotificationChannel(context)
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.subscribeToTopic("all_users")
            messaging.subscribeToTopic("devocionais")
            if (isIbrMember(context)) {
                messaging.subscribeToTopic("ibr_users")
            } else {
                messaging.unsubscribeFromTopic("ibr_users")
            }
        }.onFailure { error ->
            android.util.Log.w("NotificationHelper", "Não foi possível reconciliar tópicos FCM agora", error)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    enum class Category {
        GENERAL,
        DAILY_DEVOTIONAL,
        EVENTS,
        COURSES,
        SERMONS,
        MEDIA,
        NEXT_SERVICE,
        DAILY_NEWS,
        IBR_CONTENT,
        CONTENT_UPDATES
    }

    fun categoryFrom(value: String?): Category {
        return when (value?.trim()?.lowercase()) {
            "devotional", "devocional", "daily_devotional" -> Category.DAILY_DEVOTIONAL
            "event", "evento", "events" -> Category.EVENTS
            "course", "curso", "courses" -> Category.COURSES
            "sermon", "pregacao", "pregação", "sermons" -> Category.SERMONS
            "media", "midia", "mídia", "audio", "video" -> Category.MEDIA
            "service", "culto", "next_service" -> Category.NEXT_SERVICE
            "news", "noticia", "notícia", "daily_news" -> Category.DAILY_NEWS
            "ibr", "ibr_content", "course_ibr", "aula" -> Category.IBR_CONTENT
            "content", "conteudo", "conteúdo", "content_updates" -> Category.CONTENT_UPDATES
            else -> Category.GENERAL
        }
    }

    private fun smallIconFor(category: Category): Int = when (category) {
        Category.DAILY_DEVOTIONAL -> R.drawable.ic_notif_devotional
        Category.EVENTS -> R.drawable.ic_notif_event
        Category.COURSES -> R.drawable.ic_notif_course
        Category.SERMONS -> R.drawable.ic_notif_sermon
        Category.MEDIA -> R.drawable.ic_notif_media
        Category.NEXT_SERVICE -> R.drawable.ic_notif_service
        Category.DAILY_NEWS -> R.drawable.ic_notif_news
        Category.IBR_CONTENT -> R.drawable.ic_notif_ibr
        Category.GENERAL, Category.CONTENT_UPDATES -> R.drawable.ic_notification
    }

    private fun appLogoBitmap(context: Context) = runCatching {
        ContextCompat.getDrawable(context, R.drawable.img_rhema_logo)?.toBitmap(width = 128, height = 128)
    }.getOrNull()

    private fun localUserSettings(context: Context): UserSettings {
        val prefs = context.getSharedPreferences("micrhema_user_settings", Context.MODE_PRIVATE)
        val json = prefs.getString("settings_json", null)
        return if (json.isNullOrBlank()) currentSettingsState.value
        else runCatching { Gson().fromJson(json, UserSettings::class.java) }.getOrElse { currentSettingsState.value }
    }

    private fun adminNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("micrhema_admin_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("notificationsEnabled", adminAppSettingsState.value.notificationsEnabled)
    }

    fun rememberMediaIds(context: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val known = prefs.getStringSet("notified_media_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        known.addAll(ids)
        if (known.size > 100) {
            val trimmed = known.toList().sortedByDescending { it.toLongOrNull() ?: 0L }.take(100).toSet()
            known.clear()
            known.addAll(trimmed)
        }
        prefs.edit().putStringSet("notified_media_ids", known).apply()
    }

    fun isIbrMember(context: Context): Boolean {
        return loggedInMemberState.value?.isIbr == true ||
            context.getSharedPreferences("micrhema_member_session", Context.MODE_PRIVATE).getBoolean("isIbr", false)
    }

    private fun isAllowed(context: Context, category: Category): Boolean {
        if (!adminNotificationsEnabled(context)) return false
        val settings = localUserSettings(context)
        if (!settings.notificationsEnabled) return false
        return when (category) {
            Category.DAILY_DEVOTIONAL -> settings.notifDailyDevotional
            Category.EVENTS -> settings.notifEvents
            Category.COURSES -> settings.notifNewCourses && isIbrMember(context)
            Category.SERMONS -> settings.notifNewSermons
            Category.MEDIA -> settings.notifNewMedia
            Category.NEXT_SERVICE -> settings.notifNextService
            Category.DAILY_NEWS -> settings.notifDailyNews
            Category.IBR_CONTENT -> settings.notifIbrContent && isIbrMember(context)
            Category.GENERAL, Category.CONTENT_UPDATES -> true
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        category: Category = Category.GENERAL,
        respectPreferences: Boolean = true,
        destinationRoute: String? = null,
        destinationDocumentId: String? = null
    ) {
        if (respectPreferences && !isAllowed(context, category)) return
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val newsId = destinationRoute
            ?.takeIf { it.startsWith("news_detail/") }
            ?.substringAfter("news_detail/")
            ?.substringBefore('/')
            ?.toIntOrNull()

        val intent = if (newsId != null && newsId > 0) {
            android.content.Intent(context, NewsNotificationActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NewsNotificationActivity.EXTRA_NEWS_ID, newsId)
                destinationDocumentId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(NewsNotificationActivity.EXTRA_DOCUMENT_ID, it)
                }
            }
        } else {
            android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                destinationRoute?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_NOTIFICATION_DESTINATION, it) }
            }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconFor(category))
            .setColor(ContextCompat.getColor(context, android.R.color.black))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        appLogoBitmap(context)?.let { builder.setLargeIcon(it) }

        try {
            NotificationManagerCompat.from(context).notify(notificationId++, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Permissão de notificação não concedida", e)
        }
    }
}
