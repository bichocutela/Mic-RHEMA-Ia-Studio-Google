# Comparação Android/PWA — 08/10/2026

Referência: `Data.kt` (initializeTabs/ensureDiscipuladoTab), `MainActivity.kt` e telas nativas. Inspeção do PWA publicado confirmou ausência de Discipulado no drawer quando `app_tabs` contém configuração anterior à inclusão da aba.

| Aba Android | Tela PWA | Origem/observação |
|---|---|---|
| Início | HomeParityView | Banners, notícias, agenda e mídia compartilhados |
| Bíblia | BibleParityViewV2 | Leitura, pesquisa e favoritos; preferências locais |
| Cultos | CultosParityView | cultos_agenda e events |
| Devocionais | DevotionalsParityView | devocionais e catálogo Android gerado |
| Cursos IBR | IbrParityView | ibr_courses e progresso do membro |
| Discipulado | DiscipuladoParityViewV2 | discipulado_pdfs; inclusão automática após IBR |
| Mídia | MediaParityViewV2 | Vídeos, áudios, livros e álbuns compartilhados |
| Pedidos de Oração | PrayerParityView | Envio/histórico pelas pontes existentes |
| Planos | PlansParityView | Catálogo gerado do Android e registro de XP |
| Equipe | TeamParityView | equipe |
| Membros | MembersParityView | Acesso pelo perfil existente |
| Sobre | AboutParityView | settings/about |
| Configurações | SettingsParityViewV2 | Preferências locais do navegador |
| Dízimos e Ofertas | DonationsParityView | settings/donations |
| Área ADM | AdminParityView | Login administrativo separado |

## Correções

- Lista padrão gerada de initializeTabs durante check/build; mudanças em Data.kt acionam verificação/publicação da PWA.
- Migração Discipulado equivalente ao Android, mantendo aba explicitamente oculta, renomeada ou reordenada.
- Abas personalizadas antes descartadas agora exibem customContents e respeitam acesso privado. Links abrem externamente, como no Android.
- Estudos acompanham edição/ocultação/exclusão em tempo real enquanto abertos. DOCX usa o leitor interno existente, com fonte ajustável e posição salva; DOC/PDF conservam o visualizador compatível.
- Layout de Discipulado acompanha cabeçalho/cartão/biblioteca Android.
- Dock comporta várias abas sem cortar os botões; rótulos acessíveis nos ícones. Textos longos quebram dentro dos cartões, e grupos do drawer persistem localmente.

## Limites da verificação

A comparação das 15 abas foi feita no código; isso não equivale a executar todos os fluxos autenticados em Android e iPhone. Dados publicados e configuração remota usam o mesmo servidor, mas preferências locais, arquivos baixados e permissões de notificações continuam próprios de cada plataforma. A criação/exclusão de conteúdo de abas personalizadas continua disponível no Android; a PWA consome esse conteúdo e mantém o editor de ordem/visibilidade existente.

## Validação executada

- `node --test tests/*.test.mjs`: 10 testes aprovados.
- `pnpm check`: aprovado.
- `pnpm build`: aprovado.
- PWA público: Discipulado carrega por link direto e possui 3 estudos; o menu publicado não contém a aba.
- Prévia local compilada: navegador remoto retornou ERR_CONNECTION_REFUSED; validação visual móvel da versão corrigida permanece pendente.
- Envio ao GitHub: bloqueado pela revisão automática, que exige autorização explícita para publicação. Nenhuma alteração foi publicada.
