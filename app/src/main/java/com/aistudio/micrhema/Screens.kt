package com.aistudio.micrhema
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background

import androidx.compose.ui.unit.sp


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

@Composable
fun MembersScreen() {
    val loggedInMember = loggedInMemberState.value
    if (loggedInMember == null) {
        LoginScreen(onLoginSuccess = {})
        return
    }
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Área de Membros",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!loggedInMember.isApproved) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Solicitação Enviada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aguarde a aprovação do administrador para acessar os conteúdos exclusivos. Você será notificado ou poderá verificar aqui mais tarde.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { MemberManager.setLoggedInMember(context, null) }) {
                        Text("Sair")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Olá, ${loggedInMember.name}!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(loggedInMember.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                if (loggedInMember.isIbr) AssistChip(onClick = {}, label = { Text("Aluno IBR") }, leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) })
                            }
                        }
                    }
                    


                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Meus Favoritos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (favoriteItemsState.isEmpty()) {
                        Text("Você ainda não adicionou nenhum devocional ou versículo aos favoritos.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteItemsState) { fav ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(), 
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(fav.reference, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(fav.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            }
                                            IconButton(onClick = {
                                                BibleReadingPreferences.removeLocalFavorite(context, fav.id)
                                                removeFavorite(fav.id)
                                            }) {
                                                Icon(Icons.Default.Favorite, contentDescription = "Remover favorito", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (favoriteItemsState.isEmpty()) Spacer(modifier = Modifier.weight(1f))
                    else Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { MemberManager.setLoggedInMember(context, null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sair da Conta")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loggedIn = loggedInMemberState.value
    var isAuthenticated by adminAuthenticatedState
    
    LaunchedEffect(loggedIn) {
        if (loggedIn?.isAdmin == true) {
            isAuthenticated = true
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            MemberManager.syncFromFirestore(context)
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (firebaseAuth.currentUser == null) {
                firebaseAuth.signInAnonymously().addOnCompleteListener {
                    if (it.isSuccessful) MemberManager.syncFromFirestore(context)
                    else android.util.Log.w("AdminScreen", "Não foi possível autenticar a sessão interna do painel", it.exception)
                }
            } else {
                MemberManager.syncFromFirestore(context)
            }
        }
    }
    
    val adminUiPrefs = remember {
        context.getSharedPreferences("micrhema_admin_ui", android.content.Context.MODE_PRIVATE)
    }
    var adminFontScale by remember {
        mutableFloatStateOf(
            adminUiPrefs.getFloat("admin_font_scale", 1f).coerceIn(0.8f, 1.5f)
        )
    }

    fun updateAdminFontScale(value: Float) {
        val normalized = value.coerceIn(0.8f, 1.5f)
        adminFontScale = normalized
        adminUiPrefs.edit().putFloat("admin_font_scale", normalized).apply()
    }
    
    // O painel herda o mesmo esquema global para refletir Claro/Escuro e a cor de destaque escolhida.
    val appColorScheme = MaterialTheme.colorScheme
    val baseTypography = MaterialTheme.typography
    val adminTypography = Typography(
        displayLarge = baseTypography.displayLarge.copy(fontSize = baseTypography.displayLarge.fontSize * adminFontScale),
        displayMedium = baseTypography.displayMedium.copy(fontSize = baseTypography.displayMedium.fontSize * adminFontScale),
        displaySmall = baseTypography.displaySmall.copy(fontSize = baseTypography.displaySmall.fontSize * adminFontScale),
        headlineLarge = baseTypography.headlineLarge.copy(fontSize = baseTypography.headlineLarge.fontSize * adminFontScale),
        headlineMedium = baseTypography.headlineMedium.copy(fontSize = baseTypography.headlineMedium.fontSize * adminFontScale),
        headlineSmall = baseTypography.headlineSmall.copy(fontSize = baseTypography.headlineSmall.fontSize * adminFontScale),
        titleLarge = baseTypography.titleLarge.copy(fontSize = baseTypography.titleLarge.fontSize * adminFontScale),
        titleMedium = baseTypography.titleMedium.copy(fontSize = baseTypography.titleMedium.fontSize * adminFontScale),
        titleSmall = baseTypography.titleSmall.copy(fontSize = baseTypography.titleSmall.fontSize * adminFontScale),
        bodyLarge = baseTypography.bodyLarge.copy(fontSize = baseTypography.bodyLarge.fontSize * adminFontScale),
        bodyMedium = baseTypography.bodyMedium.copy(fontSize = baseTypography.bodyMedium.fontSize * adminFontScale),
        bodySmall = baseTypography.bodySmall.copy(fontSize = baseTypography.bodySmall.fontSize * adminFontScale),
        labelLarge = baseTypography.labelLarge.copy(fontSize = baseTypography.labelLarge.fontSize * adminFontScale),
        labelMedium = baseTypography.labelMedium.copy(fontSize = baseTypography.labelMedium.fontSize * adminFontScale),
        labelSmall = baseTypography.labelSmall.copy(fontSize = baseTypography.labelSmall.fontSize * adminFontScale)
    )

    MaterialTheme(
        colorScheme = appColorScheme,
        typography = adminTypography
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (isAuthenticated) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Painel Administrativo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { updateAdminFontScale(adminFontScale - 0.1f) }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Diminuir Fonte", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("Aa", fontSize = 16.sp * adminFontScale, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { updateAdminFontScale(adminFontScale + 0.1f) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Aumentar Fonte", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { 
                                isAuthenticated = false 
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (!isAuthenticated) {
                var password by remember { mutableStateOf("") }
                var error by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Acesso Restrito", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Digite a senha de administrador:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = "" },
                        label = { Text("Senha") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        singleLine = true,
                        isError = error.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (password == "igreja10" || loggedIn?.isAdmin == true) {
                            isAuthenticated = true
                        } else {
                            error = "Senha incorreta"
                        }
                    }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text("Entrar")
                    }
                }
                return@Scaffold
            }

            var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }

            BackHandler(enabled = currentSection != AdminSection.DASHBOARD) {
                currentSection = AdminSection.DASHBOARD
            }

            if (currentSection == AdminSection.DASHBOARD) {
                AdminDashboard(
                    onNavigate = { currentSection = it },
                    paddingValues = paddingValues
                )
            } else {
                val sectionName = when(currentSection) {
                    AdminSection.DEVOTIONALS -> "Devocionais"
                    AdminSection.NEWS -> "Notícias"
                    AdminSection.MEDIA -> "Mídia"
                    AdminSection.PLANS -> "Planos Bíblicos"
                    AdminSection.IBR -> "Instituto Bíblico Rhema"
                    AdminSection.DISCIPULADO -> "Discipulado"
                    AdminSection.SERVICES -> "Cultos"
                    AdminSection.BANNERS -> "Destaques"
                    AdminSection.DONATIONS -> "Dízimos e Ofertas"
                    AdminSection.TEAM -> "Equipe"
                    AdminSection.MEMBERS -> "Membros"
                    AdminSection.PROFILES -> "Perfis dos Membros"
                    AdminSection.TABS -> "Abas do Aplicativo"
                    AdminSection.SETTINGS -> "Configurações"
                    AdminSection.ABOUT -> "Sobre"
                    else -> ""
                }
                
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentSection = AdminSection.DASHBOARD }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Voltar para o Painel Administrativo", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Painel Administrativo",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (sectionName.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = sectionName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (currentSection) {
                            AdminSection.TABS -> AdminTabsScreen()
                            AdminSection.PLANS -> EditPlansSection()
                            AdminSection.SERVICES -> EditServicesSection()
                            AdminSection.DEVOTIONALS -> EditDevotionalsSection()
                            AdminSection.NEWS -> EditNewsSection()
                            AdminSection.MEDIA -> EditMediaSection()
                            AdminSection.IBR -> EditVipSection()
                            AdminSection.DISCIPULADO -> EditDiscipuladoSection()
                            AdminSection.TEAM -> EditTeamSection()
                            AdminSection.MEMBERS -> EditMembersSection()
                            AdminSection.ABOUT -> EditAboutSection()
                            AdminSection.BANNERS -> EditBannersSection()
                            AdminSection.SETTINGS -> EditSettingsSection()
                            AdminSection.DONATIONS -> EditDonationsSection()
                            AdminSection.PROFILES -> EditProfilesSection()
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
