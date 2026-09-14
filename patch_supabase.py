import re

with open('app/src/main/java/com/example/data/remote/SupabaseModels.kt', 'a') as f:
    f.write("""

@JsonClass(generateAdapter = true)
data class EntitlementResponse(
    val plan: String,
    val status: String,
    @Json(name = "expires_at") val expiresAt: String?,
    val entitlements: Entitlements
)

@JsonClass(generateAdapter = true)
data class Entitlements(
    @Json(name = "ads_free") val adsFree: Boolean,
    @Json(name = "unlimited_lumo") val unlimitedLumo: Boolean,
    @Json(name = "high_resolution_maps") val highResolutionMaps: Boolean,
    @Json(name = "priority_support") val prioritySupport: Boolean,
    @Json(name = "ar_3d_scan") val ar3dScan: Boolean,
    @Json(name = "beta_features") val betaFeatures: Boolean,
    @Json(name = "digital_collection_badges") val digitalCollectionBadges: Boolean,
    @Json(name = "custom_profile_background") val customProfileBackground: Boolean,
    @Json(name = "animated_profile_background") val animatedProfileBackground: Boolean,
    @Json(name = "advanced_profile_customization") val advancedProfileCustomization: Boolean,
    @Json(name = "exclusive_passport_features") val exclusivePassportFeatures: Boolean
)
""")

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r"    @GET\(\"rest/v1/subscriptions\"\)\n    suspend fun getSubscriptions\(")
new_code = """    @POST("rest/v1/rpc/get_user_entitlements")
    suspend fun getUserEntitlements(): retrofit2.Response<EntitlementResponse>

    @GET("rest/v1/subscriptions")
    suspend fun getSubscriptions("""

content = pattern.sub(new_code, content)

with open('app/src/main/java/com/example/data/remote/SupabaseClient.kt', 'w') as f:
    f.write(content)
