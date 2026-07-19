import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace(
    'implementation(libs.firebase.firestore)',
    'implementation(libs.firebase.firestore)\n    implementation(libs.firebase.messaging)'
)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
