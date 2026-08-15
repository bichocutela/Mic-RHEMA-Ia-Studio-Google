package com.aistudio.micrhema

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/** Tratamento monocromático inspirado em fotografia de jornal para imagens editoriais. */
object BibleNewsVisuals {
    val monochromeFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )
}
