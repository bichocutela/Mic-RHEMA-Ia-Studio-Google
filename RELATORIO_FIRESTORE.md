# Relatório de Auditoria: Uso do Firebase Firestore no Projeto MIC Rhema

## 1. LISTA COMPLETA DE COLEÇÕES (24 Coleções encontradas no Kotlin)
- acessos_pendentes
- app_tabs
- bible_news
- bible_plans
- carousel_items
- conteudos_albums
- conteudos_audios
- conteudos_books
- conteudos_videos
- cultos
- cultos_agenda
- devocionais
- equipe
- events
- ibr_courses
- prayer_requests
- settings
- user_settings
- users
- vip_albums
- vip_audios
- vip_books
- vip_courses
- vip_videos

## 2 e 3. ANÁLISE POR COLEÇÃO E INCONSISTÊNCIAS DE NOMES

As seguintes inconsistências de nomenclatura foram encontradas comparando o código fonte do aplicativo com o arquivo `firestore.rules`. As coleções divergem, o que significa que as regras protegerão coleções inexistentes, enquanto as coleções reais utilizadas ficarão bloqueadas para gravação.

- **Coleção A (no App):** `conteudos_books`, `conteudos_audios`, `conteudos_videos`, `conteudos_albums`
  - **Coleção B (no firestore.rules):** `content_books`, `content_audios`, `content_videos`, `content_albums`
- **Coleção A (no App):** `devocionais`
  - **Coleção B (no firestore.rules):** `devotionals`
- **Coleção A (no App):** `cultos`, `cultos_agenda`
  - **Coleção B (no firestore.rules):** `services`

## 4. OPERAÇÕES QUE PODEM SER BLOQUEADAS PELAS REGRAS ATUAIS

Devido à arquitetura do Firestore, se uma coleção não tiver uma regra `allow write` explícita, a gravação é bloqueada por padrão. Como a maioria das coleções do aplicativo não está declarada no `firestore.rules`, as gravações irão falhar.

- **CRÍTICO:** Usuários comuns não conseguirão se cadastrar (escrita bloqueada em `acessos_pendentes`), não conseguirão enviar pedidos de oração (`prayer_requests`), nem salvar configurações (`user_settings`) e favoritos/progresso (`users`).
- **CRÍTICO:** Administradores não conseguirão adicionar, editar ou excluir virtualmente nenhum conteúdo do aplicativo (Mídias, Equipe, Cultos, Banners, Devocionais, etc.), exceto VIP e Eventos.
- **OK:** Leituras estão funcionando perfeitamente devido à regra global `match /{document=**} { allow read: if true; }`.

## 5. ANÁLISE ESPECÍFICA DAS FUNCIONALIDADES

- **Login / Cadastro:** Usa `acessos_pendentes`. Bloqueado para usuários criarem e ADMs aprovarem.
- **Meu Perfil / Membros:** Usa subcoleções dentro de `users` e `user_settings`. Não há regras, portanto alterações não são salvas.
- **Área ADM:** Quase todas as ações de gravação falharão devido às regras desatualizadas ou ausentes. Apenas o gerenciamento de conteúdo VIP e Eventos será salvo com sucesso no banco.
- **IBR:** Usa `ibr_courses` e `users/{id}/ibrProgress`. Bloqueado para ADM (criar) e usuários (progresso).
- **Devocionais:** App usa `devocionais`, regras usam `devotionals`. Gravação do ADM será bloqueada.
- **Cultos:** App usa `cultos` e `cultos_agenda`, regras usam `services`. Gravação do ADM bloqueada.
- **Eventos:** Usa `events`. Regras estão perfeitamente sincronizadas com o App. Funciona perfeitamente.
- **Destaques/Banners & Configurações:** Usa `carousel_items` e `settings` (dízimos/sobre). Gravação do ADM bloqueada.
- **Notícias / Planos Bíblicos / Equipe:** Usam `bible_news`, `bible_plans`, `equipe`. Gravação bloqueada.
- **Pedidos de oração:** Usa `prayer_requests`. Usuários não conseguirão submeter novos pedidos.

## 6. VERIFICAÇÃO DE SALVAMENTO E LEITURA (INCONSISTÊNCIA INTERNA)

Felizmente, **NÃO há inconsistências de leitura e escrita puramente dentro do código Kotlin**. 
O aplicativo (seja a tela de Usuário ou a tela de ADM) utiliza consistentemente as mesmas strings de coleção. Se o ADM tenta salvar um devocional, ele usa `devocionais`, e a tela do usuário lê de `devocionais`. 

