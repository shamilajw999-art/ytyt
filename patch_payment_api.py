import re

# Update SupabaseClient.kt
with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    suspend fun savePaymentOrder(\n        @Body order: SupabasePaymentOrder\n    ): Response<Unit>',
    '    suspend fun savePaymentOrder(\n        @Body order: SupabasePaymentOrder\n    ): Response<List<SupabasePaymentOrder>>'
)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)

# Update MythicRepository.kt
with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    repo = f.read()

old_create = """    suspend fun createPaymentOrder(order: SupabasePaymentOrder) = withContext(Dispatchers.IO) {
        synchronized(_cachedPaymentOrders) {
            _cachedPaymentOrders.removeAll { it.paymentReference == order.paymentReference || (it.id != null && it.id == order.id) }
            _cachedPaymentOrders.add(0, order)
        }
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.savePaymentOrder(order)
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase savePaymentOrder failed", e)
            }
        }
    }"""

new_create = """    suspend fun createPaymentOrder(order: SupabasePaymentOrder): SupabasePaymentOrder? = withContext(Dispatchers.IO) {
        var createdOrder: SupabasePaymentOrder? = null
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.savePaymentOrder(order)
                if (response.isSuccessful) {
                    val list = response.body()
                    if (!list.isNullOrEmpty()) {
                        createdOrder = list[0]
                    }
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase savePaymentOrder failed", e)
            }
        }
        val finalOrder = createdOrder ?: order
        synchronized(_cachedPaymentOrders) {
            _cachedPaymentOrders.removeAll { it.paymentReference == order.paymentReference || (it.id != null && it.id == order.id) }
            _cachedPaymentOrders.add(0, finalOrder)
        }
        return@withContext finalOrder
    }"""

repo = repo.replace(old_create, new_create)
with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(repo)
