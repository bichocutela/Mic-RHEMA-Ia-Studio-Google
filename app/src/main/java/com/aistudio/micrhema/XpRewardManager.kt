package com.aistudio.micrhema

import android.content.Context

/** Aplica recompensas que já foram realmente resgatadas na Loja XP. */
object XpRewardManager {
    const val GOLD_THEME = "tema_dourado_rhema"
    const val PROMISE_FRAME = "moldura_luz_promessa"
    const val READER_BADGE = "badge_leitor_palavra"

    private const val PREFS = "micrhema_xp_rewards"

    private fun ownedKey(memberId: String) = "owned:$memberId"

    fun syncOwned(context: Context, memberId: String, redemptions: List<XpRedemption>) {
        val owned = redemptions
            .asSequence()
            .filter { it.status != "cancelado" }
            .map { it.itemId }
            .filter { it.isNotBlank() }
            .toSet()
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(ownedKey(memberId), owned)
            .apply()
    }

    fun isOwned(context: Context, itemId: String, memberId: String? = loggedInMemberState.value?.id): Boolean {
        val id = memberId?.takeIf { it.isNotBlank() } ?: return false
        return itemId in context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(ownedKey(id), emptySet())
            .orEmpty()
    }

    fun activateGoldenTheme(context: Context, memberId: String): Boolean {
        if (!isOwned(context, GOLD_THEME, memberId)) return false
        UserSettingsManager.saveSettings(
            context,
            currentSettingsState.value.copy(accentColor = AccentColor.GOLD)
        )
        return true
    }
}
