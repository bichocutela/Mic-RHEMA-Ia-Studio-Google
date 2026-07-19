import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """                        devotionalsState.add(
                            0,
                            Devotional(
                                id = (devotionalsState.size + 1).toString(),
                                title = title,
                                date = date,
                                verse = verse,
                                verseReference = verseRef,
                                content = content
                            )
                        )
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Novo Devocional Publicado! 📖",
                            message = title
                        )
                        title = ""
                        date = ""
                        verse = ""
                        verseRef = ""
                        content = ""
                    }"""

replacement = """                        val newId = java.util.UUID.randomUUID().toString()
                        val newDevotional = hashMapOf(
                            "title" to title,
                            "date" to date,
                            "verse" to verse,
                            "verseReference" to verseRef,
                            "content" to content,
                            "likes" to 0
                        )
                        
                        try {
                            val db = com.google.firebase.Firebase.firestore
                            db.collection("devotionals").document(newId).set(newDevotional)
                                .addOnSuccessListener {
                                    NotificationHelper.showNotification(
                                        context = context,
                                        title = "Novo Devocional Publicado! 📖",
                                        message = title
                                    )
                                    title = ""
                                    date = ""
                                    verse = ""
                                    verseRef = ""
                                    content = ""
                                }
                        } catch (e: Exception) {
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Erro",
                                message = "Falha ao salvar no Firestore"
                            )
                        }
                    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
