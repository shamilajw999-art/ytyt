import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# Fix the duplicate checkSession logic
pattern = re.compile(r"    suspend fun checkSession\(\) \{\n        // Simple session recovery emulated locally\n        val existingProfile = dao\.getProfile\(\)\n        if \(existingProfile != null\) \{\n            _currentUser\.value = SupabaseUser\(\n                id = existingProfile\.id,\n                email = existingProfile\.email\n            \)\n            refreshEntitlements\(\)\n            return\n        \}\n\n                id = existingProfile\.id,\n                email = existingProfile\.email,\n                userMetadata = mapOf\(\"username\" to existingProfile\.username, \"full_name\" to existingProfile\.fullName\)\n            \)\n            // Check night_owl badge\n            val hour = java\.util\.Calendar\.getInstance\(\)\.get\(java\.util\.Calendar\.HOUR_OF_DAY\)\n            if \(hour in 0\.\.4\) \{\n                unlockBadge\(\"night_owl\", \"Midnight Explorer\", \"Use the app past midnight to study history\", \"bronze\", \"🦉\"\)\n            \}\n        \} else \{\n            _currentUser\.value = null\n        _entitlements\.value = null\n        \}\n    \}")

new_code = """    suspend fun checkSession() {
        // Simple session recovery emulated locally
        val existingProfile = dao.getProfile()
        if (existingProfile != null) {
            _currentUser.value = SupabaseUser(
                id = existingProfile.id,
                email = existingProfile.email,
                userMetadata = mapOf("username" to existingProfile.username, "full_name" to existingProfile.fullName)
            )
            refreshEntitlements()
            // Check night_owl badge
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour in 0..4) {
                unlockBadge("night_owl", "Midnight Explorer", "Use the app past midnight to study history", "bronze", "🦉")
            }
        } else {
            _currentUser.value = null
            _entitlements.value = null
        }
    }"""

content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
