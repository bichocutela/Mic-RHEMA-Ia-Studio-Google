sed -i 's/fun BooksList()/fun BooksList(selectedBook: ContentBook?, onBookSelected: (ContentBook?) -> Unit)/' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/var selectedBook by remember { mutableStateOf<ContentBook?>(null) }//' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onClick = { selectedBook = book }/onClick = { onBookSelected(book) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/clickable { selectedBook = book }/clickable { onBookSelected(book) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onBack = { selectedBook = null }/onBack = { onBookSelected(null) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt

sed -i 's/fun AudiosList()/fun AudiosList(selectedAudio: ContentAudio?, onAudioSelected: (ContentAudio?) -> Unit)/' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/var selectedAudio by remember { mutableStateOf<ContentAudio?>(null) }//' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onClick = { selectedAudio = audio }/onClick = { onAudioSelected(audio) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/clickable { selectedAudio = audio }/clickable { onAudioSelected(audio) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onClick = { selectedAudio = null }/onClick = { onAudioSelected(null) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt

sed -i 's/fun VideosList()/fun VideosList(selectedVideo: ContentVideo?, onVideoSelected: (ContentVideo?) -> Unit)/' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/var selectedVideo by remember { mutableStateOf<ContentVideo?>(null) }//' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onClick = { selectedVideo = video }/onClick = { onVideoSelected(video) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/clickable { selectedVideo = video }/clickable { onVideoSelected(video) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
sed -i 's/onClick = { selectedVideo = null }/onClick = { onVideoSelected(null) }/g' app/src/main/java/com/aistudio/micrhema/ContentScreens.kt
