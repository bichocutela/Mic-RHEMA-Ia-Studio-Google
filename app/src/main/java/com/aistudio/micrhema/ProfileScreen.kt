package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(loggedInMember.name) }
    var phone by remember { mutableStateOf(loggedInMember.phone) }
    var address by remember { mutableStateOf(loggedInMember.address) }
    var birthDate by remember { mutableStateOf(formatBirthDateInput(loggedInMember.birthDate)) }
    var email by remember { mutableStateOf(loggedInMember.email) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var isEditingBirthDate by remember { mutableStateOf(false) }
    var isEditingEmail by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.email) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = formatBirthDateInput(loggedInMember.birthDate)
        if (!isEditingEmail) email = loggedInMember.email
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
            Text(
                "Seus dados de acesso e informações pessoais",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    saveProfile(loggedInMember, name, phone, address, birthDate, loggedInMember.profilePhotoUrl, context)
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
                    saveProfile(loggedInMember, name, phone, address, birthDate, loggedInMember.profilePhotoUrl, context)
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
                    saveProfile(loggedInMember, name, phone, address, birthDate, loggedInMember.profilePhotoUrl, context)
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
                    saveProfile(loggedInMember, name, phone, address, birthDate, loggedInMember.profilePhotoUrl, context)
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
                            loggedInMember.profilePhotoUrl,
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
