import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

content = content.replace(
    ', modifier = Modifier.testTag("like_button_${devotional.id}")',
    ''
)
content = content.replace(
    ',\n                    modifier = Modifier.testTag("like_button_${devotional.id}")',
    ''
)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
