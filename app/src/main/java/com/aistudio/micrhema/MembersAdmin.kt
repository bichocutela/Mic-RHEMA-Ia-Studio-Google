package com.aistudio.micrhema
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun EditMembersSection() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Todos") }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf("") }
    var isSavingMember by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // A tela de membros sempre reabre a sincronização para listar os documentos atuais.
        MemberManager.syncFromFirestore(context)
    }

    val filteredMembers = memberRequestsState.filter { member ->
        val matchesQuery = searchQuery.isBlank() || member.name.contains(searchQuery, ignoreCase = true) || member.phone.contains(searchQuery, ignoreCase = true)
        val matchesStatus = when (statusFilter) {
            "Pendentes" -> !member.isApproved && !member.isIbr
            "Aprovados" -> member.isApproved || member.isIbr
            "IBR" -> member.isIbr
            else -> true
        }
        matchesQuery && matchesStatus
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gerenciar Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Aprovações, permissões e perfis sincronizados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = {
                newMemberName = ""
                newMemberPhone = ""
                showAddMemberDialog = true
            }) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Adicionar")
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar membros") },
            label = { Text("Buscar por nome ou telefone") }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Pendentes", "Aprovados", "IBR").forEach { filter ->
                FilterChip(selected = statusFilter == filter, onClick = { statusFilter = filter }, label = { Text(filter) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("${filteredMembers.size} membro(s) encontrado(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        if (filteredMembers.isEmpty()) {
            AdminEmptyState("Nenhum membro encontrado", "Tente outro nome, telefone ou filtro de status.")
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = filteredMembers, key = { it.id }) { member ->
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
                            AdminStatusChip(
                                text = when {
                                    member.isIbr -> "IBR"
                                    member.isApproved -> "Aprovado"
                                    else -> "Pendente"
                                },
                                positive = member.isApproved || member.isIbr
                            )
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

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingMember) showAddMemberDialog = false },
            title = { Text("Adicionar membro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "O cadastro será enviado ao Firestore como Pendente. O acesso só será liberado após a aprovação do ADM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Nome completo") },
                        singleLine = true,
                        enabled = !isSavingMember
                    )
                    OutlinedTextField(
                        value = newMemberPhone,
                        onValueChange = { newMemberPhone = it.filter(Char::isDigit).take(11) },
                        label = { Text("Número de telefone") },
                        placeholder = { Text("DDD + número") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = !isSavingMember
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSavingMember && newMemberName.trim().isNotBlank() && newMemberPhone.filter(Char::isDigit).length >= 10,
                    onClick = {
                        val completeName = newMemberName.trim()
                        val cleanPhone = newMemberPhone.filter(Char::isDigit)
                        val duplicate = memberRequestsState.any { it.phone.filter(Char::isDigit) == cleanPhone }
                        if (duplicate) {
                            Toast.makeText(context, "Já existe um membro com este telefone.", Toast.LENGTH_LONG).show()
                        } else {
                            isSavingMember = true
                        val member = MemberRequest(
                            id = java.util.UUID.randomUUID().toString(),
                            name = completeName,
                            ibrCertificateName = completeName,
                            phone = cleanPhone,
                            isApproved = false,
                            isVip = false,
                            isIbr = false,
                            status = "pendente",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        MemberManager.saveToFirestore(
                            context = context,
                            member = member,
                            onSuccess = {
                                isSavingMember = false
                                showAddMemberDialog = false
                                Toast.makeText(context, "Membro cadastrado como pendente.", Toast.LENGTH_SHORT).show()
                                MemberManager.syncFromFirestore(context)
                            },
                            onFailure = { error ->
                                isSavingMember = false
                                Toast.makeText(context, "Não foi possível cadastrar: ${error.message ?: "verifique a conexão"}", Toast.LENGTH_LONG).show()
                            }
                        )
                        }
                    }
                ) {
                    if (isSavingMember) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Cadastrar pendente")
                }
            },
            dismissButton = {
                TextButton(enabled = !isSavingMember, onClick = { showAddMemberDialog = false }) { Text("Cancelar") }
            }
        )
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
                val upload = StorageManager.uploadProfilePhoto(context, uri, target.id)
                val updated = target.copy(
                    profilePhotoUrl = upload.signedUrl,
                    supabaseStoragePath = upload.storagePath,
                    updatedAt = System.currentTimeMillis()
                )
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
                            runCatching { StorageManager.deleteProfilePhoto(target.id, context) }
                            isUploadingPhoto = false
                            Toast.makeText(context, "Não foi possível sincronizar a foto no perfil: ${error.message ?: "verifique sua conexão com o Supabase"}", Toast.LENGTH_LONG).show()
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
            onChangePhoto = { photoLauncher.launch(arrayOf("image/*")) },
            onRemovePhoto = {
                if (isUploadingPhoto) return@MemberAdminDetailsDialog
                val previousPhotoUrl = member.profilePhotoUrl
                val previousStoragePath = member.supabaseStoragePath
                isUploadingPhoto = true
                val updated = member.copy(profilePhotoUrl = "", supabaseStoragePath = "", updatedAt = System.currentTimeMillis())
                val index = memberRequestsState.indexOfFirst { it.id == member.id }
                if (index >= 0) memberRequestsState[index] = updated
                selectedMember = updated
                MemberManager.saveToFirestore(
                    context = context,
                    member = updated,
                    onSuccess = {
                        coroutineScope.launch {
                            try {
                                if (previousStoragePath.isNotBlank() || previousPhotoUrl.startsWith("http://") || previousPhotoUrl.startsWith("https://")) {
                                    StorageManager.deleteProfilePhoto(member.id, context)
                                }
                                StorageManager.deleteLocalProfilePhoto(context, member.id)
                                Toast.makeText(context, "Foto removida do perfil sincronizado", Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                Toast.makeText(context, "Perfil atualizado, mas o arquivo remoto precisa ser removido no Supabase", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploadingPhoto = false
                            }
                        }
                    },
                    onFailure = { error ->
                        if (index >= 0) memberRequestsState[index] = member
                        selectedMember = member
                        isUploadingPhoto = false
                        Toast.makeText(context, "Não foi possível remover a foto do perfil: ${error.message ?: "verifique sua conexão com o Supabase"}", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { selectedMember = null }
        )
    }
}

@Composable
private fun MemberAvatar(member: MemberRequest, size: androidx.compose.ui.unit.Dp) {
    val avatar = biblicalAvatarForId(member.avatarId)
    BiblicalAvatarImage(
        avatar = avatar,
        contentDescription = "Avatar bíblico de ${member.name}",
        modifier = Modifier.size(size).clip(CircleShape)
    )
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
                Text("Avatar bíblico: ${biblicalAvatarForId(member.avatarId).displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                MemberInfoRow("Nome completo do certificado IBR", member.ibrCertificateName)
                MemberInfoRow("E-mail para certificado IBR", member.email)
                MemberInfoRow("Endereço", member.address)
                MemberInfoRow("Nascimento", member.birthDate)
                MemberInfoRow("Status", member.status.ifBlank { if (member.isApproved) "Aprovado" else "Pendente" })
                MemberInfoRow("Aluno IBR", if (member.isIbr) "Sim" else "Não")
                MemberInfoRow("Administrador", if (member.isAdmin) "Sim" else "Não")
                MemberInfoRow("Firebase UID", member.firebaseUid)
                MemberInfoRow("Caminho da foto remota", member.supabaseStoragePath)
                MemberInfoRow("Caminho do certificado", member.ibrCertificateStoragePath)
                MemberInfoRow("Título", member.title)
                MemberInfoRow("Tipo", member.type)
                MemberInfoRow("Conteúdo", member.content)
                MemberInfoRow("Mídia", member.mediaUrl)
                MemberInfoRow("ID", member.id)
                if (member.createdAt > 0L) MemberInfoRow("Cadastrado em", dateFormat.format(java.util.Date(member.createdAt)))
                if (member.updatedAt > 0L) MemberInfoRow("Atualizado em", dateFormat.format(java.util.Date(member.updatedAt)))
                MemberInfoRow("Certificado IBR", member.ibrCertificateUrl)
                MemberInfoRow("Foto remota legada", member.profilePhotoUrl)
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
