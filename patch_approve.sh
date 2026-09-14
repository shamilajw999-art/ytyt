cat << 'INNER_EOF' > replacement.txt
    suspend fun approvePaymentOrder(
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
    }
INNER_EOF
sed -i -e '/suspend fun approvePaymentOrder(/,/} catch (e: Exception) {/!b' -e '/suspend fun approvePaymentOrder(/!b' -e 'r replacement.txt' -e 'd' app/src/main/java/com/example/data/repository/MythicRepository.kt
