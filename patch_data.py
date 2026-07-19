import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

target = """val loggedInMemberState = mutableStateOf<MemberRequest?>(null)"""

replacement = """val loggedInMemberState = mutableStateOf<MemberRequest?>(null)

object MemberManager {
    private const val PREFS_NAME = "micrhema_members_prefs"
    private const val KEY_MEMBERS = "members_list"
    private const val KEY_LOGGED_IN_ID = "logged_in_member_id"

    fun loadMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val serialized = prefs.getString(KEY_MEMBERS, "") ?: ""
        if (serialized.isNotEmpty()) {
            val list = serialized.split("||").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 6) {
                    MemberRequest(
                        id = parts[0],
                        name = parts[1],
                        email = parts[2],
                        isApproved = parts[3].toBoolean(),
                        isVip = parts[4].toBoolean(),
                        isIbr = parts[5].toBoolean()
                    )
                } else null
            }
            if (list.isNotEmpty()) {
                memberRequestsState.clear()
                memberRequestsState.addAll(list)
            }
        }
        
        val loggedInId = prefs.getString(KEY_LOGGED_IN_ID, "") ?: ""
        if (loggedInId.isNotEmpty()) {
            val member = memberRequestsState.find { it.id == loggedInId }
            if (member != null) {
                loggedInMemberState.value = member
            }
        }
    }

    fun saveMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val serialized = memberRequestsState.joinToString("||") {
            "${it.id}|${it.name}|${it.email}|${it.isApproved}|${it.isVip}|${it.isIbr}"
        }
        prefs.edit().putString(KEY_MEMBERS, serialized).apply()
    }
    
    fun setLoggedInMember(context: android.content.Context, member: MemberRequest?) {
        loggedInMemberState.value = member
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (member == null) {
            prefs.edit().remove(KEY_LOGGED_IN_ID).apply()
        } else {
            prefs.edit().putString(KEY_LOGGED_IN_ID, member.id).apply()
        }
    }
}
"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)
