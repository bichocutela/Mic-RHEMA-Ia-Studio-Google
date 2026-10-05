package com.aistudio.micrhema

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun PdfViewer(
    bookUrl: String,
    title: String,
    contentType: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember(bookUrl, contentType) { mutableStateOf(true) }
    var error by remember(bookUrl, contentType) { mutableStateOf<String?>(null) }
    var pdfFile by remember(bookUrl, contentType) { mutableStateOf<File?>(null) }
    var detectedFileType by remember(bookUrl, contentType) { mutableStateOf<GoogleDriveService.FileType?>(null) }

    LaunchedEffect(bookUrl, contentType) {
        if (bookUrl.isBlank()) {
            error = "URL do livro não informada."
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        pdfFile = null
        detectedFileType = null
        try {
            val normalizedType = contentType.trim().lowercase()
            detectedFileType = when {
                normalizedType in setOf("word", "doc", "docx") -> GoogleDriveService.FileType.WORD
                normalizedType == "epub" -> GoogleDriveService.FileType.EPUB
                normalizedType == "pdf" -> GoogleDriveService.FileType.PDF
                bookUrl.startsWith("http", ignoreCase = true) -> GoogleDriveService.identifyFileType(bookUrl)
                else -> {
                    val uri = Uri.parse(bookUrl)
                    val mime = runCatching { context.contentResolver.getType(uri).orEmpty().lowercase() }.getOrDefault("")
                    when {
                        mime.contains("epub+zip") || bookUrl.lowercase().endsWith(".epub") -> GoogleDriveService.FileType.EPUB
                        mime.contains("wordprocessingml") || mime.contains("msword") || bookUrl.lowercase().substringBefore('?').endsWith(".docx") || bookUrl.lowercase().substringBefore('?').endsWith(".doc") -> GoogleDriveService.FileType.WORD
                        else -> GoogleDriveService.FileType.PDF
                    }
                }
            }

            if (detectedFileType == GoogleDriveService.FileType.WORD || detectedFileType == GoogleDriveService.FileType.EPUB) {
                isLoading = false
                return@LaunchedEffect
            }

            val file = withContext(Dispatchers.IO) {
                val cachedFile = File(context.cacheDir, "book_v2_${bookUrl.hashCode()}.pdf")
                if (cachedFile.exists() && runCatching { DocumentFiles.detect(cachedFile) }.isFailure) {
                    cachedFile.delete()
                }
                if (!cachedFile.exists()) {
                    if (bookUrl.startsWith("http", ignoreCase = true)) {
                        RemoteDocumentDownload.download(bookUrl, cachedFile)
                    } else {
                        val input = context.contentResolver.openInputStream(Uri.parse(bookUrl))
                            ?: throw IllegalArgumentException("Não foi possível ler o documento selecionado.")
                        input.use { DocumentFiles.copy(it, cachedFile) }
                    }
                }
                cachedFile
            }
            when (withContext(Dispatchers.IO) { DocumentFiles.detect(file) }) {
                DocumentFormat.PDF -> pdfFile = file
                DocumentFormat.DOC, DocumentFormat.DOCX -> detectedFileType = GoogleDriveService.FileType.WORD
                DocumentFormat.EPUB -> detectedFileType = GoogleDriveService.FileType.EPUB
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            error = "Erro ao carregar o documento: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Baixando/Carregando livro...")
            }

            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)

            detectedFileType == GoogleDriveService.FileType.WORD -> RichDocumentReader(
                sourceUrl = bookUrl,
                title = title,
                kind = RichDocumentKind.DOCX,
                modifier = Modifier.fillMaxSize()
            )

            detectedFileType == GoogleDriveService.FileType.EPUB -> RichDocumentReader(
                sourceUrl = bookUrl,
                title = title,
                kind = RichDocumentKind.EPUB,
                modifier = Modifier.fillMaxSize()
            )

            pdfFile != null -> PdfRendererView(pdfFile!!, bookUrl)
            else -> Text("Formato de documento não reconhecido.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PdfRendererView(file: File, bookUrl: String) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }

    DisposableEffect(file) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            pageCount = pdfRenderer?.pageCount ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("book_bookmarks", Context.MODE_PRIVATE)
    val bookmarkKey = "bookmark_${bookUrl.hashCode()}"
    val initialPage = prefs.getInt(bookmarkKey, 0)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        prefs.edit().putInt(bookmarkKey, listState.firstVisibleItemIndex).apply()
    }

    // A leitura só entra no XP enquanto o visualizador permanece realmente aberto.
    // O servidor mede o tempo entre os pulsos e exige 10%/30s ou 95%/2min.
    LaunchedEffect(bookUrl, pageCount) {
        if (pageCount <= 0 || bookUrl.isBlank()) return@LaunchedEffect
        while (true) {
            val furthestVisible = listState.layoutInfo.visibleItemsInfo
                .maxOfOrNull { it.index }
                ?: listState.firstVisibleItemIndex
            val fraction = ((furthestVisible + 1).toFloat() / pageCount.toFloat()).coerceIn(0f, 1f)
            XpMediaClient.recordBook(context, bookUrl, fraction)
            delay(5_000L)
        }
    }

    if (pageCount > 0) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(Color.Gray),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pageCount) { index ->
                PdfPageItem(pdfRenderer = pdfRenderer!!, pageIndex = index)
            }
        }
    } else {
        Text("O PDF não contém páginas.")
    }
}

@Composable
fun PdfPageItem(pdfRenderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, pdfRenderer) {
        withContext(Dispatchers.IO) {
            try {
                synchronized(pdfRenderer) {
                    val page = pdfRenderer.openPage(pageIndex)
                    val destBitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    destBitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(destBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = destBitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Página ${pageIndex + 1}",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
