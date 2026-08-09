package com.aistudio.micrhema
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Person


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun EditMembersSection() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = memberRequestsState, key = { it.id }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(member.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(member.phone, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (member.isApproved || member.isIbr) {
                                Text("Aprovado", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            } else {
                                Text("Pendente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = member.isApproved,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isApproved = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso aprovado para ${member.name}" else "Acesso removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("Aprovado")
                            

                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isIbr,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isIbr = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso IBR aprovado para ${member.name}" else "Acesso IBR removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("IBR")
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isAdmin,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isAdmin = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso Admin aprovado para ${member.name}" else "Acesso Admin removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("Admin")
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { 
                                MemberManager.deleteFromFirestore(context, member); MemberManager.saveMembers(context)
                                memberRequestsState.remove(member)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfilesSection() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Perfis dos Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        val approvedMembers = memberRequestsState.filter { it.isApproved || it.isIbr }
        
        if (approvedMembers.isEmpty()) {
            Text("Nenhum membro aprovado encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = approvedMembers, key = { it.id }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (member.profilePhotoUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = member.profilePhotoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name.takeIf { it.isNotBlank() } ?: "Sem Nome", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("📱 ${member.phone.takeIf { it.isNotBlank() } ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                            if (member.address.isNotBlank()) {
                                Text("📍 ${member.address}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (member.birthDate.isNotBlank()) {
                                Text("🎂 ${member.birthDate}", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                            if (member.createdAt > 0L) {
                                Text("Cadastro: ${dateFormat.format(java.util.Date(member.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (member.updatedAt > 0L) {
                                Text("Atualizado: ${dateFormat.format(java.util.Date(member.updatedAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
