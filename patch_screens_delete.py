import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target = """                                        memberRequestsState.remove(req)
                                        MemberManager.saveMembers(context)
                                        NotificationHelper.showNotification("""

replacement = """                                        memberRequestsState.remove(req)
                                        MemberManager.saveMembers(context)
                                        MemberManager.deleteFromFirestore(req)
                                        NotificationHelper.showNotification("""
content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
