# Proposta editorial para as Notícias Bíblicas do MIC Rhema

**Status:** auditoria editorial aplicada ao código em commit separado; o relatório continua fora do repositório para revisão. Os títulos das 30 notícias atuais foram reformulados, o catálogo de 45 pautas novas foi criado e a Central passou a exibir categorias e níveis com contagem de matérias.

## 1. Resumo executivo

A base atual possui **30 notícias bíblicas estáticas**, com títulos em caixa alta e textos curtos, geralmente entre 26 e 40 palavras. O formato chama atenção, mas em alguns casos usa exageros, termos imprecisos ou detalhes que não aparecem diretamente no texto bíblico. O maior problema editorial não é falta de impacto; é falta de contexto. Para prender o usuário e, ao mesmo tempo, preservar a credibilidade da igreja, o ideal é substituir o clickbait enganoso por **títulos de curiosidade honesta**: promessas fortes, perguntas dramáticas e reviravoltas reais da narrativa, sem inventar fatos.

Também havia um conflito de navegação. O detalhe da notícia mostrava a referência como **NTLH**, mas o clique em “Ir pra história” enviava o usuário para a versão **ARA**. Esse fluxo já foi corrigido no app: o botão agora se chama **“Ir Para a História”** e abre o leitor bíblico nativo do MIC Rhema na NTLH, sem WebView externa. A mesma estrutura permite consultar as nove versões atualmente disponíveis no aplicativo.

A Central de Notícias agora ordena por `publishedAt`, usa `id` como fallback para registros antigos e carrega o conteúdo em páginas. A Home mostra somente cinco destaques, enquanto “Ver todas” abre a Central com busca, filtros e contagem por categoria.

## 2. Inventário quantitativo atual

| Medida | Resultado |
|---|---:|
| Notícias cadastradas | 30 |
| IDs duplicados | 0 |
| Referência exata repetida | Nenhuma |
| Mesmo capítulo com episódios diferentes | Mateus 2: notícias 17 e 30 |
| Mesmo arco narrativo distribuído em capítulos diferentes | Jonas 1–3: notícias 20 e 22 |
| Tamanho médio aproximado dos textos | 32 palavras |
| Títulos em caixa alta | 30 de 30 |
| Histórias com possível exagero ou imprecisão editorial | Várias, especialmente 9, 10, 11, 13, 15, 19, 20, 28 e 30 |

### Duplicidade narrativa principal

As notícias **20** e **22** não repetem o mesmo versículo, mas repetem o mesmo arco de Jonas: fuga, peixe, pregação e arrependimento de Nínive. Para o usuário, elas podem parecer duas notícias da mesma história. Recomendo unificá-las em uma matéria maior sobre o arco completo de Jonas, ou manter somente uma delas na vitrine principal e transformar a outra em continuação relacionada.

As notícias **17** e **30** usam Mateus 2, mas não são duplicadas: uma trata da violência ordenada por Herodes e a outra da visita dos magos. Elas podem permanecer, desde que recebam títulos e imagens visualmente diferentes e sejam apresentadas como episódios distintos do mesmo contexto.

As notícias **1** e **26** usam Êxodo, mas tratam da travessia do mar e do maná; as notícias **2** e **5** usam 1 Reis, mas tratam do altar de Elias e da provisão junto ao ribeiro. Também não são duplicadas.

## 3. Auditoria notícia por notícia

A coluna “ação” registra a decisão editorial aplicada antes da publicação. Os títulos propostos abaixo foram gravados no catálogo atual, sem caixa alta integral e sem continuações artificiais por “Parte 1, 2, 3”.

