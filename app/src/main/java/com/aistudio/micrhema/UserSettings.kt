package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import com.google.gson.Gson

enum class AccentColor { BLUE, GREEN, PURPLE, GOLD, WHITE }
enum class ReadingFont { ROBOTO, INTER, OPEN_SANS, SERIF }
enum class DownloadQuality { LOW, MEDIUM, HIGH }
enum class ThemeModeOption { SYSTEM, LIGHT, DARK }

data class UserSettings(
    // Appearance
    var notificationsEnabled: Boolean = true,
    var vibrationEnabled: Boolean = true,
    var animationsEnabled: Boolean = true,
    var readingModeEnabled: Boolean = false,
    var fontSizeIndex: Int = 1,
    var themeModeOption: ThemeModeOption = ThemeModeOption.SYSTEM,
    var accentColor: AccentColor = AccentColor.BLUE,
    var readingFont: ReadingFont = ReadingFont.ROBOTO,

    // Reading
    var keepScreenOn: Boolean = true,
    var autoSavePosition: Boolean = true,
    var autoScroll: Boolean = false,
    var internalBrightness: Float = 0.5f,

    // Audio
    var playbackSpeed: Float = 1.0f,
    var skipTime: Int = 15,
    var continuePlaybackWhenLocked: Boolean = true,
    var autoStartLastPlayback: Boolean = false,
    var sleepTimer: Int = 0,

    // Downloads
    var downloadQuality: DownloadQuality = DownloadQuality.MEDIUM,
    var wifiOnlyDownloads: Boolean = true,
    var storageFolder: String = "Interno",
    var autoCleanOldDownloads: Boolean = false,

    // Notifications
    var notifNewCourses: Boolean = true,
    var notifDailyDevotional: Boolean = true,
    var notifEvents: Boolean = true,
    var notifNewSermons: Boolean = true,
    var notifNewMedia: Boolean = true,
    var notifNextService: Boolean = true,
    var notifDailyNews: Boolean = true,
    var notifIbrContent: Boolean = true,

    // Internet
    var preloadImages: Boolean = true,
    var saveMobileData: Boolean = false,
    var autoUpdateContent: Boolean = true,

    // Favorites
    var syncFavorites: Boolean = true,
    var autoBackup: Boolean = true,
    var trackPlaybackHistory: Boolean = true
)

val currentSettingsState = mutableStateOf(UserSettings())

object UserSettingsManager {
    private const val PREFS_NAME = "micrhema_user_settings"
    private const val KEY_SETTINGS = "settings_json"

    fun getStoredSettings(context: Context): UserSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                Gson().fromJson(json, UserSettings::class.java)
            } catch (e: Exception) {
                UserSettings()
            }
        } else {
            UserSettings()
        }
    }

    fun loadSettings(context: Context) {
        val localSettings = getStoredSettings(context)
        
        currentSettingsState.value = localSettings
        GlobalAudioPlayer.applySettings(context, localSettings)
        syncFromFirestore(context)
    }

    fun saveSettings(context: Context, settings: UserSettings) {
        currentSettingsState.value = settings
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SETTINGS, Gson().toJson(settings)).apply()
        GlobalAudioPlayer.applySettings(context, settings)

        // Also update legacy settings for compatibility
        val mode = when (settings.themeModeOption) {
            ThemeModeOption.LIGHT -> ThemeMode.LIGHT
            ThemeModeOption.DARK -> ThemeMode.DARK
            ThemeModeOption.SYSTEM -> ThemeMode.SYSTEM
        }
        SettingsManager.setThemeMode(context, mode)
        currentThemeMode.value = mode

        val member = loggedInMemberState.value
        if (member != null && BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("user_settings").document(member.id).set(settings)
        }
    }

    private fun syncFromFirestore(context: Context) {
        val member = loggedInMemberState.value
        if (member != null && BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("user_settings").document(member.id).addSnapshotListener { doc, e ->
                if (e != null || doc == null) return@addSnapshotListener
                if (doc.exists()) {
                    val settings = doc.toObject(UserSettings::class.java)
                    if (settings != null) {
                        currentSettingsState.value = settings
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_SETTINGS, Gson().toJson(settings)).apply()
                        GlobalAudioPlayer.applySettings(context, settings)
                        
                        val mode = when (settings.themeModeOption) {
                            ThemeModeOption.LIGHT -> ThemeMode.LIGHT
                            ThemeModeOption.DARK -> ThemeMode.DARK
                            ThemeModeOption.SYSTEM -> ThemeMode.SYSTEM
                        }
                        SettingsManager.setThemeMode(context, mode)
                        currentThemeMode.value = mode
                    }
                }
            }
        }
    }
}
