import re

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    content = f.read()

# find approvePaymentOrder
pattern = re.compile(r'    suspend fun approvePaymentOrder\(.*?\n    \)(?:.*?\} catch \(e: Exception\) \{.*?\}).*?\n        \}', re.DOTALL)

def replace_fn(match):
    return """    suspend fun approvePaymentOrder(
        order: SupabasePaymentOrder,
        adminUserId: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || order.id == null) {
            return@withContext Pair(false, "Supabase is not configured or order ID is null")
        }
        try {
            val response = SupabaseClient.service.approvePaymentOrderRpc(
                mapOf("p_order_id" to order.id)
            )
            if (response.isSuccessful && response.body() == true) {
                // Refresh local payment orders and subscriptions if needed
                refreshEntitlements()
                return@withContext Pair(true, "Payment order approved successfully")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                return@withContext Pair(false, "Failed to approve: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "Supabase approvePaymentOrderRpc failed", e)
            return@withContext Pair(false, "Exception: ${e.message}")
        }
    }"""

new_content = re.sub(r'    suspend fun approvePaymentOrder\(.*?\}\n    \}', replace_fn, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(new_content)
