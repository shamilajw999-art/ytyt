import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

new_code = """
    @POST("rest/v1/rpc/can_use_lumo")
    suspend fun canUseLumo(): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/{function_name}")
    suspend fun callRpc(
        @retrofit2.http.Path("function_name") functionName: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<okhttp3.ResponseBody>
"""

content = content.replace("interface SupabaseApiService {", "interface SupabaseApiService {\n" + new_code)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
