package com.aistudio.micrhema

/**
 * Expansão leve da Jornada Bíblica.
 *
 * O banco original de 120 perguntas é preservado. As 180 perguntas adicionais ficam
 * serializadas como texto e somente a dificuldade aberta é convertida em objetos,
 * evitando carregar todo o banco extra na memória de uma vez.
 */
object BibleQuizExpansion {
    private val easyQuestions by lazy(LazyThreadSafetyMode.NONE) {
        parse(BibleQuizDifficulty.EASY, EASY_DATA)
    }
    private val mediumQuestions by lazy(LazyThreadSafetyMode.NONE) {
        parse(BibleQuizDifficulty.MEDIUM, MEDIUM_DATA)
    }
    private val hardQuestions by lazy(LazyThreadSafetyMode.NONE) {
        parse(BibleQuizDifficulty.HARD, HARD_DATA)
    }

    fun byDifficulty(difficulty: BibleQuizDifficulty): List<BibleQuizQuestion> = when (difficulty) {
        BibleQuizDifficulty.EASY -> easyQuestions
        BibleQuizDifficulty.MEDIUM -> mediumQuestions
        BibleQuizDifficulty.HARD -> hardQuestions
    }

    private fun parse(difficulty: BibleQuizDifficulty, raw: String): List<BibleQuizQuestion> {
        val questions = raw.trimIndent()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .map { line ->
                val p = line.split('|')
                require(p.size == 12) { "Linha inválida na expansão do quiz: ${p.firstOrNull().orEmpty()}" }
                val correct = p[6].toInt()
                val options = listOf(p[2], p[3], p[4], p[5])
                val reference = p[7]
                BibleQuizQuestion(
                    id = p[0],
                    prompt = p[1],
                    options = options,
                    correctOptionIndex = correct,
                    difficulty = difficulty,
                    hardHint = "Procure o detalhe no contexto de $reference.",
                    easyHint = "A resposta direta é: ${options.getOrNull(correct).orEmpty()}.",
                    bibleReference = reference,
                    explanation = "${options.getOrNull(correct).orEmpty()} é a resposta indicada em $reference.",
                    book = p[8],
                    chapter = p[9].toInt(),
                    verse = p[10].toIntOrNull(),
                    endVerse = p[11].toIntOrNull()
                )
            }
            .toList()

        require(questions.size == 60) { "Cada dificuldade da expansão precisa conter exatamente 60 perguntas." }
        require(questions.map { it.id }.distinct().size == questions.size) { "IDs repetidos na expansão ${difficulty.label}." }
        require(questions.none { question -> BibleQuizCatalog.questions.any { it.id == question.id } }) {
            "A expansão não pode reutilizar IDs do catálogo original."
        }
        val invalid = questions.mapNotNull { question ->
            BibleQuizEngine.validate(question).takeIf { !it.valid }?.let { question.id to it.errors }
        }
        require(invalid.isEmpty()) {
            invalid.joinToString(separator = " | ") { (id, errors) -> "$id: ${errors.joinToString()}" }
        }
        return questions
    }

