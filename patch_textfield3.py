import re

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'r') as f:
    content = f.read()

content = content.replace("@Composable (() -> Unit)?", "(@Composable () -> Unit)?")

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'w') as f:
    f.write(content)
