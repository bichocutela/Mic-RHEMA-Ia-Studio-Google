from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Padrão não encontrado em {path}: {old[:100]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str) -> None:
    file = ROOT / path
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(content, encoding="utf-8")


# ---------------------------------------------------------------------------
# Android: celebração em tela cheia + deep-link para o emblema recém liberado
# ---------------------------------------------------------------------------
write(
    "app/src/main/java/com/aistudio/micrhema/BadgeUnlockCelebration.kt",
    r'''package com.aistudio.micrhema

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Celebração global exibida quando um nível/emblema é conquistado. */
@Composable
fun BadgeUnlockCelebration(
    notification: BadgeAwardNotification,
    avatar: BiblicalAvatar,
    onOpenBadge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val badge = notification.badges.maxByOrNull { it.level ?: 0 } ?: return
    val haptic = LocalHapticFeedback.current
    val scale = remember(badge.id) { Animatable(0.62f) }

    LaunchedEffect(badge.id) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF51A2430),
                            Color(0xF52E271C),
                            Color(0xFA111820)
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfettiBurst(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 14.dp,
                shadowElevation = 22.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (badge.level != null) "NOVO NÍVEL DESBLOQUEADO!" else "NOVO EMBLEMA!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Parabéns!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (notification.badges.size > 1)
                            "Você conquistou ${notification.badges.size} novos emblemas de uma vez."
                        else "Sua constância fez você avançar na jornada MIC Rhema.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    BiblicalAvatarWithBadge(
                        avatar = avatar,
                        badge = badge,
                        modifier = Modifier.size(218.dp).scale(scale.value),
                        contentDescription = "Emblema ${badge.name} conquistado"
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (badge.level != null) "Nível ${badge.level} · ${badge.name}" else badge.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (notification.badges.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "+ ${notification.badges.size - 1} conquista(s) desbloqueada(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { onOpenBadge(badge.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Ver e usar meu emblema", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Continuar depois")
                    }
                }
            }
        }
    }
}
'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/BadgeActivityTracker.kt",
    'val badgeAwardNotificationState = mutableStateOf<BadgeAwardNotification?>(null)\n',
    'val badgeAwardNotificationState = mutableStateOf<BadgeAwardNotification?>(null)\n/** Emblema que deve ser destacado ao abrir Meu Perfil pela celebração. */\nval badgeUnlockFocusState = mutableStateOf<String?>(null)\n'
)

old_android_dialog = r'''        badgeAwardNotificationState.value?.let { notification ->
            val awardMember = loggedInMemberState.value
            val awardAvatar = biblicalAvatarForId(awardMember?.avatarId ?: DEFAULT_BIBLICAL_AVATAR_ID)
            AlertDialog(
                onDismissRequest = { badgeAwardNotificationState.value = null },
                title = {
                    Text(if (notification.badges.size == 1) "Novo emblema conquistado!" else "Novos emblemas conquistados!")
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        Text(
                            if (notification.badges.size == 1) "Parabéns! Você avançou na sua jornada." else "Parabéns! Você avançou em vários objetivos.",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            notification.badges.forEach { badge ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BiblicalAvatarWithBadge(
                                        avatar = awardAvatar,
                                        badge = badge,
                                        modifier = Modifier.size(94.dp),
                                        contentDescription = badge.name
                                    )
                                    Text(
                                        badge.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Vá ao Meu Perfil para ativar o emblema e exibir sua nova moldura.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ConfettiBurst(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(1f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        badgeAwardNotificationState.value = null
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }) {
                        Text("Ir para Meu Perfil")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { badgeAwardNotificationState.value = null }) { Text("Depois") }
                }
            )
        }
'''
new_android_dialog = r'''        badgeAwardNotificationState.value?.let { notification ->
            val awardMember = loggedInMemberState.value
            val awardAvatar = biblicalAvatarForId(awardMember?.avatarId ?: DEFAULT_BIBLICAL_AVATAR_ID)
            BadgeUnlockCelebration(
                notification = notification,
                avatar = awardAvatar,
                onOpenBadge = { badgeId ->
                    badgeUnlockFocusState.value = badgeId
                    badgeAwardNotificationState.value = null
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                onDismiss = { badgeAwardNotificationState.value = null }
            )
        }
'''
replace_once(
    "app/src/main/java/com/aistudio/micrhema/MainActivity.kt",
    old_android_dialog,
    new_android_dialog
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    'import androidx.compose.ui.draw.clip\n',
    'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.alpha\n'
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    '    var showBadgePicker by remember { mutableStateOf(false) }\n',
    '    var showBadgePicker by remember { mutableStateOf(false) }\n    var focusedBadgeId by remember { mutableStateOf<String?>(null) }\n'
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    '''    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.email, loggedInMember.avatarId, loggedInMember.equippedBadgeId, loggedInMember.unlockedBadgeIds) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = formatBirthDateInput(loggedInMember.birthDate)
        if (!isEditingEmail) email = loggedInMember.email
        selectedAvatarId = loggedInMember.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
        equippedBadgeId = loggedInMember.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID }
    }
''',
    '''    LaunchedEffect(loggedInMember.id, loggedInMember.name, loggedInMember.phone, loggedInMember.address, loggedInMember.birthDate, loggedInMember.email, loggedInMember.avatarId, loggedInMember.equippedBadgeId, loggedInMember.unlockedBadgeIds) {
        if (!isEditingName) name = loggedInMember.name
        if (!isEditingPhone) phone = loggedInMember.phone
        if (!isEditingAddress) address = loggedInMember.address
        if (!isEditingBirthDate) birthDate = formatBirthDateInput(loggedInMember.birthDate)
        if (!isEditingEmail) email = loggedInMember.email
        selectedAvatarId = loggedInMember.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
        equippedBadgeId = loggedInMember.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID }
    }

    LaunchedEffect(badgeUnlockFocusState.value) {
        badgeUnlockFocusState.value?.let { badgeId ->
            focusedBadgeId = badgeId
            showBadgePicker = true
            badgeUnlockFocusState.value = null
        }
    }
'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    '''                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isUnlocked) Modifier.clickable {
                                    equippedBadgeId = badge.id
                                    showBadgePicker = false
                                    saveProfile(''',
    '''                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (badge.id == focusedBadgeId) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(16.dp)
                                    ) else Modifier
                                )
                                .then(if (isUnlocked) Modifier.clickable {
                                    equippedBadgeId = badge.id
                                    focusedBadgeId = null
                                    showBadgePicker = false
                                    saveProfile('''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    '''                                Icon(
                                    if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))''',
    '''                                Box(contentAlignment = Alignment.Center) {
                                    BiblicalAvatarWithBadge(
                                        avatar = selectedAvatar,
                                        badge = badge,
                                        contentDescription = badge.name,
                                        modifier = Modifier.size(64.dp).alpha(if (isUnlocked) 1f else 0.28f)
                                    )
                                    if (!isUnlocked) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Bloqueado",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/ProfileScreen.kt",
    '''                                    Text(
                                        if (isUnlocked) badge.description else "Bloqueado: ${badge.requirement}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
''',
    '''                                    Text(
                                        if (isUnlocked) badge.description else "Bloqueado: ${badge.requirement}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (badge.id == focusedBadgeId && isUnlocked) {
                                        Text(
                                            "Novo emblema desbloqueado · toque para usar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
'''
)

# ---------------------------------------------------------------------------
# PWA: evento global + celebração visual + deep-link para emblemas no perfil
# ---------------------------------------------------------------------------
replace_once(
    "pwa/client/src/lib/badge-activity.ts",
    'export type BadgeActivityKey = "plans" | "plan_themes" | "books" | "videos" | "bible_chapters" | "bible_news" | "devotionals" | "audios" | "active_minutes";\n',
    'export type BadgeActivityKey = "plans" | "plan_themes" | "books" | "videos" | "bible_chapters" | "bible_news" | "devotionals" | "audios" | "active_minutes";\n\nexport const PWA_BADGE_UNLOCK_EVENT = "micrhema:pwa:badge-unlocked";\n'
)
replace_once(
    "pwa/client/src/lib/badge-activity.ts",
    '''  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível sincronizar a atividade agora.");
  return payload as { ok: true; unlockedBadgeIds: string[]; newlyUnlocked: string[] };
}''',
    '''  if (!response.ok || payload.ok !== true) throw new Error(payload.error || "Não foi possível sincronizar a atividade agora.");
  const result = payload as { ok: true; unlockedBadgeIds: string[]; newlyUnlocked: string[] };
  if (result.newlyUnlocked.length && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(PWA_BADGE_UNLOCK_EVENT, { detail: { badgeIds: result.newlyUnlocked } }));
  }
  return result;
}'''
)

write(
    "pwa/client/src/components/BadgeUnlockCelebration.tsx",
    r'''import { useEffect, useMemo, useState } from "react";
import { Sparkles, Trophy, X } from "lucide-react";
import { loadPwaMemberProfile } from "@/lib/firebase";
import { PWA_BADGE_UNLOCK_EVENT } from "@/lib/badge-activity";
import { BiblicalBadgeAvatar } from "./BiblicalBadgeAvatar";
import "./BadgeUnlockCelebration.css";

type BadgeInfo = { id:string; name:string; description:string; level?:number };

const badges:Record<string,BadgeInfo> = {
  caminhante:{id:"caminhante",name:"Caminhante",description:"O início de uma jornada de fé e conhecimento.",level:1},
  semeador:{id:"semeador",name:"Semeador",description:"Quem planta a Palavra no coração todos os dias.",level:2},
  discipulo:{id:"discipulo",name:"Discípulo",description:"Um passo firme no aprendizado da Palavra.",level:3},
  perseverante:{id:"perseverante",name:"Perseverante",description:"Constância para continuar mesmo nos dias difíceis.",level:4},
  estudante_rhema:{id:"estudante_rhema",name:"Estudante Rhema",description:"Dedicação reconhecida ao estudo no Instituto Bíblico Rhema.",level:5},
  mestre_da_palavra:{id:"mestre_da_palavra",name:"Mestre da Palavra",description:"Conhecimento construído com disciplina e compromisso.",level:6},
  guardiao_da_fe:{id:"guardiao_da_fe",name:"Guardião da Fé",description:"Um testemunho de perseverança, serviço e maturidade.",level:7},
  primeira_oracao:{id:"primeira_oracao",name:"Primeira Oração",description:"Um primeiro momento separado para falar com Deus."},
  leitor_da_palavra:{id:"leitor_da_palavra",name:"Leitor da Palavra",description:"A Bíblia aberta e o coração disposto a aprender."},
  coracao_grato:{id:"coracao_grato",name:"Coração Grato",description:"Reconhecimento pelas bênçãos recebidas."},
  constante:{id:"constante",name:"Constante",description:"Pequenos passos repetidos com fidelidade."},
  certificado_ibr:{id:"certificado_ibr",name:"Certificado IBR",description:"Uma conquista acadêmica no Instituto Bíblico Rhema."},
};

const confettiColors=["#ffd54f","#ffb300","#66bb6a","#42a5f5","#ef5350","#ab47bc","#ec407a"];

export function BadgeUnlockCelebration({onOpenBadges}:{onOpenBadges:()=>void}){
  const[queue,setQueue]=useState<string[]>([]);
  const[avatarId,setAvatarId]=useState("davi");
  const currentId=queue[0]||"";
  const badge=badges[currentId];
  const pieces=useMemo(()=>Array.from({length:54},(_,index)=>({
    left:(index*37)%100,
    delay:(index%9)*65,
    duration:1700+(index%7)*120,
    color:confettiColors[index%confettiColors.length],
    rotate:(index*41)%180,
  })),[]);

  useEffect(()=>{
    const handler=(event:Event)=>{
      const detail=(event as CustomEvent<{badgeIds?:string[]}>).detail;
      const ids=(detail?.badgeIds||[]).filter(id=>badges[id]);
      if(!ids.length)return;
      setQueue(current=>[...current,...ids.filter(id=>!current.includes(id))]);
      void loadPwaMemberProfile().then(profile=>setAvatarId(profile.avatarId||"davi")).catch(()=>undefined);
      if("vibrate" in navigator) navigator.vibrate?.([90,45,130]);
    };
    window.addEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
    return()=>window.removeEventListener(PWA_BADGE_UNLOCK_EVENT,handler as EventListener);
  },[]);

  if(!badge)return null;
  const close=()=>setQueue(current=>current.slice(1));
  const open=()=>{
    localStorage.setItem("micrhema:pwa:open-badge",badge.id);
    setQueue([]);
    onOpenBadges();
  };

  return <div className="badge-unlock-overlay" role="dialog" aria-modal="true" aria-label={`Emblema ${badge.name} desbloqueado`}>
    <div className="badge-unlock-confetti" aria-hidden="true">{pieces.map((piece,index)=><i key={index} style={{left:`${piece.left}%`,animationDelay:`${piece.delay}ms`,animationDuration:`${piece.duration}ms`,backgroundColor:piece.color,transform:`rotate(${piece.rotate}deg)`}}/>)}</div>
    <button className="badge-unlock-close" aria-label="Continuar depois" onClick={close}><X size={22}/></button>
    <section className="badge-unlock-card">
      <div className="badge-unlock-kicker"><Sparkles size={18}/>{badge.level?"NOVO NÍVEL DESBLOQUEADO!":"NOVO EMBLEMA DESBLOQUEADO!"}</div>
      <h2>Parabéns!</h2>
      <p className="badge-unlock-lead">Sua constância fez você avançar na jornada MIC Rhema.</p>
      <div className="badge-unlock-medal"><BiblicalBadgeAvatar avatarId={avatarId} badgeId={badge.id} size={218} title={badge.name}/></div>
      <div className="badge-unlock-title"><Trophy size={22}/><strong>{badge.level?`Nível ${badge.level} · ${badge.name}`:badge.name}</strong></div>
      <p>{badge.description}</p>
      {queue.length>1&&<small>Você ainda conquistou mais {queue.length-1} emblema(s) agora.</small>}
      <button className="badge-unlock-primary" onClick={open}><Trophy size={19}/>Ver e usar meu emblema</button>
      <button className="badge-unlock-later" onClick={close}>Continuar depois</button>
    </section>
  </div>;
}
'''
)

write(
    "pwa/client/src/components/BadgeUnlockCelebration.css",
    r'''.badge-unlock-overlay{position:fixed;inset:0;z-index:3200;display:grid;place-items:center;padding:max(24px,env(safe-area-inset-top)) 18px max(28px,env(safe-area-inset-bottom));overflow:hidden;background:radial-gradient(circle at 50% 28%,rgba(219,174,61,.24),transparent 34%),linear-gradient(180deg,rgba(12,25,38,.96),rgba(42,34,23,.97) 56%,rgba(10,19,28,.98));backdrop-filter:blur(8px)}.badge-unlock-card{position:relative;z-index:2;width:min(94vw,430px);display:grid;justify-items:center;gap:10px;padding:28px 22px 22px;border:1px solid rgba(255,216,116,.34);border-radius:30px;background:color-mix(in srgb,var(--card,#fffdf7) 95%,transparent);box-shadow:0 28px 80px rgba(0,0,0,.46),0 0 54px rgba(232,186,67,.13);text-align:center;animation:badgeCardIn .52s cubic-bezier(.18,.9,.26,1.18)}.badge-unlock-kicker{display:flex;align-items:center;gap:7px;color:var(--pwa-primary,#8a6500);font-size:.75rem;font-weight:900;letter-spacing:.11em}.badge-unlock-card h2{margin:0;font-size:2rem;line-height:1.05}.badge-unlock-card p{margin:0;color:var(--muted-foreground,#667085);line-height:1.5}.badge-unlock-lead{max-width:320px}.badge-unlock-medal{display:grid;place-items:center;margin:6px 0 3px;filter:drop-shadow(0 14px 20px rgba(0,0,0,.22));animation:badgeMedalPop .72s cubic-bezier(.14,.9,.22,1.25)}.badge-unlock-title{display:flex;align-items:center;justify-content:center;gap:8px;color:var(--foreground,#171717);font-size:1.13rem}.badge-unlock-title svg{color:var(--pwa-primary,#8a6500)}.badge-unlock-card small{color:var(--pwa-primary,#8a6500);font-weight:800}.badge-unlock-primary{width:100%;min-height:50px;margin-top:8px;display:flex;align-items:center;justify-content:center;gap:8px;border:0;border-radius:16px;background:var(--pwa-primary,#8a6500);color:#fff;font-weight:900;font-size:.98rem;box-shadow:0 10px 28px color-mix(in srgb,var(--pwa-primary,#8a6500) 35%,transparent)}.badge-unlock-later{border:0;background:transparent;color:var(--muted-foreground,#667085);font-weight:700;padding:8px 12px}.badge-unlock-close{position:fixed;z-index:4;top:max(18px,env(safe-area-inset-top));right:18px;width:42px;height:42px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.24);border-radius:50%;background:rgba(0,0,0,.24);color:#fff}.badge-unlock-confetti{position:absolute;inset:0;z-index:1;pointer-events:none;overflow:hidden}.badge-unlock-confetti i{position:absolute;top:-9%;width:9px;height:17px;border-radius:2px;opacity:0;animation-name:badgeConfettiFall;animation-timing-function:cubic-bezier(.22,.7,.28,1);animation-fill-mode:forwards}.celebration-focus{border-color:var(--pwa-primary,#8a6500)!important;box-shadow:0 0 0 2px color-mix(in srgb,var(--pwa-primary,#8a6500) 25%,transparent),0 12px 28px rgba(0,0,0,.08)!important;animation:badgeFocusPulse 1.45s ease-in-out 2}@keyframes badgeCardIn{from{opacity:0;transform:translateY(30px) scale(.9)}to{opacity:1;transform:translateY(0) scale(1)}}@keyframes badgeMedalPop{0%{transform:scale(.56) rotate(-8deg);opacity:.2}70%{transform:scale(1.08) rotate(2deg);opacity:1}100%{transform:scale(1) rotate(0)}}@keyframes badgeConfettiFall{0%{opacity:0;transform:translate3d(0,-8vh,0) rotate(0)}10%{opacity:1}100%{opacity:.95;transform:translate3d(24px,112vh,0) rotate(760deg)}}@keyframes badgeFocusPulse{0%,100%{transform:scale(1)}50%{transform:scale(1.015)}}@media(max-width:430px){.badge-unlock-card{padding:24px 18px 18px}.badge-unlock-card h2{font-size:1.75rem}.badge-unlock-medal .biblical-badge-avatar{max-width:190px;max-height:190px}}
'''
)

replace_once(
    "pwa/client/src/components/PwaShell.tsx",
    'import { DrawerBadgesParity } from "./DrawerBadgesParity";\n',
    'import { DrawerBadgesParity } from "./DrawerBadgesParity";\nimport { BadgeUnlockCelebration } from "./BadgeUnlockCelebration";\n'
)
replace_once(
    "pwa/client/src/components/PwaShell.tsx",
    '''    {drawerOpen&&<AndroidDrawer active={active} onNavigate={onNavigate} onProfile={onProfile} onClose={onCloseDrawer} session={session} onNotifications={onNotifications}/>} 
    {overlays}
''',
    '''    {drawerOpen&&<AndroidDrawer active={active} onNavigate={onNavigate} onProfile={onProfile} onClose={onCloseDrawer} session={session} onNotifications={onNotifications}/>} 
    <BadgeUnlockCelebration onOpenBadges={()=>{onCloseDrawer();onNavigate("profile")}}/>
    {overlays}
'''
)

replace_once(
    "pwa/client/src/components/ProfileParityViewV2.tsx",
    '  const [loading,setLoading]=useState(true); const [loadError,setLoadError]=useState(""); const [saving,setSaving]=useState(false); const [avatarsOpen,setAvatarsOpen]=useState(false); const [badgesOpen,setBadgesOpen]=useState(false); const [missionsOpen,setMissionsOpen]=useState(true);\n',
    '  const [loading,setLoading]=useState(true); const [loadError,setLoadError]=useState(""); const [saving,setSaving]=useState(false); const [avatarsOpen,setAvatarsOpen]=useState(false); const [badgesOpen,setBadgesOpen]=useState(false); const [missionsOpen,setMissionsOpen]=useState(true); const [focusedBadgeId,setFocusedBadgeId]=useState<string|null>(null);\n'
)
replace_once(
    "pwa/client/src/components/ProfileParityViewV2.tsx",
    '  useEffect(()=>profile?.id?listenToIbrProgress<IbrProgress>(profile.id,setIbrProgress,()=>setIbrProgress([])):()=>undefined,[profile?.id]);\n',
    '''  useEffect(()=>profile?.id?listenToIbrProgress<IbrProgress>(profile.id,setIbrProgress,()=>setIbrProgress([])):()=>undefined,[profile?.id]);
  useEffect(()=>{
    if(!profile?.id)return;
    const pending=localStorage.getItem("micrhema:pwa:open-badge");
    if(!pending)return;
    setFocusedBadgeId(pending);setBadgesOpen(true);setMissionsOpen(false);
    localStorage.removeItem("micrhema:pwa:open-badge");
    window.setTimeout(()=>document.getElementById(`profile-badge-${pending}`)?.scrollIntoView({behavior:"smooth",block:"center"}),180);
  },[profile?.id]);
'''
)
replace_once(
    "pwa/client/src/components/ProfileParityViewV2.tsx",
    '  const chooseBadge=(id:string)=>{if(!summary.calculated.has(id))return;const next={...draft,equippedBadgeId:id};setDraft(next);setBadgesOpen(false);void persist(next,"Emblema equipado na sua conta.")};\n',
    '  const chooseBadge=(id:string)=>{if(!summary.calculated.has(id))return;const next={...draft,equippedBadgeId:id};setDraft(next);setFocusedBadgeId(null);setBadgesOpen(false);void persist(next,"Emblema equipado na sua conta.")};\n'
)
replace_once(
    "pwa/client/src/components/ProfileParityViewV2.tsx",
    '''{badgesOpen&&<div className="profile-v2-badges">{badges.map((badge)=>{const unlocked=summary.calculated.has(badge.id);const selected=draft.equippedBadgeId===badge.id;return <button key={badge.id} disabled={!unlocked||saving} className={selected?"selected":""} onClick={()=>chooseBadge(badge.id)}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={64} locked={!unlocked} title={badge.name}/><span><strong>{badge.level?`Nível ${badge.level} · `:""}{badge.name}</strong><small>{unlocked?badge.description:`Bloqueado — ${badge.requirement}`}</small><em>{selected?"Emblema equipado":unlocked?"Conquistado · toque para equipar":"Ainda não conquistado"}</em></span></button>})}</div>}''',
    '''{badgesOpen&&<div className="profile-v2-badges">{badges.map((badge)=>{const unlocked=summary.calculated.has(badge.id);const selected=draft.equippedBadgeId===badge.id;const focused=focusedBadgeId===badge.id;return <button id={`profile-badge-${badge.id}`} key={badge.id} disabled={!unlocked||saving} className={`${selected?"selected":""}${focused?" celebration-focus":""}`} onClick={()=>chooseBadge(badge.id)}><BiblicalBadgeAvatar avatarId={avatar.id} badgeId={badge.id} size={64} locked={!unlocked} title={badge.name}/><span><strong>{badge.level?`Nível ${badge.level} · `:""}{badge.name}</strong><small>{unlocked?badge.description:`Bloqueado — ${badge.requirement}`}</small><em>{selected?"Emblema equipado":focused&&unlocked?"Novo emblema desbloqueado · toque para usar":unlocked?"Conquistado · toque para equipar":"Ainda não conquistado"}</em></span></button>})}</div>}'''
)

print("Celebração de emblemas aplicada com sucesso ao Android e à PWA.")
