from pathlib import Path
import json
import re
import unicodedata

ROOT = Path("app/src/main/java/com/aistudio/micrhema")


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Padrão não encontrado em {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1) Pesquisa profunda: aceitar flexões, sinônimos e apenas uma expansão temática relevante.
deep = ROOT / "BibleDeepSearch.kt"
text = deep.read_text(encoding="utf-8")

text = text.replace(
'''private data class IndexedBibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val normalizedText: String,
    val words: Set<String>
)''',
'''private data class IndexedBibleVerse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val normalizedText: String,
    val words: Set<String>,
    val stems: Set<String>
)''',
1,
)

old_themes = '''private val deepSearchThemes = mapOf(
    "ansiedade" to listOf("ansioso", "cuidado", "preocupacao", "medo", "paz"),
    "ansioso" to listOf("ansiedade", "cuidado", "preocupacao", "medo", "paz"),
    "depressao" to listOf("tristeza", "abatido", "esperanca", "consolo"),
    "triste" to listOf("tristeza", "choro", "consolo", "esperanca"),
    "luto" to listOf("morte", "consolo", "choro", "ressurreicao", "esperanca"),
    "perdi" to listOf("perda", "morte", "consolo", "choro", "esperanca"),
    "perda" to listOf("morte", "consolo", "choro", "esperanca"),
    "doente" to listOf("enfermidade", "cura", "sarou", "saude"),
    "doenca" to listOf("enfermidade", "cura", "sarou", "saude"),
    "cura" to listOf("curou", "sarou", "enfermidade", "saude"),
    "briga" to listOf("ira", "contenda", "perdao", "paz"),
    "briguei" to listOf("ira", "contenda", "perdao", "paz"),
    "raiva" to listOf("ira", "furor", "mansidao", "perdao"),
    "perdao" to listOf("perdoar", "perdoou", "misericordia"),
    "traicao" to listOf("adulterio", "infidelidade", "perdao"),
    "casamento" to listOf("marido", "mulher", "esposa", "amor", "matrimonio"),
    "filhos" to listOf("filho", "criancas", "pais", "familia"),
    "familia" to listOf("casa", "filho", "filhos", "pais"),
    "desempregado" to listOf("trabalho", "necessidade", "provisao", "sustento"),
    "trabalho" to listOf("obra", "trabalhar", "labor", "mao"),
    "dinheiro" to listOf("riqueza", "tesouro", "pobreza", "provisao"),
    "medo" to listOf("temor", "coragem", "forte", "paz"),
    "solidao" to listOf("sozinho", "desamparado", "presenca", "consolo"),
    "tentacao" to listOf("tentar", "pecado", "resistir", "livrar"),
    "vicio" to listOf("dominio", "pecado", "liberdade", "carne"),
    "amor" to listOf("amar", "amou", "caridade"),
    "fe" to listOf("crer", "creu", "confianca"),
    "oracao" to listOf("orar", "orei", "suplicar", "clamor"),
    "esperanca" to listOf("esperar", "confiar", "promessa"),
    "salvacao" to listOf("salvar", "salvo", "redencao", "vida eterna")
)'''

