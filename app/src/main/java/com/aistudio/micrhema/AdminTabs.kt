package com.aistudio.micrhema

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.BorderStroke
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

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Organize a navegação principal do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showPreview = !showPreview }) {
                    Icon(if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showPreview) "Ocultar" else "Visualizar")
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar aba")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adicionar")
                }
            }
        }
        
        if (showPreview) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val bottomTabs = appTabsState.filter { it.isVisible && it.showInBottomBar }.sortedBy { it.order }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Prévia do menu inferior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Veja como as abas ativas aparecerão para os membros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("${bottomTabs.size}/5") }
                        )
                    }
                    if (bottomTabs.isEmpty()) {
                        Text(
                            "Nenhuma aba está configurada para aparecer no menu inferior.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        NavigationBar {
                            bottomTabs.take(5).forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = index == 0,
                                    onClick = { },
                                    enabled = false,
                                    icon = { Icon(getIconFromName(tab.iconName), contentDescription = null) },
                                    label = { Text(tab.title, maxLines = 1) }
                                )
                            }
                        }
                    }
                    if (bottomTabs.size > 5) {
                        Text(
                            "Há ${bottomTabs.size} abas selecionadas, mas o menu inferior deve ter no máximo 5. Apenas as cinco primeiras aparecem na prévia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        val orderedTabs = appTabsState.sortedBy { it.order }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(orderedTabs) { tab ->
                val tabTypeLabel = if (tab.systemRoute != null) "Sistema" else "Personalizada"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (tab.isVisible) 0.55f else 0.3f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (tab.isVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                getIconFromName(tab.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tab.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "$tabTypeLabel • Ícone ${tab.iconName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(if (tab.isVisible) "Ativa" else "Oculta") },
                                leadingIcon = {
                                    Icon(
                                        if (tab.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Visível", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = tab.isVisible,
                                        onCheckedChange = { checked ->
                                            val idx = appTabsState.indexOfFirst { it.id == tab.id }
                                            if (idx != -1) {
                                                val updated = tab.copy(isVisible = checked)
                                                appTabsState[idx] = updated
                                                addAppTab(updated)
                                            }
                                        }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Menu inferior", style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = tab.showInBottomBar,
                                        onCheckedChange = { checked ->
                                            val idx = appTabsState.indexOfFirst { it.id == tab.id }
                                            if (idx != -1) {
                                                val updated = tab.copy(showInBottomBar = checked)
                                                appTabsState[idx] = updated
                                                addAppTab(updated)
                                            }
                                        }
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { moveAdminTab(tab.id, -1) },
                                    enabled = orderedTabs.indexOfFirst { it.id == tab.id } > 0
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Mover ${tab.title} para cima")
                                }
                                IconButton(
                                    onClick = { moveAdminTab(tab.id, 1) },
                                    enabled = orderedTabs.indexOfFirst { it.id == tab.id } in 0 until (orderedTabs.size - 1)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mover ${tab.title} para baixo")
                                }
                                if (tab.systemRoute == null) {
                                    IconButton(onClick = {
                                        appTabsState.removeIf { it.id == tab.id }
                                        removeAppTab(tab)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover ${tab.title}", tint = MaterialTheme.colorScheme.error)
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
        var newTitle by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(false) }
        var showInBottomBar by remember { mutableStateOf(false) }
        var selectedType by remember { mutableStateOf(TabContentType.MIXED) }
        var iconName by remember { mutableStateOf("Star") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Criar Nova Aba") },
            text = {
                Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {
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
                        Text("Aba Privada (Exige permissão)")
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
                    addAppTab(newTab)
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


private fun moveAdminTab(tabId: String, direction: Int) {
    val orderedTabs = appTabsState.sortedBy { it.order }
    val currentIndex = orderedTabs.indexOfFirst { it.id == tabId }
    val targetIndex = currentIndex + direction
    if (currentIndex < 0 || targetIndex !in orderedTabs.indices) return

    val current = orderedTabs[currentIndex]
    val target = orderedTabs[targetIndex]
    val currentIndexInState = appTabsState.indexOfFirst { it.id == current.id }
    val targetIndexInState = appTabsState.indexOfFirst { it.id == target.id }
    val updatedCurrent = current.copy(order = target.order)
    val updatedTarget = target.copy(order = current.order)
    if (currentIndexInState >= 0) appTabsState[currentIndexInState] = updatedCurrent
    if (targetIndexInState >= 0) appTabsState[targetIndexInState] = updatedTarget
    addAppTab(updatedCurrent)
    addAppTab(updatedTarget)
}
