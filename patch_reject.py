import re

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

if "rejectPaymentOrderRpc" not in content:
    content = content.replace('    @POST("rest/v1/rpc/approve_payment_order")', '    @POST("rest/v1/rpc/reject_payment_order")\n    suspend fun rejectPaymentOrderRpc(\n        @Body body: Map<String, @JvmSuppressWildcards Any>\n    ): retrofit2.Response<Boolean>\n\n    @POST("rest/v1/rpc/approve_payment_order")')

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
