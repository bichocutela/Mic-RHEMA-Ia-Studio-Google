package com.aistudio.micrhema

import android.net.Uri
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/** Tratamento monocromático inspirado em fotografia de jornal para imagens editoriais. */
object BibleNewsVisuals {
    val monochromeFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )

    /**
     * Evita que dois cartões diferentes reutilizem exatamente a mesma imagem na
     * mesma sessão. A primeira matéria mantém a imagem cadastrada; as seguintes
     * com URL duplicada recebem um fallback exclusivo e estável pelo próprio ID.
     */
    private val imageOwners = ConcurrentHashMap<String, Int>()

    private fun claimImage(url: String, newsId: Int): String? {
        if (url.isBlank()) return null
        val key = url.substringBefore('#').trim()
        val owner = imageOwners.putIfAbsent(key, newsId)
        return if (owner == null || owner == newsId) url else null
    }

    /**
     * Imagens aprovadas pelo ADM ou pelo catálogo têm prioridade. Para matérias
     * sensíveis, não geramos automaticamente uma cena externa: a UI usa o cartão
     * editorial seguro. Em pautas leves, o fallback é exclusivo por notícia para
     * impedir que duas matérias acabem exibindo a mesma ilustração.
     */
    fun imageUrlFor(news: BibleNews): String {
        val directUrl = news.imageUrl.trim()
        claimImage(directUrl, news.id)?.let { return it }

        val catalogUrl = BibleNewsData.newsList
            .firstOrNull { it.id == news.id }
            ?.imageUrl
            ?.trim()
            .orEmpty()
        claimImage(catalogUrl, news.id)?.let { return it }

        if (news.contentWarning.isNotBlank() || news.intensity >= 3) return ""

        val identity = listOf(
            news.storyKey,
            news.book,
            news.chapter.toString(),
            news.verse.toString(),
            news.title,
            news.tags.take(3).joinToString(" ")
        ).filter { it.isNotBlank() }.joinToString(", ")
        val seed = abs(news.id.toLong()).coerceAtLeast(1L)
        val prompt = "respectful symbolic black and white newspaper illustration, unique composition for $identity, biblical setting, non graphic, no violence, no text, editorial photography"
        return "https://image.pollinations.ai/prompt/${Uri.encode(prompt)}?width=900&height=520&nologo=true&seed=$seed"
    }
}
