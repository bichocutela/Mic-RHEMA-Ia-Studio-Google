import re

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'r') as f:
    content = f.read()

old_fun = """fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {"""

new_fun = """fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {"""
content = content.replace(old_fun, new_fun)

old_call = """        modifier = modifier.glassEffect(shape = shape, isDark = isDark),
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,"""

new_call = """        modifier = modifier.glassEffect(shape = shape, isDark = isDark),
        placeholder = placeholder,
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,"""
content = content.replace(old_call, new_call)

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'w') as f:
    f.write(content)
