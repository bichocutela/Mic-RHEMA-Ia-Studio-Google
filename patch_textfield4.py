import re

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'r') as f:
    content = f.read()

old_fun = """    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {"""

new_fun = """    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    colors: androidx.compose.material3.TextFieldColors? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {"""
content = content.replace(old_fun, new_fun)

old_call = """        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent, // Border is handled by glassEffect
            focusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )"""

new_call = """        maxLines = maxLines,
        colors = colors ?: OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )"""
content = content.replace(old_call, new_call)

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'w') as f:
    f.write(content)
