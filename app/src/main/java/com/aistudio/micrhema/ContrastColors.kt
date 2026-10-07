package com.aistudio.micrhema

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Chooses the foreground with the highest contrast on an opaque surface. */
internal fun contrastingContentColor(background: Color): Color {
    val luminance = background.luminance()
    val blackContrast = (luminance + 0.05f) / 0.05f
    val whiteContrast = 1.05f / (luminance + 0.05f)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}
