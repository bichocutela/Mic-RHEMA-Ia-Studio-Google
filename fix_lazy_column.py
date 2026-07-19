import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """            androidx.compose.foundation.lazy.itemsIndexed(devotionals) { index, dev ->
                DevotionalFeedItem(devotional = dev)
                
                if (index == devotionals.size - 1 && !isLoadingMore && hasMore) {
                    LaunchedEffect(index) {
                        loadMore()
                    }
                }
            }"""

replacement = """            items(devotionals.size) { index ->
                val dev = devotionals[index]
                DevotionalFeedItem(devotional = dev)
                
                if (index == devotionals.size - 1 && !isLoadingMore && hasMore) {
                    LaunchedEffect(index) {
                        loadMore()
                    }
                }
            }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
