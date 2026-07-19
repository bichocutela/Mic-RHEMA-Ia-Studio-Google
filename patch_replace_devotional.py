import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """                item {
                    DailyDevotionalCard(
                        devotional = devotionalsState.firstOrNull(),
                        onReadFull = { activeDevotionalForReading = it },
                        onShare = { dev ->"""

replacement = """                item {
                    FirestoreDailyDevotional(
                        onReadFull = { activeDevotionalForReading = it },
                        onShare = { dev ->"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
