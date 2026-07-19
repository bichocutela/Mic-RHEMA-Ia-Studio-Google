import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

new_component = """@Composable
fun FirestoreDailyDevotional(
    onReadFull: (Devotional) -> Unit,
    onShare: (Devotional) -> Unit
) {
    var devotional by remember { mutableStateOf<Devotional?>(null) }
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
                        devotional = list.first()
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
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (hasError || devotional == null) {
        // Fallback to offline or standard daily devotional card if error or empty
        DailyDevotionalCard(
            devotional = devotionalsState.firstOrNull(),
            onReadFull = onReadFull,
            onShare = onShare
        )
    } else {
        DailyDevotionalCard(
            devotional = devotional,
            onReadFull = onReadFull,
            onShare = onShare
        )
    }
}
"""

content = content + "\n\n" + new_component

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
