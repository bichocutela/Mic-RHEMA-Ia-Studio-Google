package com.aistudio.micrhema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean
) = composed {
    this
}
