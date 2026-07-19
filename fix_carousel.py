import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """                    modifier = Modifier
                        .width(280.dp)
                        .height(150.dp)
                        .scale(animatedScale)
                        .clickable { activeIndex = index }
                         Color(0xFFD4AF37) else Color(0xFF3B82F6)
                        ),"""

replacement = """                    modifier = Modifier
                        .width(280.dp)
                        .height(150.dp)
                        .scale(animatedScale)
                        .clickable { activeIndex = index },"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
