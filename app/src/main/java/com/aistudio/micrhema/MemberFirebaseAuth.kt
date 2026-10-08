package com.aistudio.micrhema

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Keeps member API tokens isolated from the administrator's default Firebase
 * session. Both apps use the same Firebase project, but maintain independent auth.
 */
object MemberFirebaseAuth {
    private const val MEMBER_APP_NAME = "micrhema_member_session"

    fun get(): FirebaseAuth {
        val defaultApp = FirebaseApp.getInstance()
        val memberApp = FirebaseApp.getApps(defaultApp.applicationContext)
            .firstOrNull { it.name == MEMBER_APP_NAME }
            ?: FirebaseApp.initializeApp(
                defaultApp.applicationContext,
                defaultApp.options,
                MEMBER_APP_NAME
            )
            ?: throw IllegalStateException("Não foi possível preparar a sessão segura do membro.")
        return FirebaseAuth.getInstance(memberApp)
    }

    /** Compatibility with APKs that signed in members on the default app.
     * Never return a different member's or the administrator's token.
     * A new sign-in always uses the dedicated member app instead.
     */
    fun forMember(memberId: String): FirebaseAuth? {
        if (memberId.isBlank()) return null
        val isolated = get()
        if (isolated.currentUser?.uid == memberId) return isolated
        val legacy = FirebaseAuth.getInstance()
        return legacy.takeIf { it.currentUser?.uid == memberId }
    }
}
