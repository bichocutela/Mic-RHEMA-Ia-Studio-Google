import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

if "com.google.firebase.Firebase" not in content:
    content = content.replace("package com.aistudio.micrhema", "package com.aistudio.micrhema\n\nimport com.google.firebase.Firebase\nimport com.google.firebase.firestore.firestore\nimport android.util.Log")

target = """    fun loadMembers(context: android.content.Context) {"""
replacement = """    fun syncFromFirestore(context: android.content.Context) {
        try {
            val db = Firebase.firestore
            db.collection("members").get()
                .addOnSuccessListener { result ->
                    val newList = mutableListOf<MemberRequest>()
                    for (document in result) {
                        val id = document.id
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        val isApproved = document.getBoolean("isApproved") ?: false
                        val isVip = document.getBoolean("isVip") ?: false
                        val isIbr = document.getBoolean("isIbr") ?: false
                        newList.add(MemberRequest(id, name, email, isApproved, isVip, isIbr))
                    }
                    if (newList.isNotEmpty()) {
                        memberRequestsState.clear()
                        memberRequestsState.addAll(newList)
                        saveMembers(context)
                        
                        val loggedInId = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                            .getString(KEY_LOGGED_IN_ID, "") ?: ""
                        if (loggedInId.isNotEmpty()) {
                            val member = memberRequestsState.find { it.id == loggedInId }
                            if (member != null) {
                                loggedInMemberState.value = member
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MemberManager", "Error getting documents: ", exception)
                }
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun saveToFirestore(member: MemberRequest) {
        try {
            val db = Firebase.firestore
            val memberMap = hashMapOf(
                "name" to member.name,
                "email" to member.email,
                "isApproved" to member.isApproved,
                "isVip" to member.isVip,
                "isIbr" to member.isIbr
            )
            db.collection("members").document(member.id).set(memberMap)
                .addOnSuccessListener { Log.d("MemberManager", "Document successfully written!") }
                .addOnFailureListener { e -> Log.w("MemberManager", "Error writing document", e) }
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun loadMembers(context: android.content.Context) {"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)
