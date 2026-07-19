import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

target_versions = "[versions]"
replacement_versions = "[versions]\nfirebaseBom = \"33.1.2\""
content = content.replace(target_versions, replacement_versions)

target_libraries = "[libraries]"
replacement_libraries = "[libraries]\nfirebase-bom = { group = \"com.google.firebase\", name = \"firebase-bom\", version.ref = \"firebaseBom\" }\nfirebase-firestore = { group = \"com.google.firebase\", name = \"firebase-firestore\" }"
content = content.replace(target_libraries, replacement_libraries)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
