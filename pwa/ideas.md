# Referência visual obrigatória — PWA MIC Rhema

## Ground truth: aplicativo Android MIC Rhema

Esta PWA deve ser uma tradução instalada no Safari do **mesmo aplicativo Android**, não um site novo e nem uma interpretação editorial da marca. A referência é o código Android atual, principalmente `MainActivity.kt`, `HomeScreen.kt`, `GlassComponents.kt`, `Theme.kt`, `Type.kt`, `ProfileScreen.kt` e `SettingsScreen.kt`.

### Sistema visual

O visual obrigatório é Material 3 acolhedor, com fundo creme `#FFFFFDF7`, cartões branco/creme `#FFFFFF` e `#FFF8E7`, dourado `#8A6500` como cor de ação e azul-marinho `#131B2E` como contraste da navegação selecionada. Os cartões devem usar aproximadamente 16 px de raio, elevação discreta e margens laterais próximas de 20 px, como no Android.

### Estrutura de tela

No iPhone, a PWA usará a barra inferior flutuante bege (`#DCC8B6`) com item ativo em pílula azul-marinho. O botão de menu abre um drawer agrupado com perfil no topo, emblemas e as seções **Conteúdo**, **Comunidade**, **Igreja**, **Sistema** e **Administração**. A estrutura não será trocada por uma navegação editorial própria.

### Página inicial

A home começa com saudação e subtítulo pastoral simples. Em seguida, reproduz o aviso de culto do dia, o carrossel 16:9, o cartão de humor, os quatro atalhos rápidos, Devocional Diário, Notícias Bíblicas, Próximos Cultos e Mídia. Não haverá hero de página inteira nem a composição “Palavra para hoje” criada para a versão anterior.

### Padrão de módulos

As telas devem abrir com app bar simples, título Material, retorno quando necessário e conteúdo em uma coluna de cartões. Perfil, Configurações, IBR, Membros e ADM seguem suas hierarquias Android existentes. A Bíblia mantém seus recursos específicos, mas dentro da casca e da navegação do aplicativo.

### Interações

As transições são curtas (slide + fade de até 300 ms); drawers, bottom sheets e diálogos seguem o padrão Material. O pedido de notificações continua explícito e só ocorre por toque. A PWA preserva os dados Firebase/Supabase e não altera o Android.

## Decisão de estilo

- A especificação acima substitui integralmente a direção visual anterior “Santuário em Movimento”.
- Em caso de dúvida, a aparência e a estrutura do Android têm precedência total.
- Toda alteração da PWA deve ser avaliada pela pergunta: “isso parece parte do mesmo aplicativo Android?”
