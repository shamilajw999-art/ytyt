import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

content = content.replace("retrofit2.Response<com.squareup.moshi.internal.NullSafeJsonAdapter<Any>>", "retrofit2.Response<okhttp3.ResponseBody>")

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
