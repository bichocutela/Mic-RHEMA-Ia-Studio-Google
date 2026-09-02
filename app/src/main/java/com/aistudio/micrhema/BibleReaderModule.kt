package com.aistudio.micrhema

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private data class ParsedBibleReference(
    val book: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null,
    val version: String? = null
)

private fun parseBibleReference(value: String): ParsedBibleReference {
    val raw = value.trim()
    if (raw.isBlank()) return ParsedBibleReference()

    val version = Regex("\\(([^)]+)\\)")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { candidate ->
            BollsBibleCatalog.translations.any { it.code.equals(candidate, ignoreCase = true) }
        }
        ?.let(BollsBibleCatalog::normalize)

    val withoutVersion = raw.replace(Regex("\\s*\\([^)]+\\)\\s*$"), "").trim()
    val book = chapterCounts.keys
        .sortedByDescending { it.length }
        .firstOrNull { candidate ->
            withoutVersion.equals(candidate, ignoreCase = true) ||
                withoutVersion.startsWith("$candidate ", ignoreCase = true)
        }
        ?: return ParsedBibleReference(version = version)

    val referencePart = withoutVersion.drop(book.length).trim()
    val chapter = referencePart.substringBefore(":").trim().toIntOrNull()
    val verse = referencePart.substringAfter(":", "").substringBefore("-").trim().toIntOrNull()
    val validChapter = validLegacyChapter(book, chapter)

    return ParsedBibleReference(
        book = book,
        chapter = validChapter,
        verse = verse?.takeIf { validChapter != null && it > 0 },
        version = version
    )
}

private fun validLegacyChapter(book: String, chapter: Int?): Int? {
    val max = chapterCounts[book] ?: return null
    return chapter?.takeIf { it in 1..max }
}

/**
 * Modal de compatibilidade usado por conteúdos que abrem uma referência bíblica.
 * Ele não mantém mais um segundo leitor com regras próprias: reutiliza exatamente
 * o mesmo fluxo contínuo da aba Bíblia, evitando capítulos inválidos e interfaces divergentes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderModal(
    onDismiss: () -> Unit,
    initialReference: String = ""
) {
    val parsed = parseBibleReference(initialReference)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxSize()
    ) {
        BibleScreen(
            initialBook = parsed.book,
            initialChapter = parsed.chapter,
            initialVersion = parsed.version,
            initialVerse = parsed.verse,
            onBack = onDismiss
        )
    }
}
