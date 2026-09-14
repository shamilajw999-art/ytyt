import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# In login
old_login = """                    val user = authBody.user
                    if (user != null) {
                        // Success!
                        val username = user.userMetadata?.get("username")?.toString() ?: user.email?.substringBefore("@") ?: "User"
                        val fullName = user.userMetadata?.get("full_name")?.toString() ?: username"""

new_login = """                    val user = authBody.user
                    if (user != null) {
                        SupabaseClient.accessToken = authBody.accessToken
                        // Success!
                        val username = user.userMetadata?.get("username")?.toString() ?: user.email?.substringBefore("@") ?: "User"
                        val fullName = user.userMetadata?.get("full_name")?.toString() ?: username"""

content = content.replace(old_login, new_login)

# In signUp
old_signup = """                    val user = authBody.user
                    if (user != null) {
                        // User needs to confirm email first, so we do NOT set _currentUser.value yet
                        _currentUser.value = null"""

new_signup = """                    val user = authBody.user
                    if (user != null) {
                        authBody.accessToken?.let { SupabaseClient.accessToken = it }
                        // User needs to confirm email first, so we do NOT set _currentUser.value yet
                        _currentUser.value = null"""

content = content.replace(old_signup, new_signup)

# In logout
old_logout = """        _profile.value = null
        _entitlements.value = null
        
        // Remote logout"""

new_logout = """        _profile.value = null
        _entitlements.value = null
        SupabaseClient.accessToken = null
        
        // Remote logout"""

content = content.replace(old_logout, new_logout)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)

