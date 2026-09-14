package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface SupabaseApiService {

    @POST("rest/v1/rpc/can_use_lumo")
    suspend fun canUseLumo(): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/award_xp")
    suspend fun awardXp(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/increment_scan_count")
    suspend fun incrementScanCount(
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/increment_badge_count")
    suspend fun incrementBadgeCount(
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/repair_missing_profiles")
    suspend fun repairMissingProfiles(
        @Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/{function_name}")
    suspend fun callRpc(
        @retrofit2.http.Path("function_name") functionName: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): retrofit2.Response<okhttp3.ResponseBody>

    
    // --- AUTH ---
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: SupabaseLoginRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("auth/v1/user")
    suspend fun getUserDetails(
        @Header("Authorization") bearerToken: String
    ): Response<SupabaseUser>

    // --- PROFILES ---
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") query: String
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/profiles")
    suspend fun getAllProfiles(
        @Query("select") select: String = "*",
        @Query("order") order: String = "id.asc"
    ): Response<List<SupabaseProfile>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") query: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("rest/v1/profiles")
    suspend fun createProfile(
        @Body profile: SupabaseProfile
    ): Response<Unit>

    // --- BADGES ---
    @GET("rest/v1/badges")
    suspend fun getBadges(
        @Query("user_id") query: String
    ): Response<List<SupabaseBadge>>

    @POST("rest/v1/badges")
    suspend fun saveBadge(
        @Body badge: SupabaseBadge
    ): Response<Unit>

    // --- QUESTS ---
    @GET("rest/v1/quests")
    suspend fun getQuests(
        @Query("user_id") query: String
    ): Response<List<SupabaseQuest>>

    @POST("rest/v1/quests")
    suspend fun saveQuests(
        @Body quests: List<SupabaseQuest>
    ): Response<Unit>

    @PATCH("rest/v1/quests")
    suspend fun updateQuest(
        @Query("id") query: String,
        @Body updates: Map<String, Any>
    ): Response<Unit>

    // --- SCAN HISTORY ---
    @GET("rest/v1/scan_history")
    suspend fun getScanHistory(
        @Query("user_id") query: String
    ): Response<List<SupabaseScan>>

    @POST("rest/v1/scan_history")
    suspend fun saveScan(
        @Body scan: SupabaseScan
    ): Response<Unit>

    // --- SAVED SITES ---
    @GET("rest/v1/saved_sites")
    suspend fun getSavedSites(
        @Query("user_id") query: String
    ): Response<List<SupabaseSavedSite>>

    @POST("rest/v1/saved_sites")
    suspend fun saveSite(
        @Body site: SupabaseSavedSite
    ): Response<Unit>

    @DELETE("rest/v1/saved_sites")
    suspend fun deleteSavedSite(
        @Query("user_id") query: String,
        @Query("site_name") siteNameQuery: String
    ): Response<Unit>

    // --- MYTHIC ---
    @GET("rest/v1/mythic_conversations")
    suspend fun getMythicConversations(
        @Query("user_id") query: String
    ): Response<List<SupabaseMythicMessage>>

    @POST("rest/v1/mythic_conversations")
    suspend fun saveMythicMessage(
        @Body message: SupabaseMythicMessage
    ): Response<Unit>

    @POST("rest/v1/reports")
    suspend fun saveReport(
        @Body report: SupabaseReport
    ): Response<Unit>

    @GET("rest/v1/reports")
    suspend fun getReports(): Response<List<SupabaseReport>>

    // --- REELS ---
    @GET("rest/v1/reels")
    suspend fun getReels(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseReel>>

    @POST("rest/v1/reels")
    suspend fun saveReel(
        @Body reel: SupabaseReel
    ): Response<Unit>

    // --- ARTICLES & REACTIONS ---
    @GET("rest/v1/heritage_articles")
    suspend fun getHeritageArticles(
        @Query("order") order: String = "id.desc"
    ): Response<List<SupabaseArticle>>

    @POST("rest/v1/heritage_articles")
    suspend fun saveHeritageArticle(
        @Body article: SupabaseArticle
    ): Response<Unit>

    @PATCH("rest/v1/heritage_articles")
    suspend fun updateHeritageArticleLikes(
        @Query("id") idQuery: String,
        @Body updates: Map<String, Int>
    ): Response<Unit>

    @POST("rest/v1/article_reactions")
    suspend fun saveArticleReaction(
        @Body reaction: SupabaseArticleReaction
    ): Response<Unit>

    // --- COMMUNITY & COMMENTS ---
    @GET("rest/v1/community_posts")
    suspend fun getCommunityPosts(
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseCommunityPost>>

    @POST("rest/v1/community_posts")
    suspend fun saveCommunityPost(
        @Body post: SupabaseCommunityPost
    ): Response<Unit>

    @GET("rest/v1/comments")
    suspend fun getComments(
        @Query("reel_id") reelId: String? = null,
        @Query("post_id") postId: String? = null
    ): Response<List<SupabaseComment>>

    @POST("rest/v1/comments")
    suspend fun saveComment(
        @Body comment: SupabaseComment
    ): Response<Unit>

    // --- SUBSCRIPTIONS & PAYMENTS ---
    @POST("rest/v1/rpc/get_user_entitlements")
    suspend fun getUserEntitlements(
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET("rest/v1/subscriptions")
    suspend fun getSubscriptions(
        @Query("user_id") userId: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<SupabaseSubscription>>

    @GET("rest/v1/payment_orders")
    suspend fun getPaymentOrders(
        @Query("user_id") userId: String? = null,
        @Query("status") status: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Header("Authorization") authHeader: String? = null
    ): Response<List<SupabasePaymentOrder>>

    // --- RPC PAYMENT OPERATIONS ---
    @POST("rest/v1/rpc/create_payment_order")
    suspend fun createPaymentOrderRpc(
        @Body request: CreatePaymentOrderRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/submit_payment_order")
    suspend fun submitPaymentOrderRpc(
        @Body request: SubmitPaymentOrderRequest,
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/submit_payment_order")
    suspend fun submitPaymentOrderRpcDynamic(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    // --- PAYMENT SUBMISSIONS ---
    @GET("rest/v1/payment_submissions")
    suspend fun getPaymentSubmissions(
        @Query("payment_order_id") paymentOrderId: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<SupabasePaymentSubmission>>

    // --- ADMIN APPROVAL & ORDER MANAGEMENT ---
    @POST("rest/v1/rpc/admin_approve_payment_order")
    suspend fun adminApprovePaymentOrderRpc(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    @POST("rest/v1/rpc/admin_reject_payment_order")
    suspend fun adminRejectPaymentOrderRpc(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") authHeader: String? = null
    ): Response<okhttp3.ResponseBody>

    @PATCH("rest/v1/payment_orders")
    suspend fun patchPaymentOrder(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Authorization") authHeader: String? = null
    ): Response<Unit>

    @POST("rest/v1/subscriptions")
    suspend fun upsertSubscription(
        @Body subscription: SupabaseSubscription,
        @Header("Authorization") authHeader: String? = null,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>

    @PATCH("rest/v1/profiles")
    suspend fun patchProfile(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Authorization") authHeader: String? = null
    ): Response<Unit>

    // --- AUTOMATED EMAILS ---
    @POST("functions/v1/send-payment-email")
    suspend fun sendPaymentEmail(
        @Body request: Map<String, String>
    ): Response<Unit>

    // --- STORAGE ---
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body file: okhttp3.RequestBody,
        @Header("Content-Type") contentType: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<Unit>
}

object SupabaseClient {
    // Production default values ensuring connectivity even if BuildConfig is stripped or defaulted
    private const val FALLBACK_SUPABASE_URL = "https://arhfpupxjcttsoyugmuc.supabase.co"
    private const val FALLBACK_SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFyaGZwdXB4amN0dHNveXVnbXVjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc5MDk1MjMsImV4cCI6MjEwMzQ4NTUyM30.rSX8MxzdOwFVHnaT17t1KWI_hMAdiBYtkKh-z2MW6SY"

    val supabaseUrl: String = run {
        val buildUrl = try { BuildConfig.SUPABASE_URL.trim().removeSuffix("/") } catch (e: Throwable) { "" }
        if (buildUrl.isNotEmpty() && buildUrl != "https://your-project.supabase.co" && !buildUrl.contains("your-project")) {
            buildUrl
        } else {
            FALLBACK_SUPABASE_URL
        }
    }

    val supabaseKey: String = run {
        val buildKey = try { BuildConfig.SUPABASE_KEY.trim() } catch (e: Throwable) { "" }
        if (buildKey.isNotEmpty() && buildKey != "your-supabase-anon-key" && !buildKey.contains("your-supabase")) {
            buildKey
        } else {
            FALLBACK_SUPABASE_KEY
        }
    }

    var accessToken: String? = null

    val isConfigured: Boolean
        get() = supabaseUrl.isNotEmpty() && 
                supabaseUrl != "https://your-project.supabase.co" && 
                supabaseKey.isNotEmpty() && 
                supabaseKey != "your-supabase-anon-key"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val hasAuth = originalRequest.header("Authorization") != null
            val token = accessToken
            val authValue = if (!token.isNullOrBlank()) "Bearer $token" else "Bearer $supabaseKey"
            
            val builder = originalRequest.newBuilder()
                .header("apikey", supabaseKey)
                .header("Content-Type", "application/json")

            if (!hasAuth) {
                builder.header("Authorization", authValue)
            }
            if (originalRequest.header("Prefer") == null) {
                builder.header("Prefer", "return=representation")
            }

            chain.proceed(builder.build())
        }

        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val service: SupabaseApiService by lazy {
        val baseUrl = if (isConfigured) "$supabaseUrl/" else "https://placeholder-supabase.co/"
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }
}