| ID | Referência | Título atual | Problema principal | Título proposto |
|---:|---|---|---|---|
| 1 | Êxodo 14:21 | O MAR SE ABRIU AO MEIO! | Impactante, mas curto e sem contexto sobre o risco da fuga. | **Sem saída diante do mar: a noite em que um povo inteiro precisou avançar** |
| 2 | 1 Reis 18:38 | FOGO CONSUMIU O ALTAR ENCHARCADO DE ÁGUA! | Bom gancho; precisa explicar o confronto e evitar espetáculo vazio. | **Doze pedras, água por toda parte e um fogo que ninguém conseguiu explicar** |
| 3 | Josué 10:13 | O SOL PAROU DURANTE A BATALHA! | A formulação apresenta uma interpretação física como fato simples. | **A oração feita no meio da guerra que mudou o rumo da batalha** |
| 4 | 1 Samuel 17:49 | O GIGANTE DERRUBADO COM UMA PEDRA! | Forte, mas a história é reduzida apenas ao golpe final. | **Um jovem sem armadura enfrentou o guerreiro que aterrorizava um exército** |
| 5 | 1 Reis 17:6 | ALIMENTADOS POR CORVOS NO DESERTO! | O local é um ribeiro, não necessariamente um deserto; falta a crise da seca. | **Quando a crise fechou as portas, a provisão chegou por um caminho improvável** |
| 6 | Êxodo 3:2 | A SARÇA QUE PEGAVA FOGO MAS NÃO SE CONSUMIA! | Correto, mas falta o chamado de Moisés e a mudança de vida. | **Ele só parou para olhar o fogo — e saiu dali com uma missão impossível** |
| 7 | Atos 5:1 | O CASAL QUE MENTIU E CAIU MORTO NA IGREJA! | Sensacionalista e pode parecer ameaça; precisa explicar a fraude e a gravidade da hipocrisia. | **A oferta parecia generosa, mas havia uma mentira escondida por trás dela** |
| 8 | Juízes 16:28 | O HOMEM MAIS FORTE E SEU FIM TRÁGICO! | “Sedução de Dalila” simplifica e pode deslocar a responsabilidade moral. | **Sansão tinha força para vencer inimigos — mas não conseguiu vencer suas próprias escolhas** |
| 9 | 1 Samuel 28:7 | O FANTASMA DE SAMUEL! | “Fantasma” é uma interpretação e transforma um texto complexo em terror. | **Desesperado por respostas, Saul procurou o caminho que Deus havia proibido** |
| 10 | Marcos 6:21 | CABEÇA DECAPITADA NUMA BANDEJA! | Gráfico e exploratório; “dança sedutora” não é afirmado pelo texto. | **Uma promessa feita no calor da festa terminou em uma decisão irreversível** |
| 11 | Oséias 1:2 | O PROFETA QUE CASOU COM UMA PROSTITUTA! | Reduz Gômer a um rótulo e perde a mensagem profética. | **O casamento de Oseias virou o retrato vivo de uma nação que abandonou seu Deus** |
| 12 | 1 Reis 3:16 | O BEBÊ CORTADO AO MEIO? | O rei não pretendia realizar a sentença; era um teste de discernimento. | **A sentença impossível que revelou quem era a verdadeira mãe** |
| 13 | Juízes 3:12 | O REI OBESO E A ESPADA PERDIDA! | Body shaming e foco gore; a estratégia é o elemento narrativo. | **A espada escondida que atravessou a segurança de um rei intocável** |
| 14 | Números 22:28 | O BURRO QUE FALOU! | Curioso, mas precisa mostrar que Balaão não percebia o perigo à frente. | **O animal viu o perigo antes do profeta — e precisou falar para ser ouvido** |
| 15 | Gênesis 19:24 | FOGO DO CÉU DESTRÓI SODOMA E GOMORRA! | “As cidades mais perversas da Terra” é uma generalização; falta o alerta e a fuga. | **A cidade foi avisada, mas a família precisou decidir se ainda valia a pena fugir** |
| 16 | Daniel 5:5 | A MÃO FLUTUANTE QUE ESCREVEU NA PAREDE! | Bom gancho, mas precisa explicar que o aviso era sobre o poder do rei. | **A festa parou quando uma mensagem apareceu na parede do palácio** |
| 17 | Mateus 2:16 | O MASSACRE DOS INOCENTES! | Forte e bíblico, mas requer tratamento sensível e não deve ser usado como choque gratuito. | **O medo de perder o poder fez Herodes transformar Belém em luto** |
| 18 | Juízes 4:17 | A MULHER QUE ASSASSINOU UM GENERAL COM ESTACA! | A expressão “general” é aproximada; a violência deve ser contextualizada. | **Ele achou que havia encontrado abrigo — mas a tenda escondia o fim da guerra** |
| 19 | Atos 20:9 | O SOLDADO QUE DORMIU NO CULTO E MORREU! | Erro factual: Êutico é apresentado como um jovem, não um soldado. | **Uma janela, uma pregação longa e o susto que interrompeu a reunião** |
| 20 | Jonas 1:17 | O HOMEM QUE SOBREVIVEU 3 DIAS DENTRO DE UM PEIXE! | A história é uma parte do arco que se repete com a notícia 22. | **Jonas tentou fugir da missão — e acabou orando no lugar mais improvável** |
| 21 | João 6:9 | O MENINO QUE ALIMENTOU UMA MULTIDÃO COM SEU LANCHE! | Linguagem atual funciona, mas “lanche” pode diminuir a dimensão do sinal. | **Cinco pães, dois peixes e uma pergunta: o que ainda pode ser entregue?** |
| 22 | Jonas 3:5 | UMA CIDADE INTEIRA SE ARREPENDEU APÓS AVISO DE UM ÚNICO HOMEM! | Repetição narrativa com a notícia 20; deve virar continuação ou ser unificada. | **Nínive recebeu um prazo de quarenta dias — e a cidade inteira precisou reagir** |
| 23 | João 9:7 | CEGO DE NASCENÇA PASSA A VER APÓS LAVAR OS OLHOS NO TANQUE! | O título revela a conclusão, mas o conflito com os líderes religiosos é omitido. | **Ele nasceu sem enxergar; depois do milagre, o problema passou a ser provar a verdade** |
| 24 | Atos 16:26 | PRISÃO TREME, CORRENTES CAEM E CARCEREIRO É SALVO! | Bom, mas pode explorar a decisão do carcereiro e a mudança de sua casa. | **O terremoto abriu as portas da prisão — mas a decisão mais importante veio depois** |
| 25 | Marcos 5:27 | A MULHER QUE TOCOU NA ORLA DO MANTO E FOI CURADA NO MEIO DA MULTIDÃO! | Muito longo e entrega tudo; falta o custo de doze anos de sofrimento. | **Doze anos invisível no meio da multidão — até que ela tocou em Jesus** |
| 26 | Êxodo 16:14 | DEUS FAZ CHOVER PÃO DO CÉU TODOS OS DIAS NO DESERTO! | Precisa abordar dependência diária e limites, não apenas o espetáculo. | **A comida chegava pela manhã — mas o povo precisava aprender a confiar dia após dia** |
| 27 | Atos 12:7 | UM ANJO ARRANCA O APÓSTOLO DA PRISÃO ENQUANTO OS GUARDAS DORMIAM! | Bom gancho; “arranca” é informal demais e a oração da igreja é omitida. | **Enquanto a igreja orava, Pedro dormia algemado — até que a cela se encheu de luz** |
| 28 | 2 Reis 2:11 | A CARRUAGEM DE FOGO QUE LEVOU O PROFETA VIVO PARA O CÉU! | O texto distingue o carro de fogo do redemoinho que levou Elias. | **Eliseu viu o mestre partir — e recebeu a responsabilidade de continuar a missão** |
| 29 | João 8:7 | A MULTIDÃO QUERIA APEDREJÁ-LA, MAS ELE DISSE: “QUEM NÃO TEM PECADO...” | A citação parcial domina o título; precisa equilibrar graça, verdade e responsabilidade. | **Todos tinham pedras nas mãos; Jesus fez cada pessoa encarar a própria consciência** |
| 30 | Mateus 2:9 | A ESTRELA MISTERIOSA QUE GUIOU REIS DO ORIENTE ATÉ UMA MANJEDOURA! | Mateus menciona uma casa, não uma manjedoura; há mistura com Lucas 2. | **Eles seguiram uma estrela por uma longa viagem — e encontraram um rei em uma casa simples** |

## 4. O que deve mudar no conteúdo

A estrutura atual guarda apenas um texto curto em `content`. Para atender ao pedido de matérias médias e bem elaboradas, a entidade deveria evoluir para campos separados:

