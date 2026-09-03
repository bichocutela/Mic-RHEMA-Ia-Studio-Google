package com.aistudio.micrhema

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Calendário editorial de segurança para 2027.
 *
 * O conteúdo automático garante um devocional para cada dia do ano sem exigir
 * 365 cadastros manuais. Quando existir um devocional aprovado vindo do ADM para
 * a mesma data, o conteúdo editorial do ADM tem prioridade sobre este fallback.
 */
object DevotionalCalendar2027 {
    private data class Seed(
        val title: String,
        val verseReference: String,
        val verse: String,
        val reflection: String
    )

    private val seeds = listOf(
        Seed("Primeiro a Presença", "Mateus 6:33", "Buscai primeiro o Reino de Deus e a sua justiça.", "Quando Deus ocupa o primeiro lugar, as demais prioridades encontram o lugar certo. Comece o dia alinhando o coração com aquilo que é eterno."),
        Seed("Coragem para Continuar", "Josué 1:9", "Sê forte e corajoso; não temas, porque o Senhor teu Deus é contigo.", "A coragem bíblica não nasce da ausência de medo, mas da certeza de que Deus permanece presente. Dê hoje o próximo passo que a fé está pedindo."),
        Seed("Descanso para a Alma", "Mateus 11:28", "Vinde a mim, todos os que estais cansados, e eu vos aliviarei.", "Jesus não exige que você carregue sozinho o peso do dia. Entregue a Ele aquilo que tem consumido suas forças e receba descanso para continuar."),
        Seed("Luz para o Caminho", "Salmos 119:105", "Lâmpada para os meus pés é a tua palavra e luz para o meu caminho.", "Nem sempre Deus mostra toda a estrada de uma vez. Muitas vezes Ele ilumina o passo de agora. Obedeça ao que já está claro na Palavra."),
        Seed("Paz no Meio da Pressa", "Filipenses 4:6-7", "A paz de Deus guardará o vosso coração e a vossa mente em Cristo Jesus.", "Ansiedade cresce quando tentamos controlar tudo. A oração devolve ao coração a perspectiva de que Deus continua soberano mesmo quando o cenário ainda não mudou."),
        Seed("Fé que Persevera", "Hebreus 10:23", "Guardemos firme a confissão da esperança, porque fiel é o que prometeu.", "A fidelidade de Deus é maior que a demora das respostas. Permaneça firme no que Ele já falou e não abandone a esperança por causa de um dia difícil."),
        Seed("Graça Suficiente", "2 Coríntios 12:9", "A minha graça te basta, porque o meu poder se aperfeiçoa na fraqueza.", "Há dias em que a força humana não alcança. Neles, a graça deixa de ser conceito e se torna sustento. Reconhecer limites também é uma forma de confiar."),
        Seed("Coração Ensinável", "Provérbios 3:5-6", "Confia no Senhor de todo o teu coração e reconhece-o em todos os teus caminhos.", "Nem toda resposta precisa nascer do nosso próprio entendimento. Um coração ensinável pergunta a Deus antes de decidir e aceita correção quando necessário."),
        Seed("Alegria que Fortalece", "Neemias 8:10", "A alegria do Senhor é a vossa força.", "A alegria do Senhor não depende de uma vida sem problemas. Ela nasce da comunhão com Deus e renova forças para enfrentar a realidade sem perder a esperança."),
        Seed("Refúgio Seguro", "Salmos 46:1", "Deus é o nosso refúgio e fortaleza, socorro bem presente na angústia.", "Em vez de correr apenas para soluções, corra primeiro para a presença de Deus. Refúgio não significa fugir da realidade, mas enfrentá-la a partir de um lugar seguro."),
        Seed("Mente Renovada", "Romanos 12:2", "Transformai-vos pela renovação da vossa mente.", "Pensamentos repetidos moldam atitudes. Permita que a Palavra confronte ideias antigas e forme em você uma maneira de pensar coerente com Cristo."),
        Seed("Amor em Movimento", "1 Coríntios 13:4", "O amor é paciente e bondoso.", "Amor cristão aparece nas pequenas escolhas: ouvir, esperar, perdoar, servir e responder com mansidão. Hoje, transforme sentimento em atitude concreta."),
        Seed("Deus Cuida de Você", "1 Pedro 5:7", "Lançando sobre ele toda a vossa ansiedade, porque ele tem cuidado de vós.", "Você não precisa esconder de Deus suas preocupações. Coloque diante Dele cada nome, conta, decisão e medo. O cuidado do Pai alcança também os detalhes."),
        Seed("Força na Fraqueza", "Isaías 40:31", "Os que esperam no Senhor renovarão as suas forças.", "Esperar em Deus não é passividade. É continuar fiel enquanto a força é renovada por dentro. Faça o que lhe cabe sem abandonar a confiança."),
        Seed("Palavras que Edificam", "Efésios 4:29", "Fale apenas o que for bom para edificação e transmitir graça aos que ouvem.", "Uma frase pode aliviar ou aumentar o peso de alguém. Antes de falar, pergunte se suas palavras carregam verdade, graça e utilidade."),
        Seed("Perdão que Liberta", "Colossenses 3:13", "Perdoai-vos uns aos outros, assim como Cristo vos perdoou.", "Perdoar não chama o erro de certo; significa recusar-se a viver prisioneiro dele. Peça a Deus graça para iniciar um processo de libertação interior."),
        Seed("Passos de Obediência", "Tiago 1:22", "Sede praticantes da palavra e não somente ouvintes.", "Conhecimento bíblico produz fruto quando se transforma em prática. Escolha hoje uma verdade que você já conhece e coloque-a em ação."),
        Seed("Esperança Renovada", "Romanos 15:13", "Que o Deus da esperança vos encha de toda alegria e paz no crer.", "Esperança cristã não é pensamento positivo; é confiança em quem Deus é. Deixe a presença do Espírito renovar a expectativa do seu coração."),
        Seed("Sabedoria para Decidir", "Tiago 1:5", "Se alguém necessita de sabedoria, peça-a a Deus, que a todos dá liberalmente.", "Decisões importantes não precisam ser tomadas na pressa. Ore, observe princípios bíblicos e permita que a sabedoria produza clareza antes da ação."),
        Seed("Fidelidade no Pouco", "Lucas 16:10", "Quem é fiel no pouco também é fiel no muito.", "Grandes responsabilidades são sustentadas por pequenas fidelidades. Cuide bem do que Deus já colocou em suas mãos hoje."),
        Seed("O Poder da Gratidão", "1 Tessalonicenses 5:18", "Em tudo dai graças, porque esta é a vontade de Deus em Cristo Jesus.", "Gratidão não ignora dificuldades; ela impede que as dificuldades sejam a única coisa que enxergamos. Reconheça pelo menos três sinais da bondade de Deus hoje."),
        Seed("Não Caminhe Sozinho", "Hebreus 10:25", "Não deixemos de congregar-nos; antes, encorajemo-nos uns aos outros.", "Deus nos formou para comunhão. Procure pessoas que aproximem você de Cristo e também seja alguém que fortalece a fé dos outros."),
        Seed("Coração Guardado", "Provérbios 4:23", "Sobre tudo o que se deve guardar, guarda o teu coração.", "O que alimentamos por dentro aparece por fora. Observe o que tem ocupado seus pensamentos e escolha proteger o coração sem endurecê-lo."),
        Seed("Servir é Grandeza", "Marcos 10:45", "O Filho do Homem não veio para ser servido, mas para servir.", "No Reino, grandeza não é posição; é disposição para servir. Procure hoje uma necessidade que você pode atender sem esperar reconhecimento."),
        Seed("Começar de Novo", "Lamentações 3:22-23", "As misericórdias do Senhor se renovam a cada manhã.", "Ontem não precisa determinar hoje. A misericórdia de Deus oferece espaço para arrependimento, aprendizado e um novo começo."),
        Seed("Confiança em Meio ao Processo", "Romanos 8:28", "Todas as coisas cooperam para o bem daqueles que amam a Deus.", "Nem tudo o que acontece é bom, mas Deus é capaz de trabalhar até em cenários difíceis. Confie no processo sem chamar de fim aquilo que ainda está sendo construído."),
        Seed("Olhos em Jesus", "Hebreus 12:2", "Olhando firmemente para Jesus, autor e consumador da fé.", "Comparações e distrações roubam energia espiritual. Reoriente o olhar para Cristo e permita que Ele defina ritmo, identidade e direção."),
        Seed("Oração que Aproxima", "Jeremias 33:3", "Clama a mim, e responder-te-ei.", "A oração é mais do que apresentar pedidos; é permanecer em relacionamento. Reserve alguns minutos sem pressa para conversar com Deus com sinceridade."),
        Seed("Bondade que Transborda", "Gálatas 6:9-10", "Não nos cansemos de fazer o bem.", "Nem todo bem que fazemos produz resultado imediato. Continue semeando. A fidelidade escondida também é vista por Deus."),
        Seed("Identidade em Cristo", "2 Coríntios 5:17", "Se alguém está em Cristo, nova criatura é.", "Você não precisa viver limitado pelas antigas definições sobre si mesmo. Em Cristo existe uma nova identidade, e ela deve orientar suas escolhas de hoje."),
        Seed("Mansidão é Força", "Provérbios 15:1", "A resposta branda desvia o furor.", "Responder com mansidão exige mais domínio do que reagir impulsivamente. Peça ao Espírito Santo sabedoria para desarmar conflitos em vez de alimentá-los."),
        Seed("Deus Está Perto", "Salmos 34:18", "Perto está o Senhor dos que têm o coração quebrantado.", "Dor não afasta automaticamente a presença de Deus. Muitas vezes é no lugar mais frágil que percebemos Sua proximidade de maneira mais profunda."),
        Seed("Fruto que Permanece", "João 15:5", "Quem permanece em mim, esse dá muito fruto.", "Fruto espiritual não nasce da correria, mas da permanência. Antes de tentar produzir mais, cuide da sua ligação diária com Cristo."),
        Seed("Escolha a Verdade", "João 8:32", "Conhecereis a verdade, e a verdade vos libertará.", "Mentiras repetidas podem parecer verdade. Confronte pensamentos de culpa, medo e desvalor com aquilo que Deus declara em Sua Palavra."),
        Seed("Generosidade com Propósito", "2 Coríntios 9:7", "Deus ama a quem dá com alegria.", "Generosidade não se resume a dinheiro. Você pode oferecer tempo, atenção, conhecimento, encorajamento e recursos com um coração livre."),
        Seed("Paciência no Tempo de Deus", "Eclesiastes 3:1", "Tudo tem o seu tempo determinado.", "A pressa pode fazer parecer atraso aquilo que ainda está amadurecendo. Aprenda a respeitar processos sem desistir daquilo que Deus colocou no coração."),
        Seed("A Fé Vê Além", "2 Coríntios 5:7", "Andamos por fé e não pelo que vemos.", "Circunstâncias são reais, mas não são a única realidade. A fé considera também o caráter de Deus, Sua Palavra e Sua capacidade de abrir caminhos."),
        Seed("Cuidado com as Comparações", "Gálatas 6:4", "Examine cada um a sua própria obra.", "Comparar bastidores pessoais com resultados alheios produz ansiedade. Valorize o processo que Deus está conduzindo em você."),
        Seed("Deus Dá Direção", "Salmos 32:8", "Instruir-te-ei e ensinar-te-ei o caminho que deves seguir.", "Quando não souber qual caminho tomar, não trate a confusão como abandono. Deus sabe orientar por Sua Palavra, por sabedoria e por paz."),
        Seed("Uma Vida de Louvor", "Salmos 34:1", "Bendirei o Senhor em todo o tempo; o seu louvor estará continuamente na minha boca.", "Louvor muda o foco do coração. Mesmo antes de as circunstâncias mudarem, adorar nos lembra quem Deus é e quem nós somos diante Dele."),
        Seed("Domínio Próprio", "2 Timóteo 1:7", "Deus não nos deu espírito de medo, mas de poder, amor e equilíbrio.", "Você não precisa obedecer a todo impulso. O Espírito Santo produz equilíbrio para escolher respostas maduras mesmo sob pressão."),
        Seed("A Presença no Cotidiano", "Colossenses 3:17", "Tudo o que fizerdes, fazei em nome do Senhor Jesus.", "Espiritualidade também acontece na rotina: trabalho, casa, trânsito, conversas e decisões simples. Convide Deus para o comum deste dia."),
        Seed("Esperar sem Parar", "Salmos 27:14", "Espera no Senhor, anima-te, e ele fortalecerá o teu coração.", "Esperar em Deus não significa ficar imóvel. Continue orando, aprendendo, servindo e preparando-se enquanto o tempo certo não chega."),
        Seed("Contentamento que Protege", "Filipenses 4:11", "Aprendi a viver contente em toda e qualquer situação.", "Contentamento não elimina sonhos; ele impede que a falta do próximo passo roube a gratidão pelo que já existe."),
        Seed("Compromisso com a Verdade", "Efésios 4:15", "Seguindo a verdade em amor, cresçamos em tudo naquele que é Cristo.", "Verdade sem amor fere; amor sem verdade confunde. Peça a Deus equilíbrio para ser sincero sem perder a graça."),
        Seed("Deus Faz Novas Coisas", "Isaías 43:19", "Eis que faço uma coisa nova; porventura não a percebeis?", "Nem toda mudança é ameaça. Às vezes Deus está abrindo uma estrada onde você esperava apenas repetição. Esteja atento ao novo sem abandonar os princípios."),
        Seed("Semente de Paz", "Mateus 5:9", "Bem-aventurados os pacificadores.", "Pacificar é mais do que evitar brigas; é contribuir ativamente para reconciliação, justiça e entendimento. Seja hoje uma presença que diminui a tensão."),
        Seed("Memória da Fidelidade", "Salmos 103:2", "Bendize, ó minha alma, ao Senhor e não te esqueças de nenhum de seus benefícios.", "Quando a preocupação aumenta, relembre respostas, livramentos e provisões que você já viveu. Memória espiritual alimenta confiança para o presente.")
    )

