package com.aistudio.micrhema

import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    )
}

@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            content = content
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    colors: androidx.compose.material3.TextFieldColors? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        maxLines = maxLines,
        colors = colors ?: OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun LocalUploadField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    mimeType: String,
    targetUid: String? = null,
    modifier: Modifier = Modifier
) {
    val isUploading = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val uploadProgress = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            isUploading.value = true
            coroutineScope.launch {
                try {
                    uploadProgress.floatValue = 0f
                    val url = com.aistudio.micrhema.StorageHelper.uploadFile(
                        context = context,
                        uri = uri,
                        path = "uploads",
                        onProgress = { progress -> uploadProgress.floatValue = progress.coerceIn(0f, 1f) },
                        targetUid = targetUid,
                        mimeTypeHint = mimeType
                    )
                    if (!url.isNullOrBlank()) {
                        onValueChange(url)
                        android.widget.Toast.makeText(context, "Arquivo enviado com sucesso.", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Não foi possível enviar o arquivo. Verifique o acesso administrativo e a conexão.", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (error: Exception) {
                    android.util.Log.e("LocalUploadField", "Falha no upload do arquivo selecionado", error)
                    android.widget.Toast.makeText(
                        context,
                        "Não foi possível enviar o arquivo: ${error.message ?: "verifique a conexão e tente novamente"}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isUploading.value = false
                    uploadProgress.floatValue = 0f
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("Cole uma URL legada ou selecione um arquivo") },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = {
            if (isUploading.value) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { uploadProgress.floatValue }, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    Text("${(uploadProgress.floatValue * 100).toInt()}%", fontSize = 10.sp)
                }
            } else {
                IconButton(onClick = { launcher.launch(arrayOf(mimeType)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Upload")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): androidx.compose.ui.graphics.Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )
        val transition = androidx.compose.animation.core.rememberInfiniteTransition()
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            )
        )
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}

@Composable
fun SkeletonItem(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    height: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .then(if (width != androidx.compose.ui.unit.Dp.Unspecified) Modifier.width(width) else Modifier)
            .then(if (height != androidx.compose.ui.unit.Dp.Unspecified) Modifier.height(height) else Modifier)
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun FloatingNavigationBar(
    items: List<com.aistudio.micrhema.AppTab>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFDCC8B6), // Light beige from image
            contentColor = Color(0xFF131B2E), // Dark color for icons
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show up to 4 items from the list
                val barItems = items
                
                barItems.forEach { item ->
                    val route = if (item.id == "bible_tab") "bible" else (item.systemRoute ?: "custom_tab/${item.id}")
                    val isSelected = currentRoute == route
                    
                    val background = if (isSelected) Color(0xFF131B2E) else Color.Transparent
                    val contentColor = if (isSelected) Color.White else Color(0xFF131B2E)
                    
                    androidx.compose.material3.Surface(
                        color = background,
                        contentColor = contentColor,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onNavigate(route) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                getIconFromName(if (isSelected) item.iconName else item.iconName.replace("Border", "")), // Simplified icon handling
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                
                // Menu Button
                androidx.compose.material3.Surface(
                    color = Color.Transparent,
                    contentColor = Color(0xFF131B2E),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onMenuClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Apps,
                            contentDescription = "Menu",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