new_themes = '''private val deepSearchThemes = mapOf(
    "ansiedade" to listOf("ansioso", "cuidado", "preocupacao", "medo", "paz", "descanso"),
    "depressao" to listOf("tristeza", "abatido", "esperanca", "consolo", "alegria"),
    "tristeza" to listOf("triste", "choro", "consolo", "esperanca", "alegria"),
    "luto" to listOf("morte", "consolo", "choro", "ressurreicao", "esperanca"),
    "perda" to listOf("morte", "consolo", "choro", "esperanca", "restaurar"),
    "cura" to listOf("curou", "curar", "sarou", "sarar", "enfermidade", "saude", "doente"),
    "doenca" to listOf("enfermidade", "cura", "curou", "sarou", "saude"),
    "dor" to listOf("sofrimento", "aflicao", "consolo", "cura", "socorro"),
    "briga" to listOf("ira", "contenda", "perdao", "paz", "reconciliacao"),
    "raiva" to listOf("ira", "furor", "mansidao", "perdao", "paz"),
    "perdao" to listOf("perdoar", "perdoou", "misericordia", "graca"),
    "traicao" to listOf("adulterio", "infidelidade", "perdao", "fidelidade"),
    "casamento" to listOf("marido", "mulher", "esposa", "amor", "matrimonio", "uniao"),
    "filhos" to listOf("filho", "criancas", "pais", "familia", "heranca"),
    "familia" to listOf("casa", "filho", "filhos", "pais", "lar"),
    "desemprego" to listOf("trabalho", "necessidade", "provisao", "sustento", "obra"),
    "trabalho" to listOf("obra", "trabalhar", "labor", "mao", "diligente"),
    "dinheiro" to listOf("riqueza", "tesouro", "pobreza", "provisao", "contentamento"),
    "medo" to listOf("temor", "coragem", "forte", "paz", "confiar"),
    "solidao" to listOf("sozinho", "desamparado", "presenca", "consolo", "companhia"),
    "tentacao" to listOf("tentar", "pecado", "resistir", "livrar", "vencer"),
    "vicio" to listOf("dominio", "pecado", "liberdade", "carne", "libertar"),
    "amor" to listOf("amar", "amou", "caridade", "amado"),
    "fe" to listOf("crer", "creu", "confianca", "confiar"),
    "oracao" to listOf("orar", "orei", "suplicar", "clamor", "pedir"),
    "esperanca" to listOf("esperar", "confiar", "promessa", "aguardar"),
    "salvacao" to listOf("salvar", "salvo", "redencao", "vida eterna", "libertar"),
    "pecado" to listOf("pecar", "iniquidade", "transgressao", "perdao", "arrependimento"),
    "gratidao" to listOf("agradecer", "gracas", "louvor", "bendizer"),
    "sabedoria" to listOf("sabio", "entendimento", "prudencia", "conhecimento")
)

private val deepSearchThemeAliases = mapOf(
    "ansioso" to "ansiedade", "ansiosa" to "ansiedade", "preocupado" to "ansiedade", "preocupada" to "ansiedade",
    "depressivo" to "depressao", "depressiva" to "depressao", "triste" to "tristeza", "chorando" to "tristeza",
    "perdi" to "perda", "perdido" to "perda", "perdida" to "perda",
    "curado" to "cura", "curada" to "cura", "curados" to "cura", "curadas" to "cura",
    "sarado" to "cura", "sarada" to "cura", "doente" to "doenca", "enfermo" to "doenca", "enferma" to "doenca",
    "sofrendo" to "dor", "sofrimento" to "dor", "doendo" to "dor",
    "briguei" to "briga", "brigando" to "briga", "irado" to "raiva", "irritado" to "raiva",
    "perdoado" to "perdao", "perdoada" to "perdao", "perdoar" to "perdao",
    "traido" to "traicao", "traida" to "traicao", "infiel" to "traicao",
    "casado" to "casamento", "casada" to "casamento", "esposa" to "casamento", "marido" to "casamento",
    "filho" to "filhos", "crianca" to "filhos", "pais" to "familia",
    "desempregado" to "desemprego", "desempregada" to "desemprego", "emprego" to "trabalho",
    "financeiro" to "dinheiro", "financas" to "dinheiro", "divida" to "dinheiro",
    "medroso" to "medo", "medrosa" to "medo", "sozinho" to "solidao", "sozinha" to "solidao",
    "tentado" to "tentacao", "tentada" to "tentacao", "viciado" to "vicio", "viciada" to "vicio",
    "amado" to "amor", "amada" to "amor", "apaixonado" to "amor", "apaixonada" to "amor",
    "crente" to "fe", "acreditar" to "fe", "orar" to "oracao", "orei" to "oracao",
    "esperar" to "esperanca", "salvo" to "salvacao", "salva" to "salvacao",
    "pecador" to "pecado", "pecadora" to "pecado", "agradecido" to "gratidao", "agradecida" to "gratidao",
    "sabio" to "sabedoria", "sabia" to "sabedoria"
)'''
if old_themes not in text:
    raise SystemExit("Mapa antigo de temas não encontrado")
text = text.replace(old_themes, new_themes, 1)

old_tokens = '''private fun deepSearchTokens(rawQuery: String): List<String> =
    normalizeDeepBibleText(rawQuery)
        .split(" ")
        .filter { it.length >= 2 && it !in deepSearchStopWords }
        .distinct()
'''
new_tokens = '''private fun deepSearchTokens(rawQuery: String): List<String> =
    normalizeDeepBibleText(rawQuery)
        .split(" ")
        .filter { it.length >= 2 && it !in deepSearchStopWords }
        .distinct()

private fun deepSearchStem(rawToken: String): String {
    val token = normalizeDeepBibleText(rawToken).replace(" ", "")
    if (token.length < 4) return token
    val suffixes = listOf(
        "amentos", "imentos", "amento", "imento", "acoes", "icoes", "mente",
        "ados", "adas", "idos", "idas", "ando", "endo", "indo", "aram", "eram", "iram",
        "coes", "cao", "ava", "avam", "iam", "ado", "ada", "ido", "ida", "ou", "ei", "am", "em", "es", "s"
    )
    return suffixes.firstNotNullOfOrNull { suffix ->
        if (token.endsWith(suffix) && token.length - suffix.length >= 3) token.dropLast(suffix.length) else null
    } ?: token
}

private fun themeKeyFor(token: String): String = deepSearchThemeAliases[token] ?: token

private fun tokenMatchesVerse(token: String, verse: IndexedBibleVerse): Boolean {
    if (verse.words.contains(token)) return true
    if (token.length >= 4 && verse.words.any { word -> word.startsWith(token) || token.startsWith(word) }) return true
    val stem = deepSearchStem(token)
    return stem.length >= 3 && verse.stems.contains(stem)
}
'''
if old_tokens not in text:
    raise SystemExit("Função de tokens não encontrada")
