# Notificações automáticas via Supabase e Firebase FCM

Esta integração mantém o Firebase Cloud Messaging no aplicativo e usa uma Edge Function do Supabase apenas para disparar as mensagens. Não há botão no aplicativo, Firebase CLI no GitHub Actions ou deploy automático de Firestore.

## Secrets da Edge Function

Configure somente estes Secrets no projeto Supabase:

```text
FIREBASE_SERVICE_ACCOUNT_JSON={conteúdo completo do JSON da Service Account do Firebase}
FIREBASE_PROJECT_ID=mic-rhema
```

O JSON deve ser obtido em **Firebase Console → Configurações do projeto → Contas de serviço → Firebase Admin SDK → Gerar nova chave privada**. Nunca coloque esse arquivo no repositório, no APK ou no Storage.

A API usada pela função é o **FCM HTTP v1**. A Service Account precisa ter permissão para criar mensagens FCM. A função foi configurada em `supabase/config.toml` como `notify-fcm`.

## Publicação da função

Depois de configurar os Secrets, publique a função `notify-fcm` no projeto Supabase usando o fluxo de Edge Functions já adotado para o projeto. A URL final será:

```text
https://SEU_PROJETO.supabase.co/functions/v1/notify-fcm
```

Os Secrets `SUPABASE_URL` e `SUPABASE_ANON_KEY` já usados pelo aplicativo e pelo workflow continuam sendo os mesmos.

## Disparos automáticos

Quando o ADM salvar um livro, áudio, vídeo, curso IBR, devocional ou horário de culto no Firestore, o aplicativo dispara a Edge Function após o salvamento bem-sucedido. O envio ocorre para estes tópicos:

| Conteúdo | Tópico | Mensagem |
|---|---|---|
| Livro | `all_users` | `Foi adicionado o livro` + título |
| Vídeo | `all_users` | `Foi adicionado o vídeo` + título |
| Áudio | `all_users` | `Foi adicionado um áudio em mídia` + título |
| Curso IBR | `ibr_users` | `Novo conteúdo IBR disponível` + título |
| Devocional | `all_users` | `Novo devocional disponível` + título |
| Culto/horário | `all_users` | `Hoje tem [título]` |

Quando o GitHub Actions criar um novo GitHub Release, ele chamará a mesma Edge Function e enviará `Tem atualização nova!` para `all_users`. Essa etapa é não bloqueante: se a função estiver temporariamente indisponível, o APK e o Release continuarão sendo criados.

## Observação operacional

A primeira publicação da Edge Function e a configuração dos Secrets são externas ao repositório. Após essa configuração, os disparos serão automáticos e não haverá botão adicional para os administradores.
