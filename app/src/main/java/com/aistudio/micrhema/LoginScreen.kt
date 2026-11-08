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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import java.util.concurrent.TimeUnit

private fun migrationLoginActivity(context: android.content.Context): android.app.Activity? {
    var current = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return current as? android.app.Activity
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verificationId by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var phoneConfirmed by remember { mutableStateOf(false) }

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
            // Previously verified identity opens locally, even when 429/offline.
            // A fresh user or a signed-out account must still validate online.
            val local = withContext(Dispatchers.IO) {
                MemberOfflineCache.restoreTrusted(context, cleanPhone)
                    ?: MemberOfflineCache.restoreLegacy(context, cleanPhone)
            }
            if (local != null && (!MigrationAuthentication.enabled || MemberFirebaseAuth.forMember(local.id)?.currentUser?.isAnonymous == false)) {
                MemberManager.setLoggedInMember(context, local, bindFirebaseIdentity = false)
                XpAccountCache.restore(context, local)
                MemberOfflineSync.schedule(context)
                isLoading = false
                onLoginSuccess()
                return@launch
            }

            if (MigrationAuthentication.enabled && !phoneConfirmed) {
                val auth = MemberFirebaseAuth.get()
                if (verificationId.isBlank()) {
                    val activity = migrationLoginActivity(context)
                    if (activity == null) { isLoading = false; errorMessage = "Reabra a tela para confirmar seu telefone."; return@launch }
                    auth.setLanguageCode("pt-BR")
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationFailed(exception: FirebaseException) {
                            isLoading = false
                            errorMessage = exception.localizedMessage ?: "Não foi possível enviar o código. Tente novamente."
                        }
                        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                            if (normalizeMemberPhone(phone) != cleanPhone) return
                            verificationId = id
                            isLoading = false
                        }
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            if (normalizeMemberPhone(phone) != cleanPhone || phoneConfirmed) return
                            scope.launch {
                                runCatching { auth.signInWithCredential(credential).await() }
                                    .onSuccess { phoneConfirmed = true; createAccessRequest() }
                                    .onFailure { isLoading = false; errorMessage = it.localizedMessage ?: "Não foi possível confirmar o telefone." }
                            }
                        }
                    }
                    runCatching {
                        PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber("+55$cleanPhone").setTimeout(60, TimeUnit.SECONDS)
                            .setActivity(activity).setCallbacks(callbacks).build())
                    }.onFailure { isLoading = false; errorMessage = it.localizedMessage }
                    return@launch
                }
                if (!verificationCode.matches(Regex("\\d{6}"))) {
                    isLoading = false; errorMessage = "Digite os 6 números do código recebido por SMS."; return@launch
                }
                try {
                    auth.signInWithCredential(PhoneAuthProvider.getCredential(verificationId, verificationCode)).await()
                    phoneConfirmed = true
                } catch (exception: Exception) {
                    isLoading = false; errorMessage = exception.localizedMessage ?: "Código inválido. Tente novamente."; return@launch
                }
            }

            val recovery = runCatching { MemberSessionClient.recover(context, cleanPhone) }
                .getOrElse { error ->
                    android.util.Log.w("LoginScreen", "Falha ao verificar cadastro", error)
                    isLoading = false
                    errorMessage = "O serviço de cadastro está temporariamente indisponível. Tente novamente mais tarde. Nenhuma nova solicitação foi criada."
                    return@launch
                }

            if (recovery.found) {
                val existing = recovery.member
                if (existing == null) {
                    isLoading = false
                    errorMessage = "O cadastro foi localizado, mas o perfil retornou incompleto. Tente novamente."
                    return@launch
                }

                MemberManager.setLoggedInMember(context, existing, bindFirebaseIdentity = false)
                MemberOfflineSync.schedule(context, immediate = false)
                loadFavoritesFromFirestore()
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

            if (MigrationAuthentication.enabled) {
                runCatching { MemberSessionClient.registerVerified(context, completeName, cleanPhone) }
                    .onSuccess { result ->
                        val member = result.member
                        if (member == null) { errorMessage = "O servidor retornou um cadastro incompleto." }
                        else {
                            MemberManager.setLoggedInMember(context, member, bindFirebaseIdentity = false)
                            MemberOfflineSync.schedule(context, immediate = false)
                            onLoginSuccess()
                        }
                    }
                    .onFailure { errorMessage = it.localizedMessage ?: "Não foi possível solicitar acesso." }
                isLoading = false
                return@launch
            }

            val newRequest = MemberRequest(
                id = java.util.UUID.randomUUID().toString(),
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
                    errorMessage = "Não foi possível enviar a solicitação agora. Tente novamente mais tarde."
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Image(
            painter = painterResource(id = R.drawable.rhema_login_logo),
            contentDescription = "Logo Ministério Igreja de Cristo Rhema",
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 8.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Entre ou peça seu acesso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Informe seu nome e telefone. Se esse número já tiver cadastro, sua conta será recuperada em vez de criar uma nova solicitação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome completo") },
                    supportingText = { Text("Se aprovado no IBR, será usado no certificado. No app exibiremos seu primeiro nome ou os dois primeiros nomes.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { character -> character.isDigit() }.take(13); verificationId = ""; verificationCode = ""; phoneConfirmed = false },
                    label = { Text("Número de telefone com DDD") },
                    placeholder = { Text("Ex: 84999832583") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (MigrationAuthentication.enabled && verificationId.isNotBlank() && !phoneConfirmed) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.filter(Char::isDigit).take(6) },
                        label = { Text("Código recebido por SMS") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Button(
                    onClick = { createAccessRequest() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Entrar ou solicitar acesso", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Seu telefone identifica a conta. Em outro aparelho, use o mesmo número para recuperar o perfil e o progresso já sincronizado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
