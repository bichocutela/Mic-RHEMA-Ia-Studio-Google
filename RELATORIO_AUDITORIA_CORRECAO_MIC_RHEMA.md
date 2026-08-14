# Relatório de Auditoria e Correção Funcional — MIC Rhema

## Escopo

Foi realizada uma auditoria direcionada ao repositório `bichocutela/Mic-RHEMA-Ia-Studio-Google`, seguindo as prioridades fornecidas. A identidade visual, `applicationId`, assinatura, keystore, GitHub Actions, UpdateChecker, UpdateDownloader, QR Code, YouTube player e ExoPlayer não foram redesenhados nem alterados funcionalmente.

## Correções implementadas

| Área | Correção | Arquivos principais |
|---|---|---|
| Firestore | Remoção da leitura pública global e criação de regras explícitas para conteúdo público, dados administrativos, pedidos de oração, usuários, favoritos e progresso IBR. | `firestore.rules` |
| Membros | Listener anterior é removido antes de nova sincronização; lista vazia válida agora limpa o estado; `isVip` só é lido para migração e o estado ativo passa a usar `isVip = false`. | `Data.kt` |
| Abas | `initializeTabs()` não limpa mais abas persistidas; a limpeza destrutiva executada no boot foi removida. | `Data.kt`, `MainActivity.kt` |
| Persistência | Loop de `LocalDataManager.saveAll()` a cada cinco segundos foi removido; a rede passou a usar o escopo da tela. | `MainActivity.kt` |
| Pedidos de oração | O pedido só é incluído no estado, os campos são limpos e o sucesso é exibido após confirmação do Firestore. Em falha, os dados permanecem e a tentativa pode ser repetida. | `PrayerScreen.kt`, `Data.kt` |
| Configurações | Espaço ocupado é calculado; downloads usam diretório MIC Rhema; cache seguro e histórico real podem ser limpos; recarregamento restaura o estado local; alteração de contato navega para o Perfil. | `SettingsScreen.kt`, `MainActivity.kt` |
| IBR | Conteúdos iniciais de demonstração foram removidos; vídeo e áudio não são concluídos ao tocar; texto exige o botão “Concluir leitura”. | `Data.kt`, `IbrScreen.kt` |
| Bíblia | `pt_aa.json` não é mais tratado como ARA; ACF/NVI não caem silenciosamente em Almeida; foi criado validador estrutural com 66 livros, capítulos não vazios, versículos não vazios e verificação de Juízes com 21 capítulos. | `LocalBibleFetcher.kt`, `BibleFetcher.kt`, `BibleDatasetValidator.kt` |
| Crash em produção | Exceções são registradas no Crashlytics e a interface de release mostra mensagem amigável, mantendo stacktrace somente em debug. | `CrashHandler.kt`, `MainActivity.kt` |
| Cliques | O preview administrativo foi explicitamente desabilitado, evitando aparência de controle funcional. O botão de certificado bloqueado já estava desabilitado. | `AdminTabs.kt`, `IbrScreen.kt` |
| Wrapper de build | O `gradle-wrapper.jar` versionado estava corrompido e o `gradlew` usava invocação incompatível; ambos foram reparados para permitir execução do Gradle 8.9. | `gradle/wrapper/gradle-wrapper.jar`, `gradlew` |

## Problemas encontrados

Foram encontrados nomes legados nas regras Firestore, regra recursiva que liberava leitura pública de qualquer documento, sincronização de membros que ignorava listas vazias, listeners sem ciclo de vida uniforme, uso de `GlobalScope`, persistência periódica agressiva, ações vazias na tela de configurações, confirmação otimista de pedidos de oração, conclusão artificial de aulas IBR, dados demo com SoundHelix/ElephantsDream, mapeamento incorreto de tradução bíblica, exposição de stacktrace na produção e reset de abas administrativas durante a inicialização.

A função `loadTeamMembersFromFirebase()` continua sendo um ponto legado vazio; a fonte efetiva de equipe permanece o listener de `equipe` em `loadContentFromFirebase()`. Isso evita duas fontes ativas, mas recomenda-se uma refatoração posterior para renomear a rotina de boot e remover a chamada vazia.

## Regras Firestore

As regras locais foram alinhadas às coleções encontradas no Kotlin. Conteúdo público permanece legível; `acessos_pendentes` é administrativo; pedidos de oração permitem criação autenticada, mas leitura, atualização e exclusão ficam administrativas; documentos de usuários e subcoleções são limitados ao proprietário ou administrador.

A comparação com as regras implantadas não pôde ser concluída porque o ambiente não possui o Firebase CLI autenticado (`firebase: command not found`). Portanto, a publicação das regras deve ser feita somente após comparar o arquivo local com o projeto `mic-rhema` em ambiente autorizado.

## Validação da Bíblia

Foi criado `BibleDatasetValidator`, sem inserir novos textos bíblicos. A ARA foi marcada como indisponível porque não há fonte local identificada como ARA licenciada; o asset `pt_aa.json` não é mais usado como substituto. O asset NVI também não está presente no repositório atual, portanto deve ser fornecido por fonte autorizada antes de ser disponibilizado. A validação efetiva dos datasets depende da execução Android ou de um leitor JSON equivalente; a rotina já reporta quantidade de livros, capítulos inválidos, capítulos vazios, versículos vazios e a quantidade específica de Juízes.

## Builds exigidos

| Comando | Resultado |
|---|---|
| `./gradlew clean --no-daemon` | **Sucesso** |
| `./gradlew assembleDebug --no-daemon` | Não concluído: o ambiente não possui Android SDK nem `ANDROID_HOME`/`ANDROID_SDK_ROOT`. |
| `./gradlew assembleRelease --no-daemon` | Não concluído pelo mesmo bloqueio de Android SDK. |

O Gradle 8.9 foi baixado com sucesso após a correção do wrapper. O bloqueio ocorreu antes da compilação Kotlin/Java, na resolução do SDK Android, portanto não foi possível afirmar que o código compila integralmente neste ambiente.

## Dependências externas pendentes

A disponibilização de ARA e NVI exige fonte e licença autorizadas. A comparação e eventual implantação das regras exige Firebase CLI e autorização no projeto correspondente. A validação final de release exige Android SDK instalado e configurado. O progresso real de vídeo e áudio ainda requer callbacks de reprodução do player para registrar aproximadamente 90% ou outra regra consistente; nesta entrega foi removida a conclusão falsa por toque, mas a conclusão automática por consumo real ainda depende da integração do callback do player.

## Arquivos modificados

`firestore.rules`, `MainActivity.kt`, `Data.kt`, `PrayerScreen.kt`, `SettingsScreen.kt`, `IbrScreen.kt`, `LocalBibleFetcher.kt`, `BibleFetcher.kt`, `BibleDatasetValidator.kt`, `CrashHandler.kt`, `AdminTabs.kt`, `gradlew` e `gradle/wrapper/gradle-wrapper.jar`.
