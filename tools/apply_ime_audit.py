from pathlib import Path

ROOT = Path("app/src/main/java/com/aistudio/micrhema")


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    if old not in text:
        raise SystemExit(f"Padrão não encontrado em {rel}: {old[:100]!r}")
    write(rel, text.replace(old, new, 1))


# Correção estrutural para toda a navegação: como o app é edge-to-edge,
# o conteúdo precisa consumir explicitamente o inset do teclado (IME).
replace_once(
    "MainActivity.kt",
    "    val isCompact = configuration.screenWidthDp < 600\n    val visibleTabs = appTabsState",
    "    val isCompact = configuration.screenWidthDp < 600\n    val density = androidx.compose.ui.platform.LocalDensity.current\n    val isImeVisible = WindowInsets.ime.getBottom(density) > 0\n    val visibleTabs = appTabsState",
)

replace_once(
    "MainActivity.kt",
    '''                bottomBar = {
                    Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                        PersistentAudioPlayerBar()
                        if (isCompact) {
                            FloatingNavigationBar(
                                items = bottomBarItems,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                },
                                onMenuClick = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }''',
    '''                bottomBar = {
                    if (!isImeVisible) {
                        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                            PersistentAudioPlayerBar()
                            if (isCompact) {
                                FloatingNavigationBar(
                                    items = bottomBarItems,
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    },
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }
                }''',
)

replace_once(
    "MainActivity.kt",
    "            Column(modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues).fillMaxSize()) {",
    "            Column(modifier = Modifier.padding(paddingValues).consumeWindowInsets(paddingValues).imePadding().fillMaxSize()) {",
)

# A lista da Bíblia contém a pesquisa profunda depois de Apocalipse. Ela recebe
# proteção própria do IME para permitir que o item focado seja trazido para cima.
bible = read("BibleModule.kt")
if "import androidx.compose.foundation.layout.imePadding" not in bible:
    bible = bible.replace(
        "import androidx.compose.foundation.layout.heightIn\n",
        "import androidx.compose.foundation.layout.heightIn\nimport androidx.compose.foundation.layout.imePadding\n",
        1,
    )
marker = '''    val bestMatch = visibleBooks.firstOrNull()

    LazyColumn(
        modifier = modifier,'''
replacement = '''    val bestMatch = visibleBooks.firstOrNull()

    LazyColumn(
        modifier = modifier.imePadding(),'''
if marker not in bible:
    raise SystemExit("Padrão do seletor da Bíblia não encontrado")
bible = bible.replace(marker, replacement, 1)
write("BibleModule.kt", bible)

# Formulários roláveis encontrados na auditoria. O padding do IME cria espaço
# para rolar automaticamente o campo focado acima do teclado.
scroll_form_files = [
    "PrayerScreen.kt",
    "ProfileScreen.kt",
    "NewsAdmin.kt",
    "MembersAdmin.kt",
    "VipAdmin.kt",
    "ContentAdmin.kt",
]
for rel in scroll_form_files:
    text = read(rel)
    if "TextField(" not in text:
        raise SystemExit(f"{rel} deixou de conter campos de texto")
    if ".verticalScroll(rememberScrollState())" not in text:
        raise SystemExit(f"{rel} deixou de conter verticalScroll esperado")
    if ".imePadding().verticalScroll(rememberScrollState())" not in text:
        text = text.replace(
            ".verticalScroll(rememberScrollState())",
            ".imePadding().verticalScroll(rememberScrollState())",
        )
    if "import androidx.compose.foundation.layout.*" not in text and "import androidx.compose.foundation.layout.imePadding" not in text:
        layout_import = "import androidx.compose.foundation.layout."
        start = text.find(layout_import)
        if start >= 0:
            insert_at = text.find("\n", start)
            text = text[: insert_at + 1] + "import androidx.compose.foundation.layout.imePadding\n" + text[insert_at + 1 :]
        else:
            text = text.replace(
                "package com.aistudio.micrhema\n",
                "package com.aistudio.micrhema\n\nimport androidx.compose.foundation.layout.imePadding\n",
                1,
            )
    write(rel, text)

# Diálogos de formulário de maior risco em telas pequenas: rolagem + IME.
dialog_targets = {
    "TeamModule.kt": [
        (
            "            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {",
            "            Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {",
        )
    ],
    "CustomTabScreen.kt": [
        (
            "                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {",
            "                Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {",
        )
    ],
    "AdminTabs.kt": [
        (
            "                Column {\n                    OutlinedTextField(",
            "                Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState())) {\n                    OutlinedTextField(",
        )
    ],
    "AdminServicesV2.kt": [
        (
            "text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {",
            "text = { Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {",
        ),
        (
            "LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp)) {",
            "LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp).imePadding()) {",
        ),
    ],
}

for rel, replacements in dialog_targets.items():
    text = read(rel)
    changed = False
    for old, new in replacements:
        if new in text:
            continue
        if old not in text:
            raise SystemExit(f"Padrão de diálogo não encontrado em {rel}: {old[:100]!r}")
        text = text.replace(old, new, 1)
        changed = True
    if changed and ".verticalScroll(rememberScrollState())" in text:
        if "import androidx.compose.foundation.rememberScrollState" not in text:
            anchor = "import androidx.compose.foundation.clickable\n"
            imports = "import androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n"
            if anchor in text:
                text = text.replace(anchor, anchor + imports, 1)
            else:
                text = text.replace("package com.aistudio.micrhema\n", "package com.aistudio.micrhema\n\n" + imports, 1)
    write(rel, text)

# Garantias mínimas antes de permitir o commit.
main = read("MainActivity.kt")
assert ".consumeWindowInsets(paddingValues).imePadding().fillMaxSize()" in main
assert "if (!isImeVisible)" in main
assert "WindowInsets.ime.getBottom(density) > 0" in main
assert "modifier = modifier.imePadding()" in read("BibleModule.kt")
print("Correção global do teclado aplicada com sucesso.")
