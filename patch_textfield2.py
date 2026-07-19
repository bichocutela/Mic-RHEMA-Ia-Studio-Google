import re

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'r') as f:
    content = f.read()

old_fun = """fun GlassTextField(
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

new_fun = """fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {"""
content = content.replace(old_fun, new_fun)

old_call = """        singleLine = singleLine,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors("""

new_call = """        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors("""
content = content.replace(old_call, new_call)

with open('app/src/main/java/com/aistudio/micrhema/GlassComponents.kt', 'w') as f:
    f.write(content)
