package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
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

/**
 * A rota atual do MainActivity já delega a montagem final para YouVersionLinks.
 * O versículo pendente vive apenas por alguns segundos para nunca contaminar
 * uma navegação posterior e não relacionada.
 */
object BibleNewsPendingNavigation {
    private const val MAX_AGE_MS = 5_000L
    private data class Pending(
        val book: String,
        val chapter: Int,
        val verse: Int,
        val createdAt: Long
    )

    @Volatile private var pending: Pending? = null

    fun remember(book: String, chapter: Int, verse: Int) {
        if (book.isNotBlank() && chapter > 0 && verse > 0) {
            pending = Pending(book, chapter, verse, System.currentTimeMillis())
        }
    }

    fun consume(book: String, chapter: Int): Int? {
        val current = pending ?: return null
        pending = null
        val isFresh = System.currentTimeMillis() - current.createdAt <= MAX_AGE_MS
        val isSameReference = current.book.equals(book, ignoreCase = true) && current.chapter == chapter
        return current.verse.takeIf { isFresh && isSameReference }
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

/** Salva/edita sem perder o ID real de documentos legados da PWA. */
fun saveBibleNewsSafely(
    news: BibleNews,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (isOfflineModeState.value) {
        onFailure(IllegalStateException("Modo offline ativado"))
        return
    }
    runCatching {
        val db = Firebase.firestore
        val documentId = BibleNewsDocumentIds.documentIdFor(news.id) ?: news.id.toString()
        val decorated = BibleNewsEditorial.decorate(news)
        val batch = db.batch()
        batch.set(db.collection("bible_news").document(documentId), decorated)
        batch.set(
            db.collection("settings").document("bible_news_editorial"),
            mapOf("hiddenIds" to FieldValue.arrayRemove(news.id)),
            SetOptions.merge()
        )
        batch.commit()
            .addOnSuccessListener {
                BibleNewsDocumentIds.register(news.id, documentId)
                hiddenBibleNewsIdsState.remove(news.id)
                onSuccess()
            }
            .addOnFailureListener(onFailure)
    }.onFailure { onFailure(it as? Exception ?: IllegalStateException(it.message)) }
}

/**
 * Excluir vira um tombstone administrativo. Assim um item que também existe no
 * catálogo empacotado não reaparece na próxima recomposição ou atualização.
 */
fun hideBibleNewsSafely(
    news: BibleNews,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (isOfflineModeState.value) {
        onFailure(IllegalStateException("Modo offline ativado"))
        return
    }
    runCatching {
        val db = Firebase.firestore
        val documentId = BibleNewsDocumentIds.documentIdFor(news.id) ?: news.id.toString()
        val batch = db.batch()
        batch.set(
            db.collection("settings").document("bible_news_editorial"),
            mapOf("hiddenIds" to FieldValue.arrayUnion(news.id)),
            SetOptions.merge()
        )
        batch.delete(db.collection("bible_news").document(documentId))
        if (dailyNewsNotificationIdState.value == news.id) {
            batch.set(
                db.collection("settings").document("daily_news"),
                mapOf(
                    "selectedNewsId" to FieldValue.delete(),
                    "selectedDocumentId" to FieldValue.delete(),
                    "title" to FieldValue.delete(),
                    "summary" to FieldValue.delete(),
                    "content" to FieldValue.delete(),
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        }
        batch.commit()
            .addOnSuccessListener {
                if (news.id !in hiddenBibleNewsIdsState) hiddenBibleNewsIdsState.add(news.id)
                bibleNewsState.removeAll { it.id == news.id }
                if (dailyNewsNotificationIdState.value == news.id) {
                    dailyNewsNotificationIdState.value = null
                }
                BibleNewsDocumentIds.forget(news.id)
                onSuccess()
            }
            .addOnFailureListener(onFailure)
    }.onFailure { onFailure(it as? Exception ?: IllegalStateException(it.message)) }
}

/** Seleção diária preserva também o ID real do documento para o Worker. */
fun selectDailyBibleNewsSafely(
    news: BibleNews,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    val documentId = BibleNewsDocumentIds.documentIdFor(news.id) ?: news.id.toString()
    Firebase.firestore.collection("settings").document("daily_news").set(
        mapOf(
            "selectedNewsId" to news.id,
            "selectedDocumentId" to documentId,
            "title" to news.title,
            "summary" to news.summary,
            "content" to news.content,
            "updatedAt" to System.currentTimeMillis()
        ),
        SetOptions.merge()
    ).addOnSuccessListener {
        dailyNewsNotificationIdState.value = news.id
        onSuccess()
    }.addOnFailureListener(onFailure)
}
