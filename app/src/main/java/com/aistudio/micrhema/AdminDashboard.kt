package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AdminSection {
    DASHBOARD,
    DEVOTIONALS, NEWS, CONTENT, PLANS, VIP,
    SERVICES, BANNERS, DONATIONS,
    MEMBERS, PROFILES, TEAM,
    TABS, SETTINGS, ABOUT
}

@Composable
fun AdminDashboard(onNavigate: (AdminSection) -> Unit, paddingValues: PaddingValues) {
    val approvedCount = memberRequestsState.count { it.isApproved || it.status == "aprovado" }
    val pendingCount = memberRequestsState.count { !it.isApproved && it.status != "aprovado" }
    val ibrCount = memberRequestsState.count { it.isIbr }
    val vipCount = memberRequestsState.count { it.isVip }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, 
            end = 16.dp, 
            top = paddingValues.calculateTopPadding() + 16.dp, 
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            AdminHeader()
        }

        item {
            Text(
                "Visão geral",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminOverviewCard(
                    title = "Aprovados",
                    value = approvedCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                AdminOverviewCard(
                    title = "Pendentes",
                    value = pendingCount.toString(),
                    icon = Icons.Default.Warning,
                    onClick = { onNavigate(AdminSection.MEMBERS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminOverviewCard(
                    title = "Alunos IBR",
                    value = ibrCount.toString(),
                    icon = Icons.Default.School,
                    modifier = Modifier.weight(1f)
                )
                AdminOverviewCard(
                    title = "Membros VIP",
                    value = vipCount.toString(),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Atenção",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        item {
            if (pendingCount > 0) {
                AdminAttentionCard(
                    message = "$pendingCount solicitações aguardando aprovação",
                    actionText = "Ver solicitações",
                    icon = Icons.Default.Warning,
                    onClick = { onNavigate(AdminSection.MEMBERS) }
                )
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Tudo certo por aqui", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // CONTEÚDO
        item { AdminCategoryTitle("CONTEÚDO") }
        item { AdminMenuItem("Devocionais", "Mensagens e reflexões", Icons.Default.Book, { onNavigate(AdminSection.DEVOTIONALS) }) }
        item { AdminMenuItem("Notícias", "Informativos e avisos", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }) }
        item { AdminMenuItem("Mídia", "Áudios, vídeos, livros e álbuns", Icons.Default.PlayArrow, { onNavigate(AdminSection.CONTENT) }) }
        item { AdminMenuItem("Planos Bíblicos", "Planos e jornadas de leitura", Icons.Default.MenuBook, { onNavigate(AdminSection.PLANS) }) }

        // ENSINO
        item { AdminCategoryTitle("ENSINO") }
        item { AdminMenuItem("Instituto Bíblico / VIP", "Cursos e conteúdos exclusivos", Icons.Default.WorkspacePremium, { onNavigate(AdminSection.VIP) }) }

        // IGREJA
        item { AdminCategoryTitle("IGREJA") }
        item { AdminMenuItem("Cultos", "Agenda e programação", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }) }
        item { AdminMenuItem("Destaques", "Banners e eventos da tela inicial", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }) }
        item { AdminMenuItem("Dízimos e Ofertas", "Contas, PIX e informações", Icons.Default.MonetizationOn, { onNavigate(AdminSection.DONATIONS) }) }
        item { AdminMenuItem("Equipe", "Líderes, pastores e ministérios", Icons.Default.Groups, { onNavigate(AdminSection.TEAM) }) }

        // MEMBROS
        item { AdminCategoryTitle("MEMBROS") }
        item { AdminMenuItem("Membros", "Aprovações e permissões", Icons.Default.People, { onNavigate(AdminSection.MEMBERS) }) }
        item { AdminMenuItem("Perfis dos Membros", "Dados e informações dos usuários", Icons.Default.AccountBox, { onNavigate(AdminSection.PROFILES) }) }

        // SISTEMA
        item { AdminCategoryTitle("SISTEMA") }
        item { AdminMenuItem("Abas do Aplicativo", "Organização dos atalhos e seções", Icons.Default.Tab, { onNavigate(AdminSection.TABS) }) }
        item { AdminMenuItem("Configurações", "Preferências gerais do aplicativo", Icons.Default.Settings, { onNavigate(AdminSection.SETTINGS) }) }
        item { AdminMenuItem("Sobre", "Informações institucionais", Icons.Default.Info, { onNavigate(AdminSection.ABOUT) }) }
    }
}

@Composable
fun AdminHeader() {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Painel Administrativo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(8.dp))
        Text("Gerencie o conteúdo e os membros do MIC Rhema", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}

@Composable
fun AdminOverviewCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AdminAttentionCard(message: String, actionText: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(actionText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun AdminCategoryTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun AdminMenuItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
