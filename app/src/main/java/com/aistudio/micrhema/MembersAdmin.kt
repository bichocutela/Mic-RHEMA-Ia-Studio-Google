package com.aistudio.micrhema

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
        Text("Gerenciar Membros (VIP/IBR)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
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
                            if (member.isApproved || member.isVip || member.isIbr) {
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
                                checked = member.isVip,
                                onCheckedChange = { 
                                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                                    if (index != -1) {
                                        val updated = member.copy(isVip = it)
                                        memberRequestsState[index] = updated
                                        MemberManager.saveToFirestore(context, updated,
                                            onSuccess = { Toast.makeText(context, if(it) "Acesso VIP aprovado para ${member.name}" else "Acesso VIP removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                            onFailure = { memberRequestsState[index] = member; Toast.makeText(context, "Erro: ${it.message}\n\nVerifique as regras de segurança (Rules) do seu Firebase Firestore.", Toast.LENGTH_LONG).show() }
                                        )
                                    }
                                }
                            )
                            Text("VIP")
                            
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
