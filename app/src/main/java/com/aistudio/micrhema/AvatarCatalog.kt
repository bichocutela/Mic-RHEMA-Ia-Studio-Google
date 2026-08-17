package com.aistudio.micrhema

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Catálogo local de avatares bíblicos disponíveis para os membros. */
data class BiblicalAvatar(
    val id: String,
    val displayName: String,
    @DrawableRes val resourceId: Int
)

val biblicalAvatarCatalog: List<BiblicalAvatar> = listOf(
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

fun biblicalAvatarForId(id: String): BiblicalAvatar =
    biblicalAvatarCatalog.firstOrNull { it.id == id } ?: biblicalAvatarCatalog.first()

@Composable
fun BiblicalAvatarImage(
    avatar: BiblicalAvatar,
    modifier: Modifier = Modifier,
    contentDescription: String? = avatar.displayName
) {
    Image(
        painter = painterResource(id = avatar.resourceId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
