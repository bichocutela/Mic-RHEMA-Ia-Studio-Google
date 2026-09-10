package com.aistudio.micrhema

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object AppUpdateStatusStore {
    private const val PREFS_NAME = "micrhema_update_status"
    private const val KEY_AVAILABLE_VERSION = "available_version"
    private const val KEY_LAST_CHECK_AT = "last_check_at"

    internal fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun persist(context: Context, info: UpdateInfo) {
        val editor = preferences(context)
            .edit()
            .putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())

        if (info.updateAvailable && info.latestVersion.isNotBlank()) {
            editor.putString(KEY_AVAILABLE_VERSION, info.latestVersion)
        } else {
            editor.remove(KEY_AVAILABLE_VERSION)
        }
        editor.apply()
    }

    fun availableVersion(context: Context): String? {
        val stored = preferences(context).getString(KEY_AVAILABLE_VERSION, null)?.trim().orEmpty()
        return stored.takeIf { it.isNotBlank() && isNewerVersion(BuildConfig.VERSION_NAME, it) }
    }

    internal fun isAvailabilityKey(key: String?): Boolean = key == KEY_AVAILABLE_VERSION

    private fun isNewerVersion(current: String, candidate: String): Boolean {
        val currentParts = current.split('.').map { it.toIntOrNull() ?: 0 }
        val candidateParts = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(currentParts.size, candidateParts.size)
        for (index in 0 until maxLength) {
            val currentPart = currentParts.getOrElse(index) { 0 }
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            if (candidatePart > currentPart) return true
            if (candidatePart < currentPart) return false
        }
        return false
    }
}

@Composable
fun DrawerVersionFooter(
    onOpenUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { AppUpdateStatusStore.preferences(context) }
    var availableVersion by remember(context) {
        mutableStateOf(AppUpdateStatusStore.availableVersion(context))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (AppUpdateStatusStore.isAvailabilityKey(key)) {
                availableVersion = AppUpdateStatusStore.availableVersion(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Versão ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val updateVersion = availableVersion
        if (!updateVersion.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenUpdate),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Atualização disponível",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Versão $updateVersion • toque para ver",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}