A inconsistência reside **apenas entre o App (Kotlin) e o Banco (firestore.rules)**.

## 7. ANÁLISE DO FIRESTORE.RULES

- **Coleções existentes nas regras mas não utilizadas pelo App:** `conteudo`, `content_books`, `content_audios`, `content_videos`, `content_albums`, `devotionals`, `services`.
- **Coleções utilizadas pelo App mas ausentes nas regras:** 19 coleções no total (ver tabela abaixo).
- **Operações permitidas/bloqueadas:** Leitura 100% permitida publicamente. Escrita restrita a administradores e limitada apenas a `vip_*` e `events`. Qualquer outra escrita falhará.

## TABELA DE RESUMO

| COLEÇÃO | LEITURA (App) | ESCRITA (App) | QUEM USA | REGRA ATUAL (firestore.rules) | PROBLEMA |
|---|---|---|---|---|---|
| `acessos_pendentes` | Sim | Sim | ADM, Usuário | Ausente | Escrita/Cadastro falha |
| `app_tabs` | Sim | Sim | ADM | Ausente | ADM não salva tabs |
| `bible_news` | Sim | Sim | ADM | Ausente | ADM não salva notícias |
| `bible_plans` | Sim | Sim | ADM | Ausente | ADM não salva planos |
| `carousel_items` | Sim | Sim | ADM | Ausente | ADM não salva banners |
| `conteudos_books` | Sim | Sim | ADM | Inconsistente (`content_books`) | ADM não salva |
| `conteudos_audios` | Sim | Sim | ADM | Inconsistente (`content_audios`) | ADM não salva |
| `conteudos_videos` | Sim | Sim | ADM | Inconsistente (`content_videos`) | ADM não salva |
| `conteudos_albums` | Sim | Sim | ADM | Inconsistente (`content_albums`) | ADM não salva |
| `cultos` | Sim | Sim | ADM | Inconsistente (`services`) | ADM não salva |
| `cultos_agenda` | Sim | Sim | ADM | Inconsistente (`services`) | ADM não salva |
| `devocionais` | Sim | Sim | ADM | Inconsistente (`devotionals`) | ADM não salva |
| `equipe` | Sim | Sim | ADM | Ausente | ADM não salva equipe |
| `events` | Sim | Sim | ADM | Correta (`events`) | **OK** |
| `ibr_courses` | Sim | Sim | ADM | Ausente | ADM não salva cursos |
| `prayer_requests` | Sim | Sim | ADM, Usuário | Ausente | Usuário não envia |
| `settings` | Sim | Sim | ADM | Ausente | ADM não altera config |
| `user_settings` | Sim | Sim | Usuário | Ausente | Usuário não altera |
| `users` (e subcoleções) | Sim | Sim | Usuário | Ausente | Progresso e favoritos falham |
| `vip_books` | Sim | Sim | ADM | Correta (`vip_books`) | **OK** |
| `vip_audios` | Sim | Sim | ADM | Correta (`vip_audios`) | **OK** |
| `vip_videos` | Sim | Sim | ADM | Correta (`vip_videos`) | **OK** |
| `vip_albums` | Sim | Sim | ADM | Correta (`vip_albums`) | **OK** |
| `vip_courses` | Sim | Não | - | Ausente | (Não usado para gravação) |

---

## CORREÇÕES RECOMENDADAS (EM ORDEM DE PRIORIDADE)

1. **Atualizar o arquivo `firestore.rules`** para incluir todas as coleções que possuem inconsistência de nomenclatura, substituindo os nomes em inglês pelos equivalentes em português/usados no código Kotlin (ex: `content_books` -> `conteudos_books`).
2. **Permitir a criação de registros por usuários autenticados** na coleção `acessos_pendentes`, para que o fluxo de registro e aprovação volte a funcionar.
3. **Adicionar regras específicas de administrador (allow write: if isAdmin())** para todas as coleções ausentes no arquivo de regras (como `carousel_items`, `settings`, `bible_news`, `equipe`, etc.), para destravar o painel administrativo.
4. **Adicionar regras na coleção `users` e `user_settings`** para permitir que os usuários autenticados gravem dados em seus próprios documentos (por exemplo, `allow write: if request.auth.uid == userId`), desbloqueando favoritos e progresso de cursos.
5. **Adicionar regras na coleção `prayer_requests`** permitindo que qualquer usuário autenticado crie um pedido de oração, mantendo a regra de que apenas ADMs podem excluir ou alterar.
