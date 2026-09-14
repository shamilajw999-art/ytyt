import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# Add _entitlements if not there
if "_entitlements" not in content:
    pattern = re.compile(r"    val currentUser: StateFlow<SupabaseUser\?> = _currentUser\.asStateFlow\(\)")
    new_code = """    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()
    
    private val _entitlements = MutableStateFlow<com.example.data.remote.EntitlementResponse?>(null)
    val entitlements: StateFlow<com.example.data.remote.EntitlementResponse?> = _entitlements.asStateFlow()"""
    content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
