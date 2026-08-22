# PWA MIC Rhema — especificação de paridade com Android

## Fonte de verdade

O aplicativo Android em `app/src/main/java/com/aistudio/micrhema/` é a referência obrigatória. A PWA não terá identidade visual, organização de páginas ou linguagem de interface própria. Ela deve traduzir o mesmo produto para Safari e iPhone, preservando as limitações naturais da web sem mudar a arquitetura do Android.

## Casca do aplicativo

| Elemento | Referência Android | Implementação exigida na PWA |
|---|---|---|
| Fundo claro | `#FFFFFDF7` | Fundo padrão de todas as telas públicas |
| Superfície e cartões | Branco e `#FFF8E7` | Cartões Material 3 de 16 px, sem hero editorial de página inteira |
| Cor primária | Dourado `#8A6500` | Títulos de seção, ícones ativos, botões e indicadores |
| Navegação inferior | Pílula bege `#DCC8B6`; item ativo azul-marinho `#131B2E` | Barra flutuante inferior com ícone, rótulo do item ativo e botão de menu |
| Menu | Drawer agrupado | Painel lateral com perfil e os grupos Conteúdo, Comunidade, Igreja, Sistema e Administração |
| Tipografia | Material 3 expressiva | Títulos robustos de 18–24 px, texto de corpo 14–16 px; sem tipografia serifada editorial fora da Bíblia |
| Transições | Slide curto + fade | Troca de aba em até 300 ms, sem animação teatral |

## Página inicial obrigatória

1. Saudação simples: “Olá, {primeiro nome}” ou “Seja bem-vindo à Rhema”, seguida de “Que a paz do Senhor esteja com você”.
2. Aviso contextual “Hoje tem {culto}” quando aplicável.
3. Carrossel de banners 16:9 com sobreposição textual inferior e indicadores em pílula.
4. Cartão “Como está seu coração hoje?” que abre a escolha de humores e direciona a planos.
5. Atalhos em linha: Bíblia, Pedidos, Planos e Membros.
6. Cartão de Devocional Diário.
7. Seções horizontais para Notícias Bíblicas, Próximos Cultos e Mídia.

## Áreas e hierarquia

| Grupo do menu Android | Telas a preservar na PWA |
|---|---|
| Conteúdo | Início, Bíblia, Devocionais, Cursos IBR, Mídia e Planos |
| Comunidade | Pedidos de Oração, Membros e Equipe |
| Igreja | Cultos, Dízimos e Ofertas |
| Sistema | Configurações e Sobre |
| Administração | Área ADM, restrita ao login administrativo existente |

## Regras de reconstrução

- Remover a direção visual “Santuário em Movimento”, o herói editorial e a navegação atual que não existem no Android.
- Reutilizar dados Firebase, Supabase, sessão, PWA, notificações Web Push e autorização administrativa já implementados, porém encaixados nos módulos e cartões equivalentes ao Android.
- Não editar arquivos Kotlin, Gradle, recursos Android, versão, assinatura, keystore ou workflow de APK.
- Só publicar a PWA após revisão visual na largura de iPhone; a aprovação será baseada em comparação direta com esta especificação Android.

## Registro de inspeção visual

Em 22 de agosto de 2026, a prévia da PWA foi conferida em largura móvel. A tela inicial já apresenta a sequência Android de saudação, carrossel 16:9, cartão de humor, atalhos, Devocional Diário, listas horizontais de notícias/cultos/mídia e a barra inferior bege com o item ativo azul-marinho. As imagens remotas carregaram após a renderização inicial. A validação final ainda inclui conferir o drawer e as telas secundárias antes da publicação.

Na prévia temporária, o navegador expõe a imagem com uma escala diferente da área DOM. A inspeção identificou o botão de menu ativo fora das coordenadas usadas pela captura, portanto os testes do drawer devem usar as coordenadas DOM reais ou o navegador publicado, sem interpretar a primeira tentativa de clique como falha da interface.

O drawer foi validado pela área DOM real: apresenta perfil, cartão de caminho, grupo Conteúdo expandido, grupos Comunidade/Igreja/Sistema/Administração recolhidos e o acionamento explícito de notificações. A composição usa fundo creme, seleção dourada e sobreposição escurecida, correspondendo à hierarquia do drawer Android.

A publicação do commit `e2840e5` foi concluída no GitHub Pages com workflow verde. A captura pública confirma fundo creme, saudação, banner 16:9, cartão de humor, atalhos, Devocional Diário, listas horizontais e barra inferior bege com item ativo azul-marinho. A revisão humana no iPhone continua necessária para comparar a sensação de uso com o APK instalado.
