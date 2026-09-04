package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
    var selectedAvatarId by remember { mutableStateOf(loggedInMember.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }) }
    var equippedBadgeId by remember { mutableStateOf(loggedInMember.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID }) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var isEditingBirthDate by remember { mutableStateOf(false) }
    var isEditingEmail by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showAvatarPreview by remember { mutableStateOf(false) }
    var showBadgePicker by remember { mutableStateOf(false) }
    var showAchievementProgress by remember { mutableStateOf(false) }
    var focusedBadgeId by remember { mutableStateOf<String?>(null) }
    var previewBadgeId by remember { mutableStateOf<String?>(null) }
    val selectedAvatar = biblicalAvatarForId(selectedAvatarId)
    val equippedBadge = biblicalBadgeForId(equippedBadgeId)
    val badgeProgress = calculateBadgeProgress(loggedInMember)
    val unlockedBadgeIds = badgeProgress.unlockedIds

    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.email, loggedInMember.avatarId, loggedInMember.equippedBadgeId, loggedInMember.unlockedBadgeIds) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = formatBirthDateInput(loggedInMember.birthDate)
        if (!isEditingEmail) email = loggedInMember.email
        selectedAvatarId = loggedInMember.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
        equippedBadgeId = loggedInMember.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID }
    }

    LaunchedEffect(badgeUnlockFocusState.value) {
        badgeUnlockFocusState.value?.let { badgeId ->
            focusedBadgeId = badgeId
            showBadgePicker = true
            badgeUnlockFocusState.value = null
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
                .imePadding().verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Seus dados de acesso e informações pessoais",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "As alterações deste perfil são sincronizadas com sua conta para recuperação em outro aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BiblicalAvatarWithBadge(
                        avatar = selectedAvatar,
                        badge = equippedBadge,
                        modifier = Modifier.size(96.dp),
                        onClick = { showAvatarPreview = true },
                        contentDescription = "Ver avatar bíblico de ${selectedAvatar.displayName} em tamanho ampliado"
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Seu avatar bíblico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(selectedAvatar.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nível ${equippedBadge.level ?: 1}: ${equippedBadge.name}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text("Toque no avatar para ampliar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showAvatarPicker = true }) {
                        Text("Trocar")
                    }
                }
                OutlinedButton(
                    onClick = { showBadgePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver emblemas e níveis")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAchievementProgress = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Progresso das conquistas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${badgeProgress.completedIbrLessons} aulas IBR concluídas • ${badgeProgress.completedIbrCourses} cursos concluídos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${badgeProgress.activityCounts[BadgeActivityKeys.DEVOTIONALS] ?: 0} devocionais • ${badgeProgress.activityCounts[BadgeActivityKeys.BOOKS] ?: 0} livros • ${badgeProgress.activityCounts[BadgeActivityKeys.VIDEOS] ?: 0} vídeos • ${badgeProgress.activityCounts[BadgeActivityKeys.BIBLE_CHAPTERS] ?: 0} capítulos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${badgeProgress.activityCounts[BadgeActivityKeys.PLAN_THEMES] ?: 0} temas • ${badgeProgress.activityCounts[BadgeActivityKeys.BIBLE_NEWS] ?: 0} notícias • ${badgeProgress.activityCounts[BadgeActivityKeys.AUDIOS] ?: 0} áudios • ${badgeProgress.activeMinutes} min ativos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    badgeProgress.nextLevel?.let { nextBadge ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Próximo: ${nextBadge.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text("${(badgeProgress.progressToNextLevel * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress = { badgeProgress.progressToNextLevel },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            nextBadge.requirement,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: Text(
                        "Todos os níveis principais foram alcançados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

    if (showAchievementProgress) {
        AchievementProgressDialog(
            progress = badgeProgress,
            onDismiss = { showAchievementProgress = false }
        )
    }

    if (showAvatarPreview) {
        Dialog(onDismissRequest = { showAvatarPreview = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Seu avatar bíblico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    BiblicalAvatarWithBadge(
                        avatar = selectedAvatar,
                        badge = equippedBadge,
                        modifier = Modifier.size(280.dp),
                        contentDescription = "Avatar ampliado de ${selectedAvatar.displayName}"
                    )
                    Text(selectedAvatar.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Nível ${equippedBadge.level ?: 1}: ${equippedBadge.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        equippedBadge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showAvatarPreview = false }) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("Escolha seu avatar bíblico") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(biblicalAvatarCatalog) { avatar ->
                        val isSelected = selectedAvatarId == avatar.id
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                                .clickable {
                                    val previousAvatarId = loggedInMember.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
                                    selectedAvatarId = avatar.id
                                    showAvatarPicker = false
                                    saveProfile(
                                        loggedInMember,
                                        name,
                                        phone,
                                        address,
                                        birthDate,
                                        loggedInMember.profilePhotoUrl,
                                        context,
                                        avatarId = avatar.id,
                                        showToast = false
                                    ) { synced, error ->
                                        if (synced) {
                                            android.widget.Toast.makeText(context, "Avatar ${avatar.displayName} salvo na sua conta.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedAvatarId = previousAvatarId
                                            android.widget.Toast.makeText(context, "Não foi possível salvar o avatar: ${error?.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BiblicalAvatarImage(
                                avatar = avatar,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape),
                                contentDescription = avatar.displayName
                            )
                            Text(avatar.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPicker = false }) { Text("Fechar") }
            }
        )
    }

    if (showBadgePicker) {
        AlertDialog(
            onDismissRequest = { showBadgePicker = false },
            title = { Text("Emblemas e níveis") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allBiblicalBadges) { badge ->
                        val isUnlocked = badge.id in unlockedBadgeIds
                        val isEquipped = badge.id == equippedBadgeId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (badge.id == focusedBadgeId) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(16.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    previewBadgeId = badge.id
                                    showBadgePicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEquipped) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isUnlocked) 0.55f else 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    BiblicalAvatarWithBadge(
                                        avatar = selectedAvatar,
                                        badge = badge,
                                        contentDescription = badge.name,
                                        modifier = Modifier.size(64.dp).alpha(if (isUnlocked) 1f else 0.28f)
                                    )
                                    if (!isUnlocked) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Bloqueado",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (badge.level != null) "Nível ${badge.level}: ${badge.name}" else badge.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        if (isUnlocked) badge.description else "Bloqueado: ${badge.requirement}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (badge.id == focusedBadgeId && isUnlocked) {
                                        Text(
                                            "Novo emblema desbloqueado · toque para usar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (isEquipped) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Emblema equipado", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBadgePicker = false }) { Text("Fechar") }
            }
        )
    }

    previewBadgeId?.let { badgeId ->
        val badge = allBiblicalBadges.firstOrNull { it.id == badgeId }
        if (badge != null) {
            val isUnlocked = badge.id in unlockedBadgeIds
            val isEquipped = badge.id == equippedBadgeId
            AlertDialog(
                onDismissRequest = {
                    previewBadgeId = null
                    showBadgePicker = true
                },
                title = {
                    Text(if (badge.level != null) "Nível ${badge.level}: ${badge.name}" else badge.name)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BiblicalAvatarWithBadge(
                                avatar = selectedAvatar,
                                badge = badge,
                                modifier = Modifier.size(230.dp),
                                contentDescription = "Visualização ampliada do emblema ${badge.name}"
                            )
                            if (!isUnlocked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Emblema bloqueado",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        badge.rarity?.let { rarity ->
                            Text(
                                rarity.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            if (isUnlocked) "Emblema conquistado" else "Ainda bloqueado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            badge.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isUnlocked) {
                            Text(
                                "Para desbloquear: ${badge.requirement}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (isUnlocked) {
                        Button(
                            onClick = {
                                val previousBadgeId = equippedBadgeId
                                equippedBadgeId = badge.id
                                previewBadgeId = null
                                focusedBadgeId = null
                                saveProfile(
                                    loggedInMember,
                                    name,
                                    phone,
                                    address,
                                    birthDate,
                                    loggedInMember.profilePhotoUrl,
                                    context,
                                    email = email,
                                    avatarId = selectedAvatarId,
                                    equippedBadgeId = badge.id,
                                    showToast = false
                                ) { synced, error ->
                                    if (!synced) {
                                        equippedBadgeId = previousBadgeId
                                        android.widget.Toast.makeText(
                                            context,
                                            "Não foi possível usar o emblema: ${error?.message ?: "verifique sua conexão"}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isEquipped
                        ) {
                            Text(if (isEquipped) "Em uso" else "Usar emblema")
                        }
                    } else {
                        TextButton(onClick = {
                            previewBadgeId = null
                            showBadgePicker = true
                        }) {
                            Text("Voltar")
                        }
                    }
                },
                dismissButton = if (isUnlocked) {
                    {
                        TextButton(onClick = {
                            previewBadgeId = null
                            showBadgePicker = true
                        }) {
                            Text("Voltar")
                        }
                    }
                } else null
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta?") },
            text = { Text("Seu perfil e progresso sincronizados permanecem na sua conta e podem ser recuperados em outro aparelho usando o mesmo telefone.") },
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
    avatarId: String = member.avatarId,
    equippedBadgeId: String = member.equippedBadgeId,
    showToast: Boolean = true,
    onResult: ((synced: Boolean, error: Exception?) -> Unit)? = null
) {
    val previousMember = member.copy()
    val phoneDigits = phone.filter(Char::isDigit)
    val normalizedPhone = if (phoneDigits.length in 12..13 && phoneDigits.startsWith("55")) phoneDigits.drop(2) else phoneDigits

    if (normalizedPhone.length !in 10..11) {
        onResult?.invoke(false, IllegalArgumentException("Telefone inválido"))
        if (showToast) {
            android.widget.Toast.makeText(context, "Digite um telefone válido com DDD.", android.widget.Toast.LENGTH_LONG).show()
        }
        return
    }

    member.name = name.trim()
    member.phone = normalizedPhone
    member.address = address.trim()
    member.birthDate = birthDate
    member.profilePhotoUrl = profilePhotoUrl
    member.supabaseStoragePath = profileStoragePath
    member.avatarId = avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
    member.equippedBadgeId = equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID }
    member.email = email.trim()
    member.updatedAt = System.currentTimeMillis()

    loggedInMemberState.value = member.copy()
    val idx = memberRequestsState.indexOfFirst { it.id == member.id }
    if (idx != -1) memberRequestsState[idx] = member.copy()

    MemberSessionClient.syncMemberState(
        context = context,
        member = member,
        identityPhone = previousMember.phone,
        onSuccess = {
            onResult?.invoke(true, null)
            if (showToast) {
                android.widget.Toast.makeText(context, "Perfil sincronizado na sua conta", android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        onFailure = { error ->
            member.name = previousMember.name
            member.phone = previousMember.phone
            member.address = previousMember.address
            member.birthDate = previousMember.birthDate
            member.profilePhotoUrl = previousMember.profilePhotoUrl
            member.supabaseStoragePath = previousMember.supabaseStoragePath
            member.avatarId = previousMember.avatarId
            member.equippedBadgeId = previousMember.equippedBadgeId
            member.email = previousMember.email
            member.updatedAt = previousMember.updatedAt
            loggedInMemberState.value = member.copy()
            val failedIndex = memberRequestsState.indexOfFirst { it.id == member.id }
            if (failedIndex >= 0) memberRequestsState[failedIndex] = member.copy()
            onResult?.invoke(false, error)
            if (showToast) {
                android.widget.Toast.makeText(context, "Erro ao sincronizar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )
}
