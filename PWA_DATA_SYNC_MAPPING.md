# Mapeamento de sincronização Android → PWA

Este documento registra as fontes Firestore e Supabase já usadas pelo aplicativo Android. A PWA deve apenas ler os mesmos dados públicos e respeitar os mesmos campos; ela não substitui nem altera a sincronização Android.

| Área PWA | Fonte Android | Campos e regra de exibição |
|---|---|---|
| Destaques da Home | `carousel_items` | `id`, `imageUrl`, `title`, `description`, `tag`, `eventDate`, `eventInfo`. Exibir apenas eventos sem data ou com `eventDate` igual/posterior à data atual. O clique só é habilitado quando `eventInfo` existir. |
| Velocidade do carrossel | `settings/app` | `bannerRotationSeconds`, limitado entre 3 e 12 segundos. Sem documento, usar 6 segundos, igual ao Android. |
| Compatibilidade de banner legado | `settings/home_banners` | `urls` continua sendo lido somente como fallback quando não houver item em `carousel_items`. |
| Notícias Bíblicas | `bible_news` | Lista pública atualizada em tempo real. |
| Vídeos | `conteudos_videos` | `title`, `description`, `videoUrl`, `thumbnailUrl`, `mediaUrl`, `isApproved`. A PWA prioriza capa cadastrada e, se estiver ausente, extrai o ID de URL `youtu.be` ou `youtube.com` para usar a thumbnail oficial do YouTube. |
| Áudios | `conteudos_audios` | `title`, `artist`, `audioUrl`, `coverUrl`, `mediaUrl`, `isApproved`. |
| Livros | `conteudos_books` | `title`, `author`, `coverUrl`, `bookUrl`, `mediaUrl`, `isApproved`. |
| Discipulado | `discipulado_pdfs` | Apenas itens `isPublished`, ordenados por `order` e `createdAt`. |
| Cultos | `cultos_agenda` | Agenda pública usada pela Home Android. |
| Configuração de sincronização | `settings/sync_trigger` | Sinal de atualização global; os listeners da PWA já recebem mudanças por `onSnapshot`. |
| Pedidos de Oração | `prayer_requests` | A PWA oferece o formulário a qualquer visitante. A função web exclusiva `pwa-prayer-request` grava os campos `id`, `name`, `request` e `date: "Hoje"`, iguais ao `PrayerRequest` do Android, sem liberar escrita direta anônima no Firestore. A leitura e o gerenciamento permanecem administrativos. |

## Validação de fonte

A consulta pública de leitura confirmou que `carousel_items` contém destaques criados no painel Android com URLs públicas do Supabase e Google Drive, além das informações de evento. O documento legado `settings/home_banners` também existe, contendo uma lista `urls`.

Na prévia da PWA, o primeiro destaque real carregado foi o banner de Culto de Missões hospedado no Supabase. A interface também recebeu três indicadores reais e habilitou o clique apenas para o item que contém `eventInfo`, confirmando que o comportamento deixou de depender de banners fixos.

O clique em um destaque com `eventInfo` abriu corretamente um painel com a mensagem real cadastrada no Android para o Culto das Mulheres. Isso preserva a regra do aplicativo: banner sem informação de evento não recebe ação; banner com informação abre os detalhes.

A versão publicada no GitHub Pages pelo commit `3b15e28` confirmou o mesmo destaque real de Culto de Missões e os indicadores do carrossel. O workflow de publicação foi concluído com sucesso, sem iniciar workflow Android.

Na prévia da etapa de notícias, a Home passou a mostrar os cinco itens editoriais atuais de `bible_news`, com categoria e referência bíblica reais. Essa lista substitui as notícias estáticas anteriormente usadas pela PWA.

A rota completa de Notícias Bíblicas foi validada na prévia com a lista real do Firestore. Os itens exibidos trazem títulos editoriais atuais, categorias como “Provérbios hoje” e referências como “Provérbios 25:25”; a PWA também preserva a ordem editorial por `featured` e `publishedAt` usada pelo Android.

Na prévia de cultos, a Home mostrou a agenda real de `cultos_agenda`: Culto de Celebração, Culto de Oração e Culto de Ensino, ordenados pelos próximos dias e horários. A regra replica a programação semanal do Android quando um item não possui uma data específica.

Durante a validação automatizada, o cartão do culto foi identificado corretamente na tela. O acionamento pelo centro real do cartão abriu o painel com os dados cadastrados para o Culto de Celebração, incluindo data, horário e descrição. A interação foi validada antes da publicação.

Na prévia da etapa de Mídia, a Home passou a reunir conteúdos reais de `conteudos_videos`, `conteudos_audios` e `conteudos_books`: vídeos recentes, o áudio “Você Não é o Centro” e livros como “Linguas”, “Quando Não Dá Mais” e “Nascido Escravo”. Os cartões usam as capas públicas já cadastradas no Supabase ou Google Drive.

A ação “Ver todas” da seção Mídia foi localizada na área de interação da Home para a validação da lista completa e dos filtros por tipo antes da publicação.

A tela completa de Mídia foi aberta na prévia com os itens reais de todas as coleções. O filtro de Áudios foi testado e reduziu a lista corretamente ao conteúdo “Você Não é o Centro”, do Diácono Sandro.

Para o Devocional Diário, o Android usa `devocionais` com os campos `id`, `title`, `date`, `verse`, `verseReference`, `content`, `isApproved`/`approved` e `timestamp`. A leitura selecionada é a de data igual ao dia atual no formato `dd/MM/yyyy`; se não existir, o aplicativo usa a mais recente por `timestamp`. A PWA seguirá a mesma regra, preservando também os formatos legados de data encontrados na coleção.

