import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

content = content.replace("        _currentUser.value = null", "        _currentUser.value = null\n        _entitlements.value = null")

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
