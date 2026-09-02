package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun CustomTabScreen(tabId: String?) {
    val context = LocalContext.current
    val tab = appTabsState.find { it.id == tabId }
    val isAdmin = loggedInMemberState.value?.isAdmin == true
    var showAddDialog by remember { mutableStateOf(false) }

    if (tab == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aba não encontrada.")
        }
        return
    }

    if (tab.isPrivate && !isAdmin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Conteúdo restrito para membros autorizados.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Conteúdo")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text(tab.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (tab.customContents.isEmpty()) {
                Text("Nenhum conteúdo disponível nesta aba ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tab.customContents) { content ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (content.fileUrl.isNotEmpty()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content.fileUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Nenhum aplicativo para abrir este arquivo", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    val icon = when (content.type) {
                                        "PDF" -> Icons.Default.PictureAsPdf
                                        "VIDEO" -> Icons.Default.PlayArrow
                                        "PHOTO" -> Icons.Default.Image
                                        "MUSIC" -> Icons.Default.Audiotrack
                                        else -> Icons.Default.PlayArrow
                                    }
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(content.title, fontWeight = FontWeight.Bold)
                                        if (content.subtitle.isNotEmpty()) {
                                            Text(content.subtitle, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (isAdmin) {
                                    IconButton(onClick = {
                                        val updatedTab = tab.copy(customContents = tab.customContents.filter { it.id != content.id })
                                        val idx = appTabsState.indexOfFirst { it.id == tab.id }
                                        if (idx != -1) {
                                            appTabsState[idx] = updatedTab
                                            addAppTab(updatedTab)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var fileUrl by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("PDF") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Conteúdo") },
            text = {
                Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtítulo/Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Tipo de Arquivo", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PDF", "VIDEO", "PHOTO", "MUSIC").forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    
                    val mimeType = when(selectedType) {
                        "PDF" -> "application/pdf"
                        "VIDEO" -> "video/*"
                        "PHOTO" -> "image/*"
                        "MUSIC" -> "audio/*"
                        else -> "*/*"
                    }
                    
                    LocalUploadField(
                        value = fileUrl,
                        onValueChange = { fileUrl = it },
                        label = "Upload de Arquivo",
                        mimeType = mimeType
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newContent = CustomTabContent(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            subtitle = subtitle,
                            fileUrl = fileUrl,
                            type = selectedType
                        )
                        val updatedTab = tab.copy(customContents = tab.customContents + newContent)
                        val idx = appTabsState.indexOfFirst { it.id == tab.id }
                        if (idx != -1) {
                            appTabsState[idx] = updatedTab
                            addAppTab(updatedTab)
                        }
                        showAddDialog = false
                    },
                    enabled = title.isNotBlank() && fileUrl.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
