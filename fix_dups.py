with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

bad_rpc = """    @POST("rest/v1/rpc/award_xp")
    suspend fun awardXp(
        @Body args: Map<String, Int>
    ): Response<Unit>

    @POST("rest/v1/rpc/increment_scan_count")
    suspend fun incrementScanCount(): Response<Unit>

    @POST("rest/v1/rpc/increment_badge_count")
    suspend fun incrementBadgeCount(): Response<Unit>"""

content = content.replace(bad_rpc, "")

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)

