# Validação da PWA MIC Rhema

## Interface móvel

- A tela inicial foi verificada no preview mobile em 22 de agosto de 2026.
- A identidade visual renderiza corretamente com azul petróleo, marfim, vinho e os ativos armazenados no Supabase.
- A navegação inferior exibe Início, Bíblia, Mídia, IBR e Mais.

## Dados externos observados

- O listener Firebase carregou conteúdos reais da coleção `bible_news`, incluindo os títulos atuais de notícias bíblicas.
- O listener Firebase carregou vídeos reais de `conteudos_videos`, incluindo “Porção Dobrada”, “Nova Vida em Cristo”, “Crente Invisível Também é Corpo” e “ATOS: O Poder de Deus em Nós”.
- O preview usa as URLs públicas dos ativos PWA no Supabase, em vez de URLs da hospedagem Manus.

## Serviços verificados

- A função Supabase `pwa-auth` foi publicada e retornou HTTP 200 no teste administrativo controlado.
- O Custom Token emitido por `pwa-auth` foi aceito pelo Firebase Authentication em teste controlado.
- A versão 13 de `storage-gateway` está ativa, com reconhecimento das sessões administrativas emitidas pela PWA.

## Fluxo interativo de sessão

A interface móvel abriu o diálogo de membro com os campos de nome e telefone, a alternativa para solicitação de acesso e a troca para o formulário administrativo. O login `admin` foi concluído com sucesso no preview controlado: a PWA exibiu “Bem-vindo, Administrador” e atualizou o avatar do cabeçalho para a inicial administrativa. Essa validação confirma o encadeamento entre a interface, `pwa-auth`, Custom Token e Firebase Authentication.

O console do navegador não reportou erro de runtime, Firebase ou autenticação após o login. Os únicos registros referem-se à tentativa manual de acionar o item de navegação “Mais” pelo console; esse comando não alterou o estado da aplicação e não representa erro da PWA.

## Publicação GitHub Pages

O GitHub Pages foi configurado para publicar por GitHub Actions. O workflow de publicação foi validado com sucesso no run `32556187029`, que compilou a tipagem, gerou o artefato estático e publicou em `https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/`.

A primeira navegação após a publicação pode entregar por alguns minutos o HTML anterior do CDN do GitHub Pages, cujo cabeçalho informa cache de até 600 segundos. A URL com parâmetro de versão, `https://bichocutela.github.io/Mic-RHEMA-Ia-Studio-Google/?release=139bfccc`, carregou a tela inicial completa, com os dados Firebase e os ativos Supabase. O service worker foi atualizado para a versão `v3`, passando a remover caches antigos nas próximas ativações.

## Notificações Web Push

- O par de chaves Web Push foi gerado no Firebase Cloud Messaging do projeto `mic-rhema` em 22 de agosto de 2026 e a chave pública foi configurada somente no cliente PWA.
- A PWA solicita autorização exclusivamente a partir do toque no sino do cabeçalho. Em iPhones, ela orienta a instalação pela Tela de Início antes da solicitação, conforme a exigência do Safari.
- As funções Supabase `pwa-push-subscribe` e `pwa-push-send` estão ativas. A primeira armazena o token Web Push de forma deduplicada no Firestore; a segunda exige token Firebase com a claim administrativa antes de enviar um aviso a inscritos da PWA.
- Os testes HTTP controlados confirmaram a validação de token inválido (`400`) na inscrição e o bloqueio de envio sem credencial administrativa (`403`). O envio real fica pendente da inscrição de um dispositivo PWA publicado, para evitar disparar notificações sem consentimento.
