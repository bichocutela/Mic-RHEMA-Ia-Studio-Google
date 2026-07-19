import re

with open('app/src/main/java/com/aistudio/micrhema/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'NotificationHelper.scheduleDailyReminder(context)',
    'NotificationHelper.scheduleDailyReminder(context)\n        try {\n            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("devotionals")\n        } catch(e: Exception) {}'
)

with open('app/src/main/java/com/aistudio/micrhema/MainActivity.kt', 'w') as f:
    f.write(content)
