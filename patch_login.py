import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

target1 = """                                                memberRequestsState.add(newReq)
                                                newReq
                                            }
                                            loggedInMemberState.value = target"""

replacement1 = """                                                memberRequestsState.add(newReq)
                                                newReq
                                            }
                                            MemberManager.saveMembers(context)
                                            MemberManager.setLoggedInMember(context, target)"""

content = content.replace(target1, replacement1)

target2 = """                                            memberRequestsState.add(newReq)
                                            newReq
                                        }
                                        loggedInMemberState.value = target"""

replacement2 = """                                            memberRequestsState.add(newReq)
                                            newReq
                                        }
                                        MemberManager.saveMembers(context)
                                        MemberManager.setLoggedInMember(context, target)"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
