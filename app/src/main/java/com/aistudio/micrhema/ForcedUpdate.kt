package com.aistudio.micrhema

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun ForcedUpdateGate() {
    val policy = adminAppSettingsState.value
    val admin = adminAuthenticatedState.value || loggedInMemberState.value?.isAdmin == true
    val required = ForcedUpdatePolicy.required(policy.forceUpdateEnabled, policy.minimumVersionName, BuildConfig.VERSION_NAME, admin)
    var updating by remember { mutableStateOf(false) }
    var adminLogin by remember { mutableStateOf(false) }
    if (!required) return
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        BackHandler { updating = false; adminLogin = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Text("Atualização necessária", style = MaterialTheme.typography.headlineSmall)
                Text("Para continuar, instale o MIC Rhema ${policy.minimumVersionName} ou uma versão mais recente.", modifier = Modifier.padding(vertical = 12.dp))
                if (updating || adminLogin) {
                    TextButton(onClick = { updating = false; adminLogin = false }) { Text("Voltar") }
                    Box(modifier = Modifier.weight(1f)) {
                        if (adminLogin) AdminScreen() else AboutScreen()
                    }
                } else {
                    Text("Sua versão: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { updating = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Atualizar aplicativo") }
                    TextButton(onClick = { adminLogin = true }) { Text("Acesso do administrador") }
                }
            }
        }
    }
}

@Composable
fun ForcedUpdateSettingsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remote = adminAppSettingsState.value
    var enabled by remember(remote.updatedAt, remote.forceUpdateEnabled) { mutableStateOf(remote.forceUpdateEnabled) }
    var minimum by remember(remote.updatedAt, remote.minimumVersionName) { mutableStateOf(remote.minimumVersionName) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun fail(message: String) { error = message; busy = false }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Atualização obrigatória", style = MaterialTheme.typography.titleMedium)
            Text("Exija uma versão mínima para continuar usando o aplicativo. Desative para permitir versões anteriores.")
            Row { Checkbox(checked = enabled, onCheckedChange = { enabled = it; error = null }, enabled = !busy); Text("Exigir atualização", modifier = Modifier.padding(top = 12.dp)) }
            OutlinedTextField(value = minimum, onValueChange = { minimum = it; error = null }, label = { Text("Versão mínima") }, placeholder = { Text(BuildConfig.VERSION_NAME) }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
            TextButton(enabled = !busy, onClick = {
                busy = true; error = null
                scope.launch {
                    when (val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)) {
                        is UpdateResult.Success -> { minimum = result.info.latestVersion; busy = false }
                        is UpdateResult.Error -> fail("Não foi possível consultar a versão publicada. Tente novamente.")
                    }
                }
            }) { Text("Usar última versão publicada") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                error = null
                val version = minimum.trim().removePrefix("v")
                if (enabled && ForcedUpdatePolicy.parts(version) == null) {
                    fail("Informe uma versão válida, como ${BuildConfig.VERSION_NAME}.")
                } else {
                    busy = true
                    scope.launch {
                        if (enabled) {
                            val result = UpdateChecker.checkForUpdates(BuildConfig.VERSION_NAME)
                            if (result !is UpdateResult.Success || result.info.downloadUrl.isNullOrBlank()) {
                                fail("Não foi possível confirmar um APK disponível. A exigência não foi ativada.")
                                return@launch
                            }
                            if (ForcedUpdatePolicy.compare(version, result.info.latestVersion)?.let { it > 0 } != false) {
                                fail("A versão mínima não pode ser maior que a última publicada (${result.info.latestVersion}).")
                                return@launch
                            }
                        }
                        Firebase.firestore.collection("settings").document("app").set(mapOf(
                            "forceUpdateEnabled" to enabled, "minimumVersionName" to version,
                            "updatedAt" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                busy = false
                                android.widget.Toast.makeText(context, "Política de atualização salva", android.widget.Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { fail("Não foi possível salvar. Verifique sua conexão.") }
                    }
                }
            }) { if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Salvar exigência de atualização") }
        }
    }
}