Na prévia, a PWA selecionou o devocional real “CRESCIMENTO ESPIRITUAL”, datado de `22-08-2026`. O toque no cartão abriu o leitor com o versículo de Efésios 4.14,15, a referência e o texto integral cadastrados no Firebase.

Para os Planos de Leitura, a PWA passou a gerar o catálogo diretamente de `PlansData.kt` do Android. A prévia confirmou as 16 categorias oficiais e 592 temas, incluindo Alegria, Depressão, Estresse, Medo, Inveja, Raiva, Paz, Paciência, Perda, Ansiedade, Orgulho, Dúvida, Esperança, Amor, Tentação e Cura.

A categoria Alegria e o tema “A Essência da Alegria” foram abertos na prévia. A PWA exibiu o versículo João 3:16, o texto integral e a capa definidos no mesmo `PlansData.kt` do Android.

Para Pedidos de Oração, foi confirmado no Android que `PrayerScreen.kt` cria um `PrayerRequest` com `id`, `name`, `request` e `date` e o grava em `prayer_requests`. A PWA preserva esse formato, com um ID gerado antes da escrita e `date: "Hoje"`. O usuário determinou que o envio precisa ser aberto para todos; por isso o formulário não pede login nem solicitação de acesso. A PWA chama uma Edge Function própria que usa a credencial já guardada no Supabase para gravar o pedido na coleção, sem alterar regras Firebase compartilhadas nem expor a credencial no navegador. A validação visual cobriu a rota pública e o endpoint confirmou a rejeição correta de payload vazio, sem inserir pedidos fictícios na coleção real.

Na etapa de miniaturas dos vídeos, a coleção real `conteudos_videos` retornou URLs curtas `youtu.be` sem `thumbnailUrl` configurada. A PWA passou a derivar a capa oficial `i.ytimg.com/vi/<id>/hqdefault.jpg` sem sobrescrever uma imagem cadastrada. A prévia confirmou as thumbnails reais de “ATOS: O Poder de Deus em Nós”, “Crente Invisível Também é Corpo” e “Nova Vida em Cristo”.

Na aba Bíblia, foi identificado que a interface anterior apenas alterava o nome do livro e do capítulo, mas reutilizava os cinco versículos estáticos de Gênesis 1 em toda navegação. A PWA agora usa a mesma API Bolls do Android, com a tradução NAA, para obter o capítulo selecionado. A prévia confirmou a troca de Gênesis 1 para Êxodo 1 e Êxodo 2, cada qual com os versículos correspondentes.

A versão publicada foi aberta com o cache da release `6f2b89c`. Gênesis 1 carregou os 31 versículos completos retornados pela API, substituindo a versão anterior de cinco versículos fixos.

Para a etapa IBR, o Android foi mapeado como fonte de verdade: a coleção `ibr_courses` contém cursos com `id`, `title`, `theme`, `description`, `imageUrl` e lista `chapters`. Cada aula contém `id`, `title`, `description`, `durationMinutes`, `type`, `videoUrl`, `audioUrl`, `textContent`, `isYoutube`/`youtube` e `youtubeId`. O aluno com `isIbr` vê o progresso e os módulos em sequência; cada progresso guarda `courseId`, `chapterId`, `lastPositionSeconds`, `totalDurationSeconds` e `isCompleted`. A consulta pública confirmou cursos reais como “Introdução à Teologia Sistemática” e “História da Igreja Cristã”, com aulas de vídeo, áudio e texto.

Na prévia, a rota “Cursos IBR” foi aberta sem sessão e exibiu corretamente o bloqueio para alunos ainda não matriculados. O formulário de entrada permanece o mesmo da PWA; após a autenticação de um membro com `isIbr`, a nova tela usa os cursos e o progresso reais, sem conteúdo de demonstração.

A validação autenticada com a sessão administrativa confirmou a leitura em tempo real dos três módulos existentes: “Introdução à Teologia Sistemática”, “História da Igreja Cristã” e “Doutrina do Rhema - Fé Prática”. O primeiro módulo abriu as três aulas reais com título, descrição, tipo e duração; nenhuma aula foi iniciada durante o teste, preservando o progresso real.

Na etapa ADM, foi corrigida a publicação de mídia que antes sempre criava apenas um vídeo sem URL de abertura. O painel agora mostra contadores reais de solicitações, mídia, alunos e cursos; lista individualmente as solicitações pendentes para aprovação; e permite escolher Vídeo, Áudio ou Livro/PDF. Cada publicação grava no respectivo `conteudos_videos`, `conteudos_audios` ou `conteudos_books` com os mesmos campos consumidos pelo Android (`videoUrl`, `audioUrl` ou `bookUrl`, além de capa e crédito). A validação com a sessão administrativa confirmou 0 solicitações, 10 mídias, 4 alunos IBR e 3 cursos, além da adaptação visual dos campos para áudio, sem inserir conteúdo de teste.

> A PWA deve renderizar primeiro os itens de `carousel_items`, pois é a fonte editada pelo painel administrativo Android. Nenhum banner fixo deve ser mantido como conteúdo principal quando houver dados sincronizados disponíveis.


A verificação do painel ADM confirmou os valores reais de 4 alunos IBR e 3 cursos IBR, obtidos pelos listeners Firestore de `acessos_pendentes` e `ibr_courses`. O listener compartilhado da PWA foi ajustado para aplicar também snapshots vazios; exclusões remotas passam a refletir na tela em vez de deixar registros antigos em memória. Nenhum cadastro, curso ou progresso foi alterado durante a validação.
