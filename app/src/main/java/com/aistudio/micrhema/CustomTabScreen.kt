package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomTabScreen(tabId: String?) {
    val tab = appTabsState.find { it.id == tabId }
    if (tab == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aba não encontrada.")
        }
        return
    }

    if (tab.isPrivate) {
        // Just a simple password check or VIP check? The prompt says: "com preferências em adicionar privacidade ou não"
        // If it's private, maybe we just show a lock if they aren't logged in, but let's assume it's just checking if it's private.
        // Actually, we can reuse `AdminLoginDialog` or something similar, or just block it. For now, just a placeholder.
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(tab.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when (tab.type) {
            TabContentType.PHOTOS -> {
                Text("Área de Fotos (Em breve)", style = MaterialTheme.typography.bodyLarge)
            }
            TabContentType.VIDEOS -> {
                Text("Área de Vídeos (Em breve)", style = MaterialTheme.typography.bodyLarge)
            }
            TabContentType.LINKS -> {
                Text("Área de Links (Em breve)", style = MaterialTheme.typography.bodyLarge)
            }
            TabContentType.MIXED -> {
                Text("Área Mista (Em breve)", style = MaterialTheme.typography.bodyLarge)
            }
            else -> {
                Text("Conteúdo", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
