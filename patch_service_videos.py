import re

with open('app/src/main/java/com/aistudio/micrhema/ServiceVideos.kt', 'r') as f:
    content = f.read()

target = """            videos = fetchedVideos"""

replacement = """            if (fetchedVideos.isEmpty()) {
                videos = listOf(
                    ServiceVideoModel(
                        id = "mock_1",
                        title = "Culto de Domingo - Família",
                        date = "Domingo, 10h",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        thumbnailUrl = "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80"
                    ),
                    ServiceVideoModel(
                        id = "mock_2",
                        title = "Culto de Celebração",
                        date = "Domingo, 18h",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        thumbnailUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65?w=500&q=80"
                    )
                )
            } else {
                videos = fetchedVideos
            }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/ServiceVideos.kt', 'w') as f:
    f.write(content)
