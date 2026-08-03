package com.aistudio.micrhema

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.img_rhema_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("MIC Rhema", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ministério Igreja de Cristo Rhema", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Peça seu acesso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Preencha os dados abaixo para solicitar ou recuperar seu acesso às abas restritas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome e sobrenome") },
                    placeholder = { Text("Ex: João Silva") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Número de telefone com DDD") },
                    placeholder = { Text("Ex: 11999999999") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (name.isBlank() || phone.isBlank()) {
                            android.widget.Toast.makeText(context, "Preencha todos os campos", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        
                        val existing = memberRequestsState.find { it.phone.replace(Regex("[^0-9]"), "") == phone.replace(Regex("[^0-9]"), "") }
                        if (existing != null) {
                            MemberManager.setLoggedInMember(context, existing)
                            android.widget.Toast.makeText(context, "Acesso recuperado!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val newReq = MemberRequest(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                phone = phone.trim(),
                                isApproved = false,
                                isVip = false,
                                isIbr = false
                            )
                            memberRequestsState.add(newReq)
                            MemberManager.saveMembers(context)
                            MemberManager.saveToFirestore(context, newReq)
                            MemberManager.setLoggedInMember(context, newReq)
                            android.widget.Toast.makeText(context, "Solicitação Enviada", android.widget.Toast.LENGTH_LONG).show()
                        }
                        
                        isLoading = false
                        onLoginSuccess()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Enviar Solicitação", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Após enviar, aguarde a aprovação do administrador para acessar os conteúdos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
