import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        label = "card_elevation"
    )

    GlassCard(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = elevation, shape = RoundedCornerShape(32.dp)),
    )"""

replacement = """    GlassCard(
        modifier = modifier
            .scale(scale),
    )"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
