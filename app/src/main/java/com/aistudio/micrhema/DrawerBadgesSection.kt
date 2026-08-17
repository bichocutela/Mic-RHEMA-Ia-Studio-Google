package com.aistudio.micrhema

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DrawerBadgesSection(member: MemberRequest?) {
    var selectedBadge by remember { mutableStateOf<BiblicalBadge?>(null) }
    val unlockedIds = member?.let { calculateBadgeProgress(it).unlockedIds.orEmpty().toSet() }.orEmpty()
    val avatar = biblicalAvatarForId(member?.avatarId ?: DEFAULT_BIBLICAL_AVATAR_ID)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Emblemas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Veja os próximos níveis e descubra como atingir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allBiblicalBadges, key = { it.id }) { badge ->
                val unlocked = badge.id in unlockedIds
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { selectedBadge = badge },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BiblicalAvatarWithBadge(
                            avatar = avatar,
                            badge = badge,
                            modifier = Modifier.size(64.dp).alpha(if (unlocked) 1f else 0.28f),
                            contentDescription = badge.name
                        )
                        if (!unlocked) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        val unlocked = badge.id in unlockedIds
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            title = {
                Text(if (badge.level != null) "Nível ${badge.level}: ${badge.name}" else badge.name)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BiblicalAvatarWithBadge(
                        avatar = avatar,
                        badge = badge,
                        modifier = Modifier.size(190.dp).alpha(if (unlocked) 1f else 0.35f),
                        contentDescription = badge.name
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (unlocked) "Emblema conquistado" else "Ainda bloqueado",
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (unlocked) badge.description else "Para atingir: ${badge.requirement}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) { Text("Fechar") }
            }
        )
    }
}
