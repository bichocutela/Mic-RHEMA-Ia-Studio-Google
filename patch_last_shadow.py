import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

content = re.sub(r'\.shadow\(1\.dp, RoundedCornerShape\(24\.dp\)\)', '', content)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
