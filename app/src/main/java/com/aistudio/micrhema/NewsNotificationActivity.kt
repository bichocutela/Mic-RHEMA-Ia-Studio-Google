package com.aistudio.micrhema

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.aistudio.micrhema.ui.theme.MICRhemaTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Destino isolado das notificações de Notícias Bíblicas. Ele evita mudar o
 * roteador principal do aplicativo e, ainda assim, abre a matéria correta e
 * mantém o leitor bíblico nativo no mesmo fluxo.
 */
class NewsNotificationActivity : ComponentActivity() {
    companion object {
        const val EXTRA_NEWS_ID = "news_notification_id"
        const val EXTRA_DOCUMENT_ID = "news_notification_document_id"
    }

    private val remoteLoadFinished = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val newsId = intent.getIntExtra(EXTRA_NEWS_ID, 0)
        val documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID)?.takeIf { it.isNotBlank() }

        if (newsId <= 0) {
            finish()
            return
        }

        documentId?.let { BibleNewsDocumentIds.register(newsId, it) }
        loadSelectedNews(newsId, documentId)

        setContent {
            MICRhemaTheme(darkTheme = isSystemInDarkTheme()) {
                val currentNews = currentResolvedBibleNews().firstOrNull { it.id == newsId }
                var reader by remember { mutableStateOf<NotificationBibleReference?>(null) }
                var comparison by remember { mutableStateOf<NotificationBibleReference?>(null) }

                when {
                    comparison != null -> {
                        val ref = comparison!!
                        BibleComparisonScreen(
                            initialBook = ref.book,
                            initialChapter = ref.chapter,
                            initialVerse = ref.verse,
                            onBack = { comparison = null }
                        )
                    }
                    reader != null -> {
                        val ref = reader!!
                        BollsBibleScreen(
                            book = ref.book,
                            chapter = ref.chapter,
                            versionCode = ref.version,
                            verse = ref.verse,
                            onBack = { reader = null },
                            onOpenChapter = { book, chapter, version ->
                                reader = NotificationBibleReference(book, chapter, null, version)
                            },
                            onOpenComparison = { book, chapter, verse ->
                                comparison = NotificationBibleReference(book, chapter, verse, ref.version)
                            },
                            onOpenReference = { book, chapter, verse, version ->
                                reader = NotificationBibleReference(book, chapter, verse, version)
                            }
                        )
                    }
                    currentNews != null -> {
                        NewsDetailScreen(
                            newsId = newsId,
                            onBack = { finish() },
                            onNavigateToBible = { book, chapter, version ->
                                val verse = BibleNewsPendingNavigation.consume(book, chapter)
                                    ?: currentNews.verse.takeIf { it > 0 }
                                reader = NotificationBibleReference(
                                    book = book,
                                    chapter = chapter,
                                    verse = verse,
                                    version = version ?: "NTLH"
                                )
                            }
                        )
                    }
                    !remoteLoadFinished.value -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Esta notícia não está mais disponível.", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    private fun loadSelectedNews(newsId: Int, documentId: String?) {
        if (documentId.isNullOrBlank() || FirebaseApp.getApps(this).isEmpty()) {
            remoteLoadFinished.value = true
            return
        }
        lifecycleScope.launch {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("bible_news")
                    .document(documentId)
                    .get()
                    .await()
                if (document.exists()) {
                    val resolvedId = BibleNewsDocumentIds.stableId(document.id, document.get("id"))
                    val effectiveId = if (resolvedId > 0) resolvedId else newsId
                    val loaded = BibleNews(
                        id = effectiveId,
                        title = document.getString("title").orEmpty(),
                        content = document.getString("content").orEmpty(),
                        book = document.getString("book").orEmpty(),
                        chapter = document.getLong("chapter")?.toInt()
                            ?: (document.get("chapter") as? String)?.toIntOrNull()
                            ?: 0,
                        verse = document.getLong("verse")?.toInt()
                            ?: (document.get("verse") as? String)?.toIntOrNull()
                            ?: 0,
                        imageUrl = document.getString("imageUrl").orEmpty(),
                        summary = document.getString("summary").orEmpty(),
                        category = document.getString("category").orEmpty(),
                        intensity = document.getLong("intensity")?.toInt()
                            ?: (document.get("intensity") as? String)?.toIntOrNull()
                            ?: 0,
                        tags = (document.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        contentWarning = document.getString("contentWarning").orEmpty(),
                        publishedAt = document.getLong("publishedAt") ?: 0L,
                        featured = document.getBoolean("featured") ?: false,
                        storyKey = document.getString("storyKey").orEmpty()
                    )
                    if (loaded.title.isNotBlank()) {
                        BibleNewsDocumentIds.register(effectiveId, documentId)
                        val index = bibleNewsState.indexOfFirst { it.id == effectiveId }
                        if (index >= 0) bibleNewsState[index] = BibleNewsEditorial.decorate(loaded)
                        else bibleNewsState.add(0, BibleNewsEditorial.decorate(loaded))
                    }
                }
            } catch (_: Exception) {
                // O catálogo local continua sendo o fallback quando disponível.
            } finally {
                remoteLoadFinished.value = true
            }
        }
    }
}

private data class NotificationBibleReference(
    val book: String,
    val chapter: Int,
    val verse: Int?,
    val version: String
)
