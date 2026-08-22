# Especificação visual — PWA completa MIC Rhema

## Referência obrigatória

O aplicativo Android MIC Rhema é a **fonte de verdade** para a navegação, a linguagem de conteúdo e a hierarquia entre as áreas públicas, perfil, IBR e administração. A PWA não será um novo produto com estética genérica: ela traduzirá essa experiência para o Safari no iPhone, preservando a sensação de aplicativo da igreja e os mesmos nomes de módulos.

## Direção escolhida — Santuário em Movimento

### Design Movement

Um sistema de aplicativo comunitário contemporâneo, com inspiração em interfaces editoriais de igreja e no ritmo visual do Android atual. A área Bíblia preserva o **Códice Contemporâneo** como sublinguagem de leitura profunda, enquanto as demais abas priorizam descoberta de conteúdo e orientação clara.

### Core Principles

1. A barra inferior conduz as áreas principais e mantém cada ação a um toque de distância, como no Android.
2. Conteúdo da igreja é apresentado por blocos editoriais vivos, nunca por painéis técnicos ou listagens áridas.
3. O leitor bíblico desacelera a experiência: tipografia serifada, muito espaço vertical e ações discretas.
4. A administração é claramente separada da área pastoral, com densidade maior, filtros e seções recolhíveis.

### Color Philosophy

Azul petróleo e grafite dão estabilidade à navegação; marfim quente mantém acolhimento nas leituras e cartões de conteúdo; vinho profundo preserva o significado de trechos bíblicos, itens salvos e chamadas importantes; dourado fosco fica reservado para emblemas, progresso e marcos espirituais.

### Layout Paradigm

No celular, a PWA usa uma superfície contínua com cabeçalho contextual e navegação inferior fixa. A página inicial intercala faixas de conteúdo, carrosséis e atalhos assimétricos. A Bíblia troca para uma coluna editorial. A administração usa cartões dobráveis e uma barra de ferramentas que evita a sobrecarga de opções abertas.

### Signature Elements

1. Uma faixa de cabeçalho em azul petróleo com o monograma da igreja e pequenos detalhes dourados.
2. Cartões de conteúdo com cantos moderados, sombras curtas e cortes de imagem 16:9 sem cortar informação essencial.
3. Molduras de emblemas metálicas e um foco vinho-dourado para leituras, salvos e progressos.

### Interaction Philosophy

Cada toque deve abrir uma camada útil: expandir uma seção, revelar o próximo capítulo, mostrar um detalhe de culto, salvar conteúdo ou iniciar um fluxo de matrícula. Estados administrativos são sempre explícitos e confirmados. A PWA nunca pede permissão de notificação sem uma ação clara do usuário.

### Animation

O conteúdo entra com opacidade e deslocamento de até 12 px em 180–240 ms. Carrosséis usam transições suaves, seções recolhíveis preservam contexto e páginas bíblicas destacam o versículo alvo por um breve foco dourado. Animações serão reduzidas para usuários que solicitarem menos movimento.

### Typography System

Manrope guia navegação, formulários e painéis. Source Serif 4 dá voz às Escrituras, devocionais e referências. Títulos são compactos e diretos; longos textos têm entrelinha generosa para leitura confortável no iPhone.

### Brand Essence

**A igreja organizada na palma da mão — conteúdo, formação e comunhão com profundidade e clareza.** Personalidade: **acolhedora, firme, viva**.

### Brand Voice

Os textos soam pastorais e objetivos. Exemplos: “Continue a leitura que fortalece sua semana.” e “Há uma nova etapa esperando sua atenção.”

### Wordmark & Logo

O símbolo se baseia no monograma atual da igreja em tamanho claramente visível, nunca reduzido a um detalhe. Na PWA, ele aparece como marca de abertura e na navegação superior; o ícone da tela inicial usa o mesmo símbolo em fundo azul petróleo.

### Signature Brand Color

**Azul do Santuário** — `#103A4C`, utilizado como a cor de navegação e reconhecimento imediato da PWA.

## Style Decisions

- A PWA deve reproduzir a estrutura e os nomes do app Android, mas adaptar interações para Safari/iPhone.
- A experiência de Bíblia mantém o Códice Contemporâneo e abre em Gênesis 1 por padrão.
- O painel administrativo usa cartões recolhíveis, todos fechados inicialmente, com controles de expandir e minimizar tudo.
- Notificações web só serão solicitadas depois que a PWA estiver instalada e o usuário tocar em um controle explícito.
- Nenhum segredo, chave privada ou credencial de servidor será enviado ao navegador ou versionado no GitHub.
