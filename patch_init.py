import re

with open('app/src/main/java/com/aistudio/micrhema/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        initializeMockContent()
        MemberManager.loadMembers(context)"""

replacement = """        initializeMockContent()
        MemberManager.loadMembers(context)
        
        // Initialize Firebase if keys are present (via Secrets panel/BuildConfig)
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty() && com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setProjectId(com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(com.aistudio.micrhema.BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(com.aistudio.micrhema.BuildConfig.FIREBASE_API_KEY)
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(context, options)
                MemberManager.syncFromFirestore(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
            MemberManager.syncFromFirestore(context)
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/aistudio/micrhema/MainActivity.kt', 'w') as f:
    f.write(content)
