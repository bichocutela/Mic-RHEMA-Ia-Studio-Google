package com.aistudio.micrhema

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.aistudio.micrhema.ui.theme.MICRhemaTheme

class DocumentReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Documento" }
        val contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE).orEmpty()

        if (sourceUrl.isBlank()) {
            finish()
            return
        }

        setContent {
            val darkTheme = when (SettingsManager.getThemeMode(this@DocumentReaderActivity)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MICRhemaTheme(darkTheme = darkTheme) {
                ReaderScreen(sourceUrl, title, contentType)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ReaderScreen(sourceUrl: String, title: String, contentType: String) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { StudyMaterialDownload.openExternalDocument(this@DocumentReaderActivity, sourceUrl) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Abrir em outro aplicativo")
                        }
                    }
                )
            }
        ) { padding ->
            PdfViewer(
                bookUrl = sourceUrl,
                title = title,
                contentType = contentType,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }

    companion object {
        const val EXTRA_SOURCE_URL = "document_source_url"
        const val EXTRA_TITLE = "document_title"
        const val EXTRA_CONTENT_TYPE = "document_content_type"

        fun intent(sourceContext: android.content.Context, sourceUrl: String, title: String, contentType: String): Intent =
            Intent(sourceContext, DocumentReaderActivity::class.java).apply {
                putExtra(EXTRA_SOURCE_URL, sourceUrl)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT_TYPE, contentType)
            }
    }
}
