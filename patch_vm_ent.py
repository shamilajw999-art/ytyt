import re

with open('app/src/main/java/com/example/viewmodel/MythicViewModel.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    val currentProfile = repository\.currentProfile")
new_code = """    val currentProfile = repository.currentProfile
    val entitlements = repository.entitlements"""

content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/viewmodel/MythicViewModel.kt', 'w') as f:
    f.write(content)
