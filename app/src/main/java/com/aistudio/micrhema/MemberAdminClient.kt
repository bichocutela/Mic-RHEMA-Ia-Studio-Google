package com.aistudio.micrhema

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Alterações administrativas de identidade e perfil do membro.
 *
 * O memberId nunca muda ao trocar o telefone. Com isso XP, quiz, Jornada,
 * Loja XP, favoritos, IBR e subcoleções continuam ligados à mesma conta.
 * Apenas o número usado para localizar/recuperar essa conta é transferido.
 */
object MemberAdminClient {
    private fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length in 12..13 && digits.startsWith("55")) digits.drop(2) else digits
    }

    private suspend fun applyMemberPatch(
        original: MemberRequest,
        patch: Map<String, Any>,
        updated: MemberRequest
    ): MemberRequest {
        val db = Firebase.firestore
        db.runBatch { batch ->
            batch.set(
                db.collection("acessos_pendentes").document(original.id),
                patch,
                SetOptions.merge()
            )
            batch.set(
                db.collection("users").document(original.id),
                patch,
                SetOptions.merge()
            )
        }.await()

        withContext(Dispatchers.Main.immediate) {
            val index = memberRequestsState.indexOfFirst { it.id == original.id }
            if (index >= 0) memberRequestsState[index] = updated
            if (loggedInMemberState.value?.id == original.id) {
                loggedInMemberState.value = updated.copy()
            }
        }
        return updated
    }

    suspend fun updateMemberData(
        original: MemberRequest,
        name: String,
        phone: String,
        email: String,
        address: String,
        birthDate: String,
        certificateName: String
    ): MemberRequest = withContext(Dispatchers.IO) {
        val cleanPhone = normalizePhone(phone)
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanAddress = address.trim()
        val cleanBirthDate = birthDate.trim()
        val cleanCertificateName = certificateName.trim()

        require(cleanName.isNotBlank()) { "O nome do membro não pode ficar vazio." }
        require(cleanPhone.length in 10..11) { "Informe um telefone válido com DDD." }
        if (cleanEmail.isNotBlank()) {
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                "Informe um e-mail válido."
            }
        }

        val db = Firebase.firestore
        val matches = linkedMapOf<String, MemberRequest>()
        for (variant in linkedSetOf(cleanPhone, "55$cleanPhone")) {
            val snapshot = db.collection("acessos_pendentes")
                .whereEqualTo("phone", variant)
                .get(Source.SERVER)
                .await()
            snapshot.documents.forEach { document ->
                if (document.id != original.id) {
                    val storedPhone = normalizePhone(document.getString("phone").orEmpty())
                    if (storedPhone == cleanPhone) {
                        matches[document.id] = original.copy(id = document.id, phone = storedPhone)
                    }
                }
            }
        }
        if (matches.isNotEmpty()) {
            throw IllegalStateException("Este telefone já pertence a outro cadastro. Escolha outro número antes de salvar.")
        }

        val now = System.currentTimeMillis()
        val updated = original.copy(
            name = cleanName,
            phone = cleanPhone,
            email = cleanEmail,
            address = cleanAddress,
            birthDate = cleanBirthDate,
            ibrCertificateName = cleanCertificateName.ifBlank { cleanName },
            updatedAt = now
        )

        val patch = hashMapOf<String, Any>(
            "name" to updated.name,
            "phone" to updated.phone,
            "email" to updated.email,
            "address" to updated.address,
            "birthDate" to updated.birthDate,
            "ibrCertificateName" to updated.ibrCertificateName,
            "updatedAt" to now
        )
        if (normalizePhone(original.phone) != cleanPhone) {
            patch["previousPhone"] = normalizePhone(original.phone)
            patch["phoneChangedByAdminAt"] = now
        }

        applyMemberPatch(original, patch, updated)
    }

    /**
     * Atualiza somente o vínculo da foto. Não regrava permissões, badges, XP ou
     * outros dados do membro, reduzindo o risco de uma falha de sincronização.
     */
    suspend fun updateMemberPhoto(
        original: MemberRequest,
        signedUrl: String,
        storagePath: String
    ): MemberRequest = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = original.copy(
            profilePhotoUrl = signedUrl.trim(),
            supabaseStoragePath = storagePath.trim(),
            updatedAt = now
        )
        val patch = mapOf<String, Any>(
            // O caminho é a referência persistente. A URL assinada é mantida no
            // estado local para aparecer imediatamente e é renovada nos próximos syncs.
            "profilePhotoUrl" to "",
            "supabaseStoragePath" to updated.supabaseStoragePath,
            "updatedAt" to now
        )
        applyMemberPatch(original, patch, updated)
    }
}
