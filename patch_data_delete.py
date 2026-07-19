import re

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'r') as f:
    content = f.read()

target = """    fun saveToFirestore(member: MemberRequest) {"""
replacement = """    fun deleteFromFirestore(member: MemberRequest) {
        try {
            val db = Firebase.firestore
            db.collection("members").document(member.id).delete()
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun saveToFirestore(member: MemberRequest) {"""
content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/Data.kt', 'w') as f:
    f.write(content)
