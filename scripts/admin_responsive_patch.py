from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def path(rel: str) -> Path:
    return ROOT / rel


def replace(rel: str, old: str, new: str, count: int = 1) -> None:
    p = path(rel)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"marker not found in {rel}: {old[:100]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")


def block1() -> None:
    helper = r'''package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminResponsiveTopBar(
    title: String,
    fontScale: Float,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onExit: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val compact = maxWidth < 380.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (compact) "Painel ADM" else title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val actionSize = if (compact) 40.dp else 48.dp
            IconButton(onClick = onDecreaseFont, modifier = Modifier.size(actionSize)) {
                Icon(Icons.Default.Clear, contentDescription = "Diminuir Fonte", tint = MaterialTheme.colorScheme.onSurface)
            }
            if (!compact) Text("Aa", fontSize = 16.sp * fontScale, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onIncreaseFont, modifier = Modifier.size(actionSize)) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar Fonte", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onExit, modifier = Modifier.size(actionSize)) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AdminSectionBackHeader(sectionName: String, onBack: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 380.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 10.dp else 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar para o Painel Administrativo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Painel Administrativo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (sectionName.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    sectionName,
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AdminActionHeader(
    title: String,
    subtitle: String = "",
    actionText: String,
    actionIcon: ImageVector? = Icons.Default.Add,
    onAction: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 400.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    actionIcon?.let { Icon(it, contentDescription = null); Spacer(Modifier.width(6.dp)) }
                    Text(actionText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                Button(onClick = onAction) {
                    actionIcon?.let { Icon(it, contentDescription = null); Spacer(Modifier.width(6.dp)) }
                    Text(actionText, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AdminSafeHorizontalRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun AdminAdaptivePair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 380.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}
'''
    path("app/src/main/java/com/aistudio/micrhema/AdminResponsive.kt").write_text(helper, encoding="utf-8")

    replace(
        "app/src/main/java/com/aistudio/micrhema/Screens.kt",
        '''                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Painel Administrativo",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
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
                                adminPrayerTargetState.value = null
                                runCatching {
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("prayer_admins")
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                }
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }''',
        '''                    AdminResponsiveTopBar(
                        title = "Painel Administrativo",
                        fontScale = adminFontScale,
                        onDecreaseFont = { updateAdminFontScale(adminFontScale - 0.1f) },
                        onIncreaseFont = { updateAdminFontScale(adminFontScale + 0.1f) },
                        onExit = {
                            isAuthenticated = false
                            adminPrayerTargetState.value = null
                            runCatching {
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("prayer_admins")
                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            }
                        }
                    )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/Screens.kt",
        '''                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (currentSection == AdminSection.PRAYERS) adminPrayerTargetState.value = null
                                currentSection = AdminSection.DASHBOARD
                            }
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
                    }''',
        '''                    AdminSectionBackHeader(sectionName = sectionName) {
                        if (currentSection == AdminSection.PRAYERS) adminPrayerTargetState.value = null
                        currentSection = AdminSection.DASHBOARD
                    }'''
    )


def block2() -> None:
    p = path("app/src/main/java/com/aistudio/micrhema/MembersAdmin.kt")
    text = p.read_text(encoding="utf-8")
    old = '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gerenciar Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Aprovações, permissões e dados de cadastro sincronizados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        }'''
    new = '''        AdminActionHeader(
            title = "Gerenciar Membros",
            subtitle = "Aprovações, permissões e dados de cadastro sincronizados.",
            actionText = "Adicionar",
            actionIcon = Icons.Default.Person,
            onAction = {
                newMemberName = ""
                newMemberPhone = ""
                showAddMemberDialog = true
            }
        )'''
    if old not in text: raise SystemExit("members header marker")
    text = text.replace(old, new, 1)
    text = text.replace('''        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Pendentes", "Aprovados", "IBR").forEach { filter ->''', '''        AdminSafeHorizontalRow {
            listOf("Todos", "Pendentes", "Aprovados", "IBR").forEach { filter ->''', 1)
    text = text.replace('''                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(''', '''                            AdminSafeHorizontalRow {
                                Checkbox(''', 1)
    p.write_text(text, encoding="utf-8")

    replace(
        "app/src/main/java/com/aistudio/micrhema/NewsAdmin.kt",
        '''        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Avisos / Notícias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingNews = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }''',
        '''        AdminActionHeader(
            title = "Gerenciar Avisos / Notícias",
            actionText = "Novo",
            onAction = { showDialog = true; editingNews = null }
        )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/NewsAdmin.kt",
        '''        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = intensityFilter == null''',
        '''        AdminSafeHorizontalRow {
            FilterChip(selected = intensityFilter == null'''
    )

    replace(
        "app/src/main/java/com/aistudio/micrhema/TeamModule.kt",
        '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gerenciar Equipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Text("Adicionar Membro")
            }
        }''',
        '''        AdminActionHeader(
            title = "Gerenciar Equipe",
            actionText = "Adicionar Membro",
            onAction = { showAddDialog = true }
        )'''
    )

    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminServicesV2.kt",
        '''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Cultos Fixos", fontWeight = FontWeight.Bold); Text("Sem data de início ou término", style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { editingService = null; showServiceDialog = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo") }
            }''',
        '''            AdminActionHeader(
                title = "Cultos Fixos",
                subtitle = "Sem data de início ou término",
                actionText = "Novo",
                onAction = { editingService = null; showServiceDialog = true }
            )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminServicesV2.kt",
        '''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Eventos", fontWeight = FontWeight.Bold); Text("Com início, término, banner e publicação", style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { editingEvent = null; showEventDialog = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo") }
            }''',
        '''            AdminActionHeader(
                title = "Eventos",
                subtitle = "Com início, término, banner e publicação",
                actionText = "Novo",
                onAction = { editingEvent = null; showEventDialog = true }
            )'''
    )

    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminXpShop.kt",
        '''                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("todos" to "Todos", "pendente" to "Pendentes", "entregue" to "Entregues", "cancelado" to "Cancelados").forEach''',
        '''                AdminSafeHorizontalRow {
                    listOf("todos" to "Todos", "pendente" to "Pendentes", "entregue" to "Entregues", "cancelado" to "Cancelados").forEach'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminXpShop.kt",
        '''                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(selected = kind == "digital"''',
        '''                AdminSafeHorizontalRow {
                    FilterChip(selected = kind == "digital"'''
    )


