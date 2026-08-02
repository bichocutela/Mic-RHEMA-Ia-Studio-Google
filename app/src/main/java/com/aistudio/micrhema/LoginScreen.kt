package com.aistudio.micrhema

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    
    // We get the activity safely
    val activity = context.findActivity()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.img_rhema_logo), // assuming logo
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
                Text("Acesso para Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Entre com sua conta Google para acessar conteúdos exclusivos, pedir oração e acompanhar a vida da igreja.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { 
                        if (activity != null) {
                            isLoading = true
                            try {
                                val provider = com.google.firebase.auth.OAuthProvider.newBuilder("google.com")
                                FirebaseAuth.getInstance()
                                    .startActivityForSignInWithProvider(activity, provider.build())
                                    .addOnSuccessListener { authResult ->
                                        isLoading = false
                                        val user = authResult.user
                                        if (user != null) {
                                            val email = user.email ?: ""
                                            val name = user.displayName ?: ""
                                            
                                            val existing = memberRequestsState.find { it.email.lowercase() == email.lowercase().trim() }
                                            if (existing != null) {
                                                MemberManager.setLoggedInMember(context, existing)
                                            } else {
                                                val newReq = MemberRequest(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = name,
                                                    email = email.trim(),
                                                    isApproved = false,
                                                    isVip = false,
                                                    isIbr = false
                                                )
                                                memberRequestsState.add(newReq)
                                                MemberManager.saveToFirestore(newReq)
                                                MemberManager.setLoggedInMember(context, newReq)
                                            }
                                            onLoginSuccess()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        android.widget.Toast.makeText(context, "Erro: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                            } catch (e: Exception) {
                                isLoading = false
                                android.widget.Toast.makeText(context, "Erro ao abrir navegador", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Erro: Activity não encontrada", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("🔑 Entrar com Google", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Após o login, solicite acesso de membro para liberar conteúdos exclusivos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

fun firebaseAuthWithGoogle(idToken: String, name: String, email: String, context: Context, onSuccess: () -> Unit, onComplete: () -> Unit) {
    try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Check if member exists in Firestore
                    val existing = memberRequestsState.find { it.email.lowercase() == email.lowercase().trim() }
                    if (existing != null) {
                        MemberManager.setLoggedInMember(context, existing)
                    } else {
                        // Create new member request
                        val newReq = MemberRequest(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name,
                            email = email.trim(),
                            isApproved = false,
                            isVip = false,
                            isIbr = false
                        )
                        memberRequestsState.add(newReq)
                        MemberManager.saveToFirestore(newReq)
                        MemberManager.setLoggedInMember(context, newReq)
                    }
                    onSuccess()
                } else {
                    android.widget.Toast.makeText(context, "Erro ao autenticar", android.widget.Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }
    } catch (e: Exception) {
        onComplete()
    }
}


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
