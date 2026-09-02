# Auditoria de teclado / IME

A interface principal do MIC Rhema usa edge-to-edge. Por isso, além de `adjustResize`, o conteúdo Compose precisa respeitar explicitamente os insets do teclado.

Correções aplicadas em setembro de 2026:

- o conteúdo principal usa `imePadding()`;
- a barra inferior e o player persistente são ocultados enquanto o teclado está aberto;
- a lista da Bíblia que contém a pesquisa profunda respeita o IME;
- formulários roláveis receberam espaço de IME para manter o campo focado visível;
- diálogos longos com campos de texto receberam rolagem/proteção contra sobreposição do teclado.

Objetivo: nenhum campo de entrada deve ficar escondido atrás do teclado durante a digitação, inclusive em telas menores.
