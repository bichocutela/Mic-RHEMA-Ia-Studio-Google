import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = "MemberManager.setLoggedInMember(context, null)"
replacement = "MemberManager.setLoggedInMember(context, null)"

# It seems correct already, let's verify if the compile passes.
