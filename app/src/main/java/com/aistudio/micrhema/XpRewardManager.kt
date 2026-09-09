package com.aistudio.micrhema

import android.content.Context

/** Aplica somente recompensas que o ledger da Loja XP confirma como entitlement ativo. */
object XpRewardManager {
    val revision = androidx.compose.runtime.mutableIntStateOf(0)
    /** Mantém o mesmo ID para preservar qualquer entitlement já emitido. */
    const val GOLD_THEME = "tema_dourado_rhema"
    const val GOLD_PLUS_THEME = GOLD_THEME
    const val PROMISE_FRAME = "moldura_luz_promessa"
    const val READER_BADGE = "badge_leitor_palavra"

    private const val PREFS = "micrhema_xp_rewards"

    private fun ownedKey(memberId: String) = "owned:$memberId"
    private fun activeKey(memberId: String, itemId: String) = "active:$memberId:$itemId"
    private fun previousAccentKey(memberId: String) = "previous_accent:$memberId"

    fun syncOwned(context: Context, memberId: String, itemIds: Collection<String>) {
        val owned = itemIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(ownedKey(memberId), owned)
            .apply()
        revision.intValue++
    }

    fun isOwned(context: Context, itemId: String, memberId: String? = loggedInMemberState.value?.id): Boolean {
        val id = memberId?.takeIf { it.isNotBlank() } ?: return false
        return itemId in context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(ownedKey(id), emptySet())
            .orEmpty()
    }

    /** Recompensas cosméticas antigas continuam ativas por padrão para não mudar o visual do usuário. */
    fun isActive(context: Context, itemId: String, memberId: String? = loggedInMemberState.value?.id): Boolean {
        val id = memberId?.takeIf { it.isNotBlank() } ?: return false
        if (!isOwned(context, itemId, id)) return false
        if (itemId == GOLD_PLUS_THEME) return currentSettingsState.value.accentColor == AccentColor.GOLD
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(activeKey(id, itemId), true)
    }

    fun setActive(context: Context, memberId: String, itemId: String, active: Boolean): Boolean {
        if (!isOwned(context, itemId, memberId)) return false
        if (itemId == GOLD_PLUS_THEME) {
            return if (active) activateGoldenPlusTheme(context, memberId) else deactivateGoldenPlusTheme(context, memberId)
        }
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(activeKey(memberId, itemId), active)
            .apply()
        revision.intValue++
        return true
    }

    fun activateGoldenPlusTheme(context: Context, memberId: String): Boolean {
        if (!isOwned(context, GOLD_PLUS_THEME, memberId)) return false
        val currentAccent = currentSettingsState.value.accentColor
        if (currentAccent != AccentColor.GOLD) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(previousAccentKey(memberId), currentAccent.name)
                .apply()
        }
        UserSettingsManager.saveSettings(
            context,
            currentSettingsState.value.copy(accentColor = AccentColor.GOLD)
        )
        return true
    }

    fun deactivateGoldenPlusTheme(context: Context, memberId: String): Boolean {
        if (!isOwned(context, GOLD_PLUS_THEME, memberId)) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(previousAccentKey(memberId), null)
            ?.let { stored -> runCatching { AccentColor.valueOf(stored) }.getOrNull() }
            ?.takeIf { it != AccentColor.GOLD }
            ?: AccentColor.BLUE
        UserSettingsManager.saveSettings(
            context,
            currentSettingsState.value.copy(accentColor = previous)
        )
        return true
    }

    /** Compatibilidade com chamadas antigas; agora ativa o Dourado Plus. */
    fun activateGoldenTheme(context: Context, memberId: String): Boolean =
        activateGoldenPlusTheme(context, memberId)
}
