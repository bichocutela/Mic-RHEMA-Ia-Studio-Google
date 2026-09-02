package com.aistudio.micrhema

import androidx.compose.runtime.Composable

/**
 * Camada de compatibilidade para rotas antigas e referências abertas por outras áreas do app.
 * Em vez de manter uma segunda interface de leitura, todas elas agora reutilizam a mesma
 * experiência contínua da aba Bíblia.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BollsBibleScreen(
    book: String?,
    chapter: Int?,
    versionCode: String?,
    verse: Int?,
    onBack: () -> Unit,
    onOpenChapter: (String, Int, String) -> Unit,
    onOpenComparison: (String, Int, Int) -> Unit,
    onOpenReference: (String, Int, Int, String) -> Unit
) {
    BibleScreen(
        initialBook = book,
        initialChapter = chapter,
        initialVersion = versionCode,
        initialVerse = verse,
        onOpenBible = { selectedBook, selectedChapter, selectedVersion, selectedVerse ->
            if (selectedVerse != null) {
                onOpenReference(selectedBook, selectedChapter, selectedVerse, selectedVersion)
            } else {
                onOpenChapter(selectedBook, selectedChapter, selectedVersion)
            }
        }
    )
}
