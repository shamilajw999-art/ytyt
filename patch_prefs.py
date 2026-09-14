import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# In checkSession
old_check = """    suspend fun checkSession() {
        // Simple session recovery emulated locally
        val existingProfile = dao.getProfile()"""

new_check = """    suspend fun checkSession() {
        // Load token from prefs
        val prefs = context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE)
        SupabaseClient.accessToken = prefs.getString("access_token", null)
        
        // Simple session recovery emulated locally
        val existingProfile = dao.getProfile()"""

content = content.replace(old_check, new_check)

# Update the places where we set it
def replace_token_save(content, search, replacement):
    return content.replace(search, replacement)

content = replace_token_save(content, 
    "SupabaseClient.accessToken = authBody.accessToken",
    "SupabaseClient.accessToken = authBody.accessToken\n                        context.getSharedPreferences(\"mythic_auth\", android.content.Context.MODE_PRIVATE).edit().putString(\"access_token\", authBody.accessToken).apply()")

content = replace_token_save(content, 
    "authBody.accessToken?.let { SupabaseClient.accessToken = it }",
    "authBody.accessToken?.let { \n                            SupabaseClient.accessToken = it\n                            context.getSharedPreferences(\"mythic_auth\", android.content.Context.MODE_PRIVATE).edit().putString(\"access_token\", it).apply()\n                        }")

content = replace_token_save(content,
    "SupabaseClient.accessToken = null",
    "SupabaseClient.accessToken = null\n        context.getSharedPreferences(\"mythic_auth\", android.content.Context.MODE_PRIVATE).edit().remove(\"access_token\").apply()")

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)