| Campo sugerido | Função editorial |
|---|---|
| `title` | Título de curiosidade honesta, sem revelar toda a conclusão |
| `summary` | Duas frases para o card da Home e da lista |
| `content` | Matéria principal de aproximadamente 120–220 palavras |
| `context` | Cenário histórico e literário, sem afirmar detalhes que o texto não afirma |
| `turningPoint` | A virada da história, em linguagem clara |
| `todayApplication` | Aplicação para o cotidiano atual, sem transformar a narrativa em previsão específica |
| `consequences` | O que a Bíblia mostra como consequência das decisões dos personagens |
| `book`, `chapter`, `verse` | Referência para abertura na versão escolhida |
| `imageUrl` | Imagem temática sem texto ilegível ou cenas gráficas desnecessárias |
| `publishedAt` | Ordenação real por data de publicação |
| `tags` | Filtros como coragem, justiça, família, perdão, poder e esperança |

O tamanho recomendado é de **120 a 220 palavras por matéria**. A primeira parte deve apresentar um gancho; a segunda deve contar o episódio com contexto; a terceira deve explicar a virada; e a conclusão deve trazer a pergunta para o presente. A aplicação moderna precisa ser marcada como aplicação, não como se fosse uma profecia direta sobre o século XXI.

### Exemplo de matéria no novo padrão

**Título:** *A sentença impossível que revelou quem era a verdadeira mãe*

**Resumo:** Duas mulheres chegaram ao tribunal do rei Salomão dizendo ser mães do mesmo bebê. Sem testemunhas capazes de resolver a disputa, o rei propôs uma decisão extrema — e a reação de uma delas revelou uma verdade que ninguém conseguia provar.

**Matéria:** A disputa chegou até Salomão em um cenário sem exames, registros civis ou investigação técnica que pudesse confirmar a maternidade. As duas mulheres apresentavam versões incompatíveis, e o bebê era o único elemento que não podia falar por si. Salomão então propôs dividir a criança, não como uma ordem que pretendia executar, mas como um teste para revelar qual das duas preferia perder a própria reivindicação a ver o filho morrer. A verdadeira mãe imediatamente abriu mão do direito de ficar com a criança para que ela permanecesse viva. A outra, porém, aceitou a divisão. A decisão de Salomão expôs que a maternidade não seria identificada por quem gritasse mais alto, mas por quem estivesse disposta a proteger a vida. A história continua atual porque decisões difíceis revelam prioridades. Em conflitos familiares, comunitários ou profissionais, a pergunta não é apenas “quem está certo?”, mas também “quem está disposto a preservar o que é mais importante?”.

**Aplicação atual:** A Bíblia não apresenta esse episódio como autorização para violência ou como método jurídico moderno. O ponto narrativo é o discernimento diante de uma disputa sem testemunhas e a diferença entre posse e cuidado.

**Referência contemporânea:** abrir 1 Reis 3 na versão `NTLH` dentro do leitor bíblico nativo do MIC Rhema.

## 5. Nova pauta recomendada, sem repetir as 30 histórias atuais

Abaixo está uma seleção editorial de histórias adicionais. Ela não pretende afirmar que estes relatos são previsões literais de eventos atuais; a seleção considera temas capazes de provocar reflexão no século XXI: poder, corrupção, desigualdade, saúde emocional, família, justiça, tecnologia como metáfora de comunicação, influência social e responsabilidade.

