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
import androidx.compose.material3.AlertDialog
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
    var pendingConfirmation by remember { mutableStateOf<String?>(null) }
    val hasSdCard = remember { context.getExternalFilesDirs(null).filterNotNull().size > 1 }
    val sectionIds = listOf("appearance", "reading", "downloads", "notifications", "favorites") +
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
                syncStatus = settingsSyncStatusState.value,
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
                SettingsSection(
                    title = "Geral",
                    summary = "Tema, cor e tamanho dos textos.",
                    icon = Icons.Default.Palette,
                    expanded = "appearance" in expandedSections,
                    onToggle = { toggleSection("appearance") }
                ) {
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
                    "Leitura e áudio",
                    "Opções essenciais para ler e ouvir.",
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
                    "Downloads e dados",
                    "Rede, armazenamento e arquivos baixados.",
                    Icons.Default.Download,
                    expanded = "downloads" in expandedSections,
                    onToggle = { toggleSection("downloads") }
                ) {
                    SettingsSwitch("Apenas no Wi-Fi", "Evita downloads usando dados móveis.", settings.wifiOnlyDownloads) {
                        updateSettings(settings.copy(wifiOnlyDownloads = it))
                    }
                    if (hasSdCard) {
                        SettingsDropdown("Pasta de armazenamento", "Escolha onde guardar os downloads.", if (settings.storageFolder == "SD Card") "Cartão SD" else "Pasta do app", listOf("Pasta do app", "Cartão SD")) {
                            updateSettings(settings.copy(storageFolder = if (it == "Cartão SD") "SD Card" else "Interno"))
                        }
                    }
                    SettingsSwitch("Limpar downloads antigos", "Remove automaticamente arquivos antigos do app.", settings.autoCleanOldDownloads) {
                        updateSettings(settings.copy(autoCleanOldDownloads = it))
                    }
                    SettingsSwitch("Economizar dados móveis", "Reduz carregamentos em redes móveis.", settings.saveMobileData) { updateSettings(settings.copy(saveMobileData = it)) }
                    SettingsAction("Espaço ocupado", formatStorageSize(occupiedBytes), icon = Icons.Default.Storage) { refreshStorageSize() }
                    SettingsAction("Limpar cache", "Remove arquivos temporários, sem apagar downloads.", icon = Icons.Default.DeleteSweep) {
                        context.cacheDir.deleteRecursively()
                        android.widget.Toast.makeText(context, "Cache seguro limpo.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    SettingsAction("Limpar downloads", "Remove somente os arquivos baixados pelo MIC Rhema.", icon = Icons.Default.DeleteSweep, color = MaterialTheme.colorScheme.error) {
                        pendingConfirmation = "downloads"
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
                    SettingsSwitch("Receber notificações", "Ativa ou pausa todos os avisos.", settings.notificationsEnabled) {
                        updateSettings(settings.copy(notificationsEnabled = it))
                    }
                    SettingsSwitch("Novos cursos", "Avisos de cursos disponíveis.", settings.notifNewCourses, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifNewCourses = it)) }
                    SettingsSwitch("Devocional diário às 8h", "Lembrete diário do devocional.", settings.notifDailyDevotional, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifDailyDevotional = it)) }
                    SettingsSwitch("Avisos de eventos e cultos", "Comunicações da agenda da igreja.", settings.notifEvents, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifEvents = it)) }
                    SettingsSwitch("Próximo culto", "Lembrete antes da programação.", settings.notifNextService, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifNextService = it)) }
                    SettingsSwitch("Notícia do meio-dia", "Destaque bíblico diário ao meio-dia.", settings.notifDailyNews, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifDailyNews = it)) }
                    SettingsSwitch("Novas mídias", "Livros, vídeos e áudios adicionados.", settings.notifNewMedia, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifNewMedia = it)) }
                    if (loggedInMember?.isIbr == true) {
                        SettingsSwitch("Novas aulas e módulos IBR", "Conteúdo direcionado aos alunos IBR.", settings.notifIbrContent, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifIbrContent = it)) }
                    }
                    SettingsSwitch("Novas pregações", "Avisos de mensagens publicadas.", settings.notifNewSermons, enabled = settings.notificationsEnabled) { updateSettings(settings.copy(notifNewSermons = it)) }
                    SettingsAction(
                        "Testar notificações",
                        if (NotificationHelper.hasNotificationPermission(context))
                            "Permissão ativa. Toque para enviar um teste neste aparelho."
                        else
                            "As notificações do sistema ainda não estão liberadas neste aparelho.",
                        icon = Icons.Default.Notifications
                    ) {
                        val activity = context as? android.app.Activity
                        if (!NotificationHelper.hasNotificationPermission(context) && activity != null) {
                            NotificationHelper.requestNotificationPermission(activity)
                            android.widget.Toast.makeText(context, "Autorize as notificações e toque novamente em Testar notificações.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Teste MIC Rhema",
                                message = "As notificações estão funcionando neste aparelho.",
                                category = NotificationHelper.Category.GENERAL,
                                respectPreferences = false
                            )
                            android.widget.Toast.makeText(context, "Notificação de teste enviada.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    "Mais opções",
                    "Preferências avançadas e recuperação.",
                    Icons.Default.Tune,
                    expanded = "favorites" in expandedSections,
                    onToggle = { toggleSection("favorites") }
                ) {
                    SettingsSwitch("Pré-carregar imagens", "Carrega capas antecipadamente.", settings.preloadImages) { updateSettings(settings.copy(preloadImages = it)) }
                    SettingsSwitch("Atualizar conteúdo automaticamente", "Busca novidades quando houver internet.", settings.autoUpdateContent) { updateSettings(settings.copy(autoUpdateContent = it)) }
                    SettingsSwitch("Histórico de reprodução", "Mostra livros, vídeos e áudios acessados recentemente.", settings.trackPlaybackHistory) { updateSettings(settings.copy(trackPlaybackHistory = it)) }
                    SettingsAction("Limpar histórico", "Remove a lista de conteúdos vistos recentemente.", icon = Icons.Default.DeleteSweep, color = MaterialTheme.colorScheme.error) {
                        pendingConfirmation = "history"
                    }
                    SettingsAction("Restaurar configurações", "Volta todas as preferências aos valores recomendados.", icon = Icons.Default.RestartAlt, color = MaterialTheme.colorScheme.error) {
                        pendingConfirmation = "settings"
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
                        SettingsAction("Abrir meu perfil", "Veja e atualize seus dados pessoais.", icon = Icons.Default.Person) { onNavigateProfile() }
                        SettingsAction("Sair da conta", "Encerra esta sessão no aparelho.", icon = Icons.Default.Logout, color = MaterialTheme.colorScheme.error) {
                            MemberManager.setLoggedInMember(context, null)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    pendingConfirmation?.let { action ->
        val (title, message) = when (action) {
            "downloads" -> "Limpar downloads?" to "Os arquivos baixados pelo MIC Rhema serão removidos deste aparelho."
            "history" -> "Limpar histórico?" to "A lista de conteúdos vistos recentemente será apagada."
            else -> "Restaurar configurações?" to "Todas as preferências voltarão aos valores recomendados."
        }
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        "downloads" -> { DownloadHelper.clearDownloads(context); refreshStorageSize() }
                        "history" -> { recentlyViewedState.clear(); LocalDataManager.saveAll(context) }
                        else -> updateSettings(UserSettings())
                    }
                    pendingConfirmation = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SettingsHero(syncStatus: SettingsSyncStatus, accountName: String?) {
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
            Icon(if (syncStatus == SettingsSyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Column {
                val title = when (syncStatus) {
                    SettingsSyncStatus.SAVING -> "Salvando preferências…"
                    SettingsSyncStatus.SYNCED -> "Preferências sincronizadas"
                    SettingsSyncStatus.FAILED -> "Falha ao sincronizar"
                    SettingsSyncStatus.LOCAL -> "Preferências neste aparelho"
                }
                val detail = when (syncStatus) {
                    SettingsSyncStatus.SAVING -> "Aguarde um instante."
                    SettingsSyncStatus.SYNCED -> "As escolhas de ${accountName ?: "sua conta"} estão salvas."
                    SettingsSyncStatus.FAILED -> "Confira a internet; suas escolhas continuam salvas neste aparelho."
                    SettingsSyncStatus.LOCAL -> "Entre na sua conta para sincronizar suas escolhas."
                }
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
private fun SettingsSwitch(title: String, summary: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsSlider(title: String, summary: String, value: Float, min: Float, max: Float, steps: Int, onValueChange: (Float) -> Unit) {
    var pendingValue by remember(value) { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = pendingValue,
            onValueChange = { pendingValue = it },
            onValueChangeFinished = { onValueChange(pendingValue) },
            valueRange = min..max,
            steps = steps
        )
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
