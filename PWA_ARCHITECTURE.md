# Arquitetura da PWA completa MIC Rhema

## Hospedagem e instalação

A interface será um build estático React/Vite publicado pelo GitHub Pages. O navegador recebe somente código público, configuração web pública do Firebase e URLs públicas de mídia. Manifest, service worker e ícones tornam a aplicação instalável pelo Safari no iPhone.

## Conteúdo e sincronização

| Necessidade | Caminho na PWA |
|---|---|
| Conteúdo público | Firebase Web SDK com listeners de Firestore nas coleções públicas já usadas pelo Android. |
| PDFs, capas e mídia | URLs públicas ou URLs assinadas emitidas pela função `storage-gateway` do Supabase. |
| Solicitação de acesso | Criação pública limitada de documento pendente em `acessos_pendentes`, conforme as regras Firestore atuais. |
| Perfil, favoritos e progresso IBR | Firebase Authentication + regras Firestore de proprietário em `users/{uid}`. |
| Administração | Sessão Firebase com claim `isAdmin`, emitida apenas por função Edge, para que as regras Firestore continuem protegendo toda escrita. |

## Autenticação web compatível com a rotina atual

O navegador não pode conter senha administrativa, service account nem chave de serviço do Supabase. Uma função Edge nova, `pwa-auth`, fará a autenticação simples já usada pela igreja:

1. O membro informa nome e telefone.
2. A função consulta o pedido/aprovação correspondente em `acessos_pendentes` usando a credencial privada já armazenada como secret no Supabase.
3. Para membro aprovado, ela emite um Custom Token Firebase cujo `uid` é o identificador do membro.
4. A PWA troca esse token por uma sessão Firebase normal e passa a respeitar as mesmas regras de perfil, favoritos, IBR e uploads.
5. Para o administrador, o fluxo mantém o login `admin` e senha `igreja10`, mas a senha só é validada dentro da função Edge; a sessão retornada contém a claim `isAdmin=true`.

## Uploads e administração

A PWA reutiliza `storage-gateway`: depois de autenticada, envia o token Firebase na operação de upload. A função já verifica token e papel administrativo antes de gravar no Supabase Storage. Alterações de conteúdo e de membros são feitas pelo Firestore e continuam limitadas pelas regras existentes.

## Notificações web no iPhone

Depois de instalada na Tela de Início, a PWA exibirá uma ação explícita de ativar notificações. O service worker registra a permissão somente após o toque do usuário. Para entrega real por FCM no Safari/iPhone será necessário cadastrar a chave pública VAPID da Mensageria Web do Firebase como configuração pública de build; nenhuma credencial privada é usada no navegador.

## Limites deliberados

- A PWA não inclui service account, private key, `SUPABASE_SERVICE_ROLE_KEY` ou senha administrativa no JavaScript publicado.
- O GitHub Pages hospeda somente arquivos estáticos; lógica autenticada e upload passam pelas funções Edge já existentes no Supabase.
- A publicação da PWA não altera o APK Android, a keystore, o versionamento nem as notificações Android.
