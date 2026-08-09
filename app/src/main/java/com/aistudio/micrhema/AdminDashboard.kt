package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(
            start = 16.dp, 
            end = 16.dp, 
            top = paddingValues.calculateTopPadding() + 16.dp, 
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Visão geral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
        }

        item { StatCard("Aprovados", approvedCount.toString(), Icons.Default.CheckCircle) }
        item { StatCard("Pendentes", pendingCount.toString(), Icons.Default.Warning) }
        item { StatCard("Alunos IBR", ibrCount.toString(), Icons.Default.School) }
        item { StatCard("Membros VIP", vipCount.toString(), Icons.Default.Star) }

        // Categoria 1: Conteúdo
        item(span = { GridItemSpan(maxLineSpan) }) { CategoryTitle("Conteúdo") }
        item { AdminCard("Devocionais", "Gerencie mensagens diárias", Icons.Default.Book, { onNavigate(AdminSection.DEVOTIONALS) }) }
        item { AdminCard("Notícias", "Informativos e avisos", Icons.Default.Article, { onNavigate(AdminSection.NEWS) }) }
        item { AdminCard("Conteúdo / Mídia", "Vídeos, áudios e livros", Icons.Default.PlayArrow, { onNavigate(AdminSection.CONTENT) }) }
        item { AdminCard("Planos", "Planos de leitura", Icons.Default.MenuBook, { onNavigate(AdminSection.PLANS) }) }
        item { AdminCard("VIP / IBR", "Cursos e conteúdos exclusivos", Icons.Default.WorkspacePremium, { onNavigate(AdminSection.VIP) }) }

        // Categoria 2: Igreja
        item(span = { GridItemSpan(maxLineSpan) }) { CategoryTitle("Igreja") }
        item { AdminCard("Cultos", "Agenda e programação", Icons.Default.Event, { onNavigate(AdminSection.SERVICES) }) }
        item { AdminCard("Destaques", "Banners da tela inicial", Icons.Default.ViewCarousel, { onNavigate(AdminSection.BANNERS) }) }
        item { AdminCard("Dízimos e Ofertas", "Contas e chaves PIX", Icons.Default.MonetizationOn, { onNavigate(AdminSection.DONATIONS) }) }

        // Categoria 3: Pessoas
        item(span = { GridItemSpan(maxLineSpan) }) { CategoryTitle("Pessoas") }
        item { AdminCard("Membros", "Aprovações e permissões", Icons.Default.People, { onNavigate(AdminSection.MEMBERS) }) }
        item { AdminCard("Perfis dos Membros", "Dados dos usuários", Icons.Default.AccountBox, { onNavigate(AdminSection.PROFILES) }) }
        item { AdminCard("Equipe", "Líderes e pastores", Icons.Default.Groups, { onNavigate(AdminSection.TEAM) }) }

        // Categoria 4: Sistema
        item(span = { GridItemSpan(maxLineSpan) }) { CategoryTitle("Sistema") }
        item { AdminCard("Abas do Aplicativo", "Atalhos da home", Icons.Default.Tab, { onNavigate(AdminSection.TABS) }) }
        item { AdminCard("Configurações", "Ajustes do app", Icons.Default.Settings, { onNavigate(AdminSection.SETTINGS) }) }
        item { AdminCard("Sobre", "Informações da igreja", Icons.Default.Info, { onNavigate(AdminSection.ABOUT) }) }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CategoryTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun AdminCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
