package com.aistudio.micrhema

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamScreen() {
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos", "Pastoral", "Louvor", "Secretaria", "Infantil", "Missões")
    
    var editingMember by remember { mutableStateOf<TeamMember?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    if (editingMember != null) {
        TeamMemberDialog(
            member = editingMember,
            onDismiss = { editingMember = null },
            onSave = { newMember ->
                if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            Firebase.firestore.collection("team").document(newMember.id).set(newMember)
                        } catch (e: Exception) {
                            android.util.Log.e("TeamScreen", "Error saving", e)
                        } finally {
                            editingMember = null
                        }
                    }
                } else {
                    val idx = teamMembersState.indexOfFirst { it.id == newMember.id }
                    if (idx >= 0) teamMembersState[idx] = newMember else teamMembersState.add(newMember)
                    editingMember = null
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nossa Equipe",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
            val filteredMembers = teamMembersState.filter { 
                selectedCategory == "Todos" || it.category.equals(selectedCategory, ignoreCase = true) 
            }.sortedBy { it.order }
            
            items(filteredMembers, key = { it.id }) { member ->
                TeamMemberCard(
                    member = member, 
                    modifier = Modifier.animateItemPlacement(tween(300)),
                    onEditClick = { editingMember = member }
                )
            }
        }
        }
    }
}

@Composable
fun EditTeamSection() {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<TeamMember?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    if (showAddDialog || editingMember != null) {
        TeamMemberDialog(
            member = editingMember,
            onDismiss = {
                showAddDialog = false
                editingMember = null
            },
            onSave = { newMember ->
                if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            Firebase.firestore.collection("team").document(newMember.id).set(newMember)
                        } catch (e: Exception) {
                            android.util.Log.e("TeamAdmin", "Error saving", e)
                        } finally {
                            showAddDialog = false
                            editingMember = null
                        }
                    }
                } else {
                    if (editingMember != null) {
                        val idx = teamMembersState.indexOfFirst { it.id == newMember.id }
                        if (idx >= 0) teamMembersState[idx] = newMember
                    } else {
                        teamMembersState.add(newMember)
                    }
                    showAddDialog = false
                    editingMember = null
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gerenciar Equipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Text("Adicionar Membro")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(teamMembersState.sortedBy { it.order }) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(member.imageUrl.takeIf { it.isNotBlank() } ?: "https://ui-avatars.com/api/?name=${member.name.replace(" ", "+")}&background=random")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, fontWeight = FontWeight.Bold)
                            if (member.role.isNotBlank()) Text(member.role, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { editingMember = member }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = {
                            if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
                                Firebase.firestore.collection("team").document(member.id).delete()
                            } else {
                                teamMembersState.remove(member)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamMemberCard(member: TeamMember, modifier: Modifier = Modifier, onEditClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(if (isHovered || isPressed) 1.05f else 1f)
    val elevation by androidx.compose.animation.core.animateDpAsState(if (isHovered || isPressed) 8.dp else 2.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple()
            ) { },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.imageUrl.takeIf { it.isNotBlank() } ?: "https://ui-avatars.com/api/?name=${member.name.replace(" ", "+")}&background=random")
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    },
                    contentDescription = "Foto de ${member.name}",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                if (member.role.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = member.role,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isAdminLogged.value) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun TeamMemberDialog(
    member: TeamMember?,
    onDismiss: () -> Unit,
    onSave: (TeamMember) -> Unit
) {
    var name by remember(member) { mutableStateOf(member?.name ?: "") }
    var role by remember(member) { mutableStateOf(member?.role ?: "") }
    var category by remember(member) { mutableStateOf(member?.category ?: "Geral") }
    var imageUrl by remember(member) { mutableStateOf(member?.imageUrl ?: "") }
    var orderText by remember(member) { mutableStateOf(member?.order?.toString() ?: teamMembersState.size.toString()) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member != null) "Editar Membro" else "Novo Membro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Cargo / Função") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria (Pastoral, Louvor, etc)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL da Foto") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it },
                    label = { Text("Ordem (Número)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && !isLoading,
                onClick = {
                    isLoading = true
                    val order = orderText.toIntOrNull() ?: 0
                    val id = member?.id ?: UUID.randomUUID().toString()
                    val newMember = TeamMember(id, name, role, imageUrl, order, category)
                    onSave(newMember)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
