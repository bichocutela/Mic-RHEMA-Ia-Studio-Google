package com.aistudio.micrhema

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/** Catálogo local de avatares bíblicos disponíveis para os membros. */
data class BiblicalAvatar(
    val id: String,
    val displayName: String,
    @DrawableRes val resourceId: Int
)

private val baseBiblicalAvatarCatalog: List<BiblicalAvatar> = listOf(
    BiblicalAvatar("davi", "Davi", R.drawable.avatar_davi),
    BiblicalAvatar("ester", "Ester", R.drawable.avatar_ester),
    BiblicalAvatar("daniel", "Daniel", R.drawable.avatar_daniel),
    BiblicalAvatar("rute", "Rute", R.drawable.avatar_rute),
    BiblicalAvatar("moises", "Moisés", R.drawable.avatar_moises),
    BiblicalAvatar("noe", "Noé", R.drawable.avatar_noe),
    BiblicalAvatar("maria", "Maria", R.drawable.avatar_maria),
    BiblicalAvatar("paulo", "Paulo", R.drawable.avatar_paulo),
    BiblicalAvatar("josue", "Josué", R.drawable.avatar_josue),
    BiblicalAvatar("abraao", "Abraão", R.drawable.avatar_abraao),
    BiblicalAvatar("sara", "Sara", R.drawable.avatar_sara),
    BiblicalAvatar("rebeca", "Rebeca", R.drawable.avatar_rebeca),
    BiblicalAvatar("jaco", "Jacó", R.drawable.avatar_jaco),
    BiblicalAvatar("jose", "José", R.drawable.avatar_jose),
    BiblicalAvatar("samuel", "Samuel", R.drawable.avatar_samuel),
    BiblicalAvatar("elias", "Elias", R.drawable.avatar_elias),
    BiblicalAvatar("isaias", "Isaías", R.drawable.avatar_isaias),
    BiblicalAvatar("jeremias", "Jeremias", R.drawable.avatar_jeremias),
    BiblicalAvatar("joao_batista", "João Batista", R.drawable.avatar_joao_batista),
    BiblicalAvatar("timoteo", "Timóteo", R.drawable.avatar_timoteo),
    BiblicalAvatar("priscila", "Priscila", R.drawable.avatar_priscila),
    BiblicalAvatar("lidia", "Lídia", R.drawable.avatar_lidia)
)

const val DEFAULT_BIBLICAL_AVATAR_ID = "davi"
private const val PROFILE_PHOTO_AVATAR_PREFIX = "profile_photo::"
private const val PROFILE_PHOTO_SEPARATOR = "::"

private fun hasMemberProfilePhoto(member: MemberRequest?): Boolean =
    member != null && (member.supabaseStoragePath.isNotBlank() || member.profilePhotoUrl.isNotBlank())

fun isProfilePhotoAvatarId(id: String): Boolean = id.startsWith(PROFILE_PHOTO_AVATAR_PREFIX)

/**
 * Recupera o avatar bíblico que estava selecionado antes de o membro escolher
 * "Minha foto". Isso permite voltar para o avatar anterior se a foto for removida.
 */
fun biblicalAvatarIdBeforeProfilePhoto(id: String): String {
    if (!isProfilePhotoAvatarId(id)) {
        return baseBiblicalAvatarCatalog.firstOrNull { it.id == id }?.id ?: DEFAULT_BIBLICAL_AVATAR_ID
    }
    val previous = id.substringAfterLast(PROFILE_PHOTO_SEPARATOR, DEFAULT_BIBLICAL_AVATAR_ID)
    return baseBiblicalAvatarCatalog.firstOrNull { it.id == previous }?.id ?: DEFAULT_BIBLICAL_AVATAR_ID
}

private fun profilePhotoMemberId(id: String): String {
    if (!isProfilePhotoAvatarId(id)) return ""
    val body = id.removePrefix(PROFILE_PHOTO_AVATAR_PREFIX)
    return body.substringBefore(PROFILE_PHOTO_SEPARATOR).trim()
}

private fun profilePhotoAvatarFor(member: MemberRequest): BiblicalAvatar {
    val previousAvatarId = biblicalAvatarIdBeforeProfilePhoto(member.avatarId)
    return BiblicalAvatar(
        id = "$PROFILE_PHOTO_AVATAR_PREFIX${member.id}$PROFILE_PHOTO_SEPARATOR$previousAvatarId",
        displayName = "Minha foto",
        resourceId = baseBiblicalAvatarCatalog.firstOrNull { it.id == previousAvatarId }?.resourceId
            ?: baseBiblicalAvatarCatalog.first().resourceId
    )
}

/**
 * A foto do cadastro aparece como uma opção especial somente para o membro que
 * realmente possui uma foto salva. Como a seleção continua sendo gravada em
 * avatarId, ela usa a mesma sincronização de conta já existente entre aparelhos.
 */
val biblicalAvatarCatalog: List<BiblicalAvatar>
    get() {
        val member = loggedInMemberState.value
        return if (hasMemberProfilePhoto(member)) {
            listOf(profilePhotoAvatarFor(member!!)) + baseBiblicalAvatarCatalog
        } else {
            baseBiblicalAvatarCatalog
        }
    }

fun biblicalAvatarForId(id: String): BiblicalAvatar {
    if (isProfilePhotoAvatarId(id)) {
        val previousId = biblicalAvatarIdBeforeProfilePhoto(id)
        return BiblicalAvatar(
            id = id,
            displayName = "Minha foto",
            resourceId = baseBiblicalAvatarCatalog.firstOrNull { it.id == previousId }?.resourceId
                ?: baseBiblicalAvatarCatalog.first().resourceId
        )
    }
    return baseBiblicalAvatarCatalog.firstOrNull { it.id == id } ?: baseBiblicalAvatarCatalog.first()
}

@Composable
fun BiblicalAvatarImage(
    avatar: BiblicalAvatar,
    modifier: Modifier = Modifier,
    contentDescription: String? = avatar.displayName
) {
    if (isProfilePhotoAvatarId(avatar.id)) {
        val memberId = profilePhotoMemberId(avatar.id)
        val member = memberRequestsState.firstOrNull { it.id == memberId }
            ?: loggedInMemberState.value?.takeIf { it.id == memberId }
        val context = LocalContext.current
        val localPhoto = member?.let { StorageManager.getLocalProfilePhotoUri(context, it.id) }.orEmpty()
        // Prioriza o cache persistente do próprio aparelho. A URL remota continua
        // como fallback para primeiro acesso, reinstalação ou aparelho novo.
        val model = localPhoto.ifBlank { member?.profilePhotoUrl.orEmpty() }
        val fallback = painterResource(id = avatar.resourceId)

        if (model.isNotBlank()) {
            coil.compose.AsyncImage(
                model = model,
                contentDescription = contentDescription ?: "Foto do perfil",
                modifier = modifier,
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                error = fallback,
                fallback = fallback
            )
        } else {
            Image(
                painter = fallback,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        return
    }

    Image(
        painter = painterResource(id = avatar.resourceId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
