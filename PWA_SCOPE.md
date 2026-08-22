# MIC Rhema PWA — Escopo da primeira versão completa

## Objetivo

Entregar uma experiência instalável no iPhone, publicada pelo GitHub Pages e alinhada ao aplicativo Android MIC Rhema. A PWA reutilizará apenas configurações públicas no navegador e manterá operações administrativas protegidas pelos serviços já existentes.

## Módulos

| Área | Primeira versão |
|---|---|
| Conteúdo público | Início, banners, cultos, notícias, mídia, planos, Bíblia e Discipulado. |
| Conta | Perfil, avatar bíblico, emblemas, preferências e solicitação de acesso. |
| Formação | Área IBR, cursos, módulos e progresso visível ao aluno aprovado. |
| Administração | Gestão de conteúdo, membros, aprovações e configurações, limitada a contas autorizadas. |
| PWA | Instalação pelo Safari, cache de navegação e preparação para notificações web. |

## Publicação

O build será estático e publicado pelo GitHub Pages. Dados públicos serão lidos dos serviços já utilizados pelo aplicativo Android. Segredos de servidor não serão colocados no navegador, no repositório ou no bundle publicado.
