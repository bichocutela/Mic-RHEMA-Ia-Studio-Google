package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DonationsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Dízimos e Ofertas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Contribua com a obra de Deus.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            
            val pixKey = pixKeyState.value
            val qrCodeUrl = pixQrCodeUrlState.value
            
            if (pixKey.isEmpty() && qrCodeUrl.isEmpty()) {
                Text("As informações de doação ainda não foram configuradas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            } else {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (qrCodeUrl.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = qrCodeUrl,
                                contentDescription = "QR Code Pix",
                                modifier = Modifier.size(200.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (pixKey.isNotEmpty()) {
                            Text("Chave PIX", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(pixKey, style = MaterialTheme.typography.bodyLarge)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipData = android.content.ClipData.newPlainText("Chave Pix", pixKey)
                                clipboardManager.setPrimaryClip(clipData)
                                android.widget.Toast.makeText(context, "Chave Pix copiada!", android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = "Copiar")
                                Spacer(Modifier.width(8.dp))
                                Text("Copiar Chave")
                            }
                        }
                    }
                }
            }
        }
    }
}
