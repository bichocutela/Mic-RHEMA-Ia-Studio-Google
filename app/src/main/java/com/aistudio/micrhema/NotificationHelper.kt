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
    private const val CHANNEL_ID = "micrhema_reminders_v2"
    private const val CHANNEL_NAME = "Notificações MIC Rhema"
    private const val CHANNEL_DESCRIPTION = "Alertas de novos devocionais, eventos e notícias"
    private const val DEDUPE_PREFS = "micrhema_notification_dedupe"
    private const val DEDUPE_EVENTS_KEY = "recent_events"
    private const val DEFAULT_DEDUPE_TTL_MS = 6L * 60L * 60L * 1000L
    const val EXTRA_NOTIFICATION_DESTINATION = "notification_destination"

    fun scheduleDailyReminder(context: Context) = BackgroundNotificationCoordinator.reconcile(context)
    fun scheduleServiceAlert(context: Context) = BackgroundNotificationCoordinator.reconcile(context)
    fun scheduleDevotionalSync(context: Context) = SilentContentSyncManager.enqueueImmediate(context)
    fun scheduleDailyNews(context: Context) = BackgroundNotificationCoordinator.reconcile(context)
    fun scheduleMediaSync(context: Context) = BackgroundNotificationCoordinator.reconcile(context)
    fun scheduleIbrContentSync(context: Context) = BackgroundNotificationCoordinator.reconcile(context)
    fun scheduleAppUpdateCheck(context: Context) = BackgroundNotificationCoordinator.reconcile(context)

    fun applyAdminNotificationPolicy(context: Context, enabled: Boolean) {
        context.getSharedPreferences("micrhema_admin_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notificationsEnabled", enabled)
            .apply()
        BackgroundNotificationCoordinator.reconcile(context.applicationContext, enabled)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(NotificationChannel("micrhema_content_v2", "Novos conteúdos", NotificationManager.IMPORTANCE_DEFAULT))
            notificationManager.createNotificationChannel(NotificationChannel("micrhema_updates_v2", "Atualizações do aplicativo", NotificationManager.IMPORTANCE_LOW))
            notificationManager.createNotificationChannel(NotificationChannel("micrhema_prayer_v2", "Pedidos e respostas de oração", NotificationManager.IMPORTANCE_DEFAULT))
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
        CONTENT_UPDATES,
        PRAYER
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
            "prayer", "oracao", "oração", "prayer_response" -> Category.PRAYER
            "content", "conteudo", "conteúdo", "content_updates", "app_update" -> Category.CONTENT_UPDATES
            else -> Category.GENERAL
        }
    }

    private fun smallIconFor(kind: NotificationKind): Int = when (kind) {
        NotificationKind.DEVOTIONAL -> R.drawable.ic_notif_devotional
        NotificationKind.DISCIPULADO -> R.drawable.ic_notif_discipulado
        NotificationKind.PLAN -> R.drawable.ic_notif_plan
        NotificationKind.COURSE -> R.drawable.ic_notif_course
        NotificationKind.LESSON -> R.drawable.ic_notif_ibr
        NotificationKind.SERMON -> R.drawable.ic_notif_sermon
        NotificationKind.MEDIA -> R.drawable.ic_notif_media
        NotificationKind.VIDEO -> R.drawable.ic_notif_video
        NotificationKind.AUDIO -> R.drawable.ic_notif_audio
        NotificationKind.BOOK -> R.drawable.ic_notif_book
        NotificationKind.ALBUM -> R.drawable.ic_notif_album
        NotificationKind.EVENT -> R.drawable.ic_notif_event
        NotificationKind.SERVICE -> R.drawable.ic_notif_service
        NotificationKind.NEWS -> R.drawable.ic_notif_news
        NotificationKind.PRAYER -> R.drawable.ic_notif_prayer
        NotificationKind.UPDATE -> R.drawable.ic_notif_update
        NotificationKind.BIBLE -> R.drawable.ic_notif_book
        NotificationKind.LIVE -> R.drawable.ic_notif_video
        NotificationKind.MEMBER -> R.drawable.ic_notif_member
        NotificationKind.GENERAL -> R.drawable.ic_notification
    }

    private fun notificationColor(context: Context): Int {
        val dark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (dark) android.graphics.Color.rgb(144, 202, 249) else android.graphics.Color.rgb(21, 101, 192)
    }

    private fun notificationIconBitmap(context: Context, kind: NotificationKind) = runCatching {
        ContextCompat.getDrawable(context, smallIconFor(kind))?.mutate()?.apply {
            setTint(notificationColor(context))
        }?.toBitmap(width = 128, height = 128)
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

    fun rememberMediaIds(context: Context, ids: Collection<String>, collection: String = "") {
        if (ids.isEmpty() || collection.isBlank()) return
        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val known = prefs.getStringSet("notified_media_keys_v2", emptySet()).orEmpty().toMutableSet()
        known.addAll(ids.map { "$collection:$it" })
        prefs.edit().putStringSet("notified_media_keys_v2", known).apply()
    }

    /**
     * Reserva uma chave semântica de notificação por algumas horas. Fontes diferentes
     * (FCM, WorkManager etc.) podem usar a mesma chave e somente a primeira exibirá o aviso.
     */
    @Synchronized
    fun claimNotificationEvent(
        context: Context,
        eventKey: String,
        ttlMillis: Long = DEFAULT_DEDUPE_TTL_MS
    ): Boolean {
        val key = eventKey.trim()
        if (key.isBlank()) return true

        val prefs = context.getSharedPreferences(DEDUPE_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val validEntries = prefs.getStringSet(DEDUPE_EVENTS_KEY, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separator = entry.lastIndexOf('|')
                if (separator <= 0) return@mapNotNull null
                val storedKey = entry.substring(0, separator)
                val timestamp = entry.substring(separator + 1).toLongOrNull() ?: return@mapNotNull null
                if (now - timestamp <= ttlMillis) storedKey to timestamp else null
            }
            .toMutableList()

        if (validEntries.any { it.first == key }) {
            prefs.edit().putStringSet(
                DEDUPE_EVENTS_KEY,
                validEntries.sortedByDescending { it.second }.take(160).map { "${it.first}|${it.second}" }.toSet()
            ).apply()
            return false
        }

        validEntries.add(key to now)
        prefs.edit().putStringSet(
            DEDUPE_EVENTS_KEY,
            validEntries.sortedByDescending { it.second }.take(160).map { "${it.first}|${it.second}" }.toSet()
        ).apply()
        return true
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
            Category.PRAYER, Category.GENERAL, Category.CONTENT_UPDATES -> true
        }
    }

    private fun isDeprecatedRealtimeDuplicate(title: String): Boolean {
        return title == "Novo vídeo em Mídia" || title == "Novo áudio em Mídia"
    }

    @Synchronized
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        category: Category = Category.GENERAL,
        respectPreferences: Boolean = true,
        destinationRoute: String? = null,
        destinationDocumentId: String? = null,
        notificationData: Map<String, String> = emptyMap()
    ): Boolean {
        // Esses dois títulos pertenciam a listeners locais legados que duplicavam o FCM oficial.
        // O fallback em segundo plano usa títulos próprios e continua funcionando normalmente.
        if (isDeprecatedRealtimeDuplicate(title)) return false
        if (respectPreferences && !isAllowed(context, category)) return false
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val now = NotificationTiming.now()
        val slot = NotificationTiming.slot(category.name, notificationData["collection"].orEmpty(), destinationRoute.orEmpty())
        val expired = notificationData["expiresAt"]?.toLongOrNull()?.let { it < System.currentTimeMillis() } == true
        if (expired && NotificationTiming.isReminder(slot)) return false
        if (NotificationTiming.isReminder(slot)) {
            if (now.hour != slot) return false
            if (notificationData["notificationDate"]?.let { it != now.toLocalDate().toString() } == true) return false
        }
        val eventKey = when {
            NotificationTiming.isReminder(slot) -> "daily:$slot:${now.toLocalDate()}"
            notificationData["collection"]?.startsWith("conteudos_") == true && !destinationDocumentId.isNullOrBlank() -> "content:${notificationData["collection"]}:$destinationDocumentId"
            !notificationData["chapterId"].isNullOrBlank() -> "lesson:${notificationData["courseId"] ?: destinationDocumentId}:${notificationData["chapterId"]}"
            !notificationData["collection"].isNullOrBlank() && !destinationDocumentId.isNullOrBlank() -> "content:${notificationData["collection"]}:$destinationDocumentId"
            notificationData["eventKey"].isNullOrBlank().not() -> notificationData.getValue("eventKey")
            !destinationDocumentId.isNullOrBlank() -> "content:${notificationData["collection"] ?: category.name}:$destinationDocumentId"
            else -> "notice:${category.name}:$title:$message"
        }
        val effectiveSlot = slot ?: if (category != Category.PRAYER && (now.hour < 7 || now.hour >= 22)) 9 else null
        if (effectiveSlot != null && now.hour != effectiveSlot) {
            val data = androidx.work.Data.Builder().putString("title", title).putString("message", message)
                .putString("category", category.name).putBoolean("respect", respectPreferences)
                .putString("route", destinationRoute).putString("documentId", destinationDocumentId)
                .putString("data", Gson().toJson(notificationData)).build()
            val request = androidx.work.OneTimeWorkRequestBuilder<PendingNotificationWorker>()
                .setInitialDelay(NotificationTiming.delayMillis(effectiveSlot, now), TimeUnit.MILLISECONDS).setInputData(data).build()
            WorkManager.getInstance(context).enqueueUniqueWork("PendingNotice:$eventKey", androidx.work.ExistingWorkPolicy.REPLACE, request)
            return true
        }
        val contentKeys: List<String> = runCatching {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(notificationData["contentKeys"] ?: "[]", type)
        }.getOrDefault(emptyList())
        if (contentKeys.isNotEmpty()) {
            val fresh = contentKeys.map { claimNotificationEvent(context, it, TimeUnit.DAYS.toMillis(8)) }.any { it }
            if (!fresh) return false
        } else if (!claimNotificationEvent(context, eventKey, TimeUnit.DAYS.toMillis(8))) return false

        val navigation = NotificationNavigation.resolve(buildMap {
            putAll(notificationData)
            putIfAbsent("category", category.name.lowercase())
            destinationRoute?.takeIf(String::isNotBlank)?.let { put("destination", it) }
            destinationDocumentId?.takeIf(String::isNotBlank)?.let { put("documentId", it) }
        })
        val route = navigation.route
        val grouped = slot == 14 || slot == 15 || slot == 17 || slot == 19
        val id = ((if (grouped) "digest:$slot:${now.toLocalDate()}" else eventKey).hashCode() and Int.MAX_VALUE)
        val newsId = route
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
                putExtra(EXTRA_NOTIFICATION_DESTINATION, route)
                destinationDocumentId?.let { putExtra("notification_document_id", it) }
            }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            id,
            intent.apply { data = android.net.Uri.parse("micrhema://notification/$id") },
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = when {
            destinationRoute == "about" -> "micrhema_updates_v2"
            category == Category.PRAYER -> "micrhema_prayer_v2"
            grouped -> "micrhema_content_v2"
            else -> CHANNEL_ID
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconFor(navigation.kind))
            .setColor(notificationColor(context))
            .setOnlyAlertOnce(true)
            .setGroup("micrhema-$channelId")
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (grouped) {
            val prefs = context.getSharedPreferences("micrhema_notice_digests", Context.MODE_PRIVATE)
            val digestKey = "$slot:${now.toLocalDate()}"
            val lines = prefs.getStringSet(digestKey, emptySet()).orEmpty().toMutableSet()
            lines.add("$title — $message")
            // The digest state is only needed for today's notifications.
            prefs.edit().clear().putStringSet(digestKey, lines.toList().takeLast(10).toSet()).apply()
            if (lines.size > 1) {
                val style = NotificationCompat.InboxStyle().setBigContentTitle("${lines.size} novidades no MIC Rhema")
                lines.forEach { style.addLine(it) }
                builder.setContentTitle("${lines.size} novidades no MIC Rhema").setContentText(lines.joinToString(" • ")).setStyle(style)
                val summaryIntent = android.content.Intent(context, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = android.net.Uri.parse("micrhema://notification/$id")
                    putExtra(EXTRA_NOTIFICATION_DESTINATION, destinationRoute ?: route)
                }
                builder.setContentIntent(android.app.PendingIntent.getActivity(context, id, summaryIntent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT))
            }
        }
        notificationIconBitmap(context, navigation.kind)?.let { builder.setLargeIcon(it) }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
            return true
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Permissão de notificação não concedida", e)
            return false
        }
    }
}