def block3() -> None:
    p = path("app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt")
    text = p.read_text(encoding="utf-8")
    pairs = [
        (
'''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminOverviewCard(
                        title = "Pendentes",
                        value = pendingCount.toString(),
                        icon = Icons.Default.Warning,
                        accent = MaterialTheme.colorScheme.error,
                        onClick = { onNavigate(AdminSection.MEMBERS) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminOverviewCard(
                        title = "Aprovados",
                        value = approvedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }''',
'''                AdminAdaptivePair(
                    first = { modifier -> AdminOverviewCard(
                        title = "Pendentes",
                        value = pendingCount.toString(),
                        icon = Icons.Default.Warning,
                        accent = MaterialTheme.colorScheme.error,
                        onClick = { onNavigate(AdminSection.MEMBERS) },
                        modifier = modifier
                    ) },
                    second = { modifier -> AdminOverviewCard(
                        title = "Aprovados",
                        value = approvedCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = modifier
                    ) }
                )'''),
        (
'''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminOverviewCard(
                        title = "Alunos IBR",
                        value = ibrCount.toString(),
                        icon = Icons.Default.School,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    AdminOverviewCard(
                        title = "Itens de mídia",
                        value = mediaCount.toString(),
                        icon = Icons.Default.PlayCircle,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigate(AdminSection.MEDIA) },
                        modifier = Modifier.weight(1f)
                    )
                }''',
'''                AdminAdaptivePair(
                    first = { modifier -> AdminOverviewCard(
                        title = "Alunos IBR",
                        value = ibrCount.toString(),
                        icon = Icons.Default.School,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = modifier
                    ) },
                    second = { modifier -> AdminOverviewCard(
                        title = "Itens de mídia",
                        value = mediaCount.toString(),
                        icon = Icons.Default.PlayCircle,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigate(AdminSection.MEDIA) },
                        modifier = modifier
                    ) }
                )'''),
        (
'''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminQuickActionCard("Nova notícia", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }, Modifier.weight(1f))
                    AdminQuickActionCard("Adicionar mídia", Icons.Default.CloudUpload, { onNavigate(AdminSection.MEDIA) }, Modifier.weight(1f))
                }''',
'''                AdminAdaptivePair(
                    first = { modifier -> AdminQuickActionCard("Nova notícia", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }, modifier) },
                    second = { modifier -> AdminQuickActionCard("Adicionar mídia", Icons.Default.CloudUpload, { onNavigate(AdminSection.MEDIA) }, modifier) }
                )'''),
        (
'''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminQuickActionCard("Aprovar membros", Icons.Default.PersonAdd, { onNavigate(AdminSection.MEMBERS) }, Modifier.weight(1f))
                    AdminQuickActionCard("Novo destaque", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }, Modifier.weight(1f))
                }''',
'''                AdminAdaptivePair(
                    first = { modifier -> AdminQuickActionCard("Aprovar membros", Icons.Default.PersonAdd, { onNavigate(AdminSection.MEMBERS) }, modifier) },
                    second = { modifier -> AdminQuickActionCard("Novo destaque", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }, modifier) }
                )'''),
        (
'''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminQuickActionCard("Atualizar culto", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }, Modifier.weight(1f))
                    AdminQuickActionCard("Curso IBR", Icons.Default.School, { onNavigate(AdminSection.IBR) }, Modifier.weight(1f))
                }''',
'''                AdminAdaptivePair(
                    first = { modifier -> AdminQuickActionCard("Atualizar culto", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }, modifier) },
                    second = { modifier -> AdminQuickActionCard("Curso IBR", Icons.Default.School, { onNavigate(AdminSection.IBR) }, modifier) }
                )''')
    ]
    for old, new in pairs:
        if old not in text: raise SystemExit(f"dashboard marker not found: {old[:60]!r}")
        text = text.replace(old, new, 1)
    p.write_text(text, encoding="utf-8")

    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminMoreSections.kt",
        '''        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Planos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingPlan = null; currentThemes = mutableListOf() }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }''',
        '''        AdminActionHeader(
            title = "Gerenciar Planos",
            actionText = "Novo",
            onAction = { showDialog = true; editingPlan = null; currentThemes = mutableListOf() }
        )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminMoreSections.kt",
        '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Devocionais", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Alimente a fé da igreja com uma nova palavra para cada dia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { showDialog = true; editingDevotional = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar devocional")
                Spacer(Modifier.width(6.dp))
                Text("Novo devocional")
            }
        }''',
        '''        AdminActionHeader(
            title = "Devocionais",
            subtitle = "Alimente a fé da igreja com uma nova palavra para cada dia.",
            actionText = "Novo devocional",
            onAction = { showDialog = true; editingDevotional = null }
        )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminMoreSections.kt",
        '''        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    editingBanner = CarouselItem(id = java.util.UUID.randomUUID().toString())
                    showDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Adicionar URL")
            }
            
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.weight(1f),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Imagem")
                    Spacer(Modifier.width(4.dp))
                    Text("Galeria")
                }
            }
        }''',
        '''        AdminAdaptivePair(
            first = { modifier ->
                Button(
                    onClick = {
                        editingBanner = CarouselItem(id = java.util.UUID.randomUUID().toString())
                        showDialog = true
                    },
                    modifier = modifier
                ) { Text("Adicionar URL") }
            },
            second = { modifier ->
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = modifier,
                    enabled = !isUploading
                ) {
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Imagem")
                        Spacer(Modifier.width(4.dp))
                        Text("Galeria")
                    }
                }
            }
        )'''
    )
    replace(
        "app/src/main/java/com/aistudio/micrhema/AdminXpShop.kt",
        '''                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDeliver, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Entregue")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                }''',
        '''                AdminAdaptivePair(
                    first = { modifier ->
                        Button(onClick = onDeliver, modifier = modifier) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(5.dp))
                            Text("Entregue")
                        }
                    },
                    second = { modifier -> OutlinedButton(onClick = onCancel, modifier = modifier) { Text("Cancelar") } }
                )'''
    )


if __name__ == "__main__":
    block = sys.argv[1] if len(sys.argv) > 1 else ""
    {"1": block1, "2": block2, "3": block3}.get(block, lambda: (_ for _ in ()).throw(SystemExit("use block 1, 2 or 3")))()
