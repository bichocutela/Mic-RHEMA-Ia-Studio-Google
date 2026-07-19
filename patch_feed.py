import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

new_components = """
@Composable
fun DevotionalFeed() {
    var devotionals by remember { mutableStateOf<List<Devotional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val db = com.google.firebase.Firebase.firestore
            db.collection("devotionals")
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<Devotional>()
                    for (document in result) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val date = document.getString("date") ?: ""
                        val verse = document.getString("verse") ?: ""
                        val verseReference = document.getString("verseReference") ?: ""
                        val textContent = document.getString("content") ?: ""
                        list.add(Devotional(id, title, date, verse, verseReference, textContent))
                    }
                    if (list.isNotEmpty()) {
                        list.sortByDescending { it.date }
                        devotionals = list
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    hasError = true
                    isLoading = false
                }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (hasError || devotionals.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum devocional encontrado.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(devotionals) { dev ->
                DevotionalFeedItem(devotional = dev)
            }
        }
    }
}

@Composable
fun DevotionalFeedItem(devotional: Devotional) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = devotional.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = devotional.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = devotional.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
"""

content = content + "\n" + new_components

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
