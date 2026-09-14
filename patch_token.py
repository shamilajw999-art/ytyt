import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

# Add accessToken property
content = content.replace("private val supabaseKey = BuildConfig.SUPABASE_KEY.trim()", 
    "private val supabaseKey = BuildConfig.SUPABASE_KEY.trim()\n    var accessToken: String? = null")

# Update authInterceptor
old_interceptor = """        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Build absolute URL if needed or modify headers
            val builder = originalRequest.newBuilder()
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
            chain.proceed(builder.build())
        }"""

new_interceptor = """        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            
            val token = accessToken ?: supabaseKey
            
            // Build absolute URL if needed or modify headers
            val builder = originalRequest.newBuilder()
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
            chain.proceed(builder.build())
        }"""

content = content.replace(old_interceptor, new_interceptor)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)

