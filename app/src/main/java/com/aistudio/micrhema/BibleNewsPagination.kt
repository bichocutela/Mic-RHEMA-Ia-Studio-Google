package com.aistudio.micrhema

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

object BibleNewsPagination {
    /**
     * A interface continua revelando 20 cards por vez. A janela de sincronização
     * é maior para que edições remotas de matérias antigas também sobrescrevam o
     * fallback empacotado antes de o usuário rolar até elas.
     */
    const val pageSize = 100L

    private var firstPageListener: ListenerRegistration? = null
    private var hiddenIdsListener: ListenerRegistration? = null
    private var lastDocument: DocumentSnapshot? = null
    private var isLoadingNextPage = false
    private var loadedAdditionalPages = 0

    var hasMore: Boolean = true
        private set

    fun start() {
        val db = FirebaseFirestore.getInstance()
        firstPageListener?.remove()
        hiddenIdsListener?.remove()
        loadedAdditionalPages = 0
        lastDocument = null

        // O catálogo empacotado continua como fallback, mas nunca tenta escrever no
        // Firestore a partir de um aparelho comum. Isso evita erros de permissão e
        // mantém o conteúdo remoto como camada de sobrescrita.
        (BibleNewsData.newsList + BibleNewsEditorialCatalog.additionalNews).forEach { news ->
            BibleNewsDocumentIds.register(news.id, news.id.toString())
        }

        hiddenIdsListener = db.collection("settings")
            .document("bible_news_editorial")
            .addSnapshotListener { snapshot, _ ->
                val hidden = (snapshot?.get("hiddenIds") as? List<*>)
                    .orEmpty()
                    .mapNotNull { value ->
                        when (value) {
                            is Number -> value.toInt()
                            is String -> value.toIntOrNull()
                            else -> null
                        }
                    }
                    .filter { it > 0 }
                    .distinct()
                hiddenBibleNewsIdsState.clear()
                hiddenBibleNewsIdsState.addAll(hidden)
            }

        firstPageListener = db.collection("bible_news")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(pageSize)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.isEmpty) {
                    bibleNewsState.clear()
                    lastDocument = null
                    hasMore = false
                    return@addSnapshotListener
                }

                val page = BibleNewsEditorial.decorateAll(snapshot.documents.mapNotNull(::fromDocument))
                if (loadedAdditionalPages == 0) {
                    bibleNewsState.clear()
                    bibleNewsState.addAll(page)
                    lastDocument = snapshot.documents.lastOrNull()
                } else {
                    mergeFirstPage(page)
                }
                hasMore = snapshot.documents.size >= pageSize.toInt()
            }
    }

    /** Atualiza a janela sincronizada sem desmontar páginas adicionais já carregadas. */
    suspend fun refresh() {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("bible_news")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(pageSize)
            .get(Source.SERVER)
            .await()

        if (snapshot.isEmpty) {
            if (loadedAdditionalPages == 0) bibleNewsState.clear()
            hasMore = false
            return
        }

        val page = BibleNewsEditorial.decorateAll(snapshot.documents.mapNotNull(::fromDocument))
        if (loadedAdditionalPages == 0) {
            bibleNewsState.clear()
            bibleNewsState.addAll(page)
            lastDocument = snapshot.documents.lastOrNull()
        } else {
            mergeFirstPage(page)
        }
        hasMore = snapshot.documents.size >= pageSize.toInt()
    }

    suspend fun loadNextPage() {
        val cursor = lastDocument ?: return
        if (!hasMore || isLoadingNextPage) return
        isLoadingNextPage = true
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("bible_news")
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .startAfter(cursor)
                .limit(pageSize)
                .get()
                .await()
            val nextPage = BibleNewsEditorial.decorateAll(snapshot.documents.mapNotNull(::fromDocument))
            val existingIds = bibleNewsState.map { it.id }.toSet()
            bibleNewsState.addAll(nextPage.filterNot { it.id in existingIds })
            lastDocument = snapshot.documents.lastOrNull() ?: cursor
            if (snapshot.documents.isNotEmpty()) loadedAdditionalPages += 1
            hasMore = snapshot.documents.size >= pageSize.toInt()
        } finally {
            isLoadingNextPage = false
        }
    }

    private fun mergeFirstPage(page: List<BibleNews>) {
        val firstPageIds = page.map { it.id }.toSet()
        val tail = bibleNewsState.filterNot { it.id in firstPageIds }
        bibleNewsState.clear()
        bibleNewsState.addAll(page + tail)
    }

    private fun fromDocument(document: DocumentSnapshot): BibleNews? = runCatching {
        val stableId = BibleNewsDocumentIds.stableId(document.id, document.get("id"))
        BibleNews(
            id = stableId,
            title = document.getString("title") ?: "",
            content = document.getString("content") ?: "",
            book = document.getString("book") ?: "",
            chapter = document.getLong("chapter")?.toInt()
                ?: (document.get("chapter") as? String)?.toIntOrNull()
                ?: 0,
            verse = document.getLong("verse")?.toInt()
                ?: (document.get("verse") as? String)?.toIntOrNull()
                ?: 0,
            imageUrl = document.getString("imageUrl") ?: "",
            summary = document.getString("summary") ?: "",
            category = document.getString("category") ?: "",
            intensity = document.getLong("intensity")?.toInt()
                ?: (document.get("intensity") as? String)?.toIntOrNull()
                ?: 0,
            tags = (document.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            contentWarning = document.getString("contentWarning") ?: "",
            publishedAt = document.getLong("publishedAt") ?: 0L,
            featured = document.getBoolean("featured") ?: false,
            storyKey = document.getString("storyKey") ?: ""
        )
    }.getOrNull()?.takeIf { it.id > 0 && it.title.isNotBlank() }
}
