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
     * Resolve a imagem mesmo quando o registro remoto substituiu a notícia local
     * com imageUrl vazio. O fallback continua sendo uma URL pública; a UI também
     * possui um cartão visual caso a rede não consiga carregá-la.
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

        val prompt = "black and white newspaper illustration, biblical story from ${news.book}, ${news.title}, dramatic editorial lighting, no text"
        return "https://image.pollinations.ai/prompt/${Uri.encode(prompt)}?width=900&height=520&nologo=true"
    }
}
