import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

pattern = r'    suspend fun rejectPaymentOrder\(.*?true\n    \}'

def replace_fn(match):
    return """    suspend fun rejectPaymentOrder(
        orderId: String,
        adminUserId: String,
        targetUserId: String,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext false
        try {
            val response = SupabaseClient.service.rejectPaymentOrderRpc(
                mapOf("p_order_id" to orderId, "p_reason" to reason)
            )
            return@withContext response.isSuccessful && response.body() == true
        } catch (e: Exception) {
            Log.e("MythicRepository", "rejectPaymentOrder error", e)
            return@withContext false
        }
    }"""

new_content = re.sub(pattern, replace_fn, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(new_content)
