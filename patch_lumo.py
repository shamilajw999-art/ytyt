import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

new_code = """
    @POST("rest/v1/rpc/can_use_lumo")
    suspend fun canUseLumo(): retrofit2.Response<com.squareup.moshi.internal.NullSafeJsonAdapter<Any>>
"""
new_code = new_code.replace("com.squareup.moshi.internal.NullSafeJsonAdapter<Any>", "okhttp3.ResponseBody")

content = content.replace("interface SupabaseApi {", "interface SupabaseApi {\n" + new_code)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
