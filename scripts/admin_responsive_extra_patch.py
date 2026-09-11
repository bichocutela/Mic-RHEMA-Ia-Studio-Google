from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def replace(rel, old, new, count=1):
    p = ROOT / rel
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'marker not found in {rel}: {old[:100]!r}')
    p.write_text(text.replace(old, new, count), encoding='utf-8')

# Abas do aplicativo: ações não disputam espaço com o texto e filtros podem rolar.
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminTabs.kt',
    '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Organize a navegação principal do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(onClick = { showPreview = !showPreview }) {
                    Icon(if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showPreview) "Ocultar" else "Visualizar")
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar aba")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
        }''',
    '''        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Organize a navegação principal do aplicativo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AdminAdaptivePair(
                first = { modifier ->
                    FilledTonalButton(onClick = { showPreview = !showPreview }, modifier = modifier) {
                        Icon(if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showPreview) "Ocultar" else "Visualizar")
                    }
                },
                second = { modifier ->
                    Button(onClick = { showAddDialog = true }, modifier = modifier) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar aba")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar")
                    }
                }
            )
        }'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminTabs.kt',
    '''                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {''',
    '''                        AdminAdaptivePair(
                            first = { modifier -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminTabs.kt',
    '''                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { moveAdminTab(tab.id, -1) },''',
    '''                                }
                            } },
                            second = { modifier -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                IconButton(
                                    onClick = { moveAdminTab(tab.id, -1) },'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminTabs.kt',
    '''                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {''',
    '''                                }
                            } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminTabs.kt',
    '''                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TabContentType.values().filter { it != TabContentType.SYSTEM }.forEach { type ->''',
    '''                    AdminSafeHorizontalRow {
                        TabContentType.values().filter { it != TabContentType.SYSTEM }.forEach { type ->'''
)

# Discipulado: título e botão empilham em tela pequena.
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminDiscipulado.kt',
    '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Estudos de Discipulado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Publique PDFs para todos os usuários do aplicativo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { pdfPicker.launch("application/pdf") }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar PDF")
                Spacer(Modifier.size(6.dp))
                Text("Novo PDF")
            }
        }''',
    '''        AdminActionHeader(
            title = "Estudos de Discipulado",
            subtitle = "Publique PDFs para todos os usuários do aplicativo.",
            actionText = "Novo PDF",
            onAction = { pdfPicker.launch("application/pdf") }
        )'''
)

# Paginação compartilhada: página fica acima dos botões apenas em telas estreitas.
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminPagination.kt',
    '''            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                ) { Text("Anterior") }

                Text(
                    "Página ${pagerState.currentPage + 1} de $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    enabled = pagerState.currentPage < totalPages - 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                ) { Text("Próxima") }
            }''',
    '''            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 380.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Página ${pagerState.currentPage + 1} de $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = pagerState.currentPage > 0,
                                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                                modifier = Modifier.weight(1f)
                            ) { Text("Anterior") }
                            OutlinedButton(
                                enabled = pagerState.currentPage < totalPages - 1,
                                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                                modifier = Modifier.weight(1f)
                            ) { Text("Próxima") }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            enabled = pagerState.currentPage > 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                        ) { Text("Anterior") }
                        Text(
                            "Página ${pagerState.currentPage + 1} de $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            enabled = pagerState.currentPage < totalPages - 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                        ) { Text("Próxima") }
                    }
                }
            }'''
)

# Efeitos de luz: pares de botões empilham quando a largura não comportar; tonalidade rola.
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminXpLightEffects.kt',
    '''                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { preview = item to false }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Prévia")
                        }
                        OutlinedButton(onClick = { preview = item to true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Testar")
                        }
                    }''',
    '''                    AdminAdaptivePair(
                        first = { modifier -> OutlinedButton(onClick = { preview = item to false }, modifier = modifier) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Prévia")
                        } },
                        second = { modifier -> OutlinedButton(onClick = { preview = item to true }, modifier = modifier) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Testar")
                        } }
                    )'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminXpLightEffects.kt',
    '''                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { editor = item }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Editar")
                        }
                        OutlinedButton(onClick = { deleting = item }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Excluir")
                        }
                    }''',
    '''                    AdminAdaptivePair(
                        first = { modifier -> OutlinedButton(onClick = { editor = item }, modifier = modifier) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Editar")
                        } },
                        second = { modifier -> OutlinedButton(onClick = { deleting = item }, modifier = modifier) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Excluir")
                        } }
                    )'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminXpLightEffects.kt',
    '''                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("suave" to "Suave", "medio" to "Médio", "forte" to "Luz forte").forEach''',
    '''                AdminSafeHorizontalRow {
                    listOf("suave" to "Suave", "medio" to "Médio", "forte" to "Luz forte").forEach'''
)

# Chips de dificuldade nos editores XP podem rolar em fonte grande/tela pequena.
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminXpBadges.kt',
    '''                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach''',
    '''                AdminSafeHorizontalRow {
                    listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach'''
)
replace(
    'app/src/main/java/com/aistudio/micrhema/AdminXpCosmetics.kt',
    '''                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach''',
    '''                    AdminSafeHorizontalRow {
                        listOf("easy" to "Fácil", "medium" to "Médio", "hard" to "Difícil").forEach'''
)