    private val monthlyFocus = listOf(
        "fundamentos firmes", "amor que se transforma em atitude", "coragem para avançar",
        "vida de oração", "sabedoria nas escolhas", "família e relacionamentos",
        "perseverança", "serviço e propósito", "maturidade espiritual",
        "gratidão", "esperança", "presença de Deus"
    )

    private val weekdayPractice = mapOf(
        DayOfWeek.MONDAY to "Defina uma atitude prática de obediência para começar bem a semana.",
        DayOfWeek.TUESDAY to "Ore por uma pessoa específica e, se puder, encoraje-a hoje.",
        DayOfWeek.WEDNESDAY to "Separe alguns minutos para reler a passagem e anotar o que Deus lhe ensinou.",
        DayOfWeek.THURSDAY to "Observe uma área da rotina em que esta verdade pode ser colocada em prática.",
        DayOfWeek.FRIDAY to "Agradeça por três acontecimentos da semana e entregue a Deus o que ainda está pendente.",
        DayOfWeek.SATURDAY to "Use parte do dia para desacelerar, servir alguém e cuidar da comunhão com Deus.",
        DayOfWeek.SUNDAY to "Prepare o coração para adorar, congregar e compartilhar aquilo que recebeu durante a semana."
    )

    val items: List<Devotional> by lazy {
        val first = LocalDate.of(2027, 1, 1)
        val last = LocalDate.of(2027, 12, 31)
        generateSequence(first) { current -> current.plusDays(1).takeIf { !it.isAfter(last) } }
            .mapIndexed { index, date ->
                // O salto 13 evita sequências visivelmente repetitivas ao longo dos meses.
                val seed = seeds[(index * 13 + date.monthValue) % seeds.size]
                val focus = monthlyFocus[date.monthValue - 1]
                val practice = weekdayPractice[date.dayOfWeek].orEmpty()
                val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                Devotional(
                    id = "auto-2027-$date",
                    title = seed.title,
                    date = date.toString(),
                    verse = seed.verse,
                    verseReference = seed.verseReference,
                    content = buildString {
                        append(seed.reflection)
                        append("\n\nNeste mês, mantenha atenção especial a ")
                        append(focus)
                        append(". ")
                        append(practice)
                        append("\n\nOração: Senhor, firma meu coração na tua Palavra, dá-me sabedoria para este dia e ajuda-me a viver de modo que minhas escolhas revelem Cristo. Amém.")
                    },
                    type = "devocional_auto",
                    isApproved = true,
                    timestamp = timestamp
                )
            }
            .toList()
    }
}

object DevotionalDateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val acceptedFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    )

    fun parse(raw: String): LocalDate? {
        val value = raw.trim()
        if (value.isBlank()) return null
        acceptedFormatters.forEach { formatter ->
            try {
                return LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    fun display(raw: String): String = parse(raw)?.format(displayFormatter) ?: raw

    fun mergeWithAutomatic2027(base: List<Devotional>): List<Devotional> {
        val approvedBase = base.filter { it.isApproved }
        val baseDates = approvedBase.mapNotNull { parse(it.date) }.toSet()
        val baseIds = approvedBase.map { it.id }.toSet()
        val automatic = DevotionalCalendar2027.items.filter { devotional ->
            devotional.id !in baseIds && parse(devotional.date) !in baseDates
        }
        return (approvedBase + automatic)
            .distinctBy { it.id.ifBlank { "${it.date}:${it.title}" } }
    }

    fun availableUntilToday(base: List<Devotional>, today: LocalDate = LocalDate.now()): List<Devotional> =
        mergeWithAutomatic2027(base).filter { devotional ->
            val date = parse(devotional.date)
            date == null || !date.isAfter(today)
        }

    fun todayOrLatest(base: List<Devotional>, today: LocalDate = LocalDate.now()): Devotional? {
        val available = availableUntilToday(base, today)
        return available.firstOrNull { parse(it.date) == today }
            ?: available.maxWithOrNull(
                compareBy<Devotional> { parse(it.date) ?: LocalDate.MIN }
                    .thenBy { it.timestamp }
            )
    }
}
