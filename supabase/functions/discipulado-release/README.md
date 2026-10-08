# Publicação de Discipulado

O painel Android/PWA grava futuros estudos em `discipulado_schedules`, acessível somente ao administrador. Cada estudo pode ser publicado imediatamente, ocultado sem apagar, editado ou excluído no painel; editar preserva a visibilidade existente. `scheduledPublishAt` é um instante em milissegundos; os seletores usam America/Fortaleza. A biblioteca pública continua lendo apenas `discipulado_pdfs`.

A função move cada estudo vencido para a biblioteca em um commit atômico com precondições, depois envia título e aviso pelo gateway `notify-fcm`. A fila de notificação tem lease e retoma falhas. Publicações imediatas acordam a mesma função com o ID token Firebase do administrador. O processamento também funciona com o aparelho fechado.

## Autorização

`verify_jwt=false` é intencional: a função valida o ID token Firebase via Identity Toolkit para `release`, ou a chave privada do cron via SHA-256. Importação, inspeção e validação de FCM são restritas à chave do cron. Nenhuma credencial Firebase privada sai do servidor.

## Cron nativo

O job `micrhema-discipulado-daily-release` roda a cada minuto (verificado em produção em 08/10/2026) e processa os horários cadastrados, inclusive após outubro de 2026. A chave está no Vault como `micrhema_discipulado_scheduler_key`; o valor não pertence ao código nem ao cliente.

```sql
select cron.schedule('micrhema-discipulado-daily-release', '* * * * *', $job$
  select net.http_post(
    url := 'https://cwphbkdtorfpgmnlafqb.supabase.co/functions/v1/discipulado-release',
    headers := jsonb_build_object('Content-Type', 'application/json',
      'x-scheduler-key', (select decrypted_secret from vault.decrypted_secrets
        where name = 'micrhema_discipulado_scheduler_key')),
    body := '{"action":"release"}'::jsonb,
    timeout_milliseconds := 30000);
$job$);
```

`studies.ts` registra a importação inicial dos 19 materiais restantes da pasta do Drive, sem Fé e Atos. A ação `import` é idempotente pelo arquivo de origem. O primeiro está programado para 08/10/2026 às 12h e o último para 26/10/2026 às 12h, horário de Fortaleza.

## Validação

```bash
node --test supabase/functions/discipulado-release/tests/publishing.test.mjs
```

Os testes usam relógio e serviços simulados: antecipação, metadados completos, publicação antes do aviso, repetição, concorrência, retomada e autenticação. `validateNotification` usa FCM `validate_only=true`, sem avisar usuários.
