import re

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'r') as f:
    content = f.read()

# Replace loggedInMemberState.value = req
content = content.replace(
    "loggedInMemberState.value = req",
    "MemberManager.setLoggedInMember(context, req)"
)

# Replace loggedInMemberState.value = null
content = content.replace(
    "loggedInMemberState.value = null",
    "MemberManager.setLoggedInMember(context, null)"
)

# For EditMembersSection
content = content.replace(
    "memberRequestsState.remove(req)",
    "memberRequestsState.remove(req)\n                                        MemberManager.saveMembers(context)"
)

content = content.replace(
    "memberRequestsState[idx] = updated",
    "memberRequestsState[idx] = updated\n                                            MemberManager.saveMembers(context)"
)

with open('app/src/main/java/com/aistudio/micrhema/Screens.kt', 'w') as f:
    f.write(content)
