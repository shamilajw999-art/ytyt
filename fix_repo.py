import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# I will find the duplicate `order: SupabasePaymentOrder,` and the remaining old function, and delete it.
pattern = r"        order: SupabasePaymentOrder,\n        adminUserId: String\n    \): Pair<Boolean, String> = withContext\(Dispatchers\.IO\) {\n        val nowIso = java\.time\.Instant\.now\(\)\.toString\(\).*?SupabaseClient\.service\.updateProfile.*?\}\n        \}\n\n        return@withContext Pair\(true, \"Payment order approved\"(?:.*?)\n    \}"

# Remove the old function body remainder
content = re.sub(pattern, "", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
