# Paridade PWA × Android — execução segura

A referência funcional continua sendo o Android. As mudanças da PWA são entregues em blocos pequenos e validadas com `pnpm check` e `pnpm build` antes do merge.

## Blocos

- Bloco 1 — Perfil: distintivos, molduras, efeitos de luz e regras de liberação idênticas às do Android.
- Bloco 2 — Loja XP do usuário: organização, prévia e passagem segura para personalização após o resgate.
- Bloco 3 — Loja XP administrativa: recompensas, resgates, emblemas, distintivos, molduras e efeitos de luz.
- Bloco 4 — Integrações web: deep links de notificações e revisão das limitações que são exclusivas do Android.
- Bloco 5 — validação final e publicação.

## Regra de segurança

Nenhum bloco altera Kotlin, Gradle, `applicationId`, keystore, versionamento do APK ou o fluxo nativo de atualização do Android.
