import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

target = """fun loadDevotionalsFromJson(context: Context) {"""
replacement = """object DevotionalManager {
    fun syncDevotionals(context: Context) {
        try {
            val db = Firebase.firestore
            db.collection("devotionals").get()
                .addOnSuccessListener { result ->
                    val newList = mutableListOf<Devotional>()
                    for (document in result) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val date = document.getString("date") ?: ""
                        val verse = document.getString("verse") ?: ""
                        val verseReference = document.getString("verseReference") ?: ""
                        val textContent = document.getString("content") ?: ""
                        newList.add(Devotional(id, title, date, verse, verseReference, textContent))
                    }
                    if (newList.isNotEmpty()) {
                        newList.sortByDescending { it.date }
                        devotionalsState.clear()
                        devotionalsState.addAll(newList)
                        
                        val dbHelper = IbrDatabaseHelper(context)
                        dbHelper.saveCachedDevotionals(newList)
                    }
                }
                .addOnFailureListener {
                    // fallback to local cache
                    val cachedDevotionals = IbrDatabaseHelper(context).getCachedDevotionals()
                    if (cachedDevotionals.isNotEmpty()) {
                        devotionalsState.clear()
                        devotionalsState.addAll(cachedDevotionals)
                    }
                }
        } catch (e: Exception) {
            Log.e("DevotionalManager", "Firestore not initialized or error", e)
        }
    }
}

fun loadDevotionalsFromJson(context: Context) {"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)
