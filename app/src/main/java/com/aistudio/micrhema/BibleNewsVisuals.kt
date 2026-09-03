package com.aistudio.micrhema

import android.net.Uri
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/** Tratamento monocromático inspirado em fotografia de jornal para imagens editoriais. */
object BibleNewsVisuals {
    val monochromeFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )

    /**
     * Imagens aprovadas pelo ADM ou pelo catálogo têm prioridade. Para matérias
     * sensíveis, não geramos automaticamente uma cena externa: a UI usa o cartão
     * editorial seguro. Em pautas leves, o fallback continua disponível, mas com
     * prompt explicitamente simbólico e não gráfico.
     */
    fun imageUrlFor(news: BibleNews): String {
        val directUrl = news.imageUrl.trim()
        if (directUrl.isNotBlank()) return directUrl

        val catalogUrl = BibleNewsData.newsList
            .firstOrNull { it.id == news.id }
            ?.imageUrl
            ?.trim()
            .orEmpty()
        if (catalogUrl.isNotBlank()) return catalogUrl

        if (news.contentWarning.isNotBlank() || news.intensity >= 3) return ""

        val prompt = "respectful symbolic black and white newspaper illustration, biblical story from ${news.book}, ${news.title}, non graphic, no violence, no text"
        return "https://image.pollinations.ai/prompt/${Uri.encode(prompt)}?width=900&height=520&nologo=true"
    }
}