| Pauta | Referência | Título de curiosidade honesta | Tema contemporâneo | Consequência a explicar |
|---|---|---|---|---|
| José vendido pelos irmãos | Gênesis 37; 45 | **A família que vendeu o próprio irmão — e depois precisou pedir ajuda a ele** | inveja, abuso de confiança e reconciliação | A inveja rompe relações; a restauração exige verdade e mudança |
| José na prisão | Gênesis 39–41 | **Ele foi acusado injustamente, ficou esquecido e acabou diante do homem mais poderoso do país** | injustiça e perseverança | Uma acusação pode atrasar a vida, mas caráter e responsabilidade continuam relevantes |
| Tamar e Judá | Gênesis 38 | **Quando uma mulher invisível expôs a incoerência de um líder** | abuso de poder, hipocrisia e justiça | O poder não elimina responsabilidade; a verdade pode revelar dois pesos e duas medidas |
| Rute e Noemi | Rute 1–4 | **Sem dinheiro, sem proteção e longe de casa: duas mulheres recomeçaram do zero** | migração, luto e rede de apoio | Solidariedade e lealdade podem abrir caminhos de dignidade |
| Ana e sua oração | 1 Samuel 1 | **Todos achavam que Ana estava exagerando — até sua dor ganhar um nome** | sofrimento emocional e estigma religioso | A dor não deve ser ridicularizada; acolhimento é parte da vida comunitária |
| Davi e Bate-Seba | 2 Samuel 11–12 | **O rei usou o poder para esconder um erro — e a verdade bateu à porta** | abuso de poder e encobrimento | Decisões de quem governa atingem outras vidas; arrependimento não apaga consequências automaticamente |
| Natã confronta Davi | 2 Samuel 12 | **O profeta contou uma história simples e fez o rei condenar a si mesmo** | responsabilidade de líderes | Liderança saudável precisa aceitar correção e reparar danos |
| Absalão e a crise familiar | 2 Samuel 13–18 | **A ferida dentro da família virou uma guerra pelo trono** | trauma, vingança e polarização | Dor não tratada e vingança podem transformar uma casa em campo de batalha |
| Elias em esgotamento | 1 Reis 19 | **Depois do grande milagre, o profeta pediu para morrer** | burnout e saúde emocional | Coragem pública não elimina cansaço; descanso, alimento, escuta e direção importam |
| Nabote e a vinha | 1 Reis 21 | **O rei queria um terreno; a máquina do poder fabricou uma acusação** | corrupção e apropriação indevida | Quando poder e mentira se unem, pessoas comuns pagam a conta |
| Naamã e o orgulho | 2 Reis 5 | **Ele tinha status, dinheiro e influência — mas precisou obedecer a uma instrução simples** | orgulho e vulnerabilidade | Nem todo problema se resolve com posição; humildade pode abrir caminho para mudança |
| Ezequias diante da ameaça | 2 Reis 18–19 | **A carta de ameaça chegou ao palácio, mas o rei fez outra coisa com ela** | medo coletivo e crise | A resposta à ameaça precisa combinar oração, estratégia e responsabilidade |
| Josias e o livro esquecido | 2 Reis 22–23 | **Um texto antigo foi encontrado no templo — e o rei descobriu que o país estava vivendo errado** | memória, revisão de valores e reforma | Mudança começa quando uma comunidade confronta a distância entre discurso e prática |
| Neemias e os muros | Neemias 1–6 | **A cidade estava quebrada, mas a reconstrução começou com uma conversa e um plano** | liderança comunitária e reconstrução | Obras duradouras precisam de visão, organização, cooperação e resistência à distração |
| Ester diante do risco | Ester 4–7 | **Ela chegou ao palácio sem poder real — e precisou decidir se ficaria em silêncio** | coragem cívica e defesa dos vulneráveis | O silêncio de quem tem acesso pode permitir que a injustiça avance |
| Daniel na cova dos leões | Daniel 6 | **O decreto dizia para ele parar de orar; Daniel decidiu não esconder sua fé** | pressão institucional e integridade | Convicções têm custo; integridade não deve ser confundida com agressividade |
| Três jovens diante do fogo | Daniel 3 | **Eles não sabiam se seriam salvos, mas se recusaram a adorar por medo** | pressão social e coerência | A convicção não é garantia de conforto, mas revela quem controla as escolhas |
| Jó perde tudo | Jó 1–2; 38–42 | **Em um dia, Jó perdeu bens, segurança e respostas — e seus amigos pioraram a dor** | luto, sofrimento e respostas fáceis | Nem todo sofrimento é explicado por culpa pessoal; presença pode ser melhor que discursos |
| Pedro nega Jesus | Lucas 22; João 21 | **Ele prometeu que nunca falharia — e depois negou conhecer Jesus três vezes** | culpa e restauração | Um fracasso grave não precisa ser a última palavra, mas restauração envolve verdade e responsabilidade |
| Bom samaritano | Lucas 10:25–37 | **Três pessoas viram um homem ferido; só uma decidiu parar** | indiferença e cuidado | Compaixão custa tempo, recursos e proximidade |
| Filho pródigo | Lucas 15:11–32 | **Ele queimou o futuro em pouco tempo — e voltou para casa sem saber se seria recebido** | família, vício em consumo e reconciliação | Perdão não significa negar a ruptura; significa abrir caminho para mudança e responsabilidade |
| Zaqueu | Lucas 19:1–10 | **O homem rico que todos odiavam decidiu devolver muito mais do que havia tomado** | corrupção, dinheiro e reparação | Arrependimento bíblico aparece também na forma como o dano é reparado |
| Jesus acalma a tempestade | Marcos 4:35–41 | **A tempestade cresceu enquanto Jesus dormia — e os discípulos acharam que estavam sozinhos** | ansiedade e medo | Fé não elimina tempestades, mas muda a forma de atravessá-las |
| Lázaro | João 11 | **Quando Jesus chegou, a família já achava que era tarde demais** | luto, esperança e limites humanos | Esperança cristã não apaga o luto; ela o atravessa com presença e promessa |
| Maria Madalena no túmulo | João 20 | **Ela foi ao túmulo para chorar e voltou com a notícia que mudou a história** | testemunho e dignidade das mulheres | Pessoas ignoradas podem carregar a mensagem mais importante |
| Paulo no caminho de Damasco | Atos 9 | **O perseguidor saiu para prender pessoas e voltou contando que havia sido confrontado** | radicalização e transformação | Mudança real altera comportamento, relacionamentos e missão |
| Pentecostes | Atos 2 | **Pessoas de muitos lugares ouviram a mesma mensagem em suas próprias línguas** | comunicação, diversidade e comunidade | Unidade não exige apagar diferenças; exige comunicação compreensível e propósito comum |
| Barnabé acolhe Saulo | Atos 9:26–28 | **Ninguém confiava no novo convertido — até que alguém decidiu dar-lhe uma chance** | reinserção e reputação | Perdão comunitário precisa de discernimento, mas ninguém muda sozinho |
| Tiago e a língua | Tiago 3:1–12 | **Uma pequena fala pode incendiar uma comunidade inteira** | redes sociais, fofoca e discurso público | Palavras podem destruir reputações, famílias e ambientes de fé |
| Ananias e Safira | Atos 5:1–11 | **Quando a imagem de generosidade vale mais do que a verdade** | performance social e transparência | A busca por parecer melhor do que se é destrói confiança |

## 6. Regras para títulos chamativos sem clickbait enganoso

O título deve criar uma pergunta real, não uma promessa falsa. É válido usar contraste, urgência narrativa e consequência, como “Ele tinha poder, mas não conseguiu esconder o erro”. Não é válido inventar fatos, afirmar que a Bíblia prevê uma notícia contemporânea específica ou usar uma citação bíblica fora do contexto apenas para causar choque.

Recomendo abandonar a caixa alta integral. Uma composição com capitalização normal, uma palavra em destaque e um subtítulo curto comunica mais profissionalismo e melhora a leitura em telas pequenas. Os cards podem exibir um selo de tema, como **JUSTIÇA**, **CORAGEM**, **FAMÍLIA**, **MILAGRE**, **PODER** ou **RECOMEÇO**, além do tempo estimado de leitura.

A matéria deve terminar com uma pergunta de reflexão e uma ação clara, por exemplo: **“Ir Para a História”**, **“Veja a referência em contexto”** ou **“Compartilhe com alguém que precisa refletir sobre isso”**. A referência deve ser aberta automaticamente na **NTLH** dentro do leitor nativo do MIC Rhema, não na ARA, quando o usuário tocar em “Ir Para a História”.

## 7. Mudanças técnicas recomendadas

