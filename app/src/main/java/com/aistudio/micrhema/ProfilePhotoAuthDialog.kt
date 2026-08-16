package com.aistudio.micrhema

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private fun photoAuthPhoneE164(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.startsWith("55")) "+$digits" else "+55$digits"
}

@Composable
fun ProfilePhotoAuthDialog(
    member: MemberRequest,
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var codeSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }

    fun completeWithCredential(credential: PhoneAuthCredential) {
        if (completed) return
        completed = true
        isLoading = true
        errorMessage = null
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null || user.isAnonymous) {
                    completed = false
                    isLoading = false
                    errorMessage = "Não foi possível confirmar uma sessão autenticada."
                    return@addOnSuccessListener
                }
                MemberManager.setLoggedInMember(context, member)
                coroutineScope.launch {
                    runCatching {
                        MemberManager.bindFirebaseUidToLoggedInMember(context, user.uid)
                    }.onSuccess {
                        isLoading = false
                        onAuthenticated()
                    }.onFailure { error ->
                        FirebaseAuth.getInstance().signOut()
                        completed = false
                        isLoading = false
                        errorMessage = "Não foi possível vincular o telefone ao perfil: ${error.message ?: "tente novamente"}"
                    }
                }
            }
            .addOnFailureListener { error ->
                completed = false
                isLoading = false
                errorMessage = "Código inválido ou expirado: ${error.message ?: "tente novamente"}"
            }
    }

    fun requestCode() {
        if (activity == null) {
            errorMessage = "Não foi possível abrir a validação nesta tela."
            return
        }
        val phone = photoAuthPhoneE164(member.phone)
        if (phone.length < 12) {
            errorMessage = "O telefone cadastrado não é válido. Peça ao ADM para atualizar o número."
            return
        }
        isLoading = true
        errorMessage = null
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                completeWithCredential(credential)
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                isLoading = false
                errorMessage = "Não foi possível enviar o SMS. Se você não tem acesso a este número, a foto não poderá ser alterada."
            }

            override fun onCodeSent(
                newVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = newVerificationId
                codeSent = true
                isLoading = false
            }
        }
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Confirmar troca de foto") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (codeSent) "Enviamos um código SMS para o telefone cadastrado. Confirme o código para liberar a troca da foto."
                    else "Para proteger seu perfil, confirme por SMS que você tem acesso ao telefone cadastrado."
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (codeSent) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter { character -> character.isDigit() }.take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código SMS") },
                        placeholder = { Text("000000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!codeSent) {
                        requestCode()
                    } else {
                        val id = verificationId
                        if (id.isNullOrBlank() || code.length != 6) {
                            errorMessage = "Digite o código de 6 dígitos recebido por SMS."
                        } else {
                            completeWithCredential(PhoneAuthProvider.getCredential(id, code))
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Text(if (codeSent) "Confirmar" else "Enviar SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}
