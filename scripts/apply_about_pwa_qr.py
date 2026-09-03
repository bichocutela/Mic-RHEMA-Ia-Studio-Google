from pathlib import Path

path = Path('app/src/main/java/com/aistudio/micrhema/AboutScreen.kt')
text = path.read_text(encoding='utf-8')

old = '''    var showQrModal by remember { mutableStateOf(false) }
    var showErrorModal by remember { mutableStateOf(false) }
    var isGeneratingQr by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var publishedVersionName by remember { mutableStateOf("") }
    var apkDownloadUrl by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }
'''
new = '''    var showQrModal by remember { mutableStateOf(false) }
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
'''
if old not in text:
    raise SystemExit('Estado do QR Code não encontrado')
text = text.replace(old, new, 1)

old_button = '''                            OutlinedButton(
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
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGeneratingQr
                            ) {
                                if (isGeneratingQr) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gerando...")
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gerar versão em QR Code")
                                }
                            }
'''
new_button = '''                            Text(
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
'''
if old_button not in text:
    raise SystemExit('Botão antigo de QR Code não encontrado')
text = text.replace(old_button, new_button, 1)

anchor = '''    if (showErrorModal) {
'''
pwa_modal = '''    if (showPwaQrModal) {
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

'''
if anchor not in text:
    raise SystemExit('Âncora do modal de erro não encontrada')
text = text.replace(anchor, pwa_modal + anchor, 1)

path.write_text(text, encoding='utf-8')
print('QR Code PWA adicionado ao lado do Android na aba Sobre.')
