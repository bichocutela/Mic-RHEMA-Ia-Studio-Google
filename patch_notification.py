import re

with open('app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt', 'r') as f:
    content = f.read()

new_method = """
    fun scheduleDevotionalSync(context: Context) {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<DevotionalSyncWorker>(1, TimeUnit.HOURS)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DevotionalSyncWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
"""

content = content.replace(
    'fun createNotificationChannel(context: Context) {',
    new_method + '\n    fun createNotificationChannel(context: Context) {'
)

with open('app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt', 'w') as f:
    f.write(content)
