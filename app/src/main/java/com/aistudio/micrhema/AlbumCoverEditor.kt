package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Altura real da capa no cartão da lista de álbuns.
 * A tela de Fotos tem 16dp de espaçamento nas duas laterais.
 */
const val ALBUM_CARD_COVER_HEIGHT_DP = 160
const val ALBUM_CARD_SIDE_PADDING_DP = 16

/**
 * Mesma imagem/enquadramento em todas as capas e na prévia do editor.
 * Os valores são metadados: a foto original não é modificada.
 */
@Composable
fun AlbumCoverImage(
    url: String?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    fit: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Capa do álbum",
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val zoom = scale.coerceIn(0.75f, 2.5f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = size.width * offsetX.coerceIn(-0.5f, 0.5f)
                    translationY = size.height * offsetY.coerceIn(-0.5f, 0.5f)
                }
            )
        }
    }
}

@Composable
fun AlbumCoverEditor(
    coverUrl: String?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    fit: Boolean,
    onScaleChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onFitChange: (Boolean) -> Unit
) {
    // O editor é um diálogo estreito, mas o cartão verdadeiro usa toda
    // a largura da página (menos o padding lateral). Exibir 160dp de
    // altura nos dois lugares cria proporções diferentes e engana o editor.
    // Manter a proporção da capa real, escalando a prévia por inteiro,
    // reproduz exatamente o recorte, o zoom e as faixas laterais do app.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val cardCoverAspectRatio = (screenWidthDp - 2 * ALBUM_CARD_SIDE_PADDING_DP)
        .coerceAtLeast(ALBUM_CARD_COVER_HEIGHT_DP)
        .toFloat() / ALBUM_CARD_COVER_HEIGHT_DP

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Redimensionar capa", fontWeight = FontWeight.SemiBold)
        Text(
            "Prévia com a mesma proporção da capa na aba Fotos. " +
                "A imagem original não será alterada.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        AlbumCoverImage(
            url = coverUrl,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            fit = fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(cardCoverAspectRatio)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fit, onCheckedChange = onFitChange)
            Text("Mostrar foto inteira (sem cortar)")
        }
        Text("Zoom: " + (scale * 100).toInt() + "%", style = MaterialTheme.typography.labelMedium)
        Slider(value = scale.coerceIn(0.75f, 2.5f), onValueChange = onScaleChange, valueRange = 0.75f..2.5f)
        Text("Mover para os lados", style = MaterialTheme.typography.labelMedium)
        Slider(value = offsetX.coerceIn(-0.5f, 0.5f), onValueChange = onOffsetXChange, valueRange = -0.5f..0.5f)
        Text("Mover para cima / baixo", style = MaterialTheme.typography.labelMedium)
        Slider(value = offsetY.coerceIn(-0.5f, 0.5f), onValueChange = onOffsetYChange, valueRange = -0.5f..0.5f)
    }
}
