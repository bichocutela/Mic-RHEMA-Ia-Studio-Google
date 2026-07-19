import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace(
    'firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }',
    'firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }\nfirebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }'
)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