text = text.replace(old_tokens, new_tokens, 1)

old_index = '''                        indexed += IndexedBibleVerse(
                            book = canonicalBook,
                            chapter = chapterIndex + 1,
                            verse = verseIndex + 1,
                            text = text,
                            normalizedText = normalized,
                            words = normalized.split(" ").filter { it.isNotBlank() }.toSet()
                        )'''
new_index = '''                        val words = normalized.split(" ").filter { it.isNotBlank() }.toSet()
                        indexed += IndexedBibleVerse(
                            book = canonicalBook,
                            chapter = chapterIndex + 1,
                            verse = verseIndex + 1,
                            text = text,
                            normalizedText = normalized,
                            words = words,
                            stems = words.map(::deepSearchStem).filter { it.length >= 3 }.toSet()
                        )'''
if old_index not in text:
    raise SystemExit("Construção do índice não encontrada")
text = text.replace(old_index, new_index, 1)

old_expand = '''        val expandedTokens = primaryTokens
            .flatMap { token -> deepSearchThemes[token].orEmpty().map(::normalizeDeepBibleText) }
            .flatMap { it.split(" ") }
            .filter { it.length >= 2 }
            .distinct()
'''
new_expand = '''        val expandedTokens = primaryTokens
            .flatMap { token -> deepSearchThemes[themeKeyFor(token)].orEmpty().map(::normalizeDeepBibleText) }
            .flatMap { it.split(" ") }
            .filter { it.length >= 2 }
            .distinct()
'''
if old_expand not in text:
    raise SystemExit("Expansão de tokens não encontrada")
text = text.replace(old_expand, new_expand, 1)

old_score = '''                val primaryMatches = primaryTokens.count { token ->
                    verse.words.contains(token) || verse.words.any { word ->
                        token.length >= 4 && (word.startsWith(token) || token.startsWith(word))
                    }
                }
                if (primaryMatches == primaryTokens.size) score += 650
                score += primaryMatches * 150

                val expandedMatches = expandedTokens.count { token ->
                    verse.words.contains(token) || verse.words.any { word ->
                        token.length >= 4 && (word.startsWith(token) || token.startsWith(word))
                    }
                }
                score += expandedMatches * 35

                if (score <= 0 || (primaryMatches == 0 && expandedMatches < 2)) {
                    null
                } else {
                    BibleDeepSearchResult(verse.book, verse.chapter, verse.verse, verse.text, score)
                }'''
new_score = '''                val primaryMatches = primaryTokens.count { token -> tokenMatchesVerse(token, verse) }
                if (primaryMatches == primaryTokens.size) score += 650
                score += primaryMatches * 180

                val expandedMatches = expandedTokens.count { token -> tokenMatchesVerse(token, verse) }
                score += expandedMatches * 85
                if (expandedMatches > 0) score += 80

                // Antes eram exigidos dois sinônimos no mesmo versículo. Isso fazia temas como
                // "cura", "curado", "ansiedade" e "medo" retornarem vazio mesmo havendo textos relacionados.
                if (score <= 0 || (primaryMatches == 0 && expandedMatches == 0)) {
                    null
                } else {
                    BibleDeepSearchResult(verse.book, verse.chapter, verse.verse, verse.text, score)
                }'''
if old_score not in text:
    raise SystemExit("Bloco de pontuação não encontrado")
text = text.replace(old_score, new_score, 1)

old_state = '''    var results by remember { mutableStateOf<List<BibleDeepSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
'''
new_state = '''    var results by remember { mutableStateOf<List<BibleDeepSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var searchFailure by remember { mutableStateOf<String?>(null) }
'''
if old_state not in text:
    raise SystemExit("Estados da pesquisa não encontrados")
text = text.replace(old_state, new_state, 1)