    private const val EASY_DATA = """
easy_041|Quem era o irmão de Abel que o matou?|Sete|Caim|Enoque|Lameque|1|Gênesis 4:8|Gênesis|4|8|
easy_042|Qual era o nome da esposa de Abraão?|Sara|Rebeca|Raquel|Lia|0|Gênesis 17:15|Gênesis|17|15|
easy_043|Quem se tornou esposa de Isaque?|Miriã|Débora|Rebeca|Rute|2|Gênesis 24:67|Gênesis|24|67|
easy_044|Qual era o nome do irmão gêmeo de Jacó?|Esaú|José|Benjamim|Ismael|0|Gênesis 25:24–26|Gênesis|25|24|26
easy_045|Qual era o nome do irmão de Moisés que também serviu como sacerdote?|Calebe|Arão|Josué|Hur|1|Êxodo 4:14–16|Êxodo|4|14|16
easy_046|Qual era o nome da irmã de Moisés?|Miriã|Ana|Abigail|Débora|0|Êxodo 15:20|Êxodo|15|20|
easy_047|Qual mar se abriu para Israel escapar do Egito?|Mar Morto|Mar da Galileia|Mar Vermelho|Mar Mediterrâneo|2|Êxodo 14:21–22|Êxodo|14|21|22
easy_048|Como era chamado o alimento que caiu do céu no deserto?|Maná|Cevada|Uvas|Figos|0|Êxodo 16:14–15|Êxodo|16|14|15
easy_049|O que guiava Israel durante a noite no deserto?|Uma estrela|Uma coluna de fogo|Uma tocha de bronze|A lua|1|Êxodo 13:21–22|Êxodo|13|21|22
easy_050|Qual mulher escondeu os espias israelitas em Jericó?|Raabe|Rute|Ester|Jael|0|Josué 2:1–6|Josué|2|1|6
easy_051|Quem se casou com Rute em Belém?|Boaz|Obede|Elimeleque|Maalom|0|Rute 4:13|Rute|4|13|
easy_052|Quem chamou Samuel quando ele ainda era menino no santuário?|Saul|Davi|Deus|Eli|2|1 Samuel 3:4–10|1 Samuel|3|4|10
easy_053|Qual era o nome do pai de Davi?|Jessé|Saul|Samuel|Obede|0|1 Samuel 16:1|1 Samuel|16|1|
easy_054|Quem era o grande amigo de Davi, filho do rei Saul?|Abner|Jônatas|Joabe|Natã|1|1 Samuel 18:1–4|1 Samuel|18|1|4
easy_055|Quem era a mãe do rei Salomão?|Mical|Bate-Seba|Abigail|Tamar|1|1 Reis 1:11–13|1 Reis|1|11|13
easy_056|Qual profeta foi levado ao céu em um redemoinho?|Elias|Eliseu|Isaías|Jeremias|0|2 Reis 2:11|2 Reis|2|11|
easy_057|Quem sucedeu Elias como profeta?|Amós|Oséias|Eliseu|Miquéias|2|2 Reis 2:13–15|2 Reis|2|13|15
easy_058|Em qual rio Naamã mergulhou para ser curado?|Jordão|Nilo|Eufrates|Tigre|0|2 Reis 5:10–14|2 Reis|5|10|14
easy_059|Para qual cidade Jonas foi enviado para pregar?|Belém|Nínive|Jericó|Damasco|1|Jonas 1:1–2|Jonas|1|1|2
easy_060|Qual profeta teve a visão de um vale cheio de ossos secos?|Daniel|Ezequiel|Amós|Joel|1|Ezequiel 37:1–10|Ezequiel|37|1|10
easy_061|Quem interpretou sonhos do rei Nabucodonosor?|Daniel|Neemias|Esdras|Mardoqueu|0|Daniel 2:26–28|Daniel|2|26|28
easy_062|Qual livro bíblico conta a história de uma rainha judia na Pérsia?|Rute|Ester|Juízes|Esdras|1|Ester 2:17|Ester|2|17|
easy_063|Como começa a conhecida frase do Salmo 23?|O Senhor é meu pastor|Cantai ao Senhor|Bem-aventurado o homem|Deus é nosso refúgio|0|Salmos 23:1|Salmos|23|1|
easy_064|Qual rei é tradicionalmente associado a muitos provérbios bíblicos?|Salomão|Saul|Acabe|Herodes|0|Provérbios 1:1|Provérbios|1|1|
easy_065|Qual profeta anunciou que uma virgem conceberia e daria à luz um filho chamado Emanuel?|Isaías|Jeremias|Ezequiel|Amós|0|Isaías 7:14|Isaías|7|14|
easy_066|Qual profeta ficou conhecido pela mensagem do vale de ossos secos?|Ezequiel|Jonas|Ageu|Malaquias|0|Ezequiel 37:1–14|Ezequiel|37|1|14
easy_067|Quem era a mãe de João Batista?|Maria|Isabel|Marta|Ana|1|Lucas 1:13|Lucas|1|13|
easy_068|Quem cuidou de Jesus como pai terreno e era carpinteiro?|José|Zacarias|Simeão|Nicodemos|0|Mateus 1:20–24|Mateus|1|20|24
easy_069|Qual menina Jesus ressuscitou após entrar na casa de Jairo?|A filha de Jairo|A filha de Herodes|A filha de Pilatos|A filha de Nicodemos|0|Marcos 5:35–42|Marcos|5|35|42
easy_070|Qual cego chamou Jesus de 'Filho de Davi' perto de Jericó?|Bartimeu|Lázaro|Zaqueu|Tomé|0|Marcos 10:46–52|Marcos|10|46|52
easy_071|Quais eram as duas irmãs de Lázaro?|Marta e Maria|Rute e Noemi|Priscila e Lídia|Ana e Isabel|0|João 11:1–3|João|11|1|3
easy_072|Qual discípulo havia sido cobrador de impostos?|Mateus|André|João|Tiago|0|Mateus 9:9|Mateus|9|9|
easy_073|Qual discípulo era irmão de Pedro?|André|Tomé|Filipe|Judas|0|Mateus 4:18|Mateus|4|18|
easy_074|Quais discípulos eram filhos de Zebedeu?|Tiago e João|Pedro e André|Filipe e Bartolomeu|Mateus e Tomé|0|Mateus 4:21|Mateus|4|21|
easy_075|Na oração ensinada por Jesus, com quais palavras ela começa?|Pai nosso que estás nos céus|Senhor é meu pastor|Santo, santo, santo|Vem, Senhor Jesus|0|Mateus 6:9|Mateus|6|9|
easy_076|Na parábola, quem ajudou o homem ferido na estrada para Jericó?|Um samaritano|Um fariseu|Um sacerdote|Um soldado romano|0|Lucas 10:33–35|Lucas|10|33|35
easy_077|Na parábola do filho pródigo, para quem o filho decidiu voltar?|Para seu pai|Para seu irmão|Para um sacerdote|Para um rei|0|Lucas 15:17–20|Lucas|15|17|20
easy_078|Na parábola da ovelha perdida, quantas ovelhas o pastor deixa para procurar uma que se perdeu?|Noventa e nove|Doze|Setenta|Cinquenta|0|Lucas 15:4|Lucas|15|4|
easy_079|Quem foi obrigado a ajudar Jesus a carregar a cruz?|Simão de Cirene|José de Arimateia|Nicodemos|Bartimeu|0|Lucas 23:26|Lucas|23|26|
easy_080|Como era chamado o lugar onde Jesus foi crucificado?|Gólgota|Getsêmani|Betânia|Siloé|0|João 19:17–18|João|19|17|18
easy_081|Qual governador romano autorizou a crucificação de Jesus?|Pôncio Pilatos|Félix|Festo|Quirino|0|Mateus 27:24–26|Mateus|27|24|26
easy_082|Quem foi escolhido para ocupar o lugar de Judas entre os Doze?|Matias|Barnabé|Silas|Estêvão|0|Atos 1:24–26|Atos|1|24|26
easy_083|O que desceu sobre os discípulos no dia de Pentecostes?|O Espírito Santo|Maná|Uma pomba de ouro|Chuva de pedras|0|Atos 2:1–4|Atos|2|1|4
easy_084|Quem é lembrado como o primeiro mártir cristão em Atos?|Estêvão|Filipe|Barnabé|Timóteo|0|Atos 7:54–60|Atos|7|54|60
easy_085|Qual perseguidor dos cristãos se tornou o apóstolo Paulo?|Saulo|Silas|Apolo|Tito|0|Atos 9:1–6|Atos|9|1|6
easy_086|Quem acompanhou Paulo em sua primeira viagem missionária?|Barnabé|Pedro|Tomé|Mateus|0|Atos 13:2–4|Atos|13|2|4
easy_087|Qual mulher de Filipos vendia púrpura e recebeu Paulo em sua casa?|Lídia|Priscila|Dorcas|Febe|0|Atos 16:14–15|Atos|16|14|15
easy_088|Qual livro narra a conversão de Saulo no caminho de Damasco?|Atos|Romanos|Hebreus|Apocalipse|0|Atos 9:1–9|Atos|9|1|9
easy_089|Quem escreveu o Evangelho que também é autor do livro de Atos?|Lucas|Marcos|Pedro|Paulo|0|Lucas 1:1–4|Lucas|1|1|4
easy_090|Em qual ilha João estava quando recebeu as visões do Apocalipse?|Patmos|Malta|Creta|Chipre|0|Apocalipse 1:9|Apocalipse|1|9|
easy_091|Qual é o versículo conhecido pela frase 'Jesus chorou'?|João 11:35|Mateus 5:3|Lucas 2:14|Marcos 1:1|0|João 11:35|João|11|35|
easy_092|Em qual carta aparece a lista do fruto do Espírito?|Gálatas|Romanos|Hebreus|Judas|0|Gálatas 5:22–23|Gálatas|5|22|23
easy_093|Qual apóstolo escreveu sobre a 'armadura de Deus'?|Paulo|João|Pedro|Tiago|0|Efésios 6:10–18|Efésios|6|10|18
easy_094|Em qual carta encontramos 'tudo posso naquele que me fortalece'?|Filipenses|Filemom|2 João|Judas|0|Filipenses 4:13|Filipenses|4|13|
easy_095|Qual carta começa ensinando que Deus falou antigamente pelos profetas e agora pelo Filho?|Hebreus|Tiago|Gálatas|1 João|0|Hebreus 1:1–2|Hebreus|1|1|2
easy_096|Quem escreveu que 'a fé sem obras é morta'?|Tiago|Paulo|Lucas|Judas|0|Tiago 2:26|Tiago|2|26|
easy_097|Qual apóstolo escreveu que devemos lançar sobre Deus toda a nossa ansiedade?|Pedro|Tomé|Mateus|André|0|1 Pedro 5:7|1 Pedro|5|7|
easy_098|Qual carta ensina que 'Deus é amor'?|1 João|Romanos|2 Coríntios|Tito|0|1 João 4:8|1 João|4|8|
easy_099|Qual livro termina com a oração 'Vem, Senhor Jesus'?|Apocalipse|Atos|João|Hebreus|0|Apocalipse 22:20|Apocalipse|22|20|
easy_100|Quem é chamado de 'Cordeiro de Deus' por João Batista?|Jesus|Pedro|Moisés|Elias|0|João 1:29|João|1|29|
"""

