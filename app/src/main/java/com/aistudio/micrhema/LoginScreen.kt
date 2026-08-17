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
import com.google.firebase.firestore.DocumentSnapshot

private fun shortMemberName(fullName: String): String {
    return fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" ")
}

private fun memberFromLoginDocument(document: DocumentSnapshot): MemberRequest {
    val rawName = document.getString("name") ?: ""
    return MemberRequest(
        id = document.id,
        firebaseUid = document.getString("firebaseUid") ?: "",
        name = shortMemberName(rawName),
        ibrCertificateName = document.getString("ibrCertificateName").orEmpty().ifBlank { rawName },
        phone = document.getString("phone") ?: "",
        email = document.getString("email") ?: "",
        isApproved = document.getBoolean("isApproved") ?: false,
        isIbr = document.getBoolean("isIbr") ?: false,
        isAdmin = document.getBoolean("isAdmin") ?: false,
        ibrCertificateUrl = document.getString("ibrCertificateUrl") ?: "",
        ibrCertificateStoragePath = document.getString("ibrCertificateStoragePath") ?: "",
        avatarId = document.getString("avatarId").orEmpty().ifBlank { DEFAULT_BIBLICAL_AVATAR_ID },
        unlockedBadgeIds = (document.get("unlockedBadgeIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }
            ?: listOf(DEFAULT_BIBLICAL_BADGE_ID),
        equippedBadgeId = document.getString("equippedBadgeId").orEmpty().ifBlank { DEFAULT_BIBLICAL_BADGE_ID },
        badgeActivityIds = (document.get("badgeActivityIds") as? Map<*, *>).orEmpty().mapNotNull { (key, value) ->
            val activity = key?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            activity to ((value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList())
        }.toMap(),
        status = document.getString("status") ?: "pendente",
        address = document.getString("address") ?: "",
        birthDate = document.getString("birthDate") ?: "",
        supabaseStoragePath = document.getString("supabaseStoragePath") ?: ""
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun createAccessRequest() {
        val completeName = name.trim()
        val cleanPhone = phone.filter { it.isDigit() }
        if (completeName.isBlank() || cleanPhone.length < 10) {
            errorMessage = "Preencha seu nome completo e um telefone válido com DDD."
            return
        }
        isLoading = true
        errorMessage = null
        val createNewRequest = {
            val newRequest = MemberRequest(
                id = java.util.UUID.randomUUID().toString(),
                name = shortMemberName(completeName),
                ibrCertificateName = completeName,
                phone = cleanPhone,
                isApproved = false,
                isVip = false,
                isIbr = false
            )
            // O pedido público grava somente os campos pendentes permitidos pela regra.
            MemberManager.submitPendingAccessRequest(
                context,
                newRequest,
                onSuccess = {
                    MemberManager.setLoggedInMember(context, newRequest)
                    isLoading = false
                    android.widget.Toast.makeText(context, "Solicitação enviada. Aguarde a aprovação do administrador.", android.widget.Toast.LENGTH_LONG).show()
                    onLoginSuccess()
                },
                onFailure = { error ->
                    isLoading = false
                    errorMessage = "Não foi possível enviar sua solicitação: ${error.message ?: "verifique sua conexão"}"
                }
            )
        }

        val firebaseReady = runCatching {
            com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()
        }.getOrDefault(false)
        if (firebaseReady) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("acessos_pendentes")
                .get()
                .addOnSuccessListener { snapshot ->
                    val existing = snapshot.documents.firstOrNull {
                        (it.getString("phone") ?: "").filter { character -> character.isDigit() } == cleanPhone
                    }
                    if (existing != null) {
                        MemberManager.setLoggedInMember(context, memberFromLoginDocument(existing))
                        isLoading = false
                        android.widget.Toast.makeText(context, "Acesso recuperado.", android.widget.Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        createNewRequest()
                    }
                }
                .addOnFailureListener {
                    createNewRequest()
                }
        } else {
            val existing = memberRequestsState.find { it.phone.filter { character -> character.isDigit() } == cleanPhone }
            if (existing != null) {
                MemberManager.setLoggedInMember(context, existing)
                isLoading = false
                onLoginSuccess()
            } else {
                createNewRequest()
            }
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
                Text("Peça ou recupere seu acesso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Informe seus dados para solicitar acesso ou recuperar seu perfil. O acesso normal usa apenas o nome e o telefone cadastrados.",
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
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Enviar Solicitação", color = Color.White, fontWeight = FontWeight.Bold)
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Após enviar, aguarde a aprovação do administrador para acessar os conteúdos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
