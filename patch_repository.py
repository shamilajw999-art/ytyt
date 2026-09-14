import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    private val _currentUser = MutableStateFlow<SupabaseUser\?>\(null\)\n    val currentUser = _currentUser\.asStateFlow\(\)")
new_code = """    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    private val _entitlements = MutableStateFlow<com.example.data.remote.EntitlementResponse?>(null)
    val entitlements = _entitlements.asStateFlow()"""

content = pattern.sub(new_code, content)

pattern2 = re.compile(r"    suspend fun loadProfile\(\) = withContext\(Dispatchers\.IO\) \{\n        try \{")
new_code2 = """    suspend fun refreshEntitlements() = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.service.getUserEntitlements()
            if (response.isSuccessful) {
                _entitlements.value = response.body()
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "Failed to refresh entitlements", e)
        }
    }

    suspend fun loadProfile() = withContext(Dispatchers.IO) {
        try {
            refreshEntitlements()"""

content = pattern2.sub(new_code2, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
