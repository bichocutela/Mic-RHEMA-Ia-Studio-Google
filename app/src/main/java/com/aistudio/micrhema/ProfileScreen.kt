package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private fun formatBirthDateInput(value: String): String {
    val digits = value.filter { it.isDigit() }
    val normalized = when {
        digits.length >= 8 -> digits.take(2) + digits.substring(2, 4) + digits.substring(6, 8)
        else -> digits.take(6)
    }
    return buildString {
        normalized.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('/')
            append(char)
        }
    }
}

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
    var birthDate by remember { mutableStateOf(formatBirthDateInput(loggedInMember.birthDate)) }
    var profilePhotoUrl by remember { mutableStateOf(loggedInMember.profilePhotoUrl) }
    var profileStoragePath by remember { mutableStateOf(loggedInMember.supabaseStoragePath) }
    var email by remember { mutableStateOf(loggedInMember.email) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var isEditingBirthDate by remember { mutableStateOf(false) }
    var isEditingEmail by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPhotoAuthDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.email, loggedInMember.profilePhotoUrl, loggedInMember.supabaseStoragePath) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = formatBirthDateInput(loggedInMember.birthDate)
        if (!isEditingEmail) email = loggedInMember.email
        profilePhotoUrl = loggedInMember.profilePhotoUrl
        profileStoragePath = loggedInMember.supabaseStoragePath
    }

    fun uploadOwnProfilePhoto(uri: android.net.Uri) {
        isUploading = true
        coroutineScope.launch {
            try {
                val upload = com.aistudio.micrhema.StorageManager.uploadProfilePhoto(context, uri, loggedInMember.id)
                profilePhotoUrl = upload.signedUrl
                profileStoragePath = upload.storagePath
                saveProfile(
                    loggedInMember,
                    name,
                    phone,
                    address,
                    birthDate,
                    profilePhotoUrl,
                    context,
                    profileStoragePath = profileStoragePath,
                    showToast = false
                ) { synced, error ->
                    val message = if (synced && profileStoragePath.isNotBlank()) {
                        "Foto atualizada e sincronizada no perfil"
                    } else if (synced) {
                        "Perfil atualizado, mas confirme a configuração do armazenamento remoto"
                    } else {
                        "Não foi possível sincronizar o perfil: ${error?.message ?: "verifique sua conexão"}"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "Erro ao fazer upload da foto", e)
                android.widget.Toast.makeText(
                    context,
                    "Não foi possível atualizar a foto: ${e.message ?: "verifique sua conexão"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } finally {
                isUploading = false
            }
        }
    }

    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val authenticated = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == false
            if (authenticated) {
                uploadOwnProfilePhoto(uri)
            } else {
                pendingPhotoUri = uri
                showPhotoAuthDialog = true
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
                    .clickable { imageLauncher.launch(arrayOf("image/*")) },
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
                        if (isUploading) return@TextButton
                        val previousPhotoUrl = profilePhotoUrl
                        val previousStoragePath = profileStoragePath
                        isUploading = true
                        profilePhotoUrl = ""
                        profileStoragePath = ""
                        saveProfile(
                            loggedInMember,
                            name,
                            phone,
                            address,
                            birthDate,
                            profilePhotoUrl,
                            context,
                            profileStoragePath = profileStoragePath,
                            showToast = false
                        ) { synced, error ->
                            if (!synced) {
                                profilePhotoUrl = previousPhotoUrl
                                profileStoragePath = previousStoragePath
                                android.widget.Toast.makeText(
                                    context,
                                    "Não foi possível remover a foto do perfil: ${error?.message ?: "verifique sua conexão"}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                isUploading = false
                            } else {
                                coroutineScope.launch {
                                    try {
                                        if (previousStoragePath.isNotBlank() || previousPhotoUrl.startsWith("http://") || previousPhotoUrl.startsWith("https://")) {
                                            StorageManager.deleteProfilePhoto(loggedInMember.id, context)
                                        }
                                        StorageManager.deleteLocalProfilePhoto(context, loggedInMember.id)
                                        android.widget.Toast.makeText(context, "Foto removida do perfil sincronizado", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.util.Log.w("ProfileScreen", "Perfil atualizado, mas não foi possível excluir o arquivo remoto", e)
                                        android.widget.Toast.makeText(context, "Perfil atualizado, mas o arquivo remoto precisa ser removido no Supabase", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            }
                        }
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
                onValueChange = { phone = it.filter { character -> character.isDigit() }.take(15) },
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
                onValueChange = { birthDate = formatBirthDateInput(it) },
                onEditClick = { isEditingBirthDate = true },
                onSaveClick = {
                    isEditingBirthDate = false
                    saveProfile(loggedInMember, name, phone, address, birthDate, profilePhotoUrl, context)
                }
            )

            ProfileField(
                label = "E-mail para envio do certificado IBR",
                value = email,
                isEditing = isEditingEmail,
                onValueChange = { email = it },
                onEditClick = { isEditingEmail = true },
                onSaveClick = {
                    val normalizedEmail = email.trim()
                    if (normalizedEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                        android.widget.Toast.makeText(context, "Digite um e-mail válido para receber o certificado.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        email = normalizedEmail
                        isEditingEmail = false
                        saveProfile(
                            loggedInMember,
                            name,
                            phone,
                            address,
                            birthDate,
                            profilePhotoUrl,
                            context,
                            email = normalizedEmail
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da conta", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPhotoAuthDialog) {
        ProfilePhotoAuthDialog(
            member = loggedInMember,
            onAuthenticated = {
                showPhotoAuthDialog = false
                val uri = pendingPhotoUri
                pendingPhotoUri = null
                if (uri != null) uploadOwnProfilePhoto(uri)
            },
            onDismiss = {
                showPhotoAuthDialog = false
                pendingPhotoUri = null
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta?") },
            text = { Text("Seus favoritos e dados salvos no aparelho serão preservados. Você poderá entrar novamente depois.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        MemberManager.setLoggedInMember(context, null)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
    val isPhoneField = label.equals("Telefone", ignoreCase = true)
    val isBirthDateField = label.equals("Data de nascimento", ignoreCase = true)
    val displayValue = if (isBirthDateField) formatBirthDateInput(value) else value

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
                        value = displayValue,
                        onValueChange = { input ->
                            when {
                                isBirthDateField -> onValueChange(formatBirthDateInput(input))
                                isPhoneField -> onValueChange(input.filter { character -> character.isDigit() }.take(15))
                                else -> onValueChange(input)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPhoneField || isBirthDateField) KeyboardType.Number else KeyboardType.Text
                        ),
                        placeholder = if (isBirthDateField) ({ Text("dd/mm/aa") }) else null,
                        supportingText = if (isBirthDateField) ({ Text("Digite 6 números: dia, mês e ano") }) else null
                    )
                    IconButton(onClick = onSaveClick) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = displayValue.takeIf { it.isNotBlank() } ?: "Não informado",
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
    email: String = member.email,
    profileStoragePath: String = member.supabaseStoragePath,
    showToast: Boolean = true,
    onResult: ((synced: Boolean, error: Exception?) -> Unit)? = null
) {
    val previousMember = member.copy()
    member.name = name
    member.phone = phone
    member.address = address
    member.birthDate = birthDate
    member.profilePhotoUrl = profilePhotoUrl
    member.supabaseStoragePath = profileStoragePath
    member.email = email.trim()
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
            member.name = previousMember.name
            member.phone = previousMember.phone
            member.address = previousMember.address
            member.birthDate = previousMember.birthDate
            member.profilePhotoUrl = previousMember.profilePhotoUrl
            member.supabaseStoragePath = previousMember.supabaseStoragePath
            member.email = previousMember.email
            member.updatedAt = previousMember.updatedAt
            loggedInMemberState.value = member.copy()
            val failedIndex = memberRequestsState.indexOfFirst { it.id == member.id }
            if (failedIndex >= 0) memberRequestsState[failedIndex] = member
            onResult?.invoke(false, error)
            if (showToast) {
                android.widget.Toast.makeText(context, "Erro ao sincronizar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )
}
