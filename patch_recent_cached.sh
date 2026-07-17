sed -i 's/val type: ContentType/val type: ContentType,\n    val isCached: Boolean = false/g' app/src/main/java/com/aistudio/micrhema/Data.kt
sed -i 's/ContentType.BOOK))/ContentType.BOOK, it.isCached))/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/ContentType.AUDIO))/ContentType.AUDIO, it.isCached))/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/ContentType.VIDEO))/ContentType.VIDEO, it.isCached))/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
