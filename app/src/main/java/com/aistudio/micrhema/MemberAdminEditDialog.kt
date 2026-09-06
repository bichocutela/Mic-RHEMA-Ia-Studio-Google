package com.aistudio.micrhema

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun MemberAdminEditDialog(
    member: MemberRequest,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        email: String,
        address: String,
        birthDate: String,
        certificateName: String
    ) -> Unit
) {
    var name by remember(member.id) { mutableStateOf(member.name) }
    var phone by remember(member.id) { mutableStateOf(member.phone.filter(Char::isDigit)) }
    var email by remember(member.id) { mutableStateOf(member.email) }
    var address by remember(member.id) { mutableStateOf(member.address) }
    var birthDate by remember(member.id) { mutableStateOf(member.birthDate) }
    var certificateName by remember(member.id) {
        mutableStateOf(member.ibrCertificateName.ifBlank { member.name })
    }

    val originalPhone = member.phone.filter(Char::isDigit).let {
        if (it.length in 12..13 && it.startsWith("55")) it.drop(2) else it
    }
    val normalizedNewPhone = phone.filter(Char::isDigit).let {
        if (it.length in 12..13 && it.startsWith("55")) it.drop(2) else it
    }
    val phoneChanged = originalPhone != normalizedNewPhone
    val canSave = name.trim().isNotBlank() && normalizedNewPhone.length in 10..11 && !isSaving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Editar cadastro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "As alterações são gravadas no cadastro real do membro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(13) },
                    label = { Text("Telefone com DDD") },
                    supportingText = {
                        Text(
                            if (phoneChanged)
                                "O acesso será transferido para este novo número. XP, nível, Quiz, Jornada, Loja e IBR permanecem na mesma conta."
                            else
                                "O telefone é a identidade usada para recuperar a conta."
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it.filter { character -> character.isDigit() || character == '/' }.take(10) },
                    label = { Text("Data de nascimento") },
                    placeholder = { Text("dd/mm/aaaa") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = certificateName,
                    onValueChange = { certificateName = it },
                    label = { Text("Nome completo do certificado IBR") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                if (phoneChanged) {
                    Text(
                        "Depois de salvar, o número antigo deixa de localizar esta conta e o novo número passa a recuperar o mesmo memberId.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        name.trim(),
                        normalizedNewPhone,
                        email.trim(),
                        address.trim(),
                        birthDate.trim(),
                        certificateName.trim()
                    )
                }
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.fillMaxWidth(0.12f))
                else Text(if (phoneChanged) "Salvar e transferir" else "Salvar alterações")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
