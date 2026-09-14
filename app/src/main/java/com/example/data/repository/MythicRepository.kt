package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.remote.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID

import okhttp3.MediaType.Companion.toMediaTypeOrNull

class MythicRepository(private val context: Context) :
    ProfileRepository,
    XPRepository,
    BadgeRepository,
    QuestRepository,
    ScanHistoryRepository,
    SavedSitesRepository,
    UserPreferencesRepository {
    companion object {
        private val _cachedPaymentOrders = mutableListOf<SupabasePaymentOrder>()
        private val _cachedSubscriptions = mutableListOf<SupabaseSubscription>()
    }

    private val db = MythicDatabase.getDatabase(context)
    private val dao = db.mythicDao()

    // --- Local Logged In User State ---
    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()
    private val _entitlements = MutableStateFlow<com.example.data.remote.EntitlementResponse?>(null)
    val entitlements: StateFlow<com.example.data.remote.EntitlementResponse?> = _entitlements.asStateFlow()

    // --- Reactive flows for UI ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val profile: Flow<ProfileEntity?> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getProfileFlowById(user.id)
        } else {
            dao.getProfileFlow()
        }
    }

    override val allProfiles: Flow<List<ProfileEntity>> = dao.getAllProfilesFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val badges: Flow<List<BadgeEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getBadgesFlowByUserId(user.id)
        } else {
            dao.getBadgesFlow()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val quests: Flow<List<QuestEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getQuestsFlowByUserId(user.id)
        } else {
            dao.getQuestsFlow()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val scanHistory: Flow<List<ScanHistoryEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getScanHistoryFlowByUserId(user.id)
        } else {
            flowOf(emptyList())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val savedSites: Flow<List<SavedSiteEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getSavedSitesFlowByUserId(user.id)
        } else {
            flowOf(emptyList())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mythicMessages: Flow<List<MythicConversationEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            dao.getMythicConversationsFlowByUserId(user.id)
        } else {
            flowOf(emptyList())
        }
    }
    override val preferences: Flow<UserPreferencesEntity?> = dao.getUserPreferencesFlow()

    init {
        // Start as logged out by default so users are forced to log in or sign up
        _currentUser.value = null
        _entitlements.value = null
    }


    suspend fun refreshEntitlements() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val token = SupabaseClient.accessToken 
                ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
            
            if (token.isNullOrBlank() || !SupabaseClient.isConfigured) {
                Log.d("MythicEntitlements", "refreshEntitlements: No authenticated session, defaulting entitlements to null")
                _entitlements.value = null
                return@withContext
            }
            SupabaseClient.accessToken = token
            
            // 1. Fetch remote profile to sync XP/Level/Streak and get manual tier override
            var profileTier = "free"
            val currentUserId = _currentUser.value?.id
            if (currentUserId != null) {
                try {
                    val pRes = SupabaseClient.service.getProfile("eq.$currentUserId")
                    if (pRes.isSuccessful && pRes.body()?.isNotEmpty() == true) {
                        val p = pRes.body()!![0]
                        profileTier = p.subscriptionTier?.lowercase() ?: "free"
                        dao.insertProfile(
                            ProfileEntity(
                                id = p.id,
                                username = p.username ?: "Unknown",
                                email = p.email ?: "",
                                fullName = p.fullName ?: p.username ?: "Unknown",
                                avatarUrl = p.avatarUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${p.username ?: "Unknown"}",
                                level = p.level ?: 1,
                                xp = p.xp ?: 0,
                                streak = p.streak ?: 0,
                                scansCount = p.scansCount ?: 0,
                                badgesCount = p.badgesCount ?: 0,
                                subscriptionTier = p.subscriptionTier ?: "free",
                                profileBackgroundUrl = p.profileBackgroundUrl,
                                isAnimatedBackground = p.isAnimatedBackground,
                                bio = p.bio,
                                interests = p.interests?.joinToString(","),
                                featuredBadgeId = p.featuredBadgeId
                            )
                        )
                    }
                } catch(e: Exception) {
                    Log.e("MythicEntitlements", "Profile sync failed", e)
                }
            }

            // 2. Fetch RPC
            val authHeader = "Bearer $token"

            val response = SupabaseClient.service.getUserEntitlements(authHeader)
            val statusCode = response.code()
            
            var plan = "free"
            var status = "active"
            var expiresAt: String? = null
            var entObj: org.json.JSONObject? = null
            
            if (response.isSuccessful) {
                val rawBody = response.body()?.string()?.trim() ?: ""
                Log.d("MythicEntitlements", "getUserEntitlements raw response ($statusCode): $rawBody")

                if (rawBody.isNotEmpty()) {
                    val json = if (rawBody.startsWith("[")) {
                        val arr = org.json.JSONArray(rawBody)
                        if (arr.length() > 0) arr.getJSONObject(0) else null
                    } else if (rawBody.startsWith("{")) {
                        org.json.JSONObject(rawBody)
                    } else null

                    if (json != null) {
                        plan = json.optString("plan", "free")
                        status = json.optString("status", "active")
                        expiresAt = if (json.has("expires_at") && !json.isNull("expires_at")) json.getString("expires_at") else null

                        entObj = json.optJSONObject("entitlements")
                    }
                }
            } else {
                val err = response.errorBody()?.string() ?: ""
                Log.e("MythicEntitlements", "getUserEntitlements RPC error ($statusCode): $err")
            }
            
            // Override plan with manual profile tier if RPC returned free or if profile tier is higher
            if ((plan == "free" || plan == "null" || plan.isEmpty()) && profileTier != "free") {
                plan = profileTier
                status = "active"
            }
            // Ensure consistency - if they typed 'prime', map to 'max'
            if (plan.lowercase() == "prime") plan = "max"

            val isPro = plan.lowercase() == "pro" && status == "active"
            val isMax = plan.lowercase() == "max" && status == "active"
            val isPaid = isPro || isMax

            val parsedEntitlements = Entitlements(
                adsFree = entObj?.optBoolean("ads_free", isPaid) ?: isPaid,
                unlimitedLumo = entObj?.optBoolean("unlimited_lumo", isPaid) ?: isPaid,
                highResolutionMaps = entObj?.optBoolean("high_resolution_maps", isPaid) ?: isPaid,
                prioritySupport = entObj?.optBoolean("priority_support", isPaid) ?: isPaid,
                ar3dScan = entObj?.optBoolean("ar_3d_scan", isMax) ?: isMax,
                betaFeatures = entObj?.optBoolean("beta_features", isMax) ?: isMax,
                digitalCollectionBadges = entObj?.optBoolean("digital_collection_badges", isMax) ?: isMax,
                customProfileBackground = entObj?.optBoolean("custom_profile_background", isMax) ?: isMax,
                animatedProfileBackground = entObj?.optBoolean("animated_profile_background", isMax) ?: isMax,
                advancedProfileCustomization = entObj?.optBoolean("advanced_profile_customization", isMax) ?: isMax,
                exclusivePassportFeatures = entObj?.optBoolean("exclusive_passport_features", isMax) ?: isMax
            )

            val entitlementResponse = EntitlementResponse(
                plan = plan,
                status = status,
                expiresAt = expiresAt,
                entitlements = parsedEntitlements
            )
            _entitlements.value = entitlementResponse
            Log.d("MythicEntitlements", "Entitlements refreshed -> Plan: $plan, Status: $status, Expires: $expiresAt")
            return@withContext
        } catch (e: Exception) {
            android.util.Log.e("MythicRepository", "Failed to refresh entitlements", e)
        }
    }

    suspend fun checkSession() {
        // Load token from prefs
        val prefs = context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE)
        SupabaseClient.accessToken = prefs.getString("access_token", null)
        
        // Simple session recovery emulated locally
        val existingProfile = dao.getProfile()
        if (existingProfile != null) {
            _currentUser.value = SupabaseUser(
                id = existingProfile.id,
                email = existingProfile.email,
                userMetadata = mapOf("username" to existingProfile.username, "full_name" to existingProfile.fullName)
            )
            refreshEntitlements()
            // Check night_owl badge
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour in 0..4) {
                unlockBadge("night_owl", "Midnight Explorer", "Use the app past midnight to study history", "bronze", "🦉")
            }
        } else {
            _currentUser.value = null
            _entitlements.value = null
        }
    }
    suspend fun seedUserData(userId: String, username: String, email: String, isDemo: Boolean = false, avatarUrl: String? = null) = withContext(Dispatchers.IO) {
        val defaultId = userId
        val level = if (isDemo) 5 else 1
        val xp = if (isDemo) 2750 else 0
        val streak = if (isDemo) 7 else 1
        val scansCount = if (isDemo) 23 else 0
        val badgesCount = if (isDemo) 12 else 0

        val profile = ProfileEntity(
            id = defaultId,
            username = username,
            email = email,
            fullName = username,
            avatarUrl = avatarUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$username",
            level = level,
            xp = xp,
            streak = streak,
            scansCount = scansCount,
            badgesCount = badgesCount + 1,
            subscriptionTier = "free"
        )
        dao.insertProfile(profile)

        dao.insertBadge(
            com.example.data.local.BadgeEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = defaultId,
                code = "pioneer",
                name = "Pioneer First 100",
                description = "Awarded to the first 100 users who joined the platform.",
                tier = "ultra rare",
                unlockedAt = System.currentTimeMillis(),
                icon = "💎"
            )
        )

        // Seed default quests
        val quests = listOf(
            QuestEntity(
                id = "quest_scan_1",
                userId = defaultId,
                title = "Scan a Heritage Site",
                description = "Scan any site using your camera",
                rewardXp = 50,
                progress = 0,
                target = 1,
                type = "daily",
                completed = false
            ),
            QuestEntity(
                id = "quest_read_3",
                userId = defaultId,
                title = "Read 3 Articles",
                description = "Read 3 heritage articles",
                rewardXp = 30,
                progress = 1,
                target = 3,
                type = "daily",
                completed = false
            ),
            QuestEntity(
                id = "quest_like_5",
                userId = defaultId,
                title = "Like 5 Articles",
                description = "Like 5 articles in feed",
                rewardXp = 20,
                progress = 2,
                target = 5,
                type = "daily",
                completed = false
            ),
            QuestEntity(
                id = "quest_weekly_explorer",
                userId = defaultId,
                title = "Weekly Explorer",
                description = "Explore heritage locations this week",
                rewardXp = 100,
                progress = 1,
                target = 1,
                type = "weekly",
                completed = true
            )
        )
        dao.insertQuests(quests)

        // Seed some default badges
        val defaultBadges = listOf(
            BadgeEntity(
                id = "badge_explorer",
                userId = defaultId,
                code = "explorer",
                name = "Explorer Badge",
                description = "Unlock by exploring your first site",
                tier = "bronze",
                unlockedAt = System.currentTimeMillis() - 86400000 * 3,
                icon = "🏆"
            ),
            BadgeEntity(
                id = "badge_archaeologist",
                userId = defaultId,
                code = "archaeologist",
                name = "Heritage Guardian",
                description = "Successfully scan 5 historical sites",
                tier = "silver",
                unlockedAt = System.currentTimeMillis() - 86400000 * 2,
                icon = "🏛️"
            ),
            BadgeEntity(
                id = "badge_photographer",
                userId = defaultId,
                code = "photo_expert",
                name = "Expert Photographer",
                description = "Scan multiple heritage views",
                tier = "gold",
                unlockedAt = System.currentTimeMillis() - 86400000,
                icon = "📸"
            ),
            BadgeEntity(
                id = "badge_historian",
                userId = defaultId,
                code = "historian",
                name = "Knowledge Seeker",
                description = "Read all suggestions in feed",
                tier = "platinum",
                unlockedAt = System.currentTimeMillis(),
                icon = "📜"
            )
        )
        dao.insertBadges(defaultBadges)

        // Seed pre-existing scan history
        val defaultScans = listOf(
            ScanHistoryEntity(
                id = "scan_sigiriya_seed",
                userId = defaultId,
                siteName = "Sigiriya Rock Fortress",
                province = "Central Province",
                description = "An ancient rock fortress with ancient frescoes.",
                unescoStatus = "UNESCO World Heritage Site",
                era = "5th Century AD",
                facts = "Mirror Wall, Lion's Paw gate, Water gardens",
                xpEarned = 50,
                imageUrl = "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600",
                timestamp = System.currentTimeMillis() - 3600000 * 4
            ),
            ScanHistoryEntity(
                id = "scan_galle_seed",
                userId = defaultId,
                siteName = "Galle Fort",
                province = "Southern Province",
                description = "A historical fortified city built by the Portuguese.",
                unescoStatus = "UNESCO World Heritage Site",
                era = "16th Century AD",
                facts = "Dutch Reformed Church, Lighthouse, Bastions",
                xpEarned = 50,
                imageUrl = "https://images.unsplash.com/photo-1546708973-b339540b5162?w=600",
                timestamp = System.currentTimeMillis() - 3600000 * 24
            )
        )
        dao.insertScans(defaultScans)

        // Seed some conversation with MYTHIC
        dao.insertMythicMessage(
            MythicConversationEntity(
                id = "msg_welcome",
                userId = defaultId,
                messageId = UUID.randomUUID().toString(),
                role = "assistant",
                content = "Aayubowan! 🇱🇰 I am MYTHIC, your Sri Lankan heritage guide. Ask me anything about Sigiriya, Anuradhapura, or our glorious history!",
                timestamp = System.currentTimeMillis() - 600000
            )
        )
    }

    // --- AUTH OPERATIONS ---
    suspend fun signUp(username: String, email: String, password: String, avatarUrl: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            if (SupabaseClient.isConfigured) {
                val response = SupabaseClient.service.signUp(
                    SupabaseSignUpRequest(email, password, mapOf("username" to username, "full_name" to username))
                )
                if (response.isSuccessful && response.body() != null) {
                    val authBody = response.body()!!
                    val user = authBody.user
                    if (user != null) {
                        authBody.accessToken?.let { 
                            SupabaseClient.accessToken = it
                            context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).edit().putString("access_token", it).apply()
                        }
                        // User needs to confirm email first, so we do NOT set _currentUser.value yet
                        _currentUser.value = null
        _entitlements.value = null
                        seedUserData(user.id, username, email, avatarUrl = avatarUrl)
                        
                        // Try to create profile remotely if possible (might fail if email not confirmed yet depending on Supabase settings, but we try)
                        try {
                            SupabaseClient.service.createProfile(
                                SupabaseProfile(
                                    id = user.id,
                                    username = username,
                                    email = email,
                                    fullName = username,
                                    avatarUrl = avatarUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=$username"
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("MythicRepository", "Initial remote profile creation failed", e)
                        }

                        return@withContext "VERIFICATION_SENT"
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (errorBody.contains("already registered", ignoreCase = true)) {
                        return@withContext "This email is already registered."
                    }
                    return@withContext "Registration failed. Try a different email or password."
                }
            }
            
            // Local emulation fallback (emulates email verification prompt)
            val mockId = UUID.randomUUID().toString()
            seedUserData(mockId, username, email, isDemo = false, avatarUrl = avatarUrl)
            return@withContext "VERIFICATION_SENT_MOCK"
        } catch (e: Exception) {
            Log.e("MythicRepository", "SignUp failed", e)
            return@withContext "An unexpected network error occurred: ${e.message}"
        }
    }

    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            if (SupabaseClient.isConfigured) {
                try {
                    val response = SupabaseClient.service.login(SupabaseLoginRequest(email, password))
                if (response.isSuccessful && response.body()?.user != null) {
                    val authBody = response.body()!!
                    val user = authBody.user!!
                    authBody.accessToken?.let { token ->
                        SupabaseClient.accessToken = token
                        context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).edit().putString("access_token", token).apply()
                    }
                    
                    // If email verification is enabled on Supabase, emailConfirmedAt will be null on unconfirmed users
                    if (user.emailConfirmedAt == null) {
                        return@withContext "EMAIL_NOT_CONFIRMED"
                    }
                    
                    _currentUser.value = user
                    
                    // Pull remote profile
                    refreshEntitlements()
                    val profileResponse = SupabaseClient.service.getProfile("eq.${user.id}")
                    if (profileResponse.isSuccessful && profileResponse.body()?.isNotEmpty() == true) {
                        val p = profileResponse.body()!![0]
                        dao.insertProfile(
                            ProfileEntity(
                                id = p.id,
                                username = p.username ?: "Unknown",
                                email = p.email ?: "",
                                fullName = p.fullName ?: p.username ?: "Unknown",
                                avatarUrl = p.avatarUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${p.username ?: "Unknown"}",
                                level = p.level ?: 1,
                                xp = p.xp ?: 0,
                                streak = p.streak ?: 0,
                                scansCount = p.scansCount ?: 0,
                                badgesCount = p.badgesCount ?: 0,
                                subscriptionTier = p.subscriptionTier ?: "free",
                                profileBackgroundUrl = p.profileBackgroundUrl,
                                isAnimatedBackground = p.isAnimatedBackground,
                                bio = p.bio,
                                interests = p.interests?.joinToString(","),
                                featuredBadgeId = p.featuredBadgeId
                            )
                        )
                    } else {
                        // Check if we already have this user's profile locally in Room
                        val existingLocal = dao.getProfileById(user.id)
                        if (existingLocal == null) {
                            val extractedUsername = (user.userMetadata?.get("username") as? String)
                                ?: (user.userMetadata?.get("full_name") as? String)
                                ?: user.email?.substringBefore("@")
                                ?: "Explorer"
                            
                            val userEmail = user.email ?: email
                            seedUserData(user.id, extractedUsername, userEmail)
                            
                            // Try to create/insert profile remotely in Supabase
                            try {
                                SupabaseClient.service.createProfile(
                                    SupabaseProfile(
                                        id = user.id,
                                        username = extractedUsername,
                                        email = userEmail,
                                        fullName = extractedUsername,
                                        avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$extractedUsername"
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("MythicRepository", "Failed to push fallback profile to Supabase", e)
                            }
                        }
                    }

                    // Pull remote badges, quests, scan history, saved sites for this user
                    syncUserDataFromRemote(user.id)

                    return@withContext null // Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (errorBody.contains("Email not confirmed", ignoreCase = true)) {
                        return@withContext "EMAIL_NOT_CONFIRMED"
                    }
                    // Remote login unsuccessful (or mock/test credentials); attempt local fallback
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Remote login failed, trying local fallback", e)
            }
        }

            // Local fallback login - search for exact user by email
            val existing = dao.getProfileByEmail(email)
            if (existing != null) {
                _currentUser.value = SupabaseUser(
                    existing.id,
                    existing.email,
                    emailConfirmedAt = "2026-06-24T00:00:00Z", // Emulate confirmed email
                    mapOf("username" to existing.username, "full_name" to existing.fullName)
                )
                return@withContext null // Success
            } else if (email.isNotEmpty() && password.isNotEmpty()) {
                // Emulate dynamic creation of separate user profile
                val mockId = UUID.randomUUID().toString()
                val name = email.split("@")[0]
                val user = SupabaseUser(
                    mockId, 
                    email, 
                    emailConfirmedAt = "2026-06-24T00:00:00Z", // Emulate confirmed email
                    mapOf("username" to name)
                )
                _currentUser.value = user
                seedUserData(mockId, name, email, isDemo = false)
                return@withContext null // Success
            }
            return@withContext "Incorrect email or password."
        } catch (e: Exception) {
            Log.e("MythicRepository", "Login failed", e)
            return@withContext "Network connection issue: ${e.message}"
        }
    }

    private suspend fun syncUserDataFromRemote(userId: String) {
        if (!SupabaseClient.isConfigured) return
        try {
            val badgesRes = SupabaseClient.service.getBadges("eq.$userId")
            if (badgesRes.isSuccessful && badgesRes.body() != null) {
                val entities = badgesRes.body()!!.map { b ->
                    BadgeEntity(
                        id = b.id ?: UUID.randomUUID().toString(),
                        userId = b.userId,
                        code = b.code,
                        name = b.name,
                        description = b.description,
                        tier = b.tier,
                        unlockedAt = System.currentTimeMillis(),
                        icon = b.icon
                    )
                }
                if (entities.isNotEmpty()) dao.insertBadges(entities)
            }

            val questsRes = SupabaseClient.service.getQuests("eq.$userId")
            if (questsRes.isSuccessful && questsRes.body() != null) {
                val entities = questsRes.body()!!.map { q ->
                    QuestEntity(
                        id = q.id ?: UUID.randomUUID().toString(),
                        userId = q.userId,
                        title = q.title,
                        description = q.description,
                        rewardXp = q.rewardXp,
                        progress = q.progress,
                        target = q.target,
                        type = q.type,
                        completed = q.completed
                    )
                }
                if (entities.isNotEmpty()) dao.insertQuests(entities)
            }

            val scansRes = SupabaseClient.service.getScanHistory("eq.$userId")
            if (scansRes.isSuccessful && scansRes.body() != null) {
                val entities = scansRes.body()!!.map { s ->
                    ScanHistoryEntity(
                        id = s.id ?: UUID.randomUUID().toString(),
                        userId = s.userId,
                        siteName = s.siteName,
                        province = s.province,
                        description = s.description,
                        unescoStatus = s.unescoStatus,
                        era = s.era,
                        facts = s.facts,
                        xpEarned = s.xpEarned,
                        imageUrl = s.imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                }
                if (entities.isNotEmpty()) dao.insertScans(entities)
            }

            val savedRes = SupabaseClient.service.getSavedSites("eq.$userId")
            if (savedRes.isSuccessful && savedRes.body() != null) {
                for (s in savedRes.body()!!) {
                    dao.insertSavedSite(
                        SavedSiteEntity(
                            id = s.id ?: UUID.randomUUID().toString(),
                            userId = s.userId,
                            siteName = s.siteName,
                            province = s.province,
                            imageUrl = s.imageUrl,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "Error syncing user remote data", e)
        }
    }

    suspend fun recoverPassword(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (SupabaseClient.isConfigured) {
                val response = SupabaseClient.service.recoverPassword(mapOf("email" to email))
                return@withContext response.isSuccessful
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("MythicRepository", "Recover password failed", e)
            false
        }
    }

    override suspend fun getProfile(): ProfileEntity? = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.getProfileById(userId)
        } else {
            dao.getProfile()
        }
    }

    override suspend fun getProfileById(userId: String): ProfileEntity? = withContext(Dispatchers.IO) {
        dao.getProfileById(userId)
    }

    override suspend fun insertProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        dao.insertProfile(profile)
    }

    override suspend fun updateStreak(userId: String) = withContext(Dispatchers.IO) {
        val targetUserId = userId.ifEmpty { _currentUser.value?.id ?: return@withContext }
        val currentProfile = dao.getProfileById(targetUserId) ?: return@withContext
        val newStreak = currentProfile.streak + 1
        val updated = currentProfile.copy(streak = newStreak)
        dao.insertProfile(updated)

        if (SupabaseClient.isConfigured && _currentUser.value != null) {
            try {
                SupabaseClient.service.updateProfile(
                    "eq.${updated.id}",
                    mapOf("streak" to newStreak)
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase streak sync failed", e)
            }
        }
    }

    override suspend fun updateAvatar(userId: String, newAvatarUrl: String) = withContext(Dispatchers.IO) {
        val targetUserId = userId.ifEmpty { _currentUser.value?.id ?: return@withContext }
        val currentProfile = dao.getProfileById(targetUserId) ?: return@withContext
        val updated = currentProfile.copy(avatarUrl = newAvatarUrl)
        dao.insertProfile(updated)

        if (SupabaseClient.isConfigured && _currentUser.value != null) {
            try {
                SupabaseClient.service.updateProfile(
                    "eq.${updated.id}",
                    mapOf("avatar_url" to newAvatarUrl)
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase avatar sync failed", e)
            }
        }
    }

    override suspend fun clearProfile() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.deleteProfileById(userId)
        } else {
            dao.clearProfile()
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            if (SupabaseClient.isConfigured) {
                SupabaseClient.service.logout()
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "Remote logout failed", e)
        }
        _currentUser.value = null
        _entitlements.value = null
        SupabaseClient.accessToken = null
        context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).edit().remove("access_token").apply()
        dao.clearProfile()
    }

    suspend fun uploadFile(
        bucket: String, 
        path: String, 
        bytes: ByteArray, 
        contentType: String,
        authHeader: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext null
        try {
            val requestBody = okhttp3.RequestBody.create(
                contentType.toMediaTypeOrNull(),
                bytes
            )
            val response = SupabaseClient.service.uploadFile(bucket, path, requestBody, contentType, authHeader)
            if (response.isSuccessful) {
                val supabaseUrl = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
                return@withContext "$supabaseUrl/storage/v1/object/public/$bucket/$path"
            } else {
                Log.e("MythicRepository", "File upload failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "File upload exception", e)
        }
        return@withContext null
    }

    suspend fun getSubscriptions(userId: String): List<SupabaseSubscription> = withContext(Dispatchers.IO) {
        val remoteList = if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.getSubscriptions("eq.$userId")
                if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Failed to fetch subscriptions", e)
                emptyList()
            }
        } else emptyList()

        val localList = synchronized(_cachedSubscriptions) {
            _cachedSubscriptions.filter { it.userId == userId || userId.isEmpty() }
        }

        (localList + remoteList).distinctBy { "${it.userId}_${it.plan}_${it.status}" }
    }

    suspend fun sendPaymentEmail(email: String, tier: String, amount: String, transactionId: String): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext false
        try {
            val emailRequest = mapOf(
                "email" to email,
                "tier" to tier,
                "amount" to amount,
                "transaction_id" to transactionId
            )
            val response = SupabaseClient.service.sendPaymentEmail(emailRequest)
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e("MythicRepository", "Failed to send payment email", e)
        }
        return@withContext false
    }

    // --- GAME XP SYSTEM ---
    override suspend fun addXp(amount: Int) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: dao.getProfile()?.id ?: return@withContext
        val currentProfile = dao.getProfileById(userId) ?: dao.getProfile() ?: return@withContext
        val newXp = currentProfile.xp + amount
        // XP calculations: level changes every 600 XP (Level = newXp / 600 + 1)
        val calculatedLevel = (newXp / 600) + 1
        val newLevel = maxOf(currentProfile.level, calculatedLevel)
        
        val updated = currentProfile.copy(
            xp = newXp,
            level = newLevel
        )
        dao.insertProfile(updated)

        // Atomic update via Supabase award_xp RPC (server-authoritative)
        if (SupabaseClient.isConfigured && _currentUser.value != null) {
            try {
                val rpcResponse = SupabaseClient.service.awardXp(
                    mapOf("p_amount" to amount, "p_user_id" to updated.id)
                )
                if (!rpcResponse.isSuccessful) {
                    // Fallback to direct profile update if RPC is pending
                    SupabaseClient.service.updateProfile(
                        "eq.${updated.id}",
                        mapOf("xp" to newXp, "level" to newLevel)
                    )
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase sync of XP failed", e)
            }
        }
    }

    suspend fun addScanCount() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: dao.getProfile()?.id ?: return@withContext
        val currentProfile = dao.getProfileById(userId) ?: dao.getProfile() ?: return@withContext
        val updated = currentProfile.copy(scansCount = currentProfile.scansCount + 1)
        dao.insertProfile(updated)
        
        // Atomic update via Supabase increment_scan_count RPC
        if (SupabaseClient.isConfigured && _currentUser.value != null) {
            try {
                val rpcResponse = SupabaseClient.service.incrementScanCount(
                    mapOf("p_user_id" to updated.id)
                )
                if (!rpcResponse.isSuccessful) {
                    SupabaseClient.service.updateProfile(
                        "eq.${updated.id}",
                        mapOf("scans_count" to updated.scansCount)
                    )
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase sync of scans_count failed", e)
            }
        }
    }

    // --- BADGES ---
    override suspend fun unlockBadge(
        code: String,
        name: String,
        description: String,
        tier: String,
        icon: String?
    ) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: dao.getProfile()?.id ?: "guest"
        // Prevent duplicate unlocks for this user
        val existing = dao.getBadgeByCodeAndUser(userId, code) ?: dao.getBadgeByCode(code)
        if (existing != null) return@withContext

        val badge = BadgeEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            code = code,
            name = name,
            description = description,
            tier = tier,
            unlockedAt = System.currentTimeMillis(),
            icon = icon ?: "🏆"
        )
        dao.insertBadge(badge)

        // Increment badgesCount in the local profile
        val currentProfile = dao.getProfileById(userId) ?: dao.getProfile()
        if (currentProfile != null) {
            val updated = currentProfile.copy(badgesCount = currentProfile.badgesCount + 1)
            dao.insertProfile(updated)
            if (SupabaseClient.isConfigured && _currentUser.value != null) {
                try {
                    val rpcResponse = SupabaseClient.service.incrementBadgeCount(
                        mapOf("p_user_id" to updated.id)
                    )
                    if (!rpcResponse.isSuccessful) {
                        SupabaseClient.service.updateProfile(
                            "eq.${updated.id}",
                            mapOf("badges_count" to updated.badgesCount)
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MythicRepository", "Supabase sync of badges_count failed", e)
                }
            }
        }

        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveBadge(
                    SupabaseBadge(
                        id = badge.id,
                        userId = badge.userId,
                        code = badge.code,
                        name = badge.name,
                        description = badge.description,
                        tier = badge.tier,
                        icon = badge.icon
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase badge sync failed", e)
            }
        }
    }

    override suspend fun clearBadges() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.clearBadgesByUserId(userId)
        } else {
            dao.clearBadges()
        }
    }

    // --- SAVED SITES ---
    override suspend fun toggleSaveSite(siteName: String, province: String, imageUrl: String?) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        if (dao.isSiteSavedByUser(userId, siteName)) {
            dao.deleteSavedSiteByUser(userId, siteName)
            if (SupabaseClient.isConfigured) {
                try {
                    SupabaseClient.service.deleteSavedSite("eq.$userId", "site_name=eq.$siteName")
                } catch (e: Exception) {
                    Log.e("MythicRepository", "Supabase saved sites delete failed", e)
                }
            }
        } else {
            val savedSite = SavedSiteEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                siteName = siteName,
                province = province,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            dao.insertSavedSite(savedSite)
            if (SupabaseClient.isConfigured) {
                try {
                    SupabaseClient.service.saveSite(
                        SupabaseSavedSite(
                            id = savedSite.id,
                            userId = savedSite.userId,
                            siteName = savedSite.siteName,
                            province = savedSite.province,
                            imageUrl = savedSite.imageUrl
                        )
                    )
                } catch (e: Exception) {
                    Log.e("MythicRepository", "Supabase save site failed", e)
                }
            }
        }
    }

    override suspend fun isSiteSaved(siteName: String): Boolean = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        dao.isSiteSavedByUser(userId, siteName)
    }

    override suspend fun clearSavedSites() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.clearSavedSitesByUserId(userId)
        } else {
            dao.clearSavedSites()
        }
    }

    // --- QUESTS PROGRESS ---
    override suspend fun incrementQuestProgress(type: String) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext
        val currentQuests = dao.getQuestsByUserId(userId)
        for (q in currentQuests) {
            // increment daily scan/read/like quests based on triggers
            if (!q.completed) {
                var triggered = false
                var inc = 0
                if (type == "scan" && q.title.contains("Scan", ignoreCase = true)) {
                    triggered = true
                    inc = 1
                } else if (type == "read" && q.title.contains("Read", ignoreCase = true)) {
                    triggered = true
                    inc = 1
                } else if (type == "like" && q.title.contains("Like", ignoreCase = true)) {
                    triggered = true
                    inc = 1
                }

                if (triggered) {
                    val newProgress = (q.progress + inc).coerceAtMost(q.target)
                    val completed = newProgress >= q.target
                    val updated = q.copy(progress = newProgress, completed = completed)
                    dao.updateQuest(updated)
                    if (completed) {
                        addXp(q.rewardXp)
                    }
                    
                    if (SupabaseClient.isConfigured) {
                        try {
                            SupabaseClient.service.updateQuest(
                                "eq.${q.id}",
                                mapOf("progress" to newProgress, "completed" to completed)
                            )
                        } catch (e: Exception) {
                            Log.e("MythicRepository", "Supabase quest update failed", e)
                        }
                    }
                }
            }
        }
    }

    override suspend fun clearQuests() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.clearQuestsByUserId(userId)
        } else {
            dao.clearQuests()
        }
    }

    // --- SCAN LANDMARK ---
    override suspend fun scanHeritage(siteName: String, imageUrl: String?): ScanHistoryEntity = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        
        // Define site metadata
        val province = when(siteName) {
            "Sigiriya Rock Fortress" -> "Central Province"
            "Temple of the Tooth" -> "Central Province"
            "Dambulla Cave Temple" -> "Central Province"
            "Galle Fort" -> "Southern Province"
            "Anuradhapura" -> "North Central Province"
            "Polonnaruwa Ancient City" -> "North Central Province"
            "Jaffna Fort" -> "Northern Province"
            else -> "Central Province"
        }

        val description = when(siteName) {
            "Sigiriya Rock Fortress" -> "An ancient rock fortress and castle complex built by King Kashyapa in the 5th century AD. It is a UNESCO World Heritage Site famous for its frescoes, water gardens, and advanced urban planning."
            "Temple of the Tooth" -> "A glorious golden-roofed Buddhist temple in Kandy which houses the sacred tooth relic of the Buddha."
            "Dambulla Cave Temple" -> "The largest and best-preserved cave temple complex in Sri Lanka, boasting magnificent Buddha statues and complex historical wall murals."
            "Galle Fort" -> "A historical fortified city built first by the Portuguese, then heavily fortified by the Dutch, standing as an archaeological treasure."
            "Anuradhapura" -> "The magnificent ancient capital of Sri Lanka, home to colossal stupas like Ruwanwelisaya and Jetavanaramaya."
            "Polonnaruwa Ancient City" -> "The second ancient capital of Sri Lanka, renowned for its incredible stone sculptures at Gal Vihara and ruins of grand palaces."
            "Jaffna Fort" -> "A beautiful historical star-shaped coastal fort built by the Portuguese in 1618, overlooking the scenic Jaffna lagoon."
            else -> "A majestic Sri Lankan heritage site filled with centuries of cultural wealth, sacred history, and architectural marvel."
        }

        val unescoStatus = when(siteName) {
            "Sigiriya Rock Fortress", "Temple of the Tooth", "Dambulla Cave Temple", "Galle Fort", "Anuradhapura", "Polonnaruwa Ancient City" -> "UNESCO World Heritage Site"
            else -> "Protected Cultural Monument"
        }

        val era = when(siteName) {
            "Sigiriya Rock Fortress" -> "5th Century AD"
            "Temple of the Tooth" -> "16th Century AD"
            "Dambulla Cave Temple" -> "1st Century BC"
            "Galle Fort" -> "16th Century AD"
            "Anuradhapura" -> "3rd Century BC"
            "Polonnaruwa Ancient City" -> "11th Century AD"
            "Jaffna Fort" -> "17th Century AD"
            else -> "Ancient Era"
        }

        val facts = when(siteName) {
            "Sigiriya Rock Fortress" -> "The mirror wall still contains ancient graffiti; The lion gate was a massive masonry lion; Advanced hydraulic systems feed the active garden fountains."
            "Temple of the Tooth" -> "Houses the sacred tooth relic of Gautama Buddha; Inside the Inner Sanctuary; Holds the famous annual Esala Perahera festival."
            "Dambulla Cave Temple" -> "Spread over five separate cavern structures; Houses 153 Buddha statues; Pre-historic human skeletal remains found in nearby caves."
            "Galle Fort" -> "A fusion of European architectural styles and South Asian traditions; Stands unharmed by the 2004 tsunami; Houses colonial-era villas."
            "Anuradhapura" -> "Ruwanwelisaya is a marvel of ancient engineering; The sacred Jaya Sri Maha Bodhi is the oldest human-planted tree; Complex water reservoirs."
            "Polonnaruwa Ancient City" -> "Features the majestic Gal Vihara rock-cut Buddha statues; Parakrama Samudra is an immense artificial lake; Polonnaruwa Vatadage is fully intact."
            "Jaffna Fort" -> "Portugal established key fortifications; Captured and modified into a star-fort by the Dutch; Star-shaped layout is highly visible."
            else -> "Houses rich archaeological discoveries; Serves as a vital cultural monument; Reflects outstanding engineering and ancient design."
        }

        val scan = ScanHistoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            siteName = siteName,
            province = province,
            description = description,
            unescoStatus = unescoStatus,
            era = era,
            facts = facts,
            xpEarned = 50,
            imageUrl = imageUrl ?: "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600",
            timestamp = System.currentTimeMillis()
        )

        // Store locally
        dao.insertScan(scan)
        
        // Add game statistics and progress
        addXp(50)
        addScanCount()
        incrementQuestProgress("scan")

        // Try to update Supabase
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveScan(
                    SupabaseScan(
                        id = scan.id,
                        userId = scan.userId,
                        siteName = scan.siteName,
                        province = scan.province,
                        description = scan.description,
                        unescoStatus = scan.unescoStatus,
                        era = scan.era,
                        facts = scan.facts,
                        xpEarned = scan.xpEarned,
                        imageUrl = scan.imageUrl
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase scan save failed", e)
            }
        }

        // Trigger badge unlock checks
        checkForScanBadges(siteName)

        return@withContext scan
    }

    override suspend fun saveCustomScan(
        siteName: String,
        province: String,
        description: String,
        unescoStatus: String,
        era: String,
        facts: String,
        imageUrl: String?
    ): ScanHistoryEntity = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        val scan = ScanHistoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            siteName = siteName,
            province = province,
            description = description,
            unescoStatus = unescoStatus,
            era = era,
            facts = facts,
            xpEarned = 50,
            imageUrl = imageUrl ?: "https://images.unsplash.com/photo-1588598126781-db26040a4cfc?w=600",
            timestamp = System.currentTimeMillis()
        )
        dao.insertScan(scan)
        addXp(50)
        addScanCount()
        incrementQuestProgress("scan")

        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveScan(
                    SupabaseScan(
                        id = scan.id,
                        userId = scan.userId,
                        siteName = scan.siteName,
                        province = scan.province,
                        description = scan.description,
                        unescoStatus = scan.unescoStatus,
                        era = scan.era,
                        facts = scan.facts,
                        xpEarned = scan.xpEarned,
                        imageUrl = scan.imageUrl
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase custom scan save failed", e)
            }
        }

        // Trigger badge unlock checks
        checkForScanBadges(siteName)

        return@withContext scan
    }

    private suspend fun checkForScanBadges(siteName: String) {
        // Unlock "first step" badge on first scan if not already unlocked
        unlockBadge("first_step", "Aayubowan!", "Welcome to the Sri Lanka Heritage explorer community!", "bronze", "🇱🇰")

        // Unlock "explorer" badge
        unlockBadge("explorer", "Explorer Badge", "Unlock by exploring your first heritage site", "bronze", "🏆")

        // Unlock site specific badge
        val badgeDef = when {
            siteName.contains("Sigiriya", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "sigiriya_explorer" }
            siteName.contains("Tooth", ignoreCase = true) || siteName.contains("Kandy", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "kandy_explorer" }
            siteName.contains("Galle", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "galle_explorer" }
            siteName.contains("Dambulla", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "dambulla_explorer" }
            siteName.contains("Anuradhapura", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "anuradhapura_explorer" }
            siteName.contains("Polonnaruwa", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "polonnaruwa_explorer" }
            siteName.contains("Pidurangala", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "pidurangala_explorer" }
            siteName.contains("Nine Arch", ignoreCase = true) || siteName.contains("Ella", ignoreCase = true) -> 
                com.example.data.local.ALL_BADGES.find { it.code == "ella_explorer" }
            else -> null
        }

        if (badgeDef != null) {
            unlockBadge(badgeDef.code, badgeDef.name, badgeDef.description, badgeDef.tier, badgeDef.icon)
        }

        // Count how many unique heritage site badges we have unlocked
        val userId = _currentUser.value?.id ?: "guest"
        val allUnlocked = dao.getAllBadgesByUserId(userId)
        val scannedSiteCodes = listOf(
            "sigiriya_explorer", "kandy_explorer", "galle_explorer", "dambulla_explorer",
            "anuradhapura_explorer", "polonnaruwa_explorer", "pidurangala_explorer", "ella_explorer"
        )
        val unlockedScannedCodesCount = allUnlocked.count { it.code in scannedSiteCodes }

        // Heritage Guardian: Successfully scan 5 historical sites
        if (unlockedScannedCodesCount >= 5) {
            val badgeArchaeologist = com.example.data.local.ALL_BADGES.find { it.code == "archaeologist" }
            if (badgeArchaeologist != null) {
                unlockBadge(
                    badgeArchaeologist.code,
                    badgeArchaeologist.name,
                    badgeArchaeologist.description,
                    badgeArchaeologist.tier,
                    badgeArchaeologist.icon
                )
            }
        }

        // Heritage Conqueror: Explore all 8 major historical sites
        if (unlockedScannedCodesCount >= 8) {
            val badgeConqueror = com.example.data.local.ALL_BADGES.find { it.code == "heritage_conqueror" }
            if (badgeConqueror != null) {
                unlockBadge(
                    badgeConqueror.code,
                    badgeConqueror.name,
                    badgeConqueror.description,
                    badgeConqueror.tier,
                    badgeConqueror.icon
                )
            }
        }
    }

    override suspend fun clearScanHistory() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (userId != null) {
            dao.clearScanHistoryByUserId(userId)
        } else {
            dao.clearScanHistory()
        }
    }

    // --- REPORTS ---
    suspend fun submitReport(report: SupabaseReport) = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveReport(report)
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase report save failed", e)
            }
        }
    }

    suspend fun getRecentReports(): List<SupabaseReport> = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.getReports().body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase reports fetch failed", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // --- COMMUNITY ---
    suspend fun getCommunityPosts(): List<SupabaseCommunityPost> = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.getCommunityPosts().body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase community posts fetch failed", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun submitCommunityPost(content: String, imageUrl: String? = null) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        val post = SupabaseCommunityPost(
            id = UUID.randomUUID().toString(),
            userId = userId,
            content = content,
            imageUrl = imageUrl,
            likes = 0,
            createdAt = System.currentTimeMillis().toString()
        )
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveCommunityPost(post)
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase community post save failed", e)
            }
        }
    }

    suspend fun submitComment(reelId: String? = null, postId: String? = null, content: String) = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        val comment = SupabaseComment(
            id = UUID.randomUUID().toString(),
            reelId = reelId,
            postId = postId,
            userId = userId,
            content = content,
            createdAt = System.currentTimeMillis().toString()
        )
        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveComment(comment)
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase comment save failed", e)
            }
        }
    }

    // --- MYTHIC COMPANION CHAT V2 ---
    suspend fun sendMythicMessage(userMessageContent: String): String = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: "guest"
        
        if (userId != "guest" && SupabaseClient.isConfigured) {
            try {
                val lumoCheck = SupabaseClient.service.canUseLumo()
                if (lumoCheck.isSuccessful) {
                    val responseStr = lumoCheck.body()?.string() ?: ""
                    if (responseStr.contains("\"allowed\":false") || responseStr.contains("\"allowed\": false")) {
                        return@withContext "You have reached your daily limit for Lumo. Please upgrade to Pro or Max to continue chatting."
                    }
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Error checking lumo entitlements", e)
            }
        }
        
        // Fetch Conversation Memory (last 5 messages)
        val history = dao.getMythicConversations().takeLast(5)
        
        // Save user message
        val userMsg = MythicConversationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            messageId = UUID.randomUUID().toString(),
            role = "user",
            content = userMessageContent,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMythicMessage(userMsg)

        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveMythicMessage(
                    SupabaseMythicMessage(
                        id = userMsg.id,
                        userId = userMsg.userId,
                        messageId = userMsg.messageId,
                        role = userMsg.role,
                        content = userMsg.content
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase userMsg save failed", e)
            }
        }

        // Call Gemini with Memory
        val assistantResponse = if (GeminiClient.isConfigured) {
            try {
                val systemInstruction = """
                    You are MYTHIC V2, a production-ready advanced AI heritage guide for Sri Lanka.
                    Capabilities:
                    1. Heritage Expert: Deep knowledge of ancient cities (Sigiriya, Anuradhapura, etc.), kingdoms, and archeology.
                    2. Tour Guide & Trip Planner: Can plan 3-day or 7-day heritage tours.
                    3. Translator: Can translate phrases between English, Sinhala, and Tamil.
                    4. Quiz Generator: Create engaging heritage quizzes.
                    5. Cite Sources: Always mention Wikipedia or National Heritage Database when possible.
                    
                    Memory: You have access to the recent conversation history. Use it to provide contextual answers.
                    Search: You can 'simulate' searching the heritage database or Wikipedia for the latest info.
                    
                    Tone: Conversational, knowledgeable, inspiring. Use emojis.
                """.trimIndent()
                
                val historyParts = history.map { entity: MythicConversationEntity -> 
                    GeminiContent(
                        role = if (entity.role == "user") "user" else "model",
                        parts = listOf(GeminiPart(text = entity.content))
                    )
                }
                
                val currentRequestContent = GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userMessageContent))
                )

                val contents = historyParts + listOf(currentRequestContent)

                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiContent(
                        role = "system",
                        parts = listOf(GeminiPart(text = systemInstruction))
                    )
                )
                
                val res = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                if (res.isSuccessful && res.body() != null) {
                    res.body()!!.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                        ?: getLocalMythicResponse(userMessageContent)
                } else {
                    getLocalMythicResponse(userMessageContent)
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Gemini API failed, falling back to local Mythic", e)
                getLocalMythicResponse(userMessageContent)
            }
        } else {
            getLocalMythicResponse(userMessageContent)
        }

        // Save assistant response
        val assistantMsg = MythicConversationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            messageId = UUID.randomUUID().toString(),
            role = "assistant",
            content = assistantResponse,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMythicMessage(assistantMsg)

        if (SupabaseClient.isConfigured) {
            try {
                SupabaseClient.service.saveMythicMessage(
                    SupabaseMythicMessage(
                        id = assistantMsg.id,
                        userId = assistantMsg.userId,
                        messageId = assistantMsg.messageId,
                        role = assistantMsg.role,
                        content = assistantMsg.content
                    )
                )
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase assistantMsg save failed", e)
            }
        }

        // Check and unlock "curious_mind" badge
        val allConvs = dao.getMythicConversations()
        val userMessagesCount = allConvs.count { it.role == "user" }
        if (userMessagesCount >= 10) {
            unlockBadge("curious_mind", "Curious Historian", "Ask Mythic AI 10 questions about Sri Lankan history", "bronze", "🧠")
        }

        return@withContext assistantResponse
    }
    
    private fun getLocalMythicResponse(userQuery: String): String {
        val q = userQuery.lowercase()
        return when {
            q.contains("sigiriya") || q.contains("kashyapa") -> {
                "Sigiriya Rock Fortress is one of Sri Lanka's most iconic sites! 🏛️ Built in the 5th century AD by King Kashyapa, it served as a secure royal palace and a dramatic canvas for the famous Sigiriya frescoes (the beautiful cloud maidens). The site is an engineering masterpiece with working hydraulic gravity-fed fountains. Don't forget to climb through the massive Lion's Paw Gate!"
            }
            q.contains("tooth") || q.contains("kandy") || q.contains("temple") -> {
                "The Sri Dalada Maligawa (Temple of the Sacred Tooth Relic) in Kandy is a deeply revered Buddhist site. 🏰 It houses the sacred left canine tooth of Gautama Buddha, which was brought to Sri Lanka in the 4th century AD. It represents a symbol of royal authority—whoever holds the relic is believed to hold the right to rule the land. Every year, the city hosts the grand Esala Perahera festival, featuring glowing lanterns, traditional Kandyan dancers, and majestic adorned elephants!"
            }
            q.contains("galle") || q.contains("fort") -> {
                "Galle Fort in the Southern Province is a magnificent coastal bastion! 🌊 Initially built by the Portuguese in 1588, it was heavily fortified by the Dutch in the 17th century. It stands as a living museum of colonial architecture fused with South Asian traditions. Be sure to check out the Galle Lighthouse, the Dutch Reformed Church, and walk along the massive stone walls at sunset!"
            }
            q.contains("anuradhapura") || q.contains("ruwanweli") -> {
                "Anuradhapura was the first glorious capital of ancient Sri Lanka, founded in the 4th century BC! 🏛️ It houses some of the tallest brick stupas in the world, such as the colossal Ruwanwelisaya and Jetavanaramaya. It is also home to the sacred Jaya Sri Maha Bodhi, a sapling from the original Bodhi Tree under which the Buddha attained enlightenment, making it the oldest historical tree planted by humans in the world!"
            }
            q.contains("polonnaruwa") || q.contains("gal vihara") -> {
                "Polonnaruwa succeeded Anuradhapura as the medieval capital of Sri Lanka (11th - 13th century AD). 📜 It is famous for its outstanding stone architecture! The Gal Vihara temple features four massive Buddha statues carved directly out of a single granite rock face. It also features the beautiful round relic house called the Vatadage and the enormous Parakrama Samudra artificial lake, built by King Parakramabahu the Great."
            }
            q.contains("quiz") || q.contains("test") -> {
                "Let's test your knowledge! 🧠 Here is a quick heritage quiz:\n\n*Which ancient Sri Lankan king built the magnificent Sigiriya Rock Fortress in the 5th century AD?*\n\nA) King Devanampiyatissa\nB) King Kashyapa\nC) King Parakramabahu\n\nReply with the answer and I'll verify it!"
            }
            q.contains("b") || q.contains("kashyapa") -> {
                "Correct! 🎉 King Kashyapa built Sigiriya as his spectacular royal fortress in the sky to protect himself from his brother, Mugalan. You've earned a virtual Explorer Star! ⭐ Ask me about another heritage site to learn more!"
            }
            q.contains("recommend") || q.contains("visit") || q.contains("where") -> {
                "I highly recommend visiting the **Dambulla Cave Temple**! ⛰️ It consists of five majestic caves carved into a massive rock, housing 153 outstanding statues of the Buddha and covered in ancient ceiling paintings from over 2,000 years ago. It is incredibly well preserved and deeply spiritual."
            }
            q.contains("history") || q.contains("culture") || q.contains("sri lanka") -> {
                "Sri Lanka has a documented history spanning over 3,000 years, with some of the most advanced hydraulic civilizations of the ancient world! 🇱🇰 Our culture is shaped by Buddhism, introduced in the 3rd century BC, and enriched by multi-ethnic trade routes connecting East and West. We boast 8 UNESCO World Heritage Sites! Ask me about Sigiriya, Anuradhapura, Galle Fort, or Dambulla to dive deeper."
            }
            else -> {
                "That is a fascinating question! 📜 Sri Lankan history and archaeology are incredibly rich. Whether it's the colossal stupas of Anuradhapura, the engineering wonders of Sigiriya, or the coastal fortifications of Galle Fort, there is always a story to tell. Ask me specifically about Sigiriya, Temple of the Tooth, Galle Fort, or Anuradhapura to get a detailed historical explanation!"
            }
        }
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) {
        dao.clearMythicConversations()
    }

    // --- QUIZ GENERATION ---
    suspend fun generateQuiz(siteName: String): HeritageQuiz? = withContext(Dispatchers.IO) {
        if (!GeminiClient.isConfigured) return@withContext null

        val prompt = "Generate a 3-question multiple choice educational quiz about $siteName in Sri Lanka."
        
        val systemInstruction = """
            You are a Heritage Educator. Generate a structured educational quiz about a specific Sri Lankan heritage site.
            The output MUST be in valid JSON format matching the following schema:
            {
              "siteName": "string",
              "questions": [
                {
                  "question": "string",
                  "options": ["string", "string", "string", "string"],
                  "correctAnswerIndex": number,
                  "explanation": "string"
                }
              ]
            }
            Keep questions engaging and factually accurate.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        try {
            val response = GeminiClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonString != null) {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(HeritageQuiz::class.java)
                    return@withContext adapter.fromJson(jsonString)
                }
            }
        } catch (e: Exception) {
            Log.e("MythicRepository", "Quiz generation failed", e)
        }
        return@withContext null
    }

    // --- ARTICLES ---
    suspend fun getHeritageArticles(): List<SupabaseArticle> = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.getHeritageArticles()
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    Log.e("MythicRepository", "Error getting heritage articles: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase heritage articles fetch failed", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun saveHeritageArticle(article: SupabaseArticle) = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.saveHeritageArticle(article)
                if (!response.isSuccessful) {
                    Log.e("MythicRepository", "Error saving heritage article: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase heritage article save failed", e)
            }
        }
    }

    suspend fun saveArticleReaction(reaction: SupabaseArticleReaction) = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.saveArticleReaction(reaction)
                if (!response.isSuccessful) {
                    Log.e("MythicRepository", "Error saving article reaction: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase article reaction save failed", e)
            }
        }
    }

    suspend fun updateArticleLikes(id: String, likes: Int) = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.updateHeritageArticleLikes("eq.$id", mapOf("likes" to likes))
                if (!response.isSuccessful) {
                    Log.e("MythicRepository", "Error updating article likes: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase article likes update failed", e)
            }
        }
    }

    // --- REELS ---
    suspend fun saveReel(reel: SupabaseReel) = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.saveReel(reel)
                if (!response.isSuccessful) {
                    Log.e("MythicRepository", "Error saving reel: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase reel save failed", e)
            }
        }
    }

    suspend fun getReels(): List<SupabaseReel> = withContext(Dispatchers.IO) {
        if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.getReels()
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase reels fetch failed", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override suspend fun insertUserPreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        dao.insertUserPreferences(prefs)
    }

    // --- PAYMENTS & SUBSCRIPTIONS (RPC-BASED SECURE FLOW) ---
    suspend fun createPaymentOrderRpc(plan: String): PaymentOrderResult = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val err = "Supabase is not configured."
            Log.e("MythicPayment", "[create_payment_order] $err")
            return@withContext PaymentOrderResult.Error(err)
        }

        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)

        if (token.isNullOrBlank()) {
            val err = "Authentication required. You must be signed in with an active session to purchase."
            Log.e("MythicPayment", "[create_payment_order] FAILED: $err")
            return@withContext PaymentOrderResult.Error(err, 401)
        }
        SupabaseClient.accessToken = token

        val user = _currentUser.value
        val userId = user?.id ?: dao.getProfile()?.id ?: ""
        val userEmail = user?.email ?: dao.getProfile()?.email ?: ""
        val authHeader = "Bearer $token"

        val backendPlan = when (plan.lowercase().trim()) {
            "prime", "max" -> "max"
            "pro" -> "pro"
            else -> plan.lowercase().trim()
        }
        if (backendPlan != "pro" && backendPlan != "max") {
            val err = "Invalid plan '$plan'. Must be 'pro' or 'prime'."
            Log.e("MythicPayment", "[create_payment_order] $err")
            return@withContext PaymentOrderResult.Error(err, 400)
        }

        Log.d("MythicPayment", "--> Calling RPC: POST /rest/v1/rpc/create_payment_order (plan: $backendPlan, user_id: $userId)")

        try {
            val request = CreatePaymentOrderRequest(plan = backendPlan)
            val response = SupabaseClient.service.createPaymentOrderRpc(request, authHeader)
            val statusCode = response.code()
            Log.d("MythicPayment", "PAYMENT CREATE HTTP STATUS: $statusCode")

            if (response.isSuccessful) {
                val rawBody = response.body()?.string()?.trim() ?: ""
                Log.d("MythicPayment", "PAYMENT CREATE RESPONSE: $rawBody")

                if (rawBody.isEmpty()) {
                    val err = "Empty response returned from server create_payment_order RPC."
                    Log.e("MythicPayment", "PAYMENT CREATE FAILED: $err")
                    return@withContext PaymentOrderResult.Error(err, statusCode)
                }

                val json = if (rawBody.startsWith("[")) {
                    val arr = org.json.JSONArray(rawBody)
                    if (arr.length() == 0) {
                        val err = "Empty array returned from create_payment_order RPC."
                        Log.e("MythicPayment", "PAYMENT CREATE FAILED: $err")
                        return@withContext PaymentOrderResult.Error(err, statusCode)
                    }
                    arr.getJSONObject(0)
                } else if (rawBody.startsWith("{")) {
                    org.json.JSONObject(rawBody)
                } else {
                    val err = "Unexpected non-JSON response from server: $rawBody"
                    Log.e("MythicPayment", "PAYMENT CREATE FAILED: $err")
                    return@withContext PaymentOrderResult.Error(err, statusCode)
                }

                val realOrderId = json.optString("id", "").trim()
                if (realOrderId.isEmpty() || realOrderId == "null" || realOrderId.startsWith("MYTHIC-")) {
                    val err = "Server returned invalid payment_orders.id UUID ('$realOrderId')."
                    Log.e("MythicPayment", "PAYMENT CREATE FAILED: $err")
                    return@withContext PaymentOrderResult.Error(err, statusCode)
                }

                val returnedPaymentRef = json.optString("payment_reference", "").trim()
                if (returnedPaymentRef.isEmpty() || returnedPaymentRef == "null") {
                    val err = "Server returned missing payment_reference."
                    Log.e("MythicPayment", "PAYMENT CREATE FAILED: $err")
                    return@withContext PaymentOrderResult.Error(err, statusCode)
                }

                val returnedPlan = json.optString("plan", backendPlan)
                val returnedAmount = json.optDouble("amount", if (backendPlan == "max") 5.00 else 1.99)
                val returnedCurrency = json.optString("currency", "USD")
                val returnedStatus = json.optString("status", "pending")
                val returnedExpiresAt = if (json.has("expires_at") && !json.isNull("expires_at")) json.getString("expires_at") else null

                Log.d("MythicPayment", "PAYMENT ORDER UUID: $realOrderId")
                Log.d("MythicPayment", "PAYMENT REFERENCE: $returnedPaymentRef")

                val createdOrder = SupabasePaymentOrder(
                    id = realOrderId,
                    userId = userId,
                    userEmail = userEmail,
                    plan = returnedPlan,
                    amount = returnedAmount,
                    currency = returnedCurrency,
                    paymentReference = returnedPaymentRef,
                    status = returnedStatus,
                    expiresAt = returnedExpiresAt
                )

                synchronized(_cachedPaymentOrders) {
                    _cachedPaymentOrders.removeAll { it.id == realOrderId || it.paymentReference == returnedPaymentRef }
                    _cachedPaymentOrders.add(0, createdOrder)
                }

                return@withContext PaymentOrderResult.Success(createdOrder)
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP error $statusCode"
                Log.e("MythicPayment", "PAYMENT CREATE FAILED - Status: $statusCode, Error: $errorBody")
                return@withContext PaymentOrderResult.Error("Payment order creation failed ($statusCode): $errorBody", statusCode)
            }
        } catch (e: Exception) {
            val err = "Payment order creation exception: ${e.localizedMessage ?: e.message}"
            Log.e("MythicPayment", err, e)
            return@withContext PaymentOrderResult.Error(err)
        }
    }

    suspend fun getPaymentOrders(userId: String): List<SupabasePaymentOrder> = withContext(Dispatchers.IO) {
        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null

        val remoteList = if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.getPaymentOrders(userId = "eq.$userId", authHeader = authHeader)
                if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase getPaymentOrders failed", e)
                emptyList()
            }
        } else emptyList()

        val localList = synchronized(_cachedPaymentOrders) {
            _cachedPaymentOrders.filter { it.userId == userId || userId.isEmpty() }
        }

        (localList + remoteList).distinctBy { it.paymentReference }
    }

    suspend fun getAllPaymentOrdersAdmin(): List<SupabasePaymentOrder> = withContext(Dispatchers.IO) {
        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null

        val remoteList = if (SupabaseClient.isConfigured) {
            try {
                val response = SupabaseClient.service.getPaymentOrders(userId = null, authHeader = authHeader)
                if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                Log.e("MythicRepository", "Supabase getAllPaymentOrdersAdmin failed", e)
                emptyList()
            }
        } else emptyList()

        val localList = synchronized(_cachedPaymentOrders) {
            _cachedPaymentOrders.toList()
        }

        (remoteList + localList).distinctBy { it.id ?: it.paymentReference }
    }

    suspend fun adminApprovePaymentOrder(
        orderId: String,
        userId: String,
        plan: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null
        val adminUser = _currentUser.value
        val adminEmail = adminUser?.email ?: "admin@mythic.app"
        val normalizedPlan = if (plan.lowercase() in listOf("prime", "max")) "max" else "pro"

        if (SupabaseClient.isConfigured) {
            try {
                // 1. Attempt RPC admin_approve_payment_order first
                val rpcReq = mapOf(
                    "p_order_id" to orderId,
                    "p_verified_by" to adminEmail
                )
                val rpcResp = SupabaseClient.service.adminApprovePaymentOrderRpc(rpcReq, authHeader)
                if (rpcResp.isSuccessful) {
                    Log.d("MythicAdmin", "Approved via RPC successfully")
                } else {
                    Log.w("MythicAdmin", "RPC admin_approve failed (${rpcResp.code()}), applying fallback PATCH...")
                    // 2. Direct Fallback: Update payment_orders table
                    val nowIso = java.time.Instant.now().toString()
                    SupabaseClient.service.patchPaymentOrder(
                        id = "eq.$orderId",
                        body = mapOf(
                            "status" to "verified",
                            "verified_at" to nowIso,
                            "verified_by" to adminEmail
                        ),
                        authHeader = authHeader
                    )

                    // 3. Upsert Subscription table
                    val expiresAt = if (normalizedPlan == "max") {
                        java.time.Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS).toString()
                    } else {
                        java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS).toString()
                    }
                    val sub = SupabaseSubscription(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        plan = normalizedPlan,
                        status = "active",
                        paymentOrderId = orderId,
                        startedAt = nowIso,
                        expiresAt = expiresAt
                    )
                    SupabaseClient.service.upsertSubscription(sub, authHeader)

                    // 4. Update Profile
                    if (userId.isNotBlank()) {
                        SupabaseClient.service.patchProfile(
                            id = "eq.$userId",
                            body = mapOf("subscription_tier" to normalizedPlan),
                            authHeader = authHeader
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MythicAdmin", "Admin approve remote call failed", e)
            }
        }

        // Update local cache
        synchronized(_cachedPaymentOrders) {
            val idx = _cachedPaymentOrders.indexOfFirst { it.id == orderId }
            if (idx != -1) {
                val old = _cachedPaymentOrders[idx]
                _cachedPaymentOrders[idx] = old.copy(
                    status = "verified",
                    verifiedAt = java.time.Instant.now().toString(),
                    verifiedBy = adminEmail
                )
            }
        }

        // If approving current user
        if (userId == _currentUser.value?.id) {
            refreshEntitlements()
        }

        Result.success(Unit)
    }

    suspend fun adminRejectPaymentOrder(
        orderId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else null

        if (SupabaseClient.isConfigured) {
            try {
                val rpcReq = mapOf(
                    "p_order_id" to orderId,
                    "p_rejection_reason" to reason
                )
                val rpcResp = SupabaseClient.service.adminRejectPaymentOrderRpc(rpcReq, authHeader)
                if (!rpcResp.isSuccessful) {
                    SupabaseClient.service.patchPaymentOrder(
                        id = "eq.$orderId",
                        body = mapOf(
                            "status" to "rejected",
                            "rejection_reason" to reason
                        ),
                        authHeader = authHeader
                    )
                }
            } catch (e: Exception) {
                Log.e("MythicAdmin", "Admin reject remote call failed", e)
            }
        }

        synchronized(_cachedPaymentOrders) {
            val idx = _cachedPaymentOrders.indexOfFirst { it.id == orderId }
            if (idx != -1) {
                val old = _cachedPaymentOrders[idx]
                _cachedPaymentOrders[idx] = old.copy(
                    status = "rejected",
                    rejectionReason = reason
                )
            }
        }

        Result.success(Unit)
    }

    suspend fun submitPaymentOrderRpc(
        orderId: String,
        paymentReference: String,
        bankTransactionId: String,
        transferAmount: Double,
        transferDatetime: String,
        receiptPath: String
    ): PaymentSubmitResult = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val err = "Supabase is not configured."
            Log.e("MythicPayment", "[submit_payment_order] $err")
            return@withContext PaymentSubmitResult.Error(err)
        }

        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)

        if (token.isNullOrBlank()) {
            val err = "Authentication required. You must be signed in with an active session to submit payment."
            Log.e("MythicPayment", "[submit_payment_order] FAILED: $err")
            return@withContext PaymentSubmitResult.Error(err, 401)
        }
        SupabaseClient.accessToken = token

        val authHeader = "Bearer $token"

        if (orderId.isBlank() || orderId.startsWith("MYTHIC-")) {
            val err = "Invalid order ID '$orderId'. p_order_id must be the real UUID returned by create_payment_order, not payment reference."
            Log.e("MythicPayment", "[submit_payment_order] FAILED: $err")
            return@withContext PaymentSubmitResult.Error(err, 400)
        }

        if (bankTransactionId.isBlank()) {
            val err = "Bank Transaction ID cannot be empty."
            Log.e("MythicPayment", "[submit_payment_order] FAILED: $err")
            return@withContext PaymentSubmitResult.Error(err, 400)
        }

        val isoDateTime = try {
            if (transferDatetime.contains("T") && (transferDatetime.endsWith("Z") || transferDatetime.contains("+"))) {
                transferDatetime
            } else if (transferDatetime.contains("T")) {
                val ldt = java.time.LocalDateTime.parse(transferDatetime)
                ldt.atZone(java.time.ZoneId.systemDefault()).withZoneSameInstant(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } else {
                val ldt = java.time.LocalDateTime.parse(transferDatetime.trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                ldt.atZone(java.time.ZoneId.systemDefault()).withZoneSameInstant(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        } catch (e: Exception) {
            java.time.Instant.now().toString()
        }

        Log.d("MythicPayment", "--> Calling RPC: POST /rest/v1/rpc/submit_payment_order")
        Log.d("MythicPayment", "PAYMENT SUBMIT ORDER UUID: $orderId")
        Log.d("MythicPayment", "PAYMENT SUBMIT REFERENCE: $paymentReference")
        Log.d("MythicPayment", "PAYMENT SUBMIT RECEIPT PATH: $receiptPath")
        Log.d("MythicPayment", "PAYMENT SUBMIT AMOUNT: $transferAmount")
        Log.d("MythicPayment", "PAYMENT SUBMIT DATETIME: $isoDateTime")

        try {
            // Attempt with p_receipt_image_url first as specified in schema
            var requestMap: Map<String, Any> = mapOf(
                "p_order_id" to orderId,
                "p_bank_transaction_id" to bankTransactionId,
                "p_transfer_amount" to transferAmount,
                "p_transfer_datetime" to isoDateTime,
                "p_receipt_image_url" to receiptPath
            )
            var response = SupabaseClient.service.submitPaymentOrderRpcDynamic(requestMap, authHeader)
            var statusCode = response.code()
            Log.d("MythicPayment", "PAYMENT SUBMIT HTTP STATUS (attempt 1): $statusCode")

            // If 404 function not found in schema cache, retry with p_receipt_path parameter name
            if (statusCode == 404) {
                requestMap = mapOf(
                    "p_order_id" to orderId,
                    "p_bank_transaction_id" to bankTransactionId,
                    "p_transfer_amount" to transferAmount,
                    "p_transfer_datetime" to isoDateTime,
                    "p_receipt_path" to receiptPath
                )
                response = SupabaseClient.service.submitPaymentOrderRpcDynamic(requestMap, authHeader)
                statusCode = response.code()
                Log.d("MythicPayment", "PAYMENT SUBMIT HTTP STATUS (attempt 2): $statusCode")
            }

            if (response.isSuccessful) {
                val rawBody = response.body()?.string()?.trim() ?: ""
                Log.d("MythicPayment", "PAYMENT SUBMIT RESPONSE: $rawBody")

                synchronized(_cachedPaymentOrders) {
                    val index = _cachedPaymentOrders.indexOfFirst { it.id == orderId || it.paymentReference == paymentReference }
                    if (index != -1) {
                        val old = _cachedPaymentOrders[index]
                        _cachedPaymentOrders[index] = old.copy(
                            bankTransactionId = bankTransactionId,
                            receiptImageUrl = receiptPath,
                            status = "under_review"
                        )
                    }
                }
                return@withContext PaymentSubmitResult.Success
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP error $statusCode"
                Log.e("MythicPayment", "PAYMENT SUBMIT FAILED - Status: $statusCode, Error: $errorBody")
                return@withContext PaymentSubmitResult.Error("Payment submission failed ($statusCode): $errorBody", statusCode)
            }
        } catch (e: Exception) {
            val err = "Payment submission exception: ${e.localizedMessage ?: e.message}"
            Log.e("MythicPayment", err, e)
            return@withContext PaymentSubmitResult.Error(err)
        }
    }

    suspend fun uploadReceiptImage(userId: String, bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        val cleanFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val storagePath = "$userId/$cleanFileName"
        val token = SupabaseClient.accessToken 
            ?: context.getSharedPreferences("mythic_auth", android.content.Context.MODE_PRIVATE).getString("access_token", null)
        
        if (token.isNullOrBlank()) {
            Log.e("MythicPayment", "RECEIPT UPLOAD FAILED: Missing user access token")
            return@withContext null
        }
        val authHeader = "Bearer $token"

        Log.d("MythicPayment", "Uploading receipt to path: $storagePath (bucket: payment-receipts)")
        val res = uploadFile("payment-receipts", storagePath, bytes, "image/jpeg", authHeader)
            ?: uploadFile("receipts", storagePath, bytes, "image/jpeg", authHeader)
        if (res != null) {
            Log.d("MythicPayment", "RECEIPT UPLOAD SUCCESS: $storagePath")
            return@withContext storagePath
        } else {
            Log.e("MythicPayment", "RECEIPT UPLOAD FAILED for path: $storagePath")
            return@withContext null
        }
    }

    suspend fun updateProfile(userId: String, updates: Map<String, Any>) {
        if (SupabaseClient.isConfigured) {
            SupabaseClient.service.updateProfile("eq.$userId", updates)
        }
    }

    suspend fun checkAndInitializeStreak(profile: ProfileEntity) {
        val today = java.time.LocalDate.now().toString()
        if (profile.lastLoginDate == today) return // Already logged in today
        
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val newStreak = if (profile.lastLoginDate == yesterday) profile.streak + 1 else 1
        
        updateProfile(profile.id, mapOf("streak" to newStreak))
    }
}

sealed class PaymentOrderResult {
    data class Success(val order: SupabasePaymentOrder) : PaymentOrderResult()
    data class Error(val message: String, val statusCode: Int? = null) : PaymentOrderResult()
}

sealed class PaymentSubmitResult {
    data object Success : PaymentSubmitResult()
    data class Error(val message: String, val statusCode: Int? = null) : PaymentSubmitResult()
}
