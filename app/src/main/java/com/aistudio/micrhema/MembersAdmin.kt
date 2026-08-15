package com.aistudio.micrhema
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun EditMembersSection() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = memberRequestsState, key = { it.id }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(member.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(member.phone, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (member.isApproved || member.isIbr) {
                                Text("Aprovado", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            } else {
                                Text("Pendente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = member.isApproved,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isApproved = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso aprovado para ${member.name}" else "Acesso removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("Aprovado")
                            

                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isIbr,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isIbr = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso IBR aprovado para ${member.name}" else "Acesso IBR removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("IBR")
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isAdmin,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isAdmin = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso Admin aprovado para ${member.name}" else "Acesso Admin removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("Admin")
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { 
                                MemberManager.deleteFromFirestore(context, member); MemberManager.saveMembers(context)
                                memberRequestsState.remove(member)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfilesSection() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMember by remember { mutableStateOf<MemberRequest?>(null) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    val approvedMembers = memberRequestsState.filter { it.isApproved || it.isIbr }

    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = selectedMember ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        isUploadingPhoto = true
        coroutineScope.launch {
            try {
                val uploadedUrl = StorageManager.uploadProfilePhotoToFirebase(context, uri, target.id)
                val updated = target.copy(profilePhotoUrl = uploadedUrl, updatedAt = System.currentTimeMillis())
                val index = memberRequestsState.indexOfFirst { it.id == target.id }
                if (index >= 0) memberRequestsState[index] = updated
                selectedMember = updated
                MemberManager.saveToFirestore(
                    context = context,
                    member = updated,
                    onSuccess = {
                        isUploadingPhoto = false
                        Toast.makeText(context, "Foto do usuário atualizada", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        val rollbackIndex = memberRequestsState.indexOfFirst { it.id == target.id }
                        if (rollbackIndex >= 0) memberRequestsState[rollbackIndex] = target
                        selectedMember = target
                        coroutineScope.launch {
                            runCatching { StorageManager.deleteProfilePhotoFromFirebase(target.id) }
                            isUploadingPhoto = false
                            Toast.makeText(context, "Não foi possível sincronizar a foto no perfil: ${error.message ?: "verifique as regras do Firebase"}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (error: Exception) {
                isUploadingPhoto = false
                Toast.makeText(context, "Não foi possível alterar a foto: ${error.message ?: "erro de leitura"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Perfis dos Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Text("Toque em um usuário para abrir todas as informações e alterar a foto.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))

        if (approvedMembers.isEmpty()) {
            Text("Nenhum membro aprovado encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = approvedMembers, key = { it.id }) { member ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMember = member },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MemberAvatar(member = member, size = 64.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name.takeIf { it.isNotBlank() } ?: "Sem Nome", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Telefone: ${member.phone.takeIf { it.isNotBlank() } ?: "Não informado"}", style = MaterialTheme.typography.bodyMedium)
                            Text(if (member.isApproved) "Aprovado" else "IBR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Ver perfil", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    selectedMember?.let { member ->
        MemberAdminDetailsDialog(
            member = member,
            isUploadingPhoto = isUploadingPhoto,
            onChangePhoto = { photoLauncher.launch("image/*") },
            onRemovePhoto = {
                if (isUploadingPhoto) return@MemberAdminDetailsDialog
                val previousPhotoUrl = member.profilePhotoUrl
                isUploadingPhoto = true
                val updated = member.copy(profilePhotoUrl = "", updatedAt = System.currentTimeMillis())
                val index = memberRequestsState.indexOfFirst { it.id == member.id }
                if (index >= 0) memberRequestsState[index] = updated
                selectedMember = updated
                MemberManager.saveToFirestore(
                    context = context,
                    member = updated,
                    onSuccess = {
                        coroutineScope.launch {
                            try {
                                if (previousPhotoUrl.startsWith("http://") || previousPhotoUrl.startsWith("https://")) {
                                    StorageManager.deleteProfilePhotoFromFirebase(member.id)
                                }
                                StorageManager.deleteLocalProfilePhoto(context, member.id)
                                Toast.makeText(context, "Foto removida do perfil sincronizado", Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                Toast.makeText(context, "Perfil atualizado, mas o arquivo remoto precisa ser removido no Firebase", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploadingPhoto = false
                            }
                        }
                    },
                    onFailure = { error ->
                        if (index >= 0) memberRequestsState[index] = member
                        selectedMember = member
                        isUploadingPhoto = false
                        Toast.makeText(context, "Não foi possível remover a foto do perfil: ${error.message ?: "verifique as regras do Firebase"}", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { selectedMember = null }
        )
    }
}

@Composable
private fun MemberAvatar(member: MemberRequest, size: androidx.compose.ui.unit.Dp) {
    if (member.profilePhotoUrl.isNotBlank()) {
        coil.compose.AsyncImage(
            model = member.profilePhotoUrl,
            contentDescription = "Foto de ${member.name}",
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(size).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
        }
    }
}

@Composable
private fun MemberAdminDetailsDialog(
    member: MemberRequest,
    isUploadingPhoto: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Perfil do usuário") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemberAvatar(member = member, size = 112.dp)
                Text(member.name.ifBlank { "Sem nome" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                    Text("Salvando foto…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Button(onClick = onChangePhoto) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alterar foto")
                    }
                    if (member.profilePhotoUrl.isNotBlank()) {
                        TextButton(onClick = onRemovePhoto) {
                            Text("Remover foto", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                MemberInfoRow("Telefone", member.phone)
                MemberInfoRow("E-mail", member.email)
                MemberInfoRow("Endereço", member.address)
                MemberInfoRow("Nascimento", member.birthDate)
                MemberInfoRow("Status", member.status.ifBlank { if (member.isApproved) "Aprovado" else "Pendente" })
                MemberInfoRow("Aluno IBR", if (member.isIbr) "Sim" else "Não")
                MemberInfoRow("Administrador", if (member.isAdmin) "Sim" else "Não")
                MemberInfoRow("Título", member.title)
                MemberInfoRow("Tipo", member.type)
                MemberInfoRow("Conteúdo", member.content)
                MemberInfoRow("Mídia", member.mediaUrl)
                MemberInfoRow("ID", member.id)
                if (member.createdAt > 0L) MemberInfoRow("Cadastrado em", dateFormat.format(java.util.Date(member.createdAt)))
                if (member.updatedAt > 0L) MemberInfoRow("Atualizado em", dateFormat.format(java.util.Date(member.updatedAt)))
                MemberInfoRow("Certificado IBR", member.ibrCertificateUrl)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun MemberInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value.ifBlank { "Não informado" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
