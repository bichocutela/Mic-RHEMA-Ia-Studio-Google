package com.aistudio.micrhema

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun shortMemberName(fullName: String): String {
    return fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" ")
}

/**
 * O telefone é a identidade de acesso do membro.
 * Aceita DDD+número e também +55/55+DDD+número, sempre reduzindo ao mesmo valor brasileiro.
 */
private fun normalizeMemberPhone(value: String): String {
    val digits = value.filter(Char::isDigit)
    return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
}

private fun memberPhoneDocumentId(phone: String): String = "phone_${normalizeMemberPhone(phone)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun createAccessRequest() {
        val completeName = name.trim()
        val cleanPhone = normalizeMemberPhone(phone)
        if (completeName.isBlank() || cleanPhone.length !in 10..11) {
            errorMessage = "Preencha seu nome completo e um telefone válido com DDD."
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            val recovery = runCatching { MemberSessionClient.recover(context, cleanPhone) }
                .getOrElse { error ->
                    isLoading = false
                    errorMessage = "Não foi possível verificar seu cadastro agora: ${error.message ?: "verifique sua conexão"}. Nenhuma nova solicitação foi criada."
                    return@launch
                }

            if (recovery.found) {
                val existing = recovery.member
                if (existing == null) {
                    isLoading = false
                    errorMessage = "O cadastro foi localizado, mas o perfil retornou incompleto. Tente novamente."
                    return@launch
                }

                // A sessão Firebase já foi restaurada com UID estável pelo MemberSessionClient.
                // Assim IBR, favoritos e demais dados do usuário podem ser lidos no novo aparelho.
                MemberManager.setLoggedInMember(context, existing)
                loadFavoritesFromFirestore()
                MemberSessionClient.syncMemberState(
                    context = context,
                    member = existing,
                    identityPhone = existing.phone
                )
                isLoading = false
                val message = if (recovery.duplicateCount > 0) {
                    "Acesso recuperado. Registros antigos duplicados foram ignorados."
                } else {
                    "Acesso e progresso recuperados."
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                onLoginSuccess()
                return@launch
            }

            // Só cria uma solicitação depois que o backend confirmou que esse telefone
            // não pertence a nenhum cadastro existente. Se a verificação falhar, não cria nada.
            val newRequest = MemberRequest(
                id = memberPhoneDocumentId(cleanPhone),
                name = shortMemberName(completeName),
                ibrCertificateName = completeName,
                phone = cleanPhone,
                isApproved = false,
                isVip = false,
                isIbr = false
            )
            MemberManager.submitPendingAccessRequest(
                context,
                newRequest,
                onSuccess = {
                    MemberManager.setLoggedInMember(context, newRequest)
                    isLoading = false
                    android.widget.Toast.makeText(
                        context,
                        "Solicitação enviada. Aguarde a aprovação do administrador.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    onLoginSuccess()
                },
                onFailure = { error ->
                    isLoading = false
                    errorMessage = "Não foi possível enviar sua solicitação: ${error.message ?: "verifique sua conexão"}"
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.rhema_login_logo),
            contentDescription = "Logo Ministério Igreja de Cristo Rhema",
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 12.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Entre ou peça seu acesso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Informe seu nome e telefone. Se esse número já tiver cadastro, sua conta será recuperada em vez de criar uma nova solicitação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome completo") },
                    supportingText = { Text("Se aprovado no IBR, será usado no certificado. No app exibiremos seu primeiro nome ou os dois primeiros nomes.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { character -> character.isDigit() }.take(13) },
                    label = { Text("Número de telefone com DDD") },
                    placeholder = { Text("Ex: 84999832583") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { createAccessRequest() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Entrar ou solicitar acesso", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Seu telefone identifica a conta. Em outro aparelho, use o mesmo número para recuperar o perfil e o progresso já sincronizado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
