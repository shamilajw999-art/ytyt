package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseSignUpRequest(
    val email: String?,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginRequest(
    val email: String?,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "user") val user: SupabaseUser?
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String??,
    @Json(name = "email_confirmed_at") val emailConfirmedAt: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, @JvmSuppressWildcards Any>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    val id: String,
    val username: String?,
    val email: String?,
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val role: String? = "user",
    val level: Int? = 1,
    val xp: Int? = 0,
    val streak: Int? = 0,
    @Json(name = "scans_count") val scansCount: Int? = 0,
    @Json(name = "badges_count") val badgesCount: Int? = 0,
    @Json(name = "subscription_tier") val subscriptionTier: String? = "free",
    @Json(name = "last_login_date") val lastLoginDate: String? = null,
    @Json(name = "profile_background_url") val profileBackgroundUrl: String? = null,
    @Json(name = "is_animated_background") val isAnimatedBackground: Boolean = false,
    val bio: String? = null,
    val interests: List<String>? = null,
    @Json(name = "featured_badge_id") val featuredBadgeId: String? = null
)

@JsonClass(generateAdapter = true)
data class CreatePaymentOrderRequest(
    @Json(name = "p_plan") val plan: String
)

@JsonClass(generateAdapter = true)
data class SubmitPaymentOrderRequest(
    @Json(name = "p_order_id") val orderId: String,
    @Json(name = "p_bank_transaction_id") val bankTransactionId: String,
    @Json(name = "p_transfer_amount") val transferAmount: Double,
    @Json(name = "p_transfer_datetime") val transferDatetime: String,
    @Json(name = "p_receipt_path") val receiptPath: String
)

@JsonClass(generateAdapter = true)
data class SupabasePaymentOrder(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_email") val userEmail: String? = null,
    val plan: String,
    val amount: Double,
    val currency: String = "USD",
    @Json(name = "payment_reference") val paymentReference: String,
    @Json(name = "bank_transaction_id") val bankTransactionId: String? = null,
    @Json(name = "receipt_image_url") val receiptImageUrl: String? = null,
    val status: String = "pending",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "verified_at") val verifiedAt: String? = null,
    @Json(name = "verified_by") val verifiedBy: String? = null,
    @Json(name = "rejection_reason") val rejectionReason: String? = null,
    @Json(name = "subscription_id") val subscriptionId: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabasePaymentSubmission(
    val id: String? = null,
    @Json(name = "payment_order_id") val paymentOrderId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "bank_transaction_id") val bankTransactionId: String,
    @Json(name = "transfer_amount") val transferAmount: Double,
    @Json(name = "transfer_datetime") val transferDatetime: String? = null,
    @Json(name = "receipt_image_url") val receiptImageUrl: String? = null,
    @Json(name = "submitted_at") val submittedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuditLog(
    val id: String? = null,
    @Json(name = "admin_user_id") val adminUserId: String,
    val action: String,
    @Json(name = "payment_order_id") val paymentOrderId: String? = null,
    @Json(name = "target_user_id") val targetUserId: String? = null,
    val metadata: Map<String, @JvmSuppressWildcards Any>? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSubscription(
    val id: String? = null,
    @Json(name = "user_id") val userId: String,
    val plan: String, // "pro", "max"
    val status: String = "active", // "active", "expired", "cancelled"
    @Json(name = "payment_order_id") val paymentOrderId: String? = null,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseBadge(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    val code: String,
    val name: String,
    val description: String,
    val tier: String,
    val icon: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseQuest(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    val title: String,
    val description: String,
    @Json(name = "reward_xp") val rewardXp: Int,
    val progress: Int,
    val target: Int,
    val type: String,
    val completed: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseScan(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "site_name") val siteName: String,
    val province: String,
    val description: String,
    @Json(name = "unesco_status") val unescoStatus: String,
    val era: String,
    val facts: String,
    @Json(name = "xp_earned") val xpEarned: Int,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseSavedSite(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "site_name") val siteName: String,
    val province: String,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseMythicMessage(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "message_id") val messageId: String,
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class SupabaseReport(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "site_name") val siteName: String,
    val category: String,
    val description: String,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "video_url") val videoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val priority: Float = 0.5f,
    val status: String = "submitted",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReel(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "site_name") val siteName: String,
    val province: String,
    val description: String,
    @Json(name = "video_url") val videoUrl: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?,
    val category: String,
    val likes: Int = 0,
    val shares: Int = 0,
    val views: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseComment(
    val id: String?,
    @Json(name = "reel_id") val reelId: String? = null,
    @Json(name = "post_id") val postId: String? = null,
    @Json(name = "user_id") val userId: String,
    val content: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCommunityPost(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    val content: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    val likes: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseContentUpload(
    val id: String?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "file_url") val fileUrl: String,
    val status: String = "pending",
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class SupabaseArticle(
    val id: String,
    @Json(name = "site_name") val siteName: String,
    val province: String,
    val description: String,
    @Json(name = "image_url") val imageUrl: String,
    val category: String,
    @Json(name = "unesco_status") val unescoStatus: String = "UNESCO World Heritage Site",
    val era: String = "Ancient",
    val facts: String = "Historical facts",
    val likes: Int = 0
)

@JsonClass(generateAdapter = true)
data class SupabaseArticleReaction(
    val id: String? = null,
    @Json(name = "article_id") val articleId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "reaction_type") val reactionType: String = "like",
    @Json(name = "created_at") val createdAt: String? = null
)



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