A correção de navegação já foi aplicada em `NewsScreen.kt`: o callback envia `"NTLH"`, o texto do botão informa a versão e o destino é o leitor nativo. A Central já usa `publishedAt` com fallback para `id`, paginação e contagem por categoria.

O modelo `BibleNews` já foi estendido com `summary`, `tags`, `publishedAt`, `category`, `intensity`, `contentWarning`, `featured` e `storyKey`. O painel administrativo já permite editar categoria, intensidade, tags, aviso de conteúdo e destaque na Home. A decoração editorial também elimina duplicidades narrativas conhecidas, como o arco de Jonas e Ananias e Safira.

Jonas 1–3 agora aparece como um único arco editorial na Central, em vez de duas matérias concorrentes na mesma lista. Os títulos atuais foram substituídos pelos títulos revisados, e as 29 pautas bíblicas adicionais mais as 16 pautas de Provérbios foram cadastradas no catálogo editorial do app. A chave `storyKey` permanece disponível para impedir novas duplicidades.

## 8. Recomendação de processo

Não recomendo publicar de uma vez dezenas de textos novos sem revisão pastoral. O fluxo mais seguro é: primeiro aprovar a lista editorial; depois revisar cinco matérias-piloto; em seguida implementar o novo modelo de dados e a NTLH automática; por fim, inserir os demais textos em lotes de dez e verificar duplicidade, fidelidade de referência e tom pastoral.

Minha sugestão para o primeiro lote de cinco matérias-piloto é: **O rei usou o poder para esconder um erro** (Davi e Bate-Seba), **Depois do grande milagre, o profeta pediu para morrer** (Elias), **A cidade estava quebrada, mas a reconstrução começou com um plano** (Neemias), **O homem rico que decidiu reparar o dano** (Zaqueu) e **Uma pequena fala pode incendiar uma comunidade inteira** (Tiago 3). Esse conjunto testa poder, saúde emocional, liderança, dinheiro e comunicação — temas muito relevantes no século XXI.

## 9. Referências bíblicas e editoriais

As referências devem ser conferidas no texto bíblico completo antes da publicação, preferencialmente na tradução que o app abrirá para o leitor. O YouVersion oferece as versões ARA, NVI e NTLH no catálogo de versões e permite abrir capítulos por URL [1] [2] [3]. As correções factuais acima respeitam a distinção entre o que o texto narra diretamente e o que é aplicação editorial contemporânea.

### Referências