    private const val MEDIUM_DATA = """
medium_041|Quem era a mãe de Ismael?|Hagar|Sara|Quetura|Rebeca|0|Gênesis 16:15|Gênesis|16|15|
medium_042|Qual foi a primeira esposa de Jacó?|Raquel|Lia|Bila|Zilpa|1|Gênesis 29:23–25|Gênesis|29|23|25
medium_043|Qual era o nome do irmão mais novo de José?|Benjamim|Judá|Levi|Rúben|0|Gênesis 35:18|Gênesis|35|18|
medium_044|Quantos anos de fome José anunciou após interpretar os sonhos do faraó?|Três|Cinco|Sete|Doze|2|Gênesis 41:29–30|Gênesis|41|29|30
medium_045|Qual era o nome do sogro de Moisés, sacerdote de Midiã?|Jetro|Eleazar|Fineias|Calebe|0|Êxodo 3:1|Êxodo|3|1|
medium_046|Quem foi especialmente capacitado para trabalhar na construção do tabernáculo?|Bezalel|Josué|Gérson|Corá|0|Êxodo 31:1–5|Êxodo|31|1|5
medium_047|Quem foi consagrado como primeiro sumo sacerdote de Israel?|Arão|Moisés|Josué|Samuel|0|Êxodo 28:1|Êxodo|28|1|
medium_048|Quais dois espias confiaram que Israel poderia entrar na terra prometida?|Josué e Calebe|Moisés e Arão|Nadabe e Abiú|Corá e Datã|0|Números 14:6–9|Números|14|6|9
medium_049|Em qual monte Moisés viu a terra prometida antes de morrer?|Nebo|Carmelo|Tabor|Sião|0|Deuteronômio 34:1–5|Deuteronômio|34|1|5
medium_050|Qual sinal Raabe colocou na janela para que sua casa fosse poupada?|Um cordão vermelho|Uma lâmpada|Um ramo de oliveira|Uma bandeira branca|0|Josué 2:18–21|Josué|2|18|21
medium_051|Qual israelita tomou objetos proibidos de Jericó e trouxe derrota ao povo em Ai?|Acã|Calebe|Eleazar|Fineias|0|Josué 7:20–21|Josué|7|20|21
medium_052|Que sinal Gideão pediu envolvendo uma porção de lã?|O velo molhado e o chão seco|Uma estrela parada|Fogo no rio|Uma nuvem sobre o altar|0|Juízes 6:36–40|Juízes|6|36|40
medium_053|Qual era o nome do pai de Sansão?|Manoá|Boaz|Elcana|Jessé|0|Juízes 13:2–3|Juízes|13|2|3
medium_054|Quem orientou o jovem Samuel a responder 'Fala, Senhor, porque o teu servo ouve'?|Eli|Saul|Davi|Natã|0|1 Samuel 3:8–9|1 Samuel|3|8|9
medium_055|Quais animais Davi disse ter enfrentado enquanto cuidava das ovelhas?|Leão e urso|Lobo e raposa|Leão e serpente|Urso e javali|0|1 Samuel 17:34–36|1 Samuel|17|34|36
medium_056|Qual mulher impediu Davi de se vingar de Nabal?|Abigail|Mical|Tamar|Bate-Seba|0|1 Samuel 25:23–35|1 Samuel|25|23|35
medium_057|O que aconteceu com Absalão durante a batalha na floresta de Efraim?|Ficou preso pelos cabelos numa árvore|Foi preso numa caverna|Caiu no mar|Foi capturado em Jerusalém|0|2 Samuel 18:9|2 Samuel|18|9|
medium_058|Qual decisão de Salomão revelou sua sabedoria diante de duas mulheres que disputavam um bebê?|Propôs dividir a criança|Mandou ambas embora|Entregou a criança ao sacerdote|Consultou os profetas|0|1 Reis 3:24–28|1 Reis|3|24|28
medium_059|Qual rainha visitou Salomão para provar sua sabedoria com perguntas difíceis?|Rainha de Sabá|Rainha Ester|Rainha Jezabel|Rainha Vasti|0|1 Reis 10:1–3|1 Reis|10|1|3
medium_060|Quem levou alimento a Elias junto ao ribeiro de Querite?|Corvos|Pombas|Pastores|Mercadores|0|1 Reis 17:4–6|1 Reis|17|4|6
medium_061|Em qual cidade vivia a viúva que alimentou Elias durante a fome?|Sarepta|Jericó|Betel|Hebrom|0|1 Reis 17:8–16|1 Reis|17|8|16
medium_062|Qual milagre Eliseu realizou com um machado que caiu na água?|Fez o ferro flutuar|Transformou o ferro em ouro|Secou o rio|Partiu o machado ao meio|0|2 Reis 6:5–7|2 Reis|6|5|7
medium_063|Quantas vezes Naamã mergulhou no Jordão?|Sete|Três|Doze|Quarenta|0|2 Reis 5:14|2 Reis|5|14|
medium_064|Qual sacerdote encontrou o Livro da Lei no templo durante o reinado de Josias?|Hilquias|Zadoque|Abiatar|Eli|0|2 Reis 22:8|2 Reis|22|8|
medium_065|Qual era a função de Neemias na corte do rei antes de voltar a Jerusalém?|Copeiro|General|Escriba|Músico|0|Neemias 1:11|Neemias|1|11|
medium_066|Qual era o nome hebraico de Ester?|Hadassa|Mara|Diná|Jemima|0|Ester 2:7|Ester|2|7|
medium_067|Qual amigo de Jó falou primeiro após os sete dias de silêncio?|Elifaz|Bildade|Zofar|Eliú|0|Jó 4:1|Jó|4|1|
medium_068|Qual salmo contém a oração de Davi por um coração puro após ser confrontado por Natã?|Salmo 51|Salmo 23|Salmo 91|Salmo 150|0|Salmos 51:10|Salmos|51|10|
medium_069|Em qual capítulo de Eclesiastes aparece 'há tempo para todo propósito debaixo do céu'?|Capítulo 3|Capítulo 1|Capítulo 7|Capítulo 12|0|Eclesiastes 3:1|Eclesiastes|3|1|
medium_070|Que seres Isaías viu proclamando 'Santo, Santo, Santo'?|Serafins|Querubins|Anciãos|Leões|0|Isaías 6:2–3|Isaías|6|2|3
medium_071|Para qual lugar Deus mandou Jeremias ir observar o trabalho de um artesão?|Casa do oleiro|Casa do ferreiro|Palácio do rei|Porta das Águas|0|Jeremias 18:1–4|Jeremias|18|1|4
medium_072|Qual função simbólica Deus atribuiu a Ezequiel em relação à casa de Israel?|Atalaia|Rei|Sacerdote-chefe|General|0|Ezequiel 3:17|Ezequiel|3|17|
medium_073|Por quantos dias Daniel e seus amigos pediram para ser testados com legumes e água?|Dez|Três|Sete|Quarenta|0|Daniel 1:12–15|Daniel|1|12|15
medium_074|Qual rei viu a escrita misteriosa na parede durante um banquete?|Belsazar|Dario|Ciro|Nabucodonosor|0|Daniel 5:5–6|Daniel|5|5|6
medium_075|Como se chamava a esposa do profeta Oséias?|Gômer|Débora|Hulda|Mical|0|Oséias 1:3|Oséias|1|3|
medium_076|Qual profeta anunciou que Deus derramaria seu Espírito sobre toda carne?|Joel|Amós|Obadias|Naum|0|Joel 2:28–29|Joel|2|28|29
medium_077|De qual cidade era o profeta Amós?|Tecoa|Belém|Jericó|Samaria|0|Amós 1:1|Amós|1|1|
medium_078|Contra qual povo o livro de Obadias dirige sua principal mensagem?|Edom|Moabe|Filístia|Egito|0|Obadias 1:1|Obadias|1|1|
medium_079|Qual profeta incentivou o povo a retomar a reconstrução do templo após o exílio?|Ageu|Jonas|Oséias|Naum|0|Ageu 1:7–8|Ageu|1|7|8
medium_080|O que Zacarias viu ao lado de um candelabro de ouro em uma de suas visões?|Duas oliveiras|Dois leões|Duas montanhas|Dois rios|0|Zacarias 4:2–3|Zacarias|4|2|3
medium_081|Quais presentes os magos ofereceram ao menino Jesus?|Ouro, incenso e mirra|Prata, azeite e trigo|Ouro, vinho e pão|Incenso, linho e sal|0|Mateus 2:11|Mateus|2|11|
medium_082|Quem tomou Jesus nos braços no templo e louvou a Deus por ver a salvação?|Simeão|Zacarias|Nicodemos|José de Arimateia|0|Lucas 2:25–32|Lucas|2|25|32
medium_083|Qual profetisa idosa falou sobre Jesus no templo?|Ana|Isabel|Marta|Maria Madalena|0|Lucas 2:36–38|Lucas|2|36|38
medium_084|Quantos dias Jesus jejuou no deserto antes de ser tentado?|Quarenta|Sete|Doze|Trinta|0|Mateus 4:1–2|Mateus|4|1|2
medium_085|Qual fariseu visitou Jesus durante a noite?|Nicodemos|Gamaliel|Caifás|Jairo|0|João 3:1–2|João|3|1|2
medium_086|Junto a qual poço Jesus conversou com a mulher samaritana?|Poço de Jacó|Poço de Abraão|Poço de Isaque|Poço de Davi|0|João 4:5–6|João|4|5|6
medium_087|Qual oficial romano demonstrou grande fé ao pedir cura para seu servo?|Um centurião|Um tribuno|Um governador|Um senador|0|Mateus 8:5–10|Mateus|8|5|10
medium_088|Quais dois personagens do Antigo Testamento apareceram com Jesus na transfiguração?|Moisés e Elias|Abraão e Davi|Josué e Samuel|Isaías e Jeremias|0|Mateus 17:1–3|Mateus|17|1|3
medium_089|O que o jovem rico perguntou a Jesus?|O que fazer para ter a vida eterna|Como tornar-se sacerdote|Como vencer os romanos|Onde construir um templo|0|Mateus 19:16|Mateus|19|16|
medium_090|Em qual jardim Jesus orou antes de ser preso?|Getsêmani|Éden|Siloé|Carmelo|0|Mateus 26:36|Mateus|26|36|
medium_091|Quem colocou o corpo de Jesus em um túmulo novo?|José de Arimateia|Simão de Cirene|Bartimeu|Jairo|0|Mateus 27:57–60|Mateus|27|57|60
medium_092|Quem orou por Saulo para que ele recuperasse a visão em Damasco?|Ananias|Barnabé|Estêvão|Filipe|0|Atos 9:17–18|Atos|9|17|18
medium_093|Qual centurião recebeu uma visão e mandou buscar Pedro em Jope?|Cornélio|Júlio|Cláudio|Lísias|0|Atos 10:1–5|Atos|10|1|5
medium_094|Quem explicou as Escrituras ao eunuco etíope?|Filipe|Pedro|Paulo|Barnabé|0|Atos 8:30–35|Atos|8|30|35
medium_095|Qual casal trabalhou com Paulo e também ensinou Apolo com mais precisão?|Priscila e Áquila|Ananias e Safira|Herodes e Berenice|Félix e Drusila|0|Atos 18:24–26|Atos|18|24|26
medium_096|Qual jovem caiu de uma janela enquanto Paulo falava por muito tempo?|Êutico|Timóteo|Tíquico|Trófimo|0|Atos 20:9–12|Atos|20|9|12
medium_097|Em qual cidade os discípulos foram chamados cristãos pela primeira vez?|Antioquia|Jerusalém|Éfeso|Corinto|0|Atos 11:26|Atos|11|26|
medium_098|Qual apóstolo teve uma visão de um lençol com animais antes de visitar Cornélio?|Pedro|Paulo|João|Tiago|0|Atos 10:9–16|Atos|10|9|16
medium_099|Qual carta de Paulo apresenta o capítulo conhecido como 'capítulo do amor'?|1 Coríntios|Romanos|Efésios|Colossenses|0|1 Coríntios 13:1–13|1 Coríntios|13|1|13
medium_100|Em qual carta Paulo descreve a Palavra de Deus como a 'espada do Espírito'?|Efésios|Filipenses|Tito|Filemom|0|Efésios 6:17|Efésios|6|17|
"""

