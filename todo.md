# Project TODO

- [x] Configurar tema de cores (dourado, azul marinho)
- [x] Configurar ícones na tab bar (5 tabs)
- [x] Tela Home com saudação, devocional do dia e próximo culto
- [x] Tela Devocionais com lista e detalhe
- [x] Tela Cultos e Eventos com horários fixos
- [x] Tela Pedidos de Oração com formulário
- [x] Tela Sobre/Contato com informações da igreja
- [x] Gerar logo personalizada para o app
- [x] Substituir logo pela logo oficial do site
- [x] Atualizar informações com dados do site oficial
- [x] Atualizar app.config.ts com nome e branding
- [x] Schema do banco de dados (devocionais, cultos, eventos, configurações)
- [x] Rotas tRPC para admin e membros
- [x] Tela de login/cadastro com OAuth (Google)
- [x] Painel de administração completo (devocionais, cultos, eventos, info, membros, pedidos)
- [x] Área exclusiva para membros cadastrados
- [x] Integração das telas públicas com backend (dados dinâmicos)
- [x] Seed do banco de dados com dados iniciais

## Painel Admin Expandido

- [x] Schema: tabelas para customizações (temas, abas, rádio, home)
- [x] Rotas tRPC para gerenciar temas, abas, rádio e página inicial
- [x] Painel de IA: gerador de devocionais, sugestões de conteúdo
- [x] Editor de temas: cores primária, secundária, fundo, texto
- [x] Editor de abas: criar, editar, remover, reordenar abas
- [x] Editor de página inicial: customizar layout e conteúdo
- [x] Integração de rádio online: adicionar link de stream
- [ ] Tela de rádio no app (player com play/pause/volume)
- [x] UI moderna do painel admin com navegação melhorada
- [ ] Atualizar app para usar customizações do admin


## Modernização do Design

- [x] Componentes de animação (Animated, Reanimated)
- [x] Pull-to-refresh em telas principais
- [ ] Splash screen animado
- [x] Animações de entrada (fade-in, slide-up)
- [ ] Transições entre telas
- [ ] Parallax scrolling
- [x] Cards com elevação e sombras
- [x] Botões com feedback visual (scale, ripple)
- [x] Shimmer loading
- [ ] Ícones animados (pulse, bounce)

## Bíblia Dinâmica

- [x] Mapear a navegação atual entre livros, capítulos, versículos e leitura da Bíblia.
- [x] Implementar expansão inline de um capítulo com os versículos disponíveis.
- [x] Abrir a leitura diretamente no versículo selecionado com foco visual e rolagem suave.
- [x] Persistir a última passagem bíblica e perguntar se o usuário deseja continuar ou recomeçar em Gênesis 1.
- [ ] Validar exclusivamente os arquivos da aba Bíblia, compilar, fazer commit, push e acompanhar o GitHub Actions.

## Publicação da Bíblia — nova verificação

- [ ] Confirmar se as alterações da aba Bíblia já estão em um commit enviado ao branch main.
- [ ] Se necessário, validar, fazer commit e push apenas das alterações relacionadas à Bíblia.
- [ ] Acompanhar o GitHub Actions até confirmar o build Android.
- [ ] Informar o SHA real do commit e o status final do workflow.

## Avatares bíblicos pendentes

- [ ] Não alterar os avatares enquanto a geração de imagens estiver bloqueada.

## Leitor Bíblico — réplica da prévia aprovada

- [ ] Reproduzir a hierarquia visual da prévia na tela de leitura da Bíblia.
- [ ] Aplicar ações compactas por versículo, paginação de capítulos e barra inferior conforme o modelo aprovado.
- [ ] Validar a navegação, compilar o APK, publicar no main e acompanhar o workflow.

## Correção de solicitação de acesso

- [ ] Identificar a gravação do pedido de acesso e a regra que a restringe a administradores.
- [ ] Permitir criação de solicitação por usuários comuns sem liberar aprovação administrativa.
- [ ] Validar a chegada em tempo real no painel, publicar no main e acompanhar o build.

- [ ] Garantir na regra ativa: usuário comum cria apenas pedido pendente; somente ADM aprova ou muda permissões.

## Diagnóstico do erro persistente de solicitação

- [ ] Confirmar no código a chamada efetiva usada pelo APK para criar um pedido.
- [ ] Verificar autenticação anônima, coleção, ID do documento e versão instalada.
- [ ] Corrigir o bloqueio restante, testar e publicar a solução validada.

## Investigação de solicitação não gravada

- [ ] Instrumentar a tentativa de envio para registrar autenticação, coleção, ID e código do erro.
- [ ] Comprovar se o documento pendente chega ao Firestore e se o listener do ADM o recebe.
- [ ] Corrigir o ponto de interrupção, publicar e validar o fluxo completo.

## Carrossel de banners

- [ ] Transformar os banners da Home em carrossel com transição suave e indicadores.
- [ ] Armazenar a velocidade de rotação em configuração sincronizada do Firestore.
- [ ] Adicionar controle de velocidade no painel administrativo de banners.
- [ ] Validar, compilar, publicar e acompanhar o APK.

