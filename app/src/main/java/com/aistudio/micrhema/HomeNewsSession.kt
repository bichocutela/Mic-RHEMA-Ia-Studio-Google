package com.aistudio.micrhema

import kotlin.random.Random

/**
 * Mantém a seleção da Home estável durante a sessão atual do processo, mas cria
 * uma ordem nova na próxima abertura real do aplicativo. Assim recomposições,
 * rotação de tela e navegação interna não ficam trocando os cartões sozinhas.
 */
object HomeNewsSession {
    private val sessionSeed: Int = (
        System.nanoTime() xor
            System.currentTimeMillis() xor
            Runtime.getRuntime().freeMemory()
        ).hashCode()

    fun select(news: List<BibleNews>, limit: Int = 5): List<BibleNews> {
        if (limit <= 0 || news.isEmpty()) return emptyList()
        return news
            .distinctBy { it.id }
            .shuffled(Random(sessionSeed))
            .take(limit)
    }
}
