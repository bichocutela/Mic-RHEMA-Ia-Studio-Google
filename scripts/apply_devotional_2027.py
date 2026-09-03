from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Trecho esperado não encontrado em {path}: {old[:120]!r}")
    text = text.replace(old, new, 1)
    file.write_text(text, encoding="utf-8")


screen = "app/src/main/java/com/aistudio/micrhema/DevotionalsScreen.kt"
replace_once(
    screen,
    "import androidx.compose.material.icons.filled.Favorite\n",
    "import androidx.compose.material.icons.filled.Favorite\nimport androidx.compose.material.icons.filled.Sort\n",
)
replace_once(
    screen,
    """        var isRefreshing by remember { mutableStateOf(false) }\n        val coroutineScope = rememberCoroutineScope()\n        \n        Scaffold(""",
    """        var isRefreshing by remember { mutableStateOf(false) }\n        var newestFirst by rememberSaveable { mutableStateOf(true) }\n        var sortMenuExpanded by remember { mutableStateOf(false) }\n        val coroutineScope = rememberCoroutineScope()\n        val today = java.time.LocalDate.now()\n        val availableDevotionals = remember(devotionalsState.toList(), newestFirst, today) {\n            val dated = DevotionalDateUtils.availableUntilToday(devotionalsState.toList(), today)\n                .sortedWith(\n                    compareBy<Devotional> { DevotionalDateUtils.parse(it.date) ?: java.time.LocalDate.MIN }\n                        .thenBy { it.timestamp }\n                )\n            if (newestFirst) dated.asReversed() else dated\n        }\n        \n        Scaffold(""",
)
replace_once(
    screen,
    """                    Text(\n                        text = \"Devocionais\",\n                        style = MaterialTheme.typography.headlineMedium,\n                        fontWeight = FontWeight.Bold,\n                        color = MaterialTheme.colorScheme.onBackground\n                    )\n                }""",
    """                    Text(\n                        text = \"Devocionais\",\n                        style = MaterialTheme.typography.headlineMedium,\n                        fontWeight = FontWeight.Bold,\n                        color = MaterialTheme.colorScheme.onBackground,\n                        modifier = Modifier.weight(1f)\n                    )\n                    Box {\n                        FilledTonalButton(\n                            onClick = { sortMenuExpanded = true },\n                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)\n                        ) {\n                            Icon(\n                                Icons.Default.Sort,\n                                contentDescription = \"Ordenar devocionais\",\n                                modifier = Modifier.size(18.dp)\n                            )\n                            Spacer(modifier = Modifier.width(6.dp))\n                            Text(if (newestFirst) \"Recentes\" else \"Antigos\")\n                        }\n                        DropdownMenu(\n                            expanded = sortMenuExpanded,\n                            onDismissRequest = { sortMenuExpanded = false }\n                        ) {\n                            DropdownMenuItem(\n                                text = { Text(\"Mais recente\") },\n                                onClick = {\n                                    newestFirst = true\n                                    sortMenuExpanded = false\n                                }\n                            )\n                            DropdownMenuItem(\n                                text = { Text(\"Mais antigo\") },\n                                onClick = {\n                                    newestFirst = false\n                                    sortMenuExpanded = false\n                                }\n                            )\n                        }\n                    }\n                }""",
)
replace_once(
    screen,
    """                        val sdf = java.text.SimpleDateFormat(\"yyyy-MM-dd\", java.util.Locale.getDefault())\n                        val todayStr = sdf.format(java.util.Date())\n                        val availableDevotionals = devotionalsState.filter { it.date <= todayStr }\n                        items(availableDevotionals.sortedByDescending { it.date }) { devotional ->""",
    """                        items(availableDevotionals, key = { it.id.ifBlank { \"${it.date}:${it.title}\" } }) { devotional ->""",
)
# Há duas exibições de data: cartão e detalhe.
text = Path(screen).read_text(encoding="utf-8")
old = "text = devotional.date,"
if text.count(old) < 2:
    raise SystemExit("Não foram encontradas as duas exibições de data dos devocionais.")
text = text.replace(old, "text = DevotionalDateUtils.display(devotional.date),", 2)
Path(screen).write_text(text, encoding="utf-8")

home = "app/src/main/java/com/aistudio/micrhema/HomeScreen.kt"
replace_once(
    home,
    """    // Devotional Logic\n    val formatter = DateTimeFormatter.ofPattern(\"dd/MM/yyyy\")\n    val todayStr = today.format(formatter)\n    val todayDevotional = devotionalsState.find { it.date == todayStr } ?: devotionalsState.firstOrNull()""",
    """    // Devocional Diário: aceita todos os formatos usados historicamente e\n    // prefere exatamente a data de hoje. Se não houver publicação para hoje,\n    // usa somente o devocional anterior mais recente, nunca um conteúdo futuro.\n    val todayDevotional = DevotionalDateUtils.todayOrLatest(devotionalsState.toList(), today)""",
)

worker = "app/src/main/java/com/aistudio/micrhema/DevotionalReminderWorker.kt"
replace_once(
    worker,
    """            val candidates = (remote + local).distinctBy { it.id.ifBlank { \"${it.date}:${it.title}\" } }\n                .filter { it.isApproved }""",
    """            val candidates = (remote + local + DevotionalCalendar2027.items)\n                .distinctBy { it.id.ifBlank { \"${it.date}:${it.title}\" } }\n                .filter { it.isApproved }""",
)

print("Ajustes de devocionais aplicados com sucesso.")
