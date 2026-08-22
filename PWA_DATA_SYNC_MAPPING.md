# Mapeamento de sincronização Android → PWA

Este documento registra as fontes Firestore e Supabase já usadas pelo aplicativo Android. A PWA deve apenas ler os mesmos dados públicos e respeitar os mesmos campos; ela não substitui nem altera a sincronização Android.

| Área PWA | Fonte Android | Campos e regra de exibição |
|---|---|---|
| Destaques da Home | `carousel_items` | `id`, `imageUrl`, `title`, `description`, `tag`, `eventDate`, `eventInfo`. Exibir apenas eventos sem data ou com `eventDate` igual/posterior à data atual. O clique só é habilitado quando `eventInfo` existir. |
| Velocidade do carrossel | `settings/app` | `bannerRotationSeconds`, limitado entre 3 e 12 segundos. Sem documento, usar 6 segundos, igual ao Android. |
| Compatibilidade de banner legado | `settings/home_banners` | `urls` continua sendo lido somente como fallback quando não houver item em `carousel_items`. |
| Notícias Bíblicas | `bible_news` | Lista pública atualizada em tempo real. |
| Vídeos | `conteudos_videos` | `title`, `description`, `videoUrl`, `thumbnailUrl`, `mediaUrl`, `isApproved`. |
| Áudios | `conteudos_audios` | `title`, `artist`, `audioUrl`, `coverUrl`, `mediaUrl`, `isApproved`. |
| Livros | `conteudos_books` | `title`, `author`, `coverUrl`, `bookUrl`, `mediaUrl`, `isApproved`. |
| Discipulado | `discipulado_pdfs` | Apenas itens `isPublished`, ordenados por `order` e `createdAt`. |
| Cultos | `cultos_agenda` | Agenda pública usada pela Home Android. |
| Configuração de sincronização | `settings/sync_trigger` | Sinal de atualização global; os listeners da PWA já recebem mudanças por `onSnapshot`. |

## Validação de fonte

A consulta pública de leitura confirmou que `carousel_items` contém destaques criados no painel Android com URLs públicas do Supabase e Google Drive, além das informações de evento. O documento legado `settings/home_banners` também existe, contendo uma lista `urls`.

Na prévia da PWA, o primeiro destaque real carregado foi o banner de Culto de Missões hospedado no Supabase. A interface também recebeu três indicadores reais e habilitou o clique apenas para o item que contém `eventInfo`, confirmando que o comportamento deixou de depender de banners fixos.

O clique em um destaque com `eventInfo` abriu corretamente um painel com a mensagem real cadastrada no Android para o Culto das Mulheres. Isso preserva a regra do aplicativo: banner sem informação de evento não recebe ação; banner com informação abre os detalhes.

A versão publicada no GitHub Pages pelo commit `3b15e28` confirmou o mesmo destaque real de Culto de Missões e os indicadores do carrossel. O workflow de publicação foi concluído com sucesso, sem iniciar workflow Android.

Na prévia da etapa de notícias, a Home passou a mostrar os cinco itens editoriais atuais de `bible_news`, com categoria e referência bíblica reais. Essa lista substitui as notícias estáticas anteriormente usadas pela PWA.

A rota completa de Notícias Bíblicas foi validada na prévia com a lista real do Firestore. Os itens exibidos trazem títulos editoriais atuais, categorias como “Provérbios hoje” e referências como “Provérbios 25:25”; a PWA também preserva a ordem editorial por `featured` e `publishedAt` usada pelo Android.

Na prévia de cultos, a Home mostrou a agenda real de `cultos_agenda`: Culto de Celebração, Culto de Oração e Culto de Ensino, ordenados pelos próximos dias e horários. A regra replica a programação semanal do Android quando um item não possui uma data específica.

Durante a validação automatizada, o cartão do culto foi identificado corretamente na tela. O acionamento pelo centro real do cartão abriu o painel com os dados cadastrados para o Culto de Celebração, incluindo data, horário e descrição. A interação foi validada antes da publicação.

> A PWA deve renderizar primeiro os itens de `carousel_items`, pois é a fonte editada pelo painel administrativo Android. Nenhum banner fixo deve ser mantido como conteúdo principal quando houver dados sincronizados disponíveis.
