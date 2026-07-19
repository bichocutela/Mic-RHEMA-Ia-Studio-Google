import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target1 = """                                        memberRequestsState.remove(req)
                                        MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                        NotificationHelper.showNotification("""

replacement1 = """                                        memberRequestsState.remove(req)
                                        MemberManager.saveMembers(context)
                                        NotificationHelper.showNotification("""

content = content.replace(target1, replacement1)

target2 = """                                            MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                            MemberManager.setLoggedInMember(context, target)"""

replacement2 = """                                            MemberManager.saveMembers(context)
                                            MemberManager.setLoggedInMember(context, target)"""

content = content.replace(target2, replacement2)

target3 = """                                        MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                        MemberManager.setLoggedInMember(context, target)"""

replacement3 = """                                        MemberManager.saveMembers(context)
                                        MemberManager.setLoggedInMember(context, target)"""

content = content.replace(target3, replacement3)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
