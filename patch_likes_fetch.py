import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val textContent = document.getString("content") ?: ""\n                        newList.add(Devotional(id, title, date, verse, verseReference, textContent))',
    'val textContent = document.getString("content") ?: ""\n                        val likes = document.getLong("likes")?.toInt() ?: 0\n                        newList.add(Devotional(id, title, date, verse, verseReference, textContent, likes))'
)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val textContent = document.getString("content") ?: ""\n                        list.add(Devotional(id, title, date, verse, verseReference, textContent))',
    'val textContent = document.getString("content") ?: ""\n                        val likes = document.getLong("likes")?.toInt() ?: 0\n                        list.add(Devotional(id, title, date, verse, verseReference, textContent, likes))'
)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
