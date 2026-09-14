import re

# Update SupabaseClient.kt
with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

rpc_code = """    @POST("rest/v1/rpc/award_xp")
    suspend fun awardXp(
        @Body args: Map<String, Int>
    ): Response<Unit>

    @POST("rest/v1/rpc/increment_scan_count")
    suspend fun incrementScanCount(): Response<Unit>

    @POST("rest/v1/rpc/increment_badge_count")
    suspend fun incrementBadgeCount(): Response<Unit>

    @GET("rest/v1/subscriptions")"""

content = content.replace('    @GET("rest/v1/subscriptions")', rpc_code)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)

# Update MythicRepository.kt
with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'r') as f:
    repo = f.read()

# Replace xp remote update
old_xp = """        // Remote update
        if (SupabaseClient.isConfigured) {
            val userId = SupabaseClient.service.getSession()?.user?.id
            if (userId != null) {
                try {
                    SupabaseClient.service.updateProfile(
                        "eq.${updated.id}",
                        mapOf("xp" to newXp, "level" to newLevel)
                    )
                } catch (e: Exception) {
                    Log.e("MythicRepository", "Failed to sync XP/Level", e)
                }
            }
        }"""

new_xp = """        // Remote update using secure RPC
        if (SupabaseClient.isConfigured && SupabaseClient.accessToken != null) {
            try {
                SupabaseClient.service.awardXp(mapOf("p_amount" to amount))
                // Refresh profile to get authoritative XP and Level from backend
                loadProfile()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Failed to sync XP/Level via RPC", e)
            }
        }"""

repo = repo.replace(old_xp, new_xp)

# Also fix the weird "val userId = SupabaseClient.service.getSession()?.user?.id" because getSession() doesn't exist
# I'll just regex replace the block
repo = re.sub(r'        // Remote update\n        if \(SupabaseClient.isConfigured\) \{.*?        \}', new_xp, repo, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/MythicRepository.kt', 'w') as f:
    f.write(repo)

