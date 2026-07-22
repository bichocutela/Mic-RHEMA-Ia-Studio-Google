package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTabsScreen() {
    var showAddDialog by remember { mutableStateOf(false) }

    var showPreview by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gerenciamento de Abas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPreview = !showPreview }) {
                    Icon(if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showPreview) "Ocultar Preview" else "Preview")
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nova Aba")
                }
            }
        }
        
        if (showPreview) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Preview da Barra de Navegação", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    NavigationBar {
                        val bottomTabs = appTabsState.filter { it.isVisible && it.showInBottomBar }.sortedBy { it.order }
                        bottomTabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = index == 0,
                                onClick = { },
                                icon = { Icon(getIconFromName(tab.iconName), contentDescription = null) },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                    
                    if (appTabsState.filter { it.isVisible && it.showInBottomBar }.size > 5) {
                        Text(
                            "Atenção: A barra de navegação inferior padrão do Android suporta um máximo de 5 itens. Itens adicionais podem não ser exibidos corretamente ou quebrar o layout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(appTabsState.sortedBy { it.order }) { tab ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tab.title, fontWeight = FontWeight.Bold)
                            Text("Ícone: ${tab.iconName} | Tipo: ${tab.type.name}", style = MaterialTheme.typography.bodySmall)
                            Text(if (tab.systemRoute != null) "Aba do Sistema" else "Aba Personalizada", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Menu Inferior", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = tab.showInBottomBar,
                                onCheckedChange = { checked ->
                                    val idx = appTabsState.indexOfFirst { it.id == tab.id }
                                    if (idx != -1) {
                                        appTabsState[idx] = tab.copy(showInBottomBar = checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visível", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = tab.isVisible,
                                onCheckedChange = { checked ->
                                    val idx = appTabsState.indexOfFirst { it.id == tab.id }
                                    if (idx != -1) {
                                        appTabsState[idx] = tab.copy(isVisible = checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (tab.systemRoute == null) {
                                IconButton(onClick = {
                                    appTabsState.removeIf { it.id == tab.id }
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

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(false) }
        var showInBottomBar by remember { mutableStateOf(false) }
        var selectedType by remember { mutableStateOf(TabContentType.MIXED) }
        var iconName by remember { mutableStateOf("Star") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Criar Nova Aba") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Título da Aba") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tipo de Conteúdo")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TabContentType.values().filter { it != TabContentType.SYSTEM }.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ícone")
                    val availableIcons = listOf("Home", "Book", "Church", "LibraryBooks", "Favorite", "People", "Group", "Info", "Settings", "Lock", "Video", "Photo", "Link", "Star", "MenuBook")
                    var expandedIconMenu by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedIconMenu,
                        onExpandedChange = { expandedIconMenu = it }
                    ) {
                        OutlinedTextField(
                            value = iconName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(getIconFromName(iconName), contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIconMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedIconMenu,
                            onDismissRequest = { expandedIconMenu = false }
                        ) {
                            availableIcons.forEach { icon ->
                                DropdownMenuItem(
                                    text = { Text(icon) },
                                    leadingIcon = { Icon(getIconFromName(icon), contentDescription = null) },
                                    onClick = {
                                        iconName = icon
                                        expandedIconMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Aba Privada (Exige Login/VIP)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showInBottomBar, onCheckedChange = { showInBottomBar = it })
                        Text("Mostrar no Menu Inferior")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newTab = AppTab(
                        id = UUID.randomUUID().toString(),
                        title = newTitle,
                        iconName = iconName,
                        isPrivate = isPrivate,
                        isVisible = true,
                        showInBottomBar = showInBottomBar,
                        order = appTabsState.size,
                        type = selectedType
                    )
                    appTabsState.add(newTab)
                    showAddDialog = false
                }, enabled = newTitle.isNotBlank()) {
                    Text("Adicionar")
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
