import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

new_rpc = """    @POST("rest/v1/rpc/{function_name}")
    suspend fun callRpc(
        @retrofit2.http.Path("function_name") functionName: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<com.squareup.moshi.internal.NullSafeJsonAdapter<Any>>
"""

content = content.replace("interface SupabaseApi {", "interface SupabaseApi {\n" + new_rpc)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
