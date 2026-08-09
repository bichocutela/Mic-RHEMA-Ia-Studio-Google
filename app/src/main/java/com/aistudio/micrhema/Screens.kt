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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                                if (loggedInMember.isVip) AssistChip(onClick = {}, label = { Text("VIP") }, leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) })
                                if (loggedInMember.isIbr) AssistChip(onClick = {}, label = { Text("Aluno IBR") }, leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) })
                            }
                        }
                    }
                    
                    if (loggedInMember.isVip) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Conteúdo VIP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Você tem acesso aos livros, áudios e vídeos exclusivos da aba Conteúdo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
                                            IconButton(onClick = { removeFavorite(fav.id) }) {
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
    
    var adminFontScale by remember { mutableFloatStateOf(1f) }
    
    val adminColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF8C9EFF),
        secondary = androidx.compose.ui.graphics.Color(0xFFFF8A80),
        background = androidx.compose.ui.graphics.Color(0xFF1E1E2C),
        surface = androidx.compose.ui.graphics.Color(0xFF2D2D44),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3B3B5A),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        onSecondary = androidx.compose.ui.graphics.Color.White,
        onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
        onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
        onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0B0C0)
    )
    
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
        colorScheme = adminColors,
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
                            IconButton(onClick = { if (adminFontScale > 0.8f) adminFontScale -= 0.1f }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Diminuir Fonte", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("Aa", fontSize = 16.sp * adminFontScale, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { if (adminFontScale < 1.5f) adminFontScale += 0.1f }) {
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

            if (currentSection == AdminSection.DASHBOARD) {
                AdminDashboard(
                    onNavigate = { currentSection = it },
                    paddingValues = paddingValues
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentSection = AdminSection.DASHBOARD }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Voltar", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voltar ao Painel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when (currentSection) {
                            AdminSection.TABS -> AdminTabsScreen()
                            AdminSection.PLANS -> EditPlansSection()
                            AdminSection.SERVICES -> EditServicesSection()
                            AdminSection.DEVOTIONALS -> EditDevotionalsSection()
                            AdminSection.NEWS -> EditNewsSection()
                            AdminSection.CONTENT -> EditContentSection()
                            AdminSection.VIP -> EditVipSection()
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