## Avatares bíblicos pendentes — Timóteo, Priscila e Lídia

- [ ] Verificar a disponibilidade de geração sem alterar o repositório se a cota estiver bloqueada.
- [ ] Gerar três avatares 1:1 consistentes com o catálogo bíblico existente.
- [ ] Copiar os PNGs, registrar os três personagens no AvatarCatalog e validar o APK.
- [ ] Fazer commit, push, acompanhar o build verde e desativar o agendamento após sucesso.

## Verificação do carrossel de banners

- [ ] Confirmar a presença do carrossel automático na Home e do controle de velocidade no ADM.
- [ ] Corrigir qualquer parte ausente e validar a sincronização da velocidade pelo Firestore.
- [ ] Compilar, publicar se necessário e informar o SHA real.

- [ ] Fazer commit e push do carrossel de banners e confirmar o build Android verde.

## Correção da atualização interna do APK

- [ ] Verificar o artefato da release, a URL baixada e a integridade do APK.
- [ ] Corrigir o download/instalador que causa erro ao analisar o pacote.
- [ ] Validar a nova release e confirmar a atualização interna antes de publicar.

- [ ] Restringir a correção ao download e instalador; não alterar chaves, assinatura, keystore, versionamento ou workflow.

## Teste de falha de rede da atualização

- [ ] Simular URL de atualização indisponível sem alterar o aplicativo.
- [ ] Confirmar a mensagem de erro e que o instalador não é acionado.

## Nova verificação de avatares bíblicos

- [ ] Verificar a cota de geração sem alterar o repositório caso permaneça bloqueada.
- [ ] Integrar Timóteo, Priscila e Lídia somente após geração disponível e build verde.

## Notificação de atualização segmentada

- [ ] Confirmar quais dispositivos têm versão instalada registrada para segmentar a notificação.
- [ ] Enviar o aviso somente a versões antigas e abrir a área Sobre ao tocar.
- [ ] Validar o público alcançado e relatar limitações de segmentação.

## Aviso geral e preparação de atualização futura

- [ ] Confirmar o cheque automático de atualização a cada 12 horas.
- [ ] Enviar agora o aviso geral de atualização no tópico de todos os usuários.
- [ ] Registrar versão/token e abrir Sobre ao tocar em futuras notificações de atualização.

## Publicação da função de notificações

- [ ] Verificar sessão Supabase disponível e publicar notify-fcm sem solicitar credenciais se houver acesso.
- [ ] Validar secrets de FCM e enviar o aviso geral de atualização.

- [ ] Fazer uma última tentativa de sessão Supabase; se indisponível, interromper sem novas alterações.

- [ ] Verificar novamente a sessão Supabase e publicar notify-fcm se o acesso estiver disponível.

- [ ] Comparar a configuração de notificações do NRD Lojas com o MIC Rhema e reaplicar o caminho compatível.

## Notificações em segundo plano

- [x] Mapear FCM, serviços Android, ouvintes Firestore e permissões que hoje dependem da abertura do app.
- [ ] Corrigir o recebimento e a exibição de notificações quando o aplicativo estiver em segundo plano ou fechado.
- [ ] Validar o fluxo, publicar a correção e acompanhar o build Android até verde.

## Publicação autorizada da função FCM

- [x] Ativar a integração Supabase da sessão e publicar `notify-fcm`.
- [x] Configurar ou validar os secrets necessários da conta de serviço Firebase no projeto Supabase.
- [x] Confirmar que o endpoint deixa de responder 404 e validar a entrega para um tópico FCM sem criar avisos indevidos.

## Secrets FCM autorizados

- [x] Registrar `FIREBASE_SERVICE_ACCOUNT_JSON` no Supabase com a credencial privada do Firebase.
- [x] Registrar `FIREBASE_PROJECT_ID=mic-rhema` e validar a função `notify-fcm` com um teste controlado.

## Credencial Firebase autorizada

- [ ] Acessar o Firebase Console do projeto `mic-rhema` e gerar a chave privada da conta de serviço.
- [ ] Usar a chave somente como secret do Supabase para a função FCM; não adicionar o arquivo ao repositório ou APK.

## Alternativa ao CAPTCHA do Firebase

- [x] Aguardar o JSON da conta de serviço gerado pelo usuário em um dispositivo sem bloqueio CAPTCHA.
- [x] Registrar o JSON exclusivamente como secret do Supabase e validar a função FCM sem expô-lo em commits, logs ou APK.

## Credencial recebida para configuração

- [x] Cadastrar a credencial recebida somente no secret `FIREBASE_SERVICE_ACCOUNT_JSON` do Supabase.
- [x] Configurar `FIREBASE_PROJECT_ID=mic-rhema`, validar FCM e recomendar a rotação da chave ao fim da configuração.

## Teste real FCM autorizado