    private const val HARD_DATA = """
hard_041|Quem era Melquisedeque quando encontrou Abraão?|Rei de Salém e sacerdote do Deus Altíssimo|Rei do Egito e general|Profeta de Betel|Sacerdote de Baal|0|Gênesis 14:18–20|Gênesis|14|18|20
hard_042|De quem Abraão comprou a caverna de Macpela para sepultar Sara?|Efrom, o heteu|Abimeleque|Melquisedeque|Ló|0|Gênesis 23:10–20|Gênesis|23|10|20
hard_043|Qual era o nome da ama de Rebeca que foi sepultada perto de Betel?|Débora|Miriã|Zilpa|Diná|0|Gênesis 35:8|Gênesis|35|8|
hard_044|Perto de qual local Raquel foi sepultada?|Efrata, isto é, Belém|Hebrom|Siquém|Jericó|0|Gênesis 35:19|Gênesis|35|19|
hard_045|Qual era o nome da esposa egípcia de José?|Azenate|Zípora|Quetura|Tamar|0|Gênesis 41:45|Gênesis|41|45|
hard_046|Quais eram os nomes dos dois filhos de Moisés?|Gérson e Eliézer|Nadabe e Abiú|Efraim e Manassés|Hofni e Fineias|0|Êxodo 18:3–4|Êxodo|18|3|4
hard_047|Quais parteiras hebreias são nomeadas no início de Êxodo?|Sifrá e Puá|Miriã e Zípora|Lia e Raquel|Hulda e Débora|0|Êxodo 1:15|Êxodo|1|15|
hard_048|De qual tribo era Aoliabe, auxiliar de Bezalel na obra do tabernáculo?|Dã|Judá|Levi|Benjamim|0|Êxodo 31:6|Êxodo|31|6|
hard_049|Quantas pedras preciosas havia no peitoral do sumo sacerdote, representando as tribos de Israel?|Doze|Sete|Dez|Vinte e quatro|0|Êxodo 28:17–21|Êxodo|28|17|21
hard_050|Quantas filhas de Zelofeade apresentaram seu caso de herança diante de Moisés?|Cinco|Três|Sete|Doze|0|Números 27:1|Números|27|1|
hard_051|Qual rei de Moabe contratou Balaão para amaldiçoar Israel?|Balaque|Eglom|Seom|Ogue|0|Números 22:4–6|Números|22|4|6
hard_052|Que objeto Moisés levantou no deserto para que os mordidos por serpentes pudessem olhar e viver?|Uma serpente de bronze|Uma arca de madeira|Um cajado de ouro|Uma lâmpada de prata|0|Números 21:8–9|Números|21|8|9
hard_053|Sobre qual cidade Josué pediu que o sol parasse?|Gibeom|Jericó|Hebrom|Betel|0|Josué 10:12–13|Josué|10|12|13
hard_054|Quantos anos Calebe disse ter quando pediu Hebrom como herança?|Oitenta e cinco|Setenta|Noventa|Cem|0|Josué 14:10–12|Josué|14|10|12
hard_055|Qual era o nome do marido de Débora, a profetisa e juíza?|Lapidote|Baraque|Heber|Abinoão|0|Juízes 4:4|Juízes|4|4|
hard_056|Com quantos homens Baraque subiu contra Sísera?|Dez mil|Mil|Três mil|Doze mil|0|Juízes 4:10|Juízes|4|10|
hard_057|Que nome Gideão recebeu depois de derrubar o altar de Baal?|Jerubaal|Abimeleque|Otniel|Jefté|0|Juízes 6:32|Juízes|6|32|
hard_058|Como Abimeleque, filho de Gideão, foi mortalmente ferido?|Uma mulher lançou uma pedra de moinho sobre sua cabeça|Foi atingido por uma flecha de Saul|Caiu de uma muralha|Foi ferido por um leão|0|Juízes 9:52–54|Juízes|9|52|54
hard_059|Quais eram os nomes dos dois filhos do sacerdote Eli?|Hofni e Fineias|Nadabe e Abiú|Jônatas e Mefibosete|Joel e Abias|0|1 Samuel 1:3|1 Samuel|1|3|
hard_060|Que nome recebeu o menino nascido quando a arca de Deus foi tomada pelos filisteus?|Icabô|Samuel|Obede-Edom|Jabez|0|1 Samuel 4:19–22|1 Samuel|4|19|22
hard_061|O que Saul procurava quando encontrou Samuel antes de ser ungido rei?|As jumentas de seu pai|Uma espada perdida|Ovelhas roubadas|A arca da aliança|0|1 Samuel 9:3–6|1 Samuel|9|3|6
hard_062|Qual sacerdote deu a Davi os pães da proposição quando ele fugia de Saul?|Aimeleque|Abiatar|Zadoque|Eli|0|1 Samuel 21:1–6|1 Samuel|21|1|6
hard_063|Mefibosete era filho de quem?|Jônatas|Saul|Abner|Davi|0|2 Samuel 4:4|2 Samuel|4|4|
hard_064|Qual profeta apresentou a Davi três opções de castigo após o recenseamento?|Gade|Natã|Samuel|Aías|0|2 Samuel 24:11–13|2 Samuel|24|11|13
hard_065|De qual porto Salomão enviou sua frota em direção a Ofir?|Eziom-Geber|Jope|Tiro|Cesareia|0|1 Reis 9:26–28|1 Reis|9|26|28
hard_066|Em quais duas cidades Jeroboão colocou bezerros de ouro?|Betel e Dã|Jerusalém e Hebrom|Siquém e Samaria|Jericó e Gibeom|0|1 Reis 12:28–29|1 Reis|12|28|29
hard_067|Quantos profetas de Baal Elias convocou ao monte Carmelo?|Quatrocentos e cinquenta|Quatrocentos|Setecentos|Cento e cinquenta|0|1 Reis 18:19|1 Reis|18|19|
hard_068|Para qual cidade Elias correu adiante do carro de Acabe após a chuva?|Jezreel|Samaria|Jerusalém|Betel|0|1 Reis 18:45–46|1 Reis|18|45|46
hard_069|Quantos jovens foram feridos pelos ursos no episódio de Eliseu perto de Betel?|Quarenta e dois|Doze|Vinte e quatro|Setenta|0|2 Reis 2:23–24|2 Reis|2|23|24
hard_070|Qual profetisa foi consultada pelos oficiais do rei Josias após a descoberta do Livro da Lei?|Hulda|Débora|Miriã|Ana|0|2 Reis 22:14–20|2 Reis|22|14|20
hard_071|Por quantos anos Manassés reinou em Jerusalém?|Cinquenta e cinco|Quarenta|Vinte e cinco|Setenta|0|2 Reis 21:1|2 Reis|21|1|
hard_072|Quantos soldados assírios foram feridos pelo anjo do Senhor durante o cerco no tempo de Ezequias?|Cento e oitenta e cinco mil|Setenta mil|Doze mil|Quarenta mil|0|2 Reis 19:35|2 Reis|19|35|
hard_073|Qual rei persa autorizou os judeus a reconstruírem o templo em Jerusalém?|Ciro|Dario, o medo|Assuero|Artaxerxes|0|Esdras 1:1–4|Esdras|1|1|4
hard_074|Quais três opositores são destacados quando Neemias reconstruía os muros?|Sambalate, Tobias e Gesém|Hamã, Bigtã e Teres|Corá, Datã e Abirão|Festo, Félix e Agripa|0|Neemias 2:19|Neemias|2|19|
hard_075|Por quantos dias o rei Assuero exibiu as riquezas de seu reino antes do banquete de sete dias?|Cento e oitenta|Setenta|Quarenta|Trezentos|0|Ester 1:3–5|Ester|1|3|5
hard_076|Quantos anos Jó viveu depois de sua restauração?|Cento e quarenta|Setenta|Cento e vinte|Cento e oitenta|0|Jó 42:16|Jó|42|16|
hard_077|A quem é atribuído o Salmo 90 no título tradicional?|Moisés|Davi|Salomão|Asafe|0|Salmos 90:1|Salmos|90|1|
hard_078|Quantas seções alfabéticas de oito versículos compõem o Salmo 119?|Vinte e duas|Doze|Vinte e quatro|Trinta|0|Salmos 119:1–176|Salmos|119|1|176
hard_079|O que Agur pediu a Deus a respeito de pobreza e riqueza?|Nem pobreza nem riqueza|Somente grande riqueza|Somente pobreza|Um reino poderoso|0|Provérbios 30:8–9|Provérbios|30|8|9
hard_080|Qual nome simbólico Isaías deu a seu filho em Isaías 8?|Maer-Salal-Hás-Baz|Sear-Jasube|Emanuel|Lo-Ami|0|Isaías 8:1–3|Isaías|8|1|3
hard_081|Qual escriba registrou as palavras ditadas por Jeremias em um rolo?|Baruque|Esdras|Safã|Hilquias|0|Jeremias 36:4|Jeremias|36|4|
hard_082|Que perda pessoal Ezequiel sofreu quando Deus lhe deu uma mensagem simbólica sobre Jerusalém?|A morte de sua esposa|A morte de seu filho|A perda de sua casa|A prisão de seu irmão|0|Ezequiel 24:15–18|Ezequiel|24|15|18
hard_083|Qual nome babilônico foi dado a Daniel?|Beltessazar|Sadraque|Mesaque|Abede-Nego|0|Daniel 1:7|Daniel|1|7|
hard_084|Quantas vezes mais quente Nabucodonosor mandou aquecer a fornalha?|Sete vezes|Três vezes|Doze vezes|Quarenta vezes|0|Daniel 3:19|Daniel|3|19|
hard_085|Por qual valor Oséias disse ter comprado de volta a mulher em Oséias 3?|Quinze siclos de prata e cevada|Trinta siclos de ouro|Dez siclos de prata|Cem moedas de prata|0|Oséias 3:2|Oséias|3|2|
hard_086|O que Amós viu na visão que simbolizava a chegada do fim para Israel?|Um cesto de frutos de verão|Uma videira seca|Uma espada quebrada|Uma coroa de ouro|0|Amós 8:1–2|Amós|8|1|2
hard_087|De qual localidade era o profeta Miquéias?|Moresete|Tecoa|Anatote|Tisbe|0|Miquéias 1:1|Miquéias|1|1|
hard_088|Em qual livro aparece a frase 'o justo viverá pela sua fé'?|Habacuque|Naum|Ageu|Malaquias|0|Habacuque 2:4|Habacuque|2|4|
hard_089|Durante o reinado de qual rei de Judá veio a palavra do Senhor a Sofonias?|Josias|Acaz|Uzias|Manassés|0|Sofonias 1:1|Sofonias|1|1|
hard_090|De qual linhagem sacerdotal era Isabel, mãe de João Batista?|Das filhas de Arão|Da casa de Davi|Da tribo de Benjamim|Da casa de José|0|Lucas 1:5|Lucas|1|5|
hard_091|De que era feita a roupa característica de João Batista?|Pelos de camelo com cinto de couro|Linho fino com cinto de ouro|Lã branca com faixa azul|Púrpura com cinto de prata|0|Mateus 3:4|Mateus|3|4|
hard_092|Onde Jesus disse ter visto Natanael antes de Filipe chamá-lo?|Debaixo da figueira|No templo|À beira do Jordão|No monte das Oliveiras|0|João 1:47–50|João|1|47|50
hard_093|Quantos pórticos havia junto ao tanque de Betesda?|Cinco|Dois|Sete|Doze|0|João 5:2|João|5|2|
hard_094|Qual era o nome do servo do sumo sacerdote cuja orelha foi cortada na prisão de Jesus?|Malco|Bartimeu|Cléopas|Jairo|0|João 18:10|João|18|10|
hard_095|Qual prisioneiro foi solto no lugar de Jesus?|Barrabás|Simão|Malco|Matias|0|Mateus 27:15–26|Mateus|27|15|26
hard_096|Em quais três línguas João diz que foi escrita a inscrição colocada sobre a cruz?|Hebraico, latim e grego|Aramaico, egípcio e latim|Grego, persa e hebraico|Latim, árabe e grego|0|João 19:19–20|João|19|19|20
hard_097|Aproximadamente quantas pessoas foram acrescentadas à comunidade no dia de Pentecostes?|Três mil|Quinhentas|Doze mil|Setenta|0|Atos 2:41|Atos|2|41|
hard_098|Qual mestre da lei aconselhou o Sinédrio a ter cautela ao perseguir os apóstolos?|Gamaliel|Nicodemos|Caifás|Ananias|0|Atos 5:34–39|Atos|5|34|39
hard_099|Quantas filhas virgens de Filipe, o evangelista, profetizavam?|Quatro|Duas|Sete|Doze|0|Atos 21:8–9|Atos|21|8|9
hard_100|De qual cidade era Apolo, descrito como eloquente e poderoso nas Escrituras?|Alexandria|Antioquia|Tarso|Éfeso|0|Atos 18:24|Atos|18|24|
"""
}
