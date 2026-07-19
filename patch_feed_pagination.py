import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """@Composable
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
                        val likes = document.getLong("likes")?.toInt() ?: 0
                        list.add(Devotional(id, title, date, verse, verseReference, textContent, likes))
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
}"""

replacement = """@Composable
fun DevotionalFeed() {
    var devotionals by remember { mutableStateOf<List<Devotional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var lastDocument by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    val PAGE_SIZE = 10L

    val loadMore = {
        if (!isLoadingMore && hasMore) {
            if (devotionals.isNotEmpty()) isLoadingMore = true
            try {
                val db = com.google.firebase.Firebase.firestore
                var query = db.collection("devotionals")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE)
                
                lastDocument?.let { 
                    query = query.startAfter(it)
                }

                query.get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            hasMore = false
                        } else {
                            lastDocument = result.documents.lastOrNull()
                            val list = mutableListOf<Devotional>()
                            for (document in result) {
                                val id = document.id
                                val title = document.getString("title") ?: ""
                                val date = document.getString("date") ?: ""
                                val verse = document.getString("verse") ?: ""
                                val verseReference = document.getString("verseReference") ?: ""
                                val textContent = document.getString("content") ?: ""
                                val likes = document.getLong("likes")?.toInt() ?: 0
                                list.add(Devotional(id, title, date, verse, verseReference, textContent, likes))
                            }
                            devotionals = devotionals + list
                            if (result.size() < PAGE_SIZE) {
                                hasMore = false
                            }
                        }
                        isLoading = false
                        isLoadingMore = false
                    }
                    .addOnFailureListener {
                        hasError = true
                        isLoading = false
                        isLoadingMore = false
                    }
            } catch (e: Exception) {
                hasError = true
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMore()
    }

    if (isLoading && devotionals.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (hasError && devotionals.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum devocional encontrado.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.foundation.lazy.itemsIndexed(devotionals) { index, dev ->
                DevotionalFeedItem(devotional = dev)
                
                if (index == devotionals.size - 1 && !isLoadingMore && hasMore) {
                    LaunchedEffect(index) {
                        loadMore()
                    }
                }
            }
            if (isLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
