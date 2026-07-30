package com.aistudio.micrhema

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun MembersScreen() {
    val context = LocalContext.current
    val loggedInMember = loggedInMemberState.value
    var isLoginMode by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    if (loggedInMember != null) {
        // Logged In VIP Section
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Olá, ${loggedInMember.name}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(loggedInMember.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            if (loggedInMember.isApproved) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Status da Conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (loggedInMember.isVip) {
                            Text("👑 Membro VIP", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Membro Aprovado", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (loggedInMember.isIbr) {
                            Text("🎓 Aluno IBR", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Conta em Análise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sua solicitação de acesso está sob análise da nossa equipe. Aguarde a aprovação.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    (context as? MainActivity)?.let { activity ->
                        performLogout(activity, null) // Needs a way to access navController, or just reset state
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sair")
            }
        }
    } else {
        // Login / Register Form
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) "Acesso de Membros" else "Solicitar Acesso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    val auth = FirebaseAuth.getInstance()
                    
                    if (isLoginMode) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        // fetch member data
                                        FirebaseFirestore.getInstance().collection("members").document(user.uid).get()
                                            .addOnSuccessListener { doc ->
                                                if (doc.exists()) {
                                                    val member = MemberRequest(
                                                        id = user.uid,
                                                        name = doc.getString("name") ?: "",
                                                        email = doc.getString("email") ?: "",
                                                        isApproved = doc.getBoolean("isApproved") ?: false,
                                                        isVip = doc.getBoolean("isVip") ?: false,
                                                        isIbr = doc.getBoolean("isIbr") ?: false
                                                    )
                                                    MemberManager.setLoggedInMember(context, member)
                                                } else {
                                                    // Should not happen, but fallback
                                                    val member = MemberRequest(id = user.uid, email = user.email ?: "")
                                                    MemberManager.setLoggedInMember(context, member)
                                                }
                                                isLoading = false
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                Toast.makeText(context, "Erro ao buscar dados do membro", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        isLoading = false
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Erro no login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        // Register
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val newMember = hashMapOf(
                                            "name" to name,
                                            "email" to email,
                                            "isApproved" to false,
                                            "isVip" to false,
                                            "isIbr" to false
                                        )
                                        FirebaseFirestore.getInstance().collection("members").document(user.uid).set(newMember)
                                            .addOnSuccessListener {
                                                val member = MemberRequest(
                                                    id = user.uid,
                                                    name = name,
                                                    email = email,
                                                    isApproved = false,
                                                    isVip = false,
                                                    isIbr = false
                                                )
                                                MemberManager.setLoggedInMember(context, member)
                                                isLoading = false
                                                Toast.makeText(context, "Cadastro realizado! Aguardando aprovação.", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                Toast.makeText(context, "Erro ao salvar dados.", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        isLoading = false
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Erro no cadastro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isLoginMode) "Entrar" else "Cadastrar", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(if (isLoginMode) "Não tem conta? Solicite acesso" else "Já tem conta? Fazer login")
            }
        }
    }
}
