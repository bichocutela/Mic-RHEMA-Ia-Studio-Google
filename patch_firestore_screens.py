import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

# For EditMembersSection approval/toggles
content = content.replace(
    "MemberManager.saveMembers(context)",
    "MemberManager.saveMembers(context)\n                                            MemberManager.saveToFirestore(updated)"
)

# For login new request
content = content.replace(
    "memberRequestsState.add(newReq)\n                                                newReq\n                                            }\n                                            MemberManager.saveMembers(context)",
    "memberRequestsState.add(newReq)\n                                                MemberManager.saveToFirestore(newReq)\n                                                newReq\n                                            }\n                                            MemberManager.saveMembers(context)"
)
content = content.replace(
    "memberRequestsState.add(newReq)\n                                            newReq\n                                        }\n                                        MemberManager.saveMembers(context)",
    "memberRequestsState.add(newReq)\n                                            MemberManager.saveToFirestore(newReq)\n                                            newReq\n                                        }\n                                        MemberManager.saveMembers(context)"
)


with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
