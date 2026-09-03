package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateListOf
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Mantém a identidade real dos documentos de Notícias Bíblicas sem alterar o
 * modelo público BibleNews, que continua usando Int para navegação e badges.
 *
 * Registros antigos criados pela PWA podem possuir IDs alfanuméricos. Nesses
 * casos geramos um Int estável apenas para a interface e preservamos o ID real
 * do Firestore neste mapa para editar, excluir e notificar corretamente.
 */
object BibleNewsDocumentIds {
    private val documentIdsByNewsId = ConcurrentHashMap<Int, String>()

    fun stableId(documentId: String, rawId: Any?): Int {
        val numeric = when (rawId) {
            is Number -> rawId.toInt()
            is String -> rawId.toIntOrNull()
            else -> null
        }?.takeIf { it > 0 }
            ?: documentId.toIntOrNull()?.takeIf { it > 0 }

        if (numeric != null) {
            register(numeric, documentId)
            return numeric
        }

        val hash = documentId.hashCode().toLong()
        val positive = abs(hash).coerceAtLeast(1L)
        val generated = (100_000L + (positive % 2_000_000_000L)).toInt()
        register(generated, documentId)
        return generated
    }

    fun register(newsId: Int, documentId: String) {
        if (newsId > 0 && documentId.isNotBlank()) {
            documentIdsByNewsId[newsId] = documentId
        }
    }

    fun documentIdFor(newsId: Int): String? = documentIdsByNewsId[newsId]

    fun forget(newsId: Int) {
        documentIdsByNewsId.remove(newsId)
    }
}

/** IDs ocultados pelo ADM. Serve também como tombstone para itens empacotados no APK. */
val hiddenBibleNewsIdsState = mutableStateListOf<Int>()

/**
 * Resolve a lista pública de forma determinística:
 * 1) Firestore sobrescreve o item empacotado quando possui o mesmo ID;
 * 2) o catálogo local continua como fallback seguro;
 * 3) itens ocultados pelo ADM não reaparecem pelo fallback local.
 */
fun resolvedBibleNews(items: List<BibleNews>): List<BibleNews> =
    BibleNewsEditorial.withEditorialCatalog(items)
        .filterNot { it.id in hiddenBibleNewsIdsState }

fun currentResolvedBibleNews(): List<BibleNews> =
    resolvedBibleNews(
        if (bibleNewsState.isEmpty()) BibleNewsData.newsList
        else bibleNewsState.toList()
    )
