import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    suspend fun sendMythicMessage\(userMessageContent: String\): String = withContext\(Dispatchers\.IO\) \{\n        val userId = _currentUser\.value\?\.id \?: \"guest\"\n        \n        // Fetch Conversation Memory")

new_code = """    suspend fun sendMythicMessage(userMessageContent: String): String = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        
        if (userId != "guest" && SupabaseClient.isConfigured) {
            try {
                val lumoCheck = SupabaseClient.service.canUseLumo()
                if (lumoCheck.isSuccessful) {
                    val responseStr = lumoCheck.body()?.string() ?: ""
                    if (responseStr.contains("\\"allowed\\":false") || responseStr.contains("\\"allowed\\": false")) {
                        return@withContext "You have reached your daily limit for Lumo. Please upgrade to Pro or Max to continue chatting."
                    }
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Error checking lumo entitlements", e)
            }
        }
        
        // Fetch Conversation Memory"""

content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
