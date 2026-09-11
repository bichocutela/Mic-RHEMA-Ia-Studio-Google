package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateOf

data class DocumentReaderRequest(
    val sourceUrl: String,
    val title: String,
    val contentType: String
)

/** Pedido transitório para abrir um material de estudo no leitor interno. */
val documentReaderRequestState = mutableStateOf<DocumentReaderRequest?>(null)
