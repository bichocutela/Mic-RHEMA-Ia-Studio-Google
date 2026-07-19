import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

target = """data class Devotional(
    val id: String,
    val title: String,
    val date: String,
    val verse: String,
    val verseReference: String,
    val content: String
)"""

replacement = """data class Devotional(
    val id: String,
    val title: String,
    val date: String,
    val verse: String,
    val verseReference: String,
    val content: String,
    var likes: Int = 0
)"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)
