package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


enum class AdminSection {
    DASHBOARD,
    DEVOTIONALS, NEWS, MEDIA, PLANS, IBR,
    SERVICES, BANNERS, DONATIONS,
    MEMBERS, PROFILES, TEAM,
    TABS, SETTINGS, ABOUT
}

@Composable
fun AdminDashboard(onNavigate: (AdminSection) -> Unit, paddingValues: PaddingValues) {
    val approvedCount = memberRequestsState.count { it.isApproved || it.status == "aprovado" }
    val pendingCount = memberRequestsState.count { !it.isApproved && it.status != "aprovado" }
    val ibrCount = memberRequestsState.count { it.isIbr }
    val mediaCount = contentBooksState.size + contentAudiosState.size + contentVideosState.size + contentAlbumsState.size
    var contentExpanded by remember { mutableStateOf(true) }
    var teachingExpanded by remember { mutableStateOf(true) }
    var churchExpanded by remember { mutableStateOf(true) }
    var membersExpanded by remember { mutableStateOf(true) }
    var systemExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            AdminHeader(
                pendingCount = pendingCount,
                onMembersClick = { onNavigate(AdminSection.MEMBERS) }
            )
        }

        item {
            AdminSectionHeading(
                title = "Resumo do ministério",
                subtitle = "Acompanhe o que precisa da sua atenção hoje."
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
            }
        }

        item {
            if (pendingCount > 0) {
                AdminAttentionCard(
                    message = if (pendingCount == 1) "1 solicitação aguarda aprovação" else "$pendingCount solicitações aguardam aprovação",
                    actionText = "Revisar membros agora",
                    icon = Icons.Default.PriorityHigh,
                    onClick = { onNavigate(AdminSection.MEMBERS) }
                )
            } else {
                AdminStatusCard(
                    message = "Tudo certo por aqui",
                    description = "Nenhuma solicitação pendente no momento."
                )
            }
        }

        item {
            AdminSectionHeading(
                title = "Ações rápidas",
                subtitle = "Acesse as tarefas mais usadas sem procurar no menu."
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminQuickActionCard("Nova notícia", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }, Modifier.weight(1f))
                    AdminQuickActionCard("Adicionar mídia", Icons.Default.CloudUpload, { onNavigate(AdminSection.MEDIA) }, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminQuickActionCard("Aprovar membros", Icons.Default.PersonAdd, { onNavigate(AdminSection.MEMBERS) }, Modifier.weight(1f))
                    AdminQuickActionCard("Novo destaque", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminQuickActionCard("Atualizar culto", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }, Modifier.weight(1f))
                    AdminQuickActionCard("Curso IBR", Icons.Default.School, { onNavigate(AdminSection.IBR) }, Modifier.weight(1f))
                }
            }
        }

        item { AdminSectionHeading(title = "Módulos administrativos", subtitle = "Gerencie o conteúdo e a estrutura do MIC Rhema.") }

        item { AdminCategoryTitle("CONTEÚDO", contentExpanded) { contentExpanded = !contentExpanded } }
        if (contentExpanded) {
            item { AdminMenuItem("Devocionais", "Mensagens e reflexões", Icons.Default.Book, { onNavigate(AdminSection.DEVOTIONALS) }) }
            item { AdminMenuItem("Notícias", "Informativos e avisos", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }) }
            item { AdminMenuItem("Mídia", "Áudios, vídeos, livros e álbuns", Icons.Default.PlayArrow, { onNavigate(AdminSection.MEDIA) }) }
            item { AdminMenuItem("Planos Bíblicos", "Planos e jornadas de leitura", Icons.Default.MenuBook, { onNavigate(AdminSection.PLANS) }) }
        }

        item { AdminCategoryTitle("ENSINO", teachingExpanded) { teachingExpanded = !teachingExpanded } }
        if (teachingExpanded) {
            item { AdminMenuItem("Instituto Bíblico Rhema", "Cursos e formação bíblica", Icons.Default.WorkspacePremium, { onNavigate(AdminSection.IBR) }) }
        }

        item { AdminCategoryTitle("IGREJA", churchExpanded) { churchExpanded = !churchExpanded } }
        if (churchExpanded) {
            item { AdminMenuItem("Cultos", "Agenda e programação", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }) }
            item { AdminMenuItem("Destaques", "Banners e eventos da tela inicial", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }) }
            item { AdminMenuItem("Dízimos e Ofertas", "Contas, PIX e informações", Icons.Default.MonetizationOn, { onNavigate(AdminSection.DONATIONS) }) }
            item { AdminMenuItem("Equipe", "Líderes, pastores e ministérios", Icons.Default.Groups, { onNavigate(AdminSection.TEAM) }) }
        }

        item { AdminCategoryTitle("MEMBROS", membersExpanded) { membersExpanded = !membersExpanded } }
        if (membersExpanded) {
            item { AdminMenuItem("Membros", "Aprovações e permissões", Icons.Default.People, { onNavigate(AdminSection.MEMBERS) }) }
            item { AdminMenuItem("Perfis dos Membros", "Dados e informações dos usuários", Icons.Default.AccountBox, { onNavigate(AdminSection.PROFILES) }) }
        }

        item { AdminCategoryTitle("SISTEMA", systemExpanded) { systemExpanded = !systemExpanded } }
        if (systemExpanded) {
            item { AdminMenuItem("Abas do Aplicativo", "Organização dos atalhos e seções", Icons.Default.Tab, { onNavigate(AdminSection.TABS) }) }
            item { AdminMenuItem("Configurações", "Preferências gerais do aplicativo", Icons.Default.Settings, { onNavigate(AdminSection.SETTINGS) }) }
            item { AdminMenuItem("Sobre", "Informações institucionais", Icons.Default.Info, { onNavigate(AdminSection.ABOUT) }) }
        }
    }
}

@Composable
fun AdminHeader(pendingCount: Int = 0, onMembersClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Área administrativa", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Painel Administrativo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("MIC Rhema • visão geral", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
            }
            if (pendingCount > 0 && onMembersClick != null) {
                IconButton(onClick = onMembersClick) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Ver pendências", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
fun AdminSectionHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AdminOverviewCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = 0.14f), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminQuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AdminStatusCard(message: String, description: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun AdminAttentionCard(message: String, actionText: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(actionText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir pendências", tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun AdminCategoryTitle(title: String, expanded: Boolean = true, onToggle: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Recolher $title" else "Expandir $title", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AdminMenuItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir $title", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}


@Composable
fun AdminStatusChip(text: String, positive: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
