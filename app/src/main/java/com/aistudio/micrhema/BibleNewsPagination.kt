package com.aistudio.micrhema

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object BibleNewsPagination {
    const val pageSize = 20L

    private var firstPageListener: ListenerRegistration? = null
    private var lastDocument: DocumentSnapshot? = null
    private var isLoadingNextPage = false

    var hasMore: Boolean = true
        private set

    fun start() {
        val db = FirebaseFirestore.getInstance()
        firstPageListener?.remove()
        firstPageListener = db.collection("bible_news")
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(pageSize)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.isEmpty) {
                    seedLocalNews(db)
                    return@addSnapshotListener
                }

                val page = snapshot.documents.mapNotNull(::fromDocument)
                lastDocument = snapshot.documents.lastOrNull()
                hasMore = snapshot.documents.size >= pageSize.toInt()
                bibleNewsState.clear()
                bibleNewsState.addAll(BibleNewsEditorial.decorateAll(page))
            }
    }

    suspend fun loadNextPage() {
        val cursor = lastDocument ?: return
        if (!hasMore || isLoadingNextPage) return
        isLoadingNextPage = true
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("bible_news")
                .orderBy("id", Query.Direction.DESCENDING)
                .startAfter(cursor)
                .limit(pageSize)
                .get()
                .await()
            val nextPage = snapshot.documents.mapNotNull(::fromDocument)
            val existingIds = bibleNewsState.map { it.id }.toSet()
            bibleNewsState.addAll(
                BibleNewsEditorial.decorateAll(nextPage).filterNot { it.id in existingIds }
            )
            lastDocument = snapshot.documents.lastOrNull() ?: cursor
            hasMore = snapshot.documents.size >= pageSize.toInt()
        } finally {
            isLoadingNextPage = false
        }
    }

    private fun seedLocalNews(db: FirebaseFirestore) {
        val seedNews = BibleNewsEditorial.decorateAll(BibleNewsData.newsList)
        seedNews.forEach { news ->
            db.collection("bible_news").document(news.id.toString()).set(news)
        }
        bibleNewsState.clear()
        bibleNewsState.addAll(seedNews.sortedByDescending { it.publishedAt })
        hasMore = false
    }

    private fun fromDocument(document: DocumentSnapshot): BibleNews? = runCatching {
        BibleNews(
            id = document.getLong("id")?.toInt() ?: document.id.toIntOrNull() ?: 0,
            title = document.getString("title") ?: "",
            content = document.getString("content") ?: "",
            book = document.getString("book") ?: "",
            chapter = document.getLong("chapter")?.toInt() ?: 0,
            verse = document.getLong("verse")?.toInt() ?: 0,
            imageUrl = document.getString("imageUrl") ?: "",
            summary = document.getString("summary") ?: "",
            category = document.getString("category") ?: "",
            intensity = document.getLong("intensity")?.toInt() ?: 0,
            tags = (document.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            contentWarning = document.getString("contentWarning") ?: "",
            publishedAt = document.getLong("publishedAt") ?: 0L,
            featured = document.getBoolean("featured") ?: false,
            storyKey = document.getString("storyKey") ?: ""
        )
    }.getOrNull()?.takeIf { it.id > 0 && it.title.isNotBlank() }
}
