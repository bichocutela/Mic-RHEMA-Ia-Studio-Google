import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

if "import com.google.firebase.firestore.firestore" not in content:
    content = content.replace("package com.aistudio.micrhema", "package com.aistudio.micrhema\n\nimport com.google.firebase.firestore.firestore\nimport com.google.firebase.Firebase")

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
