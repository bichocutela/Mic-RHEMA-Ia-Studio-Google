import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace(
    '<application',
    '<application\n        android:name=".MICRhemaApplication"'
)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
