import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

old_card = """    Card(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = elevation, shape = RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = colors,
        border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {"""

new_card = """    GlassCard(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = elevation, shape = RoundedCornerShape(32.dp)),
    ) {"""

content = content.replace(old_card, new_card)

old_click = """        Column(
            modifier = if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material.ripple.rememberRipple(),
                    onClick = onClick
                )
            } else {
                Modifier
            }
        ) {"""

new_click = """        Column(
            modifier = if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onClick
                )
            } else {
                Modifier
            }
        ) {"""
content = content.replace(old_click, new_click)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
