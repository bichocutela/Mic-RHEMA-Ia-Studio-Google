package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isChecking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }

    var showQrModal by remember { mutableStateOf(false) }
    var showPwaQrModal by remember { mutableStateOf(false) }
    var showErrorModal by remember { mutableStateOf(false) }
    var isGeneratingQr by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var pwaQrCodeBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var publishedVersionName by remember { mutableStateOf("") }
    var apkDownloadUrl by remember { mutableStateOf("") }
    val pwaUrl = "https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/"
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }
    var showPwaCopiedToast by remember { mutableStateOf(false) }

    val updateDownloader = remember { UpdateDownloader(context) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    
    val checkUpdate: () -> Unit = {
        isChecking = true
        coroutineScope.launch {
            val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)
            updateResult = result
            isChecking = false
        }
    }
    
    // Automatically check on load
    LaunchedEffect(Unit) {
        checkUpdate()
    }

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
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sobre",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Liderança", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pastor Evaldo Leôncio", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Nossa Missão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Conectando Pessoas e Transformando Vidas. A palavra Rhema significa a palavra revelada de Deus para um momento específico.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Contato e Localização", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Rua Todos os Santos – Natal/RN", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("84 98804 1804", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("micrhema2@gmail.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Update Checker Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Versão do aplicativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Versão atual: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isChecking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verificando atualizações...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            when (val result = updateResult) {
                                is UpdateResult.Success -> {
                                    if (result.info.updateAvailable) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Nova versão disponível",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = "Versão ${result.info.latestVersion}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            
                                            if (result.info.releaseNotes.isNotBlank()) {
                                                Text(
                                                    text = "Novidades desta versão:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 8.dp)
                                                )
                                                Text(
                                                    text = result.info.releaseNotes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            when (val state = downloadState) {
                                                is DownloadState.Idle -> {
                                                    Button(
                                                        onClick = {
                                                            result.info.downloadUrl?.let { url ->
                                                                downloadJob?.cancel()
                                                                downloadJob = coroutineScope.launch {
                                                                    updateDownloader.downloadUpdate(url, result.info.latestVersion)
                                                                        .collectLatest { newState ->
                                                                            downloadState = newState
                                                                            if (newState is DownloadState.Downloaded) {
                                                                                updateDownloader.installApk(result.info.latestVersion)
                                                                            }
                                                                        }
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        enabled = result.info.downloadUrl != null
                                                    ) {
                                                        Text(if (result.info.downloadUrl != null) "Atualizar agora" else "Apk não encontrado")
                                                    }
                                                }
                                                is DownloadState.Downloading -> {
                                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Baixando atualização...", style = MaterialTheme.typography.bodyMedium)
                                                        Spacer(Modifier.height(8.dp))
                                                        LinearProgressIndicator(
                                                            progress = { state.progress / 100f },
                                                            modifier = Modifier.fillMaxWidth().height(8.dp),
                                                            color = MaterialTheme.colorScheme.primary,
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Text("${state.progress}%", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                is DownloadState.Downloaded -> {
                                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("A atualização foi baixada.", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                                        Spacer(Modifier.height(8.dp))
                                                        Button(
                                                            onClick = { updateDownloader.installApk(result.info.latestVersion) },
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("Instalar atualização")
                                                        }
                                                    }
                                                }
                                                is DownloadState.Error -> {
                                                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Não foi possível baixar a atualização.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                                        Spacer(Modifier.height(8.dp))
                                                        Button(
                                                            onClick = {
                                                                downloadState = DownloadState.Idle
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                        ) {
                                                            Text("Tentar novamente")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Seu aplicativo está atualizado",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }
                                is UpdateResult.Error -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Não foi possível verificar atualizações. Confira sua conexão e tente novamente.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                null -> {}
                            }
                        }

                        if (!isChecking) {
                            OutlinedButton(
                                onClick = checkUpdate,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (updateResult is UpdateResult.Error) "Tentar novamente" else if (updateResult is UpdateResult.Success && (updateResult as UpdateResult.Success).info.updateAvailable) "Verificar novamente" else "Verificar atualização")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Compartilhar acesso",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isGeneratingQr = true
                                        coroutineScope.launch {
                                            var info = (updateResult as? UpdateResult.Success)?.info
                                            if (info == null) {
                                                val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)
                                                if (result is UpdateResult.Success) {
                                                    info = result.info
                                                    updateResult = result
                                                }
                                            }

                                            if (info != null && info.downloadUrl != null && info.downloadUrl.startsWith("https://")) {
                                                publishedVersionName = info.latestVersion
                                                apkDownloadUrl = info.downloadUrl
                                                val bitmap = generateQrCode(info.downloadUrl)
                                                if (bitmap != null) {
                                                    qrCodeBitmap = bitmap
                                                    showQrModal = true
                                                } else {
                                                    showErrorModal = true
                                                }
                                            } else {
                                                showErrorModal = true
                                            }
                                            isGeneratingQr = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isGeneratingQr
                                ) {
                                    if (isGeneratingQr) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gerando")
                                    } else {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Android")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val bitmap = generateQrCode(pwaUrl)
                                        if (bitmap != null) {
                                            pwaQrCodeBitmap = bitmap
                                            showPwaQrModal = true
                                        } else {
                                            showErrorModal = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PWA")
                                }
                            }

                            Text(
                                text = "Android baixa a APK mais recente. PWA abre a versão web instalável diretamente no navegador.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showQrModal) {
        AlertDialog(
            onDismissRequest = { showQrModal = false },
            title = {
                Text(
                    text = "Compartilhar MIC Rhema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Versão disponível: v$publishedVersionName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    qrCodeBitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it,
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(16.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    Text(
                        text = "Escaneie para baixar esta versão",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showCopiedToast) {
                        Text(
                            text = "Link copiado!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(apkDownloadUrl))
                        showCopiedToast = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(2000)
                            showCopiedToast = false
                        }
                    }
                ) {
                    Text("Copiar link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrModal = false }) {
                    Text("Fechar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showPwaQrModal) {
        AlertDialog(
            onDismissRequest = { showPwaQrModal = false },
            title = {
                Text(
                    text = "MIC Rhema PWA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    pwaQrCodeBitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it,
                            contentDescription = "QR Code do MIC Rhema PWA",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(16.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    Text(
                        text = "Escaneie para abrir o MIC Rhema PWA no navegador",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "No celular, o PWA pode ser adicionado à tela inicial pelo próprio navegador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (showPwaCopiedToast) {
                        Text(
                            text = "Link do PWA copiado!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(pwaUrl))
                        showPwaCopiedToast = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(2000)
                            showPwaCopiedToast = false
                        }
                    }
                ) {
                    Text("Copiar link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPwaQrModal = false }) {
                    Text("Fechar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showErrorModal) {
        AlertDialog(
            onDismissRequest = { showErrorModal = false },
            title = {
                Text(
                    text = "Não foi possível gerar o QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Verifique sua conexão e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorModal = false
                        isGeneratingQr = true
                        coroutineScope.launch {
                            val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)
                            if (result is UpdateResult.Success) {
                                updateResult = result
                                val info = result.info
                                if (info.downloadUrl != null && info.downloadUrl.startsWith("https://")) {
                                    publishedVersionName = info.latestVersion
                                    apkDownloadUrl = info.downloadUrl
                                    val bitmap = generateQrCode(info.downloadUrl)
                                    if (bitmap != null) {
                                        qrCodeBitmap = bitmap
                                        showQrModal = true
                                    } else {
                                        showErrorModal = true
                                    }
                                } else {
                                    showErrorModal = true
                                }
                            } else {
                                showErrorModal = true
                            }
                            isGeneratingQr = false
                        }
                    }
                ) {
                    Text("Tentar novamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showErrorModal = false }) {
                    Text("Fechar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
