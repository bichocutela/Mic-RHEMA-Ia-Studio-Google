import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

# HomeCarousel shadow
content = re.sub(r'\.shadow\(\s*elevation = if \(isSelected\) 8\.dp else 2\.dp,\s*shape = RoundedCornerShape\(20\.dp\),?\s*.*?\)', '', content, flags=re.DOTALL)
# Another possible match format if there are more parameters
content = re.sub(r'\.shadow\(\s*elevation = if \(isSelected\) 8\.dp else 2\.dp,\s*shape = RoundedCornerShape\(20\.dp\)\s*\)', '', content)

# Audio card shadow
content = re.sub(r'\.shadow\(8\.dp, RoundedCornerShape\(24\.dp\)\)', '.shadow(1.dp, RoundedCornerShape(24.dp))', content)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
