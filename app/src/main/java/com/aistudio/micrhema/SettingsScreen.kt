package com.aistudio.micrhema

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

/** Estilo: cartões editoriais claros, hierarquia suave e descrições para cada escolha. */
private fun micRhemaDownloadsDir(context: Context): File = DownloadHelper.getDownloadDirectory(context)

private fun directorySize(file: File): Long =
    if (!file.exists()) 0L else file.walkTopDown().filter { it.isFile }.sumOf { it.length() }

private fun formatStorageSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val settings by currentSettingsState
    val loggedInMember = loggedInMemberState.value
    var occupiedBytes by remember { mutableLongStateOf(0L) }
    val sectionIds = listOf("appearance", "reading", "audio", "downloads", "notifications", "internet", "favorites", "maintenance") +
        if (loggedInMember != null) listOf("account") else emptyList()
    val sectionPreferences = remember(context) {
        context.getSharedPreferences("micrhema_settings_ui", Context.MODE_PRIVATE)
    }
    var expandedSections by remember {
        mutableStateOf(sectionPreferences.getStringSet("expanded_sections", emptySet())?.toSet() ?: emptySet())
    }

    fun refreshStorageSize() {
        occupiedBytes = directorySize(micRhemaDownloadsDir(context))
    }

    fun updateSettings(newSettings: UserSettings) {
        UserSettingsManager.saveSettings(context, newSettings)
    }

    fun updateExpandedSections(newSections: Set<String>) {
        expandedSections = newSections
        sectionPreferences.edit().putStringSet("expanded_sections", newSections).apply()
    }

    fun toggleSection(id: String) {
        updateExpandedSections(if (id in expandedSections) expandedSections - id else expandedSections + id)
    }

    LaunchedEffect(Unit) { refreshStorageSize() }

    val fontNames = listOf("Pequena", "Padrão", "Média", "Grande", "Muito grande")
    val themeNames = mapOf(
        ThemeModeOption.SYSTEM.name to "Sistema",
        ThemeModeOption.LIGHT.name to "Claro",
        ThemeModeOption.DARK.name to "Escuro"
    )
    val colorNames = mapOf(
        AccentColor.BLUE.name to "Azul",
        AccentColor.GREEN.name to "Verde",
        AccentColor.PURPLE.name to "Roxo",
        AccentColor.GOLD.name to "Dourado",
        AccentColor.WHITE.name to "Branco/Preto"
    )
    val readingFontNames = mapOf(
        ReadingFont.ROBOTO.name to "Roboto",
        ReadingFont.INTER.name to "Inter",
        ReadingFont.OPEN_SANS.name to "Open Sans",
        ReadingFont.SERIF.name to "Serif"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsHero(
                isSynced = loggedInMember != null,
                accountName = loggedInMember?.name
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsExpandControls(
                    canExpandAll = sectionIds.any { it !in expandedSections },
                    canCollapseAll = sectionIds.any { it in expandedSections },
                    onExpandAll = { updateExpandedSections(sectionIds.toSet()) },
                    onCollapseAll = { updateExpandedSections(emptySet()) }
                )
            }
            item {
                SettingsSection(
                    title = "Aparência",
                    summary = "Tema, legibilidade e modo de leitura.",
                    icon = Icons.Default.Palette,
                    expanded = "appearance" in expandedSections,
                    onToggle = { toggleSection("appearance") }
                ) {
                    SettingsSwitch("Notificações", "Permite os avisos escolhidos abaixo.", settings.notificationsEnabled) {
                        updateSettings(settings.copy(notificationsEnabled = it))
                    }
                    SettingsSwitch("Vibração ao tocar nas abas", "Retorno tátil em navegações.", settings.vibrationEnabled) {
                        updateSettings(settings.copy(vibrationEnabled = it))
                    }
                    SettingsSwitch("Animações", "Transições visuais do aplicativo.", settings.animationsEnabled) {
                        updateSettings(settings.copy(animationsEnabled = it))
                    }
                    SettingsSwitch("Modo leitura", "Oculta ações dos versículos para uma leitura mais limpa.", settings.readingModeEnabled) {
                        updateSettings(settings.copy(readingModeEnabled = it))
                    }
                    SettingsSlider(
                        title = "Tamanho da fonte",
                        summary = "Atual: ${fontNames[settings.fontSizeIndex.coerceIn(0, 4)]}",
                        value = settings.fontSizeIndex.toFloat(),
                        min = 0f,
                        max = 4f,
                        steps = 3
                    ) { updateSettings(settings.copy(fontSizeIndex = it.toInt().coerceIn(0, 4))) }
                    SettingsDropdown("Tema", "Define a aparência geral do aplicativo.", themeNames[settings.themeModeOption.name] ?: "Sistema", themeNames.values.toList()) { selected ->
                        val key = themeNames.entries.firstOrNull { it.value == selected }?.key ?: ThemeModeOption.SYSTEM.name
                        updateSettings(settings.copy(themeModeOption = ThemeModeOption.valueOf(key)))
                    }
                    SettingsDropdown("Cor de destaque", "Aplica a cor em botões e elementos principais.", colorNames[settings.accentColor.name] ?: "Azul", colorNames.values.toList()) { selected ->
                        val key = colorNames.entries.firstOrNull { it.value == selected }?.key ?: AccentColor.BLUE.name
                        updateSettings(settings.copy(accentColor = AccentColor.valueOf(key)))
                    }
                    SettingsDropdown("Fonte para leitura", "Usada no texto dos versículos.", readingFontNames[settings.readingFont.name] ?: "Roboto", readingFontNames.values.toList()) { selected ->
                        val key = readingFontNames.entries.firstOrNull { it.value == selected }?.key ?: ReadingFont.ROBOTO.name
                        updateSettings(settings.copy(readingFont = ReadingFont.valueOf(key)))
                    }
                }
            }

            item {
                SettingsSection(
                    "Leitura bíblica",
                    "Preferências aplicadas diretamente ao leitor.",
                    Icons.Default.MenuBook,
                    expanded = "reading" in expandedSections,
                    onToggle = { toggleSection("reading") }
                ) {
                    SettingsSwitch("Manter a tela ligada", "Evita que a tela apague enquanto você lê.", settings.keepScreenOn) {
                        updateSettings(settings.copy(keepScreenOn = it))
                    }
                    SettingsSwitch("Salvar posição da leitura", "Pergunta se deseja continuar de onde parou.", settings.autoSavePosition) {
                        updateSettings(settings.copy(autoSavePosition = it))
                    }
                    SettingsSwitch("Rolagem automática", "Avança suavemente pelos versículos durante a leitura.", settings.autoScroll) {
                        updateSettings(settings.copy(autoScroll = it))
                    }
                }
            }

            item {
                SettingsSection(
                    "Áudio",
                    "Retomada, velocidade e controles da reprodução.",
                    Icons.Default.Headphones,
                    expanded = "audio" in expandedSections,
                    onToggle = { toggleSection("audio") }
                ) {
                    SettingsDropdown("Velocidade", "Ajusta a velocidade do player aberto.", "${settings.playbackSpeed}x", listOf("0.75x", "1.0x", "1.25x", "1.5x", "2.0x")) {
                        updateSettings(settings.copy(playbackSpeed = it.removeSuffix("x").toFloatOrNull() ?: 1.0f))
                    }
                    SettingsDropdown("Pular", "Intervalo dos botões avançar e voltar.", "${settings.skipTime}s", listOf("10s", "15s", "30s")) {
                        updateSettings(settings.copy(skipTime = it.removeSuffix("s").toIntOrNull() ?: 15))
                    }
                    SettingsSwitch("Continuar com tela bloqueada", "Pausa ao bloquear a tela quando desligado.", settings.continuePlaybackWhenLocked) {
                        updateSettings(settings.copy(continuePlaybackWhenLocked = it))
                    }
                    SettingsSwitch("Iniciar última pregação", "Retoma o último áudio ao abrir o aplicativo.", settings.autoStartLastPlayback) {
                        updateSettings(settings.copy(autoStartLastPlayback = it))
                    }
                    SettingsDropdown("Temporizador para desligar", "Pausa o áudio depois do intervalo escolhido.", if (settings.sleepTimer == 0) "Desativado" else "${settings.sleepTimer} min", listOf("Desativado", "15 min", "30 min", "60 min")) {
                        updateSettings(settings.copy(sleepTimer = it.removeSuffix(" min").toIntOrNull() ?: 0))
                    }
                }
            }

            item {
                SettingsSection(
                    "Downloads",
                    "Rede, pasta de destino e limpeza de arquivos.",
                    Icons.Default.Download,
                    expanded = "downloads" in expandedSections,
                    onToggle = { toggleSection("downloads") }
                ) {
                    SettingsSwitch("Apenas no Wi-Fi", "Evita downloads usando dados móveis.", settings.wifiOnlyDownloads) {
                        updateSettings(settings.copy(wifiOnlyDownloads = it))
                    }
                    SettingsDropdown("Pasta de armazenamento", "Destino: ${if (settings.storageFolder == "SD Card") "Cartão SD" else "Pasta do app"}.", if (settings.storageFolder == "SD Card") "Cartão SD" else "Pasta do app", listOf("Pasta do app", "Cartão SD")) {
                        updateSettings(settings.copy(storageFolder = if (it == "Cartão SD") "SD Card" else "Interno"))
                    }
                    SettingsSwitch("Limpar downloads antigos", "Remove automaticamente arquivos antigos do app.", settings.autoCleanOldDownloads) {
                        updateSettings(settings.copy(autoCleanOldDownloads = it))
                    }
                    SettingsAction("Espaço ocupado", formatStorageSize(occupiedBytes), icon = Icons.Default.Storage) { refreshStorageSize() }
                    SettingsAction("Limpar downloads", "Remove somente os arquivos baixados pelo MIC Rhema.", icon = Icons.Default.DeleteSweep, color = MaterialTheme.colorScheme.error) {
                        DownloadHelper.clearDownloads(context)
                        refreshStorageSize()
                        android.widget.Toast.makeText(context, "Downloads do MIC Rhema removidos.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                SettingsSection(
                    "Notificações",
                    "Escolha quais avisos deseja receber.",
                    Icons.Default.Notifications,
                    expanded = "notifications" in expandedSections,
                    onToggle = { toggleSection("notifications") }
                ) {
                    SettingsSwitch("Novos cursos", "Avisos de cursos disponíveis.", settings.notifNewCourses) { updateSettings(settings.copy(notifNewCourses = it)) }
                    SettingsSwitch("Devocional diário às 8h", "Lembrete diário do devocional.", settings.notifDailyDevotional) { updateSettings(settings.copy(notifDailyDevotional = it)) }
                    SettingsSwitch("Avisos de eventos e cultos", "Comunicações da agenda da igreja.", settings.notifEvents) { updateSettings(settings.copy(notifEvents = it)) }
                    SettingsSwitch("Próximo culto", "Lembrete antes da programação.", settings.notifNextService) { updateSettings(settings.copy(notifNextService = it)) }
                    SettingsSwitch("Notícia do meio-dia", "Destaque bíblico diário ao meio-dia.", settings.notifDailyNews) { updateSettings(settings.copy(notifDailyNews = it)) }
                    SettingsSwitch("Novas mídias", "Livros, vídeos e áudios adicionados.", settings.notifNewMedia) { updateSettings(settings.copy(notifNewMedia = it)) }
                    SettingsSwitch("Novas aulas e módulos IBR", "Conteúdo direcionado aos alunos IBR.", settings.notifIbrContent) { updateSettings(settings.copy(notifIbrContent = it)) }
                    SettingsSwitch("Novas pregações", "Avisos de mensagens publicadas.", settings.notifNewSermons) { updateSettings(settings.copy(notifNewSermons = it)) }
                }
            }

            item {
                SettingsSection(
                    "Internet e dados",
                    "Controle o que é atualizado e pré-carregado.",
                    Icons.Default.Language,
                    expanded = "internet" in expandedSections,
                    onToggle = { toggleSection("internet") }
                ) {
                    SettingsSwitch("Pré-carregar imagens", "Antecipa capas para abrir listas mais rápido.", settings.preloadImages) { updateSettings(settings.copy(preloadImages = it)) }
                    SettingsSwitch("Economizar dados móveis", "Impede pré-carregamento em redes tarifadas.", settings.saveMobileData) { updateSettings(settings.copy(saveMobileData = it)) }
                    SettingsSwitch("Atualizar automaticamente", "Mantém o conteúdo em tempo real; desligue para usar apenas o cache até atualizar manualmente.", settings.autoUpdateContent) { updateSettings(settings.copy(autoUpdateContent = it)) }
                    SettingsAction("Limpar cache", "Remove imagens e arquivos temporários, sem apagar seus downloads.", icon = Icons.Default.DeleteSweep) {
                        context.cacheDir.deleteRecursively()
                        android.widget.Toast.makeText(context, "Cache seguro limpo.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                SettingsSection(
                    "Favoritos e histórico",
                    "Acompanhe conteúdos e preserve seus dados.",
                    Icons.Default.Favorite,
                    expanded = "favorites" in expandedSections,
                    onToggle = { toggleSection("favorites") }
                ) {
                    SettingsSwitch("Sincronizar favoritos", "Mantém favoritos iguais entre aparelhos conectados.", settings.syncFavorites) { updateSettings(settings.copy(syncFavorites = it)) }
                    SettingsSwitch("Backup automático", "Salva uma cópia local das listas e do histórico.", settings.autoBackup) { updateSettings(settings.copy(autoBackup = it)) }
                    SettingsSwitch("Histórico de reprodução", "Mostra livros, vídeos e áudios acessados recentemente.", settings.trackPlaybackHistory) { updateSettings(settings.copy(trackPlaybackHistory = it)) }
                    SettingsAction("Limpar histórico", "Remove a lista de conteúdos vistos recentemente.", icon = Icons.Default.DeleteSweep, color = MaterialTheme.colorScheme.error) {
                        recentlyViewedState.clear()
                        LocalDataManager.saveAll(context)
                        android.widget.Toast.makeText(context, "Histórico limpo.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                SettingsSection(
                    "Manutenção",
                    "Ações de recuperação e redefinição do aplicativo.",
                    Icons.Default.Tune,
                    expanded = "maintenance" in expandedSections,
                    onToggle = { toggleSection("maintenance") }
                ) {
                    SettingsAction("Recarregar dados locais", "Atualiza o estado com os dados salvos neste aparelho.", icon = Icons.Default.RestartAlt) {
                        LocalDataManager.loadAll(context)
                        refreshStorageSize()
                        android.widget.Toast.makeText(context, "Dados locais recarregados.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    SettingsAction("Restaurar configurações", "Volta todas as preferências aos valores recomendados.", icon = Icons.Default.RestartAlt, color = MaterialTheme.colorScheme.error) {
                        updateSettings(UserSettings())
                    }
                }
            }

            if (loggedInMember != null) {
                item {
                    SettingsSection(
                        "Conta",
                        "Dados da sua sessão no MIC Rhema.",
                        Icons.Default.Person,
                        expanded = "account" in expandedSections,
                        onToggle = { toggleSection("account") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(loggedInMember.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(loggedInMember.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(loggedInMember.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        SettingsAction("Alterar número de contato", "Abra o perfil para atualizar seus dados.", icon = Icons.Default.Person) { onNavigateProfile() }
                        SettingsAction("Sair da conta", "Encerra esta sessão no aparelho.", icon = Icons.Default.Logout, color = MaterialTheme.colorScheme.error) {
                            MemberManager.setLoggedInMember(context, null)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsHero(isSynced: Boolean, accountName: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Configurações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Ajuste o MIC Rhema ao seu ritmo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (isSynced) Icons.Default.CloudDone else Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (isSynced) "Preferências sincronizadas" else "Preferências neste aparelho", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(if (isSynced) "As escolhas de ${accountName ?: "sua conta"} são mantidas na nuvem." else "Entre na sua conta para também sincronizar suas escolhas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Minimizar $title" else "Expandir $title",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
            }
        }
    }
}

@Composable
private fun SettingsExpandControls(
    canExpandAll: Boolean,
    canCollapseAll: Boolean,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onExpandAll, enabled = canExpandAll) {
            Text("Expandir tudo")
        }
        TextButton(onClick = onCollapseAll, enabled = canCollapseAll) {
            Text("Minimizar tudo")
        }
    }
}

@Composable
private fun SettingsSwitch(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSlider(title: String, summary: String, value: Float, min: Float, max: Float, steps: Int, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max, steps = steps)
    }
}

@Composable
private fun SettingsDropdown(title: String, summary: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Text(selected, modifier = Modifier.width(112.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun SettingsAction(title: String, summary: String, icon: ImageVector, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
