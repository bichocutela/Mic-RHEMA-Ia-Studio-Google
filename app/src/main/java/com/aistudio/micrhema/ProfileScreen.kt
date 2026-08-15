package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit
) {
    val loggedInMember = loggedInMemberState.value
    if (loggedInMember == null) {
        onNavigateBack()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(loggedInMember.name) }
    var phone by remember { mutableStateOf(loggedInMember.phone) }
    var address by remember { mutableStateOf(loggedInMember.address) }
    var birthDate by remember { mutableStateOf(loggedInMember.birthDate) }
    var profilePhotoUrl by remember { mutableStateOf(loggedInMember.profilePhotoUrl) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var isEditingBirthDate by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.profilePhotoUrl) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = loggedInMember.birthDate
        profilePhotoUrl = loggedInMember.profilePhotoUrl
    }

    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                try {
                    val uploadedUrl = com.aistudio.micrhema.StorageManager.uploadProfilePhotoToFirebase(context, uri, loggedInMember.id)
                    profilePhotoUrl = uploadedUrl
                    saveProfile(
                        loggedInMember,
                        name,
                        phone,
                        address,
                        birthDate,
                        profilePhotoUrl,
                        context,
                        showToast = false
                    ) { synced, error ->
                        val message = if (synced && uploadedUrl.startsWith("http")) {
                            "Foto atualizada e sincronizada"
                        } else if (synced) {
                            "Foto salva neste aparelho"
                        } else {
                            "Foto salva neste aparelho; não foi sincronizada: ${error?.message ?: "verifique sua conexão"}"
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "Erro ao fazer upload da foto", e)
                    android.widget.Toast.makeText(context, "Erro ao atualizar foto: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable { imageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = profilePhotoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = name.firstOrNull()?.toString()?.uppercase() ?: "👤"
                    if (initial == "👤") {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Alterar foto",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (isUploading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            if (profilePhotoUrl.isNotBlank()) {
                TextButton(
                    onClick = {
                        StorageManager.deleteLocalProfilePhoto(context, loggedInMember.id)
                        profilePhotoUrl = ""
                        saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Remover Foto", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Fields
            ProfileField(
                label = "Nome completo",
                value = name,
                isEditing = isEditingName,
                onValueChange = { name = it },
                onEditClick = { isEditingName = true },
                onSaveClick = { 
                    isEditingName = false
                    saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                }
            )

            ProfileField(
                label = "Telefone",
                value = phone,
                isEditing = isEditingPhone,
                onValueChange = { phone = it },
                onEditClick = { isEditingPhone = true },
                onSaveClick = { 
                    isEditingPhone = false
                    saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                }
            )

            ProfileField(
                label = "Endereço",
                value = address,
                isEditing = isEditingAddress,
                onValueChange = { address = it },
                onEditClick = { isEditingAddress = true },
                onSaveClick = { 
                    isEditingAddress = false
                    saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                }
            )

            ProfileField(
                label = "Data de nascimento",
                value = birthDate,
                isEditing = isEditingBirthDate,
                onValueChange = { birthDate = it },
                onEditClick = { isEditingBirthDate = true },
                onSaveClick = { 
                    isEditingBirthDate = false
                    saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                }
            )
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = value.takeIf { it.isNotBlank() } ?: "Não informado",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun saveProfile(
    member: MemberRequest,
    name: String,
    phone: String,
    address: String,
    birthDate: String,
    profilePhotoUrl: String,
    context: android.content.Context,
    showToast: Boolean = true,
    onResult: ((synced: Boolean, error: Exception?) -> Unit)? = null
) {
    member.name = name
    member.phone = phone
    member.address = address
    member.birthDate = birthDate
    member.profilePhotoUrl = profilePhotoUrl
    member.updatedAt = System.currentTimeMillis()
    
    // Update local state to reflect instantly in Drawer
    loggedInMemberState.value = member.copy()
    
    val idx = memberRequestsState.indexOfFirst { it.id == member.id }
    if (idx != -1) {
        memberRequestsState[idx] = member
    }

    MemberManager.saveToFirestore(
        context = context,
        member = member,
        onSuccess = {
            onResult?.invoke(true, null)
            if (showToast) {
                android.widget.Toast.makeText(context, "Perfil atualizado", android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        onFailure = { error ->
            onResult?.invoke(false, error)
            if (showToast) {
                android.widget.Toast.makeText(context, "Erro ao salvar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )
}
