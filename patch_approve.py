import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    suspend fun approvePaymentOrder\(order: SupabasePaymentOrder, adminId: String\): Pair<Boolean, String> = withContext\(Dispatchers\.IO\) \{.*?\n            return@withContext Pair\(true, \"Payment successfully approved and subscription activated for \$\{order\.plan\.uppercase\(\)\}!\"\)\n        \} catch \(e: Exception\) \{.*?\n        \}\n    \}", re.DOTALL)

new_code = """    suspend fun approvePaymentOrder(order: SupabasePaymentOrder, adminId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = mapOf(
                "p_order_id" to order.id,
                "p_admin_id" to adminId,
                "p_duration_days" to 365
            )
            val response = SupabaseClient.service.callRpc("approve_payment_order", requestBody)
            if (response.isSuccessful) {
                return@withContext Pair(true, "Payment successfully approved and subscription activated for ${order.plan.uppercase()}!")
            } else {
                return@withContext Pair(false, "Failed to approve payment order: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("MythicRepository", "Error approving payment order", e)
            return@withContext Pair(false, "Error: ${e.message}")
        }
    }"""

content = pattern.sub(new_code, content)

pattern2 = re.compile(r"    suspend fun rejectPaymentOrder\(orderId: String, adminId: String, targetUserId: String, reason: String\): Boolean = withContext\(Dispatchers\.IO\) \{.*?\n        \}\n    \}", re.DOTALL)

new_code2 = """    suspend fun rejectPaymentOrder(orderId: String, adminId: String, targetUserId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = mapOf(
                "p_order_id" to orderId,
                "p_admin_id" to adminId,
                "p_reason" to reason
            )
            val response = SupabaseClient.service.callRpc("reject_payment_order", requestBody)
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("MythicRepository", "Error rejecting payment order", e)
            return@withContext false
        }
    }"""

content = pattern2.sub(new_code2, content)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(content)