- [ ] Enviar um aviso real de teste ao tópico `all_users` com autorização explícita do usuário.
- [ ] Confirmar a aceitação pelo FCM e orientar a verificação com o aplicativo fechado.

## PWA para iPhone via GitHub Pages

- [x] Definir a primeira versão instalável da PWA com os fluxos principais do MIC Rhema Android.
- [x] Preparar manifest, service worker, ícones e experiência de instalação pelo Safari no iPhone.
- [x] Configurar build estático e workflow de publicação pelo GitHub Pages, sem hospedagem Manus.
- [x] Validar a PWA no navegador e documentar a instalação no iPhone.

## PWA completa aprovada

- [x] Implementar os módulos públicos: início, notícias, Bíblia, mídia, cultos, discipulado e planos.
- [x] Implementar perfil, avatares, emblemas, preferências, IBR e solicitações de acesso.
- [x] Implementar o painel administrativo, aprovações e sincronização segura com Firebase e Supabase.
- [x] Integrar notificações web do iPhone e publicar a PWA completa pelo GitHub Pages.
- [ ] Validar em um iPhone instalado: autorizar o sino de notificações e conferir o primeiro aviso real enviado pelo painel ADM.

## Paridade visual obrigatória entre PWA e Android

- [x] Mapear as telas, abas, componentes, paleta, tipografia e navegação reais do aplicativo Android, sem alterar seus arquivos.
- [x] Refazer a estrutura visual da PWA para espelhar o aplicativo Android, removendo a direção visual independente atual.
- [x] Adaptar os módulos web existentes para a hierarquia de telas Android, preservando Firebase, Supabase e Web Push.
- [x] Validar visualmente em largura de iPhone, publicar somente a PWA e submeter para revisão antes de declarar paridade.
- [ ] Aguardar a revisão do usuário no iPhone comparando a sensação de uso da PWA com o APK Android instalado.

## Paridade Android por etapas

- [x] Etapa 1 — substituir exclusivamente a logo da PWA pela mesma logo oficial do aplicativo Android e publicar a validação no commit `56d2a38`.
- [ ] Etapa 2 — reproduzir progressivamente, em alterações isoladas, cada tela, opção, mídia, mensagem e detalhe do Android na PWA.
- [ ] Para cada etapa: validar somente a PWA, salvar, fazer commit, publicar no GitHub Pages e aguardar o resultado antes de seguir.
- [ ] Não alterar arquivos, build, versão, assinatura ou publicação do aplicativo Android em nenhuma etapa.

## Sincronização PWA com os dados reais

- [x] Mapear as coleções e os campos reais que o Android usa para destaques, banners, vídeos, livros, áudios, notícias, cultos e conteúdos.
- [x] Substituir os dados fixos do carrossel de destaques da Home PWA por listeners Firebase e URLs públicas do Supabase, com atualização em tempo real.
- [ ] Garantir que alterações administrativas refletidas no Android também apareçam na PWA sem publicação manual adicional.

## Isolamento obrigatório da PWA

- [x] Concluir os ajustes pendentes somente em `pwa/`, no workflow `pwa-pages.yml` e nas funções web separadas.
- [x] Não modificar arquivos Android, Gradle, Firebase Android, keystore, assinatura, versão ou workflow de APK.
- [x] Validar a PWA publicada e o cache atualizado sem disparar build nem alterar o aplicativo Android.
- [x] Investigar e corrigir a entrega/cache da página pública apenas pela publicação da PWA no GitHub Pages.
- [x] Limitar o workflow Android a alterações Android para que commits exclusivos da PWA não gerem APKs, versões ou atualizações no aplicativo.

## Auditoria da aba Configurações

- [ ] Mapear cada opção da aba Configurações e onde ela é persistida.
- [ ] Verificar se cada escolha é aplicada de fato em todas as telas e sessões.
- [ ] Avaliar a organização e propor melhorias visuais sem alterar o código nesta etapa.

## Melhorias de Configurações por etapas

- [ ] Etapa 1: aplicar o tamanho de fonte global persistente em todo o app; validar, commit, push e build verde.
- [ ] Etapa 2: corrigir preferências e organização de notificações; validar, commit, push e build verde.
- [ ] Etapa 3: tornar downloads e armazenamento coerentes; validar, commit, push e build verde.
- [ ] Etapa 4: aplicar preferências de leitura; validar, commit, push e build verde.
- [ ] Etapa 5: aplicar preferências de áudio; validar, commit, push e build verde.
- [ ] Etapa 6: aplicar internet, favoritos e histórico; validar, commit, push e build verde.
- [ ] Etapa 7: reorganizar visual e sincronização de Configurações; validar, commit, push e build verde.

## Ajuste de seções em Configurações

- [ ] Recolher todas as seções por padrão e permitir expandir ou minimizar cada cartão.
- [ ] Adicionar controles globais para expandir tudo e recolher tudo, como no painel administrativo.
- [ ] Remover a opção de brilho interno e impedir qualquer alteração de brilho do dispositivo no leitor bíblico.
- [ ] Validar, fazer commit, push e acompanhar o build Android até verde.
