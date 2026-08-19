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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Environment
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
private fun micRhemaDownloadsDir(context: android.content.Context): File =
    File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "micrhema")

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

@Composable
fun SettingsScreen(onNavigateProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val settings by currentSettingsState
    val loggedInMember = loggedInMemberState.value
    var occupiedBytes by remember { mutableLongStateOf(0L) }

    fun refreshStorageSize() {
        occupiedBytes = directorySize(micRhemaDownloadsDir(context))
    }

    LaunchedEffect(Unit) { refreshStorageSize() }

    fun updateSettings(newSettings: UserSettings) {
        UserSettingsManager.saveSettings(context, newSettings)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // CONTA (Shown first if logged in, or at the bottom? The prompt lists it at the end, let's put it at the end)

            item { SettingsCategoryTitle("Aparência") }
            item {
                SettingsSwitch("Notificações", settings.notificationsEnabled) { updateSettings(settings.copy(notificationsEnabled = it)) }
                SettingsSwitch("Vibração ao tocar nas abas", settings.vibrationEnabled) { updateSettings(settings.copy(vibrationEnabled = it)) }
                SettingsSwitch("Animações", settings.animationsEnabled) { updateSettings(settings.copy(animationsEnabled = it)) }
                SettingsSwitch("Modo Leitura", settings.readingModeEnabled) { updateSettings(settings.copy(readingModeEnabled = it)) }

                val appFontSizeName = listOf("Pequena", "Padrão", "Média", "Grande", "Muito grande")[settings.fontSizeIndex.coerceIn(0, 4)]
                SettingsSlider("Tamanho da fonte do aplicativo: $appFontSizeName", settings.fontSizeIndex.toFloat(), 0f, 4f, 4) {
                    updateSettings(settings.copy(fontSizeIndex = it.toInt()))
                }

                val themeNames = mapOf(ThemeModeOption.SYSTEM.name to "Sistema", ThemeModeOption.LIGHT.name to "Claro", ThemeModeOption.DARK.name to "Escuro")
                SettingsDropdown("Tema", themeNames[settings.themeModeOption.name] ?: "Sistema", themeNames.values.toList()) { sel ->
                    val key = themeNames.entries.firstOrNull { it.value == sel }?.key ?: ThemeModeOption.SYSTEM.name
                    updateSettings(settings.copy(themeModeOption = ThemeModeOption.valueOf(key)))
                }

                val colorNames = mapOf(AccentColor.BLUE.name to "Azul", AccentColor.GREEN.name to "Verde", AccentColor.PURPLE.name to "Roxo", AccentColor.GOLD.name to "Dourado", AccentColor.WHITE.name to "Branco/Preto")
                SettingsDropdown("Cor de destaque", colorNames[settings.accentColor.name] ?: "Azul", colorNames.values.toList()) { sel ->
                    val key = colorNames.entries.firstOrNull { it.value == sel }?.key ?: AccentColor.BLUE.name
                    updateSettings(settings.copy(accentColor = AccentColor.valueOf(key)))
                }

                if (loggedInMember != null) {
                    SettingsDropdown("Fonte para leitura", settings.readingFont.name, ReadingFont.values().map { it.name }) {
                        updateSettings(settings.copy(readingFont = ReadingFont.valueOf(it)))
                    }
                }
            }

            item { SettingsCategoryTitle("Leitura") }
            item {
                SettingsSwitch("Manter a tela ligada", settings.keepScreenOn) { updateSettings(settings.copy(keepScreenOn = it)) }
                SettingsSwitch("Salvar posição da leitura", settings.autoSavePosition) { updateSettings(settings.copy(autoSavePosition = it)) }
                SettingsSwitch("Rolagem automática", settings.autoScroll) { updateSettings(settings.copy(autoScroll = it)) }
                SettingsSlider("Brilho interno", settings.internalBrightness, 0f, 1f, 10) {
                    updateSettings(settings.copy(internalBrightness = it))
                }
            }

            item { SettingsCategoryTitle("Áudio") }
            item {
                SettingsDropdown("Velocidade de reprodução", "${settings.playbackSpeed}x", listOf("0.75x", "1.0x", "1.25x", "1.5x", "2.0x")) {
                    val s = it.replace("x", "").toFloatOrNull() ?: 1.0f
                    updateSettings(settings.copy(playbackSpeed = s))
                }
                SettingsDropdown("Pular", "${settings.skipTime}s", listOf("10s", "15s", "30s")) {
                    val s = it.replace("s", "").toIntOrNull() ?: 15
                    updateSettings(settings.copy(skipTime = s))
                }
                SettingsSwitch("Continuar com tela bloqueada", settings.continuePlaybackWhenLocked) { updateSettings(settings.copy(continuePlaybackWhenLocked = it)) }
                SettingsSwitch("Iniciar última pregação", settings.autoStartLastPlayback) { updateSettings(settings.copy(autoStartLastPlayback = it)) }
                SettingsDropdown("Temporizador para desligar", if (settings.sleepTimer == 0) "Desativado" else "${settings.sleepTimer} min", listOf("Desativado", "15 min", "30 min", "60 min")) {
                    val s = it.replace(" min", "").toIntOrNull() ?: 0
                    updateSettings(settings.copy(sleepTimer = s))
                }
            }

            item { SettingsCategoryTitle("Downloads") }
            item {
                SettingsDropdown("Qualidade", settings.downloadQuality.name, DownloadQuality.values().map { it.name }) {
                    updateSettings(settings.copy(downloadQuality = DownloadQuality.valueOf(it)))
                }
                SettingsSwitch("Apenas no Wi-Fi", settings.wifiOnlyDownloads) { updateSettings(settings.copy(wifiOnlyDownloads = it)) }
                SettingsDropdown("Pasta de armazenamento", settings.storageFolder, listOf("Interno", "SD Card")) {
                    updateSettings(settings.copy(storageFolder = it))
                }
                SettingsSwitch("Limpar downloads antigos", settings.autoCleanOldDownloads) { updateSettings(settings.copy(autoCleanOldDownloads = it)) }
                SettingsAction("Espaço ocupado: ${formatStorageSize(occupiedBytes)}") { refreshStorageSize() }
                SettingsAction("Limpar downloads") {
                    micRhemaDownloadsDir(context).deleteRecursively()
                    refreshStorageSize()
                    android.widget.Toast.makeText(context, "Downloads do MIC Rhema removidos.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            item { SettingsCategoryTitle("Notificações") }
            item {

                SettingsSwitch("Novos cursos", settings.notifNewCourses) { updateSettings(settings.copy(notifNewCourses = it)) }
                SettingsSwitch("Devocional diário às 8h", settings.notifDailyDevotional) { updateSettings(settings.copy(notifDailyDevotional = it)) }
                SettingsSwitch("Avisos de eventos e cultos", settings.notifEvents) { updateSettings(settings.copy(notifEvents = it)) }
                SettingsSwitch("Próximo culto", settings.notifNextService) { updateSettings(settings.copy(notifNextService = it)) }
                SettingsSwitch("Notícia do meio-dia", settings.notifDailyNews) { updateSettings(settings.copy(notifDailyNews = it)) }
                SettingsSwitch("Novas mídias", settings.notifNewMedia) { updateSettings(settings.copy(notifNewMedia = it)) }
                SettingsSwitch("Novas aulas e módulos IBR", settings.notifIbrContent) { updateSettings(settings.copy(notifIbrContent = it)) }
                SettingsSwitch("Novas pregações", settings.notifNewSermons) { updateSettings(settings.copy(notifNewSermons = it)) }
            }

            item { SettingsCategoryTitle("Internet") }
            item {
                SettingsSwitch("Pré-carregar imagens", settings.preloadImages) { updateSettings(settings.copy(preloadImages = it)) }
                SettingsSwitch("Economizar dados móveis", settings.saveMobileData) { updateSettings(settings.copy(saveMobileData = it)) }
                SettingsSwitch("Atualizar automaticamente", settings.autoUpdateContent) { updateSettings(settings.copy(autoUpdateContent = it)) }
                SettingsAction("Limpar cache") {
                    context.cacheDir.deleteRecursively()
                    android.widget.Toast.makeText(context, "Cache seguro limpo.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            item { SettingsCategoryTitle("Favoritos") }
            item {
                SettingsSwitch("Sincronizar favoritos", settings.syncFavorites) { updateSettings(settings.copy(syncFavorites = it)) }
                SettingsSwitch("Backup automático", settings.autoBackup) { updateSettings(settings.copy(autoBackup = it)) }
                SettingsSwitch("Histórico de reprodução", settings.trackPlaybackHistory) { updateSettings(settings.copy(trackPlaybackHistory = it)) }
                SettingsAction("Limpar histórico") {
                    recentlyViewedState.clear()
                    LocalDataManager.saveAll(context)
                    android.widget.Toast.makeText(context, "Histórico limpo.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            item { SettingsCategoryTitle("Avançado") }
            item {
                SettingsAction("Recarregar banco de dados") {
                    LocalDataManager.loadAll(context)
                    refreshStorageSize()
                    android.widget.Toast.makeText(context, "Dados locais recarregados; o conteúdo remoto é sincronizado pelos listeners ativos.", android.widget.Toast.LENGTH_SHORT).show()
                }
                SettingsAction("Resetar configurações", color = MaterialTheme.colorScheme.error) {
                    updateSettings(UserSettings())
                }
            }

            if (loggedInMember != null) {
                item { SettingsCategoryTitle("Conta") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(loggedInMember.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(loggedInMember.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(loggedInMember.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    SettingsAction("Alterar número de contato") { onNavigateProfile() }
                    SettingsAction("Sair da conta", color = MaterialTheme.colorScheme.error) {
                        MemberManager.setLoggedInMember(context, null)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).padding(top = 12.dp)
    )
}

@Composable
fun SettingsSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(title: String, value: Float, min: Float, max: Float, steps: Int, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            steps = steps
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(title: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Box {
            Text(selected, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsAction(title: String, color: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