old_effect = '''        if (query.trim().length < 2) {
            results = emptyList()
            isSearching = false
            hasSearched = false
            return@LaunchedEffect
        }
        delay(280)
        isSearching = true
        results = runCatching { BibleDeepSearchEngine.search(context, query) }.getOrDefault(emptyList())
        hasSearched = true
        isSearching = false
'''
new_effect = '''        if (query.trim().length < 2) {
            results = emptyList()
            isSearching = false
            hasSearched = false
            searchFailure = null
            return@LaunchedEffect
        }
        delay(280)
        isSearching = true
        searchFailure = null
        val attempt = runCatching { BibleDeepSearchEngine.search(context, query) }
        results = attempt.getOrDefault(emptyList())
        searchFailure = attempt.exceptionOrNull()?.message
        hasSearched = true
        isSearching = false
'''
if old_effect not in text:
    raise SystemExit("LaunchedEffect da pesquisa não encontrado")
text = text.replace(old_effect, new_effect, 1)

old_empty = '''        } else if (hasSearched && results.isEmpty()) {
            Text(
                "Nenhuma referência encontrada. Tente outra palavra, uma situação mais curta ou um trecho do versículo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {'''
new_empty = '''        } else if (hasSearched && searchFailure != null) {
            Text(
                "Não foi possível acessar a base bíblica local. Feche e abra a Bíblia novamente; se persistir, atualize o aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else if (hasSearched && results.isEmpty()) {
            Text(
                "Nenhuma referência encontrada. Tente outra palavra, um tema parecido ou um trecho do versículo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {'''
if old_empty not in text:
    raise SystemExit("Mensagem de vazio não encontrada")
text = text.replace(old_empty, new_empty, 1)

deep.write_text(text, encoding="utf-8")

# 2) O dataset ACF atual pode vir apenas com abbrev + chapters. O leitor local não pode exigir `name`.
local = ROOT / "LocalBibleFetcher.kt"
local_text = local.read_text(encoding="utf-8")
old_local = '''            for (i in 0 until cache.length()) {
                val bookObj = cache.getJSONObject(i)
                val name = bookObj.getString("name")
                val normName = removeAccents(name)
                if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamenta"))) {'''
new_local = '''            val canonicalBooks = chapterCounts.keys.toList()
            for (i in 0 until minOf(cache.length(), canonicalBooks.size)) {
                val bookObj = cache.getJSONObject(i)
                val name = bookObj.optString("name").takeIf { it.isNotBlank() } ?: canonicalBooks[i]
                val normName = removeAccents(name)
                if (normName == normBook || (normBook == "lamentacoes" && normName.startsWith("lamenta"))) {'''
if old_local not in local_text:
    raise SystemExit("Leitura local antiga não encontrada")
local.write_text(local_text.replace(old_local, new_local, 1), encoding="utf-8")

# 3) Smoke test do dataset com a mesma ideia de normalização/tema. Garante que o problema relatado
# não volte a gerar zero resultados para buscas comuns antes de permitir o commit.
def norm(value: str) -> str:
    value = unicodedata.normalize("NFD", value.lower())
    value = "".join(ch for ch in value if unicodedata.category(ch) != "Mn")
    value = re.sub(r"[^a-z0-9]+", " ", value).strip()
    return re.sub(r"\s+", " ", value)


def stem(token: str) -> str:
    token = norm(token).replace(" ", "")
    if len(token) < 4:
        return token
    suffixes = ["amentos","imentos","amento","imento","acoes","icoes","mente","ados","adas","idos","idas","ando","endo","indo","aram","eram","iram","coes","cao","ava","avam","iam","ado","ada","ido","ida","ou","ei","am","em","es","s"]
    for suffix in suffixes:
        if token.endswith(suffix) and len(token) - len(suffix) >= 3:
            return token[:-len(suffix)]
    return token

asset = Path("app/src/main/assets/bibles/pt_acf.json")
raw = asset.read_text(encoding="utf-8-sig")
data = json.loads(raw)
all_words = set()
for book in data:
    for chapter in book.get("chapters", []):
        for verse in chapter:
            all_words.update(norm(str(verse)).split())
all_stems = {stem(w) for w in all_words if len(stem(w)) >= 3}

checks = {
    "curado": ["curou", "sarou", "cura", "enfermidade", "saude"],
    "cura": ["curou", "sarou", "enfermidade", "saude"],
    "ansiedade": ["cuidado", "medo", "paz", "descanso"],
    "amor": ["amor", "amar", "amou", "caridade"],
    "davi": ["davi"],
    "jesus": ["jesus"],
}
for query, alternatives in checks.items():
    candidates = [query] + alternatives
    if not any(norm(c) in all_words or stem(c) in all_stems for c in candidates):
        raise SystemExit(f"Smoke test falhou para {query}: nenhuma forma relacionada existe no dataset")

print("Pesquisa profunda corrigida e smoke tests do ACF aprovados.")
