# Design do Aplicativo MIC Rhema

## Visão Geral

O aplicativo do Ministério Igreja de Cristo Rhema é uma ferramenta para conectar os membros da igreja com conteúdos devocionais, informações de cultos, pedidos de oração e comunicação direta com a liderança. O design segue as diretrizes Apple HIG para uma experiência nativa e fluida em dispositivos móveis.

## Paleta de Cores

| Token | Cor (Light) | Cor (Dark) | Uso |
|-------|-------------|------------|-----|
| primary | #D4A017 (Dourado) | #F0C040 | Botões, destaques, ícones ativos |
| background | #FFFFFF | #1A1A1A | Fundo das telas |
| surface | #F8F6F0 (Creme claro) | #2A2520 | Cards, superfícies elevadas |
| foreground | #1C1C1E | #F5F5F5 | Texto principal |
| muted | #6B7280 | #9CA3AF | Texto secundário |
| border | #E8E4DC | #3D3830 | Bordas e divisores |
| success | #2E7D32 (Verde escuro) | #66BB6A | Estados de sucesso |
| warning | #F59E0B | #FBBF24 | Avisos |
| error | #DC2626 | #F87171 | Erros |

## Lista de Telas

### 1. Home (Início)
Tela principal com saudação, devocional do dia em destaque, próximo culto e acesso rápido às seções.

### 2. Devocionais
Lista de devocionais com título, data e trecho. Ao tocar, abre o devocional completo com texto bíblico e reflexão.

### 3. Cultos e Eventos
Exibe os horários fixos dos cultos (Terça 19h, Quinta 19h, Domingo 18:30) e eventos especiais da igreja.

### 4. Pedidos de Oração
Formulário para enviar pedidos de oração (nome, pedido, urgência). Lista dos próprios pedidos salvos localmente.

### 5. Sobre / Contato
Informações da igreja: endereço, telefone, e-mail, pastor, missão. Botões de ação para ligar, enviar e-mail e abrir mapa.

## Conteúdo e Funcionalidade por Tela

### Home
- Saudação com hora do dia ("Bom dia", "Boa tarde", "Boa noite")
- Card do devocional do dia (título + versículo base)
- Card do próximo culto (dia da semana e horário)
- Versículo em destaque com citação bíblica
- Slogan da igreja: "Conectando Pessoas e Transformando Vidas"

### Devocionais
- Lista vertical de cards com: título, data, trecho do texto
- Tela de detalhe com: título, data, texto completo, versículo base
- Conteúdo local pré-carregado (dados estáticos inspirados nos devocionais reais)

### Cultos e Eventos
- Seção de horários fixos com ícones de calendário
- Cards de eventos futuros (título, data, descrição breve)
- Dados locais

### Pedidos de Oração
- Formulário: campo de nome, campo de pedido (textarea), botão enviar
- Lista dos pedidos enviados (salvos em AsyncStorage)
- Opção de marcar como "respondido"

### Sobre / Contato
- Foto/ícone do pastor
- Nome: Pastor Evaldo Leôncio
- Missão da igreja e significado de "Rhema"
- Endereço: Rua Todos os Santos – Natal/RN
- Telefone: 84 98804 1804
- E-mail: micrhema2@gmail.com
- Botões de ação: Ligar, E-mail, Mapa

## Fluxos de Usuário Principais

1. **Ler devocional**: Home → Toca no card do devocional → Tela de detalhe do devocional
2. **Ver cultos**: Tab Cultos → Visualiza horários e eventos
3. **Enviar pedido de oração**: Tab Oração → Preenche formulário → Salva localmente → Aparece na lista
4. **Contatar a igreja**: Tab Sobre → Toca em "Ligar" ou "E-mail" → Abre app nativo

## Navegação (Tab Bar)

| Tab | Ícone | Tela |
|-----|-------|------|
| Início | house.fill | Home |
| Devocionais | book.fill | Lista de devocionais |
| Cultos | calendar | Horários e eventos |
| Oração | hands.sparkles.fill | Pedidos de oração |
| Sobre | info.circle.fill | Informações e contato |

## Tipografia e Espaçamento

- Títulos: 24-28px, bold
- Subtítulos: 18-20px, semibold
- Corpo: 15-16px, regular
- Espaçamento entre cards: 16px
- Padding de tela: 20px horizontal
- Border radius dos cards: 16px
