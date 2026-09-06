package com.aistudio.micrhema

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Garante que a conta XP seja carregada para o membro canônico antes de exibir
 * qualquer saldo. Corrige silenciosamente sessões antigas em que o Firebase ainda
 * aponta para um UID anterior a uma consolidação de cadastro.
 */
object XpSessionSynchronizer {
    suspend fun synchronize(context: Context, sourceMember: MemberRequest): MemberRequest {
        val appContext = context.applicationContext
        var member = loggedInMemberState.value?.takeIf { it.id == sourceMember.id } ?: sourceMember

        // Primeiro restaura o último saldo confirmado no aparelho. Assim a tela não
        // volta para 0 XP enquanto aguarda rede/servidor.
        XpAccountCache.restore(appContext, member)

        if (FirebaseAuth.getInstance().currentUser?.uid != member.id) {
            val phone = member.phone.filter(Char::isDigit)
            if (phone.length in 10..13) {
                val recovered = runCatching { MemberSessionClient.recover(appContext, phone) }
                    .getOrNull()
                    ?.member
                if (recovered != null) {
                    member = recovered
                    MemberManager.setLoggedInMember(appContext, recovered)
                    // O telefone é a identidade portátil da conta. Se houve troca de
                    // UID, reaproveitamos imediatamente o mesmo cache confirmado.
                    XpAccountCache.restore(appContext, recovered)
                }
            }
        }

        runCatching { XpEngineClient.flushPendingNow(appContext, member) }
        runCatching { XpEngineClient.refreshNow(member) }
            .getOrNull()
            ?.let { account -> XpAccountCache.save(appContext, member, account) }
        return loggedInMemberState.value?.takeIf { it.id == member.id } ?: member
    }
}
