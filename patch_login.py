import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

content = content.replace("                    val profileResponse = SupabaseClient.service.getProfile", "                    refreshEntitlements()\n                    val profileResponse = SupabaseClient.service.getProfile")

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