[1]: [YouVersion — Almeida Revista e Atualizada (ARA)](https://www.bible.com/pt/versions/1608-ara-almeida-revista-e-atualizada)

[2]: [YouVersion — Nova Versão Internacional em português (NVI)](https://www.bible.com/pt/versions/129-nvi-nova-vers%C3%A3o-internacional-portugu%C3%AAs)

[3]: [YouVersion — Salmos 119 na Nova Tradução na Linguagem de Hoje (NTLH)](https://www.bible.com/pt/bible/211/PSA.119.NTLH)

[4]: [YouVersion — Termos de Uso](https://www.bible.com/pt/terms)


## 10. Linha editorial de notícias atuais baseada em Provérbios

Provérbios funciona muito bem como uma lente de leitura para notícias atuais. A matéria não deve dizer que o provérbio “previu” um caso contemporâneo. O formato correto é apresentar um fato verificável do presente e, em seguida, mostrar como o princípio de Provérbios ajuda o leitor a refletir sobre causas, escolhas, consequências e responsabilidade.

A estrutura recomendada é: **fato atual**, **pergunta que desperta curiosidade**, **princípio de Provérbios**, **o que a decisão produz**, **como agir hoje** e **link para o capítulo na NTLH**. O texto pode ter de 120 a 220 palavras, com uma fonte jornalística identificada quando falar de um acontecimento real. O provérbio deve ser apresentado como sabedoria bíblica e aplicação ética, não como prova de que determinado evento foi anunciado pela Bíblia.

| Tema atual | Referências em Provérbios | Título de curiosidade honesta | Como desenvolver a notícia |
|---|---|---|---|
| Fake news, fofoca e reputações destruídas | Pv 11:13; 12:22; 15:1, 4, 28; 18:21 | **Uma mensagem de poucos segundos pode destruir uma reputação construída em anos** | Apresentar um caso de desinformação, explicar como a informação foi compartilhada e mostrar a diferença entre verificar, comentar e espalhar. |
| Influenciadores e opinião pública | Pv 14:15; 15:2; 18:2 | **Todo mundo tem opinião — mas quem está conferindo os fatos?** | Comparar reação impulsiva com discernimento, sem atacar uma pessoa específica nem transformar o provérbio em condenação automática das redes sociais. |
| Golpes financeiros e promessa de dinheiro fácil | Pv 11:1, 4, 6; 13:11; 22:7; 28:6 | **A promessa de dinheiro rápido esconde uma conta que alguém vai pagar** | Explicar sinais de fraude, endividamento e ganância; terminar com orientação prática para verificar contratos e não comprometer a família. |
| Corrupção e suborno | Pv 15:27; 17:23; 29:4 | **Quando um favor por fora começa a custar caro para toda a cidade** | Apresentar o fato documentado, mostrar quem foi afetado e separar acusação comprovada de suspeita ainda em investigação. |
| Liderança e governo | Pv 11:14; 16:12; 29:2, 4 | **Uma cidade muda quando seus líderes escutam — ou quando ninguém pode corrigi-los** | Analisar decisões públicas, transparência, conselhos e impacto sobre os mais vulneráveis, evitando partidarismo. |
| Trabalho, preguiça e precarização | Pv 6:6–11; 10:4; 12:11, 24 | **O problema é falta de esforço, falta de oportunidade ou os dois?** | Usar Provérbios para falar de diligência sem culpar pessoas desempregadas; incluir qualificação, oportunidades e responsabilidade de empregadores. |
| Ansiedade e sobrecarga emocional | Pv 12:25; 14:30; 17:22 | **A preocupação que ninguém vê também pode adoecer uma pessoa** | Contar uma história real com cuidado, apresentar apoio comunitário e lembrar que conselho bíblico não substitui atendimento profissional. |
| Raiva, violência e conflitos familiares | Pv 14:29; 15:1, 18; 16:32; 19:11 | **Uma resposta atravessada pode acender uma briga que ninguém consegue apagar** | Mostrar a escalada de um conflito e oferecer alternativas de autocontrole, conversa segura e busca de ajuda em situações de violência. |
| Justiça e defesa dos vulneráveis | Pv 14:31; 19:17; 21:13; 31:8–9 | **Quem não tem voz aparece na notícia — mas será que alguém está ouvindo?** | Dar espaço a dados, testemunhos e respostas concretas; evitar transformar sofrimento em espetáculo. |
| Desigualdade e consumo | Pv 11:24–26; 22:2, 7; 30:8–9 | **Enquanto uns acumulam, outros não conseguem chegar ao fim do mês** | Apresentar o problema com dados e mostrar generosidade, responsabilidade e justiça sem romantizar pobreza. |
| Família, filhos e formação de caráter | Pv 15:20; 17:1; 22:6; 29:15 | **O que as crianças estão aprendendo quando os adultos acham que elas não estão olhando?** | Relacionar hábitos familiares, telas, linguagem e disciplina; evitar culpabilizar pais sem considerar contexto. |
| Relacionamentos, consentimento e infidelidade | Pv 5:1–14; 6:20–35; 7 | **Uma conversa escondida pode parecer pequena — até revelar um estrago enorme** | Tratar o tema sem exposição de pessoas reais, explicar consequências de quebra de confiança e reforçar respeito, limites e responsabilidade. |
| Orgulho, fama e queda pública | Pv 16:18; 18:12; 27:2 | **A fama subiu rápido — e a queda começou quando ninguém mais podia corrigir** | Analisar casos de reputação e liderança, distinguindo erro, acusação e condenação definitiva. |
| Decisões e conselhos antes de agir | Pv 15:22; 20:18; 24:6 | **A decisão parecia óbvia até alguém fazer a pergunta que faltava** | Mostrar como ouvir especialistas e pessoas afetadas reduz erros, especialmente em saúde, finanças e gestão pública. |
| Tecnologia, vigilância e privacidade | Pv 15:3; 16:2; 21:2 | **Quem está vendo seus dados — e o que suas escolhas revelam sobre você?** | Fazer uma aplicação ética sobre transparência, intenção e responsabilidade digital, sem afirmar que Provérbios trata diretamente de tecnologia. |
| Boas notícias em meio à crise | Pv 15:30; 25:25 | **Em uma semana de más notícias, uma decisão simples devolveu esperança a uma comunidade** | Criar uma editoria de soluções e testemunhos verificáveis, equilibrando o foco em tragédias com ações que produzem bem. |

### Modelo de notícia contemporânea baseada em Provérbios

**Título:** *Todo mundo compartilhou — mas quem conferiu os fatos?*

**Resumo:** Uma informação falsa se espalhou rapidamente e atingiu a reputação de uma pessoa antes que os fatos fossem verificados. O caso mostra como a velocidade de uma mensagem pode ser maior que a capacidade de reparar o dano.

**Matéria:** A notícia deve começar apresentando o que foi confirmado, o que ainda está sob investigação e quem foi afetado. Em seguida, o texto pode relacionar o caso ao princípio de Provérbios sobre discrição, verdade e cuidado com as palavras. A aplicação não deve afirmar que o capítulo descreve a internet, mas mostrar que o problema humano — transmitir algo sem responsabilidade — continua reconhecível em qualquer época. A conclusão deve orientar o leitor a conferir a fonte, não compartilhar acusações sem prova, corrigir publicamente um erro e procurar ajuda quando uma reputação estiver sendo atacada. O link abre Provérbios 11 ou 15 na NTLH dentro do leitor bíblico nativo do MIC Rhema.

**Pergunta para o usuário:** *Antes de encaminhar uma notícia, você está ajudando a esclarecer ou apenas aumentando o barulho?*

## 11. Regras pastorais e factuais para essa editoria

A editoria de Provérbios deve evitar três erros. O primeiro é usar um versículo como sentença contra uma pessoa antes de os fatos serem comprovados. O segundo é reduzir problemas complexos — como pobreza, ansiedade, violência ou desemprego — a falta de fé ou falta de esforço. O terceiro é transformar o livro em uma coleção de frases motivacionais desconectadas do contexto literário.

Cada matéria deve indicar quando está tratando de **fato jornalístico**, **interpretação editorial** e **aplicação bíblica**. Em casos de saúde mental, violência, finanças ou acusações criminais, a notícia deve incluir orientação responsável e não apresentar a reflexão bíblica como substituta de atendimento médico, psicológico, jurídico ou de emergência.

A seleção inicial recomendada para a nova editoria é de cinco matérias-piloto: **fake news e reputação** (Provérbios 11 e 15), **dinheiro fácil e golpes** (Provérbios 11 e 22), **ansiedade invisível** (Provérbios 12 e 17), **raiva dentro de casa** (Provérbios 15 e 16) e **liderança que aceita conselhos** (Provérbios 11 e 29). Depois da aprovação pastoral, os textos podem ser inseridos no painel administrativo com as tags `Proverbios`, `Atualidade` e o tema correspondente.

A referência contemporânea deve usar a rota interna do leitor nativo com o código `NTLH`, por exemplo: `PRO.15.NTLH`. O texto é consultado pela API do Bolls e exibido no app, sem menus ou anúncios de uma página externa; a situação de licença de cada tradução deve continuar sendo acompanhada antes de uma distribuição mais ampla.

### Fontes consultadas

As pautas acima foram fundamentadas pela leitura dos capítulos 4, 6, 11 e 15 de Provérbios na NTLH. Esses capítulos tratam, entre outros temas, de sabedoria e escolhas, prevenção e diligência, honestidade e liderança, palavras, raiva, conselhos e boa notícia [5] [6] [7] [8]. A NTLH exibida na fonte consultada possui atribuição de copyright à Sociedade Bíblica do Brasil [5] [6] [7] [8].

[5]: [YouVersion — Provérbios 4 na NTLH](https://www.bible.com/pt/bible/211/PRO.4.NTLH)

[6]: [YouVersion — Provérbios 6 na NTLH](https://www.bible.com/pt/bible/211/PRO.6.NTLH)

[7]: [YouVersion — Provérbios 11 na NTLH](https://www.bible.com/pt/bible/211/PRO.11.NTLH)

[8]: [YouVersion — Provérbios 15 na NTLH](https://www.bible.com/pt/bible/211/PRO.15.NTLH)

## 12. Organização das 500 notícias dentro da aba Notícias Bíblicas

A melhor solução não é apresentar 500 matérias em uma lista única nem classificar as histórias por “importância espiritual”. O ideal é criar uma **Central de Notícias Bíblicas**. Na Home, o usuário vê apenas quatro ou cinco destaques recentes e um botão **“Ver todas”**. Ao tocar nesse botão, ele entra em uma tela própria da aba Notícias Bíblicas, com busca, categorias, filtros e carregamento gradual.

### 12.1 Fluxo recomendado

| Área | Conteúdo | Objetivo |
|---|---|---|
| Home | 4 ou 5 notícias recentes, sendo uma destacada | Despertar interesse sem deixar a página pesada |
| Botão “Ver todas” | Acesso à Central de Notícias Bíblicas | Evitar que a Home tenha uma lista interminável |
| Central de notícias | Categorias, busca e filtros | Ajudar o usuário a encontrar uma história específica |
| Lista filtrada | Cards leves com imagem, título, resumo, referência e selo | Carregar somente o necessário para navegar |
| Detalhe | Texto completo, aplicação atual, referência e botão NTLH | Abrir o conteúdo pesado apenas quando solicitado |

A lista deve usar `LazyColumn`, mostrar inicialmente cerca de 20 matérias e carregar o próximo lote somente quando o usuário se aproximar do final. O texto completo não deve ser carregado na Home. A imagem deve usar miniatura com dimensões controladas e cache; o card deve mostrar apenas título, resumo curto, referência e categoria.

### 12.2 Em vez de “gravidade”, usar intensidade narrativa

A palavra “gravidade” pode sugerir que uma história é mais importante para Deus do que outra ou pode transformar tragédias em entretenimento. Recomendo usar **“intensidade narrativa”** como filtro editorial. Ela descreve o tipo de experiência de leitura, não o valor espiritual do acontecimento.

| Nível | Nome exibido | Característica | Exemplos |
|---:|---|---|---|
| 1 | **Para refletir** | Histórias de sabedoria, cuidado, confiança e decisões cotidianas | Rute e Noemi, Salomão, Provérbios, o bom samaritano |
| 2 | **Conflitos e escolhas** | Dilemas familiares, crises pessoais, tentação, coragem e decisões difíceis | José e os irmãos, Davi e Bate-Seba, Ester, Pedro |
| 3 | **Confrontos e consequências** | Abuso de poder, injustiça, corrupção, violência ou decisões com resultado doloroso | Nabote, Herodes, Ananias e Safira, Saul |
| 4 | **Grandes reviravoltas** | Milagres, livramentos, sinais, ressurreições e acontecimentos extraordinários | Mar Vermelho, Elias, Daniel, Lázaro, Pentecostes |

O nível não deve aparecer como “nível de fé” ou “nível de importância”. Ele serve apenas para o usuário escolher o tipo de história que deseja ler. Nos níveis 3 e 4, o card pode exibir um aviso discreto, como **“contém violência”**, **“tema sensível”** ou **“luto e sofrimento”**, quando necessário.

### 12.3 Categorias principais para a Central

Além da intensidade narrativa, cada notícia deve possuir uma categoria temática. Assim, uma mesma matéria pode estar em “Conflitos e escolhas” e também em “Família”, sem duplicar o registro.

| Categoria | O que reúne |
|---|---|
| **Milagres e sinais** | Cura, provisão, livramento e acontecimentos extraordinários |
| **Conflitos e escolhas** | Dilemas, tentação, coragem, medo e decisões difíceis |
| **Poder e justiça** | Reis, líderes, corrupção, abuso de autoridade e reparação |
| **Família e relacionamentos** | Traição, reconciliação, luto, cuidado e formação de caráter |
| **Crises e recomeços** | Perdas, exílio, prisão, arrependimento e restauração |
| **Coragem e propósito** | Chamados, missões, testemunho e resistência à pressão |
| **Sabedoria para hoje** | Provérbios, decisões, palavras, dinheiro, trabalho e prudência |
| **Notícias atuais à luz de Provérbios** | Fake news, golpes, ansiedade, violência, liderança e esperança |

Na tela, as categorias podem aparecer como cartões horizontais ou filtros em formato de chips. A primeira opção deve ser **“Tudo”**, seguida por **“Mais recentes”**, **“Mais impactantes”**, **“Para refletir”**, **“Milagres”**, **“Conflitos”**, **“Recomeços”** e **“Provérbios hoje”**.

### 12.4 Como a navegação deve funcionar

O usuário entra na aba e vê uma apresentação simples: **“Encontre uma história bíblica para este momento”**. Logo abaixo, aparece uma busca por título, personagem, livro ou tema. Em seguida, vêm os filtros de intensidade e categorias. Ao tocar em uma matéria, abre-se o detalhe completo; ao tocar em **“Ir Para a História”**, o app abre a referência no leitor bíblico nativo.

A Central não deve renderizar 500 cards ao mesmo tempo. O Firestore deve buscar somente a primeira página, ordenada por `publishedAt` ou `id`, com limite de aproximadamente 20 documentos. Ao chegar perto do fim, o app busca a próxima página usando o último documento como cursor. Se o usuário aplicar uma categoria, a consulta deve ser reiniciada com o filtro selecionado, sem manter centenas de objetos antigos na memória da tela.

### 12.5 Modelo de dados recomendado

O modelo atual possui apenas `id`, `title`, `content`, `book`, `chapter`, `verse` e `imageUrl`. Para organizar o catálogo, a notícia deve evoluir gradualmente para os seguintes campos:

| Campo | Exemplo | Uso |
|---|---|---|
| `id` | `143` | Identificador único |
| `title` | “O rei usou o poder para esconder um erro” | Título do card e detalhe |
| `summary` | “Uma decisão no palácio atingiu uma família inteira” | Card leve da lista |
| `content` | Texto de 120–220 palavras | Detalhe completo |
| `book` | `2 Samuel` | Referência bíblica |
| `chapter` | `11` | Abertura no leitor |
| `verse` | `2` | Versículo de entrada |
| `category` | `Poder e justiça` | Filtro principal |
| `tags` | `corrupção`, `liderança` | Busca e filtros secundários |
| `intensity` | `3` | Nível de intensidade narrativa |
| `contentWarning` | `violência` | Aviso pastoral discreto |
| `publishedAt` | data do cadastro | Ordenação real |
| `imageUrl` | URL da miniatura | Card visual com cache |
| `featured` | `true` ou `false` | Destaques da Home |

Para evitar duplicidade, deve existir uma chave editorial como `storyKey`, por exemplo `jonas-arco-completo`, `mateus-herodes-meninos` ou `proverbios-fake-news-reputacao`. A regra deve impedir que duas matérias sejam cadastradas com a mesma história principal, salvo quando uma delas estiver marcada explicitamente como continuação.

### 12.6 Divisão editorial sugerida para 500 matérias

Uma distribuição inicial poderia organizar as 500 matérias sem criar blocos desequilibrados:

| Coleção | Quantidade aproximada |
|---|---:|
| Para refletir | 80 |
| Conflitos e escolhas | 110 |
| Confrontos e consequências | 75 |
| Grandes reviravoltas | 90 |
| Família e relacionamentos | 55 |
| Poder e justiça | 45 |
| Crises e recomeços | 75 |
| Sabedoria para hoje e Provérbios atuais | 70 |

Esses números são coleções editoriais e podem se sobrepor por meio de tags. Portanto, uma matéria pode aparecer em uma coleção principal e também em um filtro temático sem ser copiada. O banco mantém um registro único; a interface apenas apresenta diferentes caminhos para encontrá-lo.

### 12.7 Recomendação final

Minha recomendação é aprovar a seguinte estrutura: **Home com cinco destaques; botão “Ver todas”; Central de Notícias Bíblicas com busca, intensidade narrativa e categorias; 20 itens por página; detalhe carregado apenas ao abrir; texto completo fora da Home; imagens em cache; e tags para permitir que uma história pertença a mais de uma coleção sem duplicação**.

Assim, as 500 notícias deixam de parecer um arquivo enorme e passam a funcionar como uma biblioteca editorial organizada. O usuário não precisa percorrer tudo: pode escolher **“quero algo para refletir”**, **“quero uma história de coragem”**, **“quero entender um conflito de poder”** ou **“quero uma notícia atual baseada em Provérbios”**.

Esta seção é uma proposta de arquitetura e ainda não altera o aplicativo. A implementação deve ocorrer somente depois da aprovação dos nomes, categorias e níveis.


## 12. Atualização final — Admin, IBR e infraestrutura

### 12.1 Painel administrativo

O painel administrativo recebeu cabeçalho com a identidade visual da igreja, cartões de métricas, ações rápidas, categorias expansíveis com persistência local e filtros para membros e notícias. As categorias permanecem recolhidas por padrão e a preferência do administrador é preservada entre acessos. O acesso administrativo continua usando a senha `igreja10`, sem alteração do fluxo acordado.

### 12.2 Instituto Bíblico Rhema

A área IBR foi reorganizada em visão geral, conteúdo, módulos e certificados. O aluno dispõe de cartão de continuidade, capas visuais, retomada da última aula e persistência do progresso. A experiência de vídeo, áudio e texto foi unificada, os módulos seguem progressão sequencial e os bloqueios exibem explicações visuais. O certificado mostra status, permite compartilhamento e mantém o e-mail destinado exclusivamente ao envio do certificado.

No painel IBR, a lista de cursos possui busca por título ou descrição, filtro por tema, contagem de resultados e ordenação alfabética. Esses controles filtram somente a visualização e não removem nem alteram cursos ou capítulos.

### 12.3 Supabase, Firebase e uploads

Os uploads administrativos de imagens, capas, vídeos, áudios e PDFs utilizam o Supabase Storage por meio do gateway autorizado. O projeto não embute chaves `service_role` no APK nem no GitHub. A autenticação dos membros permanece simples, baseada em nome e telefone, sem SMS obrigatório; a sessão Firebase é vinculada quando necessária para sincronização. O administrador mantém o fluxo de acesso com `igreja10`.

### 12.4 Limpeza de código temporário

Foi removido do `VipAdmin.kt` o cartão de upload em lote que criava aulas fictícias, simulava progresso e usava a URL de demonstração Big Buck Bunny. O painel permanece com os campos reais de upload de capítulos, evitando conteúdo de teste em produção.

### 12.5 Avatares bíblicos

O catálogo de avatares já cadastrados inclui Davi, Ester, Daniel, Rute, Moisés, Noé, Maria, Paulo, Josué, Abraão, Sara, Rebeca, Jacó, José, Samuel, Elias, Isaías, Jeremias e João Batista. Timóteo, Priscila e Lídia continuam pendentes de geração por indisponibilidade temporária da cota diária de imagens; isso não impede a utilização dos avatares existentes nem a persistência da escolha no perfil.

### 12.6 CI/CD e validação

O commit `0ca6775` — `feat: add IBR course search and filters` — foi enviado para `main` e validado pelo GitHub Actions no workflow [31962341410](https://github.com/bichocutela/Mic-RHEMA-Ia-Studio-Google/actions/runs/31962341410), concluído com sucesso. A compilação, limpeza de configurações temporárias, upload do APK, extração da versão e criação da release foram executados sem falhas.

### 12.7 Teste manual final recomendado

No aparelho, o fluxo final deve ser conferido nesta ordem: aprovar um membro, entrar com nome e telefone, abrir um curso IBR, iniciar uma aula, sair e retornar para confirmar a retomada, concluir os capítulos sequenciais, abrir o certificado e testar o compartilhamento por e-mail com anexo. Também é recomendado testar um upload real de imagem e PDF pelo administrador e confirmar no painel o avatar e as informações sincronizadas do membro.
