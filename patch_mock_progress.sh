sed -i 's/isCached = true)/isCached = true, progress = 0.35f)/g' app/src/main/java/com/aistudio/micrhema/Data.kt
sed -i 's/isCached = true, progress = 0.35f)/isCached = true, progress = 0.8f)/g' app/src/main/java/com/aistudio/micrhema/Data.kt
# wait, replacing all will make them all 0.8f, let's just do it directly.
