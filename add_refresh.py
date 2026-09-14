import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

new_code = """
    suspend fun refreshEntitlements() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (SupabaseClient.isConfigured) {
                val response = SupabaseClient.service.getUserEntitlements()
                if (response.isSuccessful) {
                    _entitlements.value = response.body()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MythicRepository", "Failed to refresh entitlements", e)
        }
    }
"""

content = content.replace("    suspend fun checkSession() {", new_code + "\n    suspend fun checkSession() {")

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
