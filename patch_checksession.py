import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    suspend fun checkSession\(\) \{\n        // Simple session recovery emulated locally\n        val existingProfile = dao\.getProfile\(\)\n        if \(existingProfile != null\) \{\n            _currentUser\.value = SupabaseUser\(")
new_code = """    suspend fun checkSession() {
        // Simple session recovery emulated locally
        val existingProfile = dao.getProfile()
        if (existingProfile != null) {
            _currentUser.value = SupabaseUser(
                id = existingProfile.id,
                email = existingProfile.email
            )
            refreshEntitlements()
            return
        }"""

content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
