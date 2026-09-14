package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String,
    val level: Int,
    val xp: Int,
    val streak: Int,
    val scansCount: Int,
    val badgesCount: Int,
    val subscriptionTier: String = "free",
    val lastLoginDate: String? = null,
    val profileBackgroundUrl: String? = null,
    val isAnimatedBackground: Boolean = false,
    val bio: String? = null,
    val interests: String? = null, // Stored as comma-separated or JSON
    val featuredBadgeId: String? = null
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val name: String,
    val description: String,
    val tier: String, // bronze, silver, gold, platinum
    val unlockedAt: Long,
    val icon: String?
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val rewardXp: Int,
    val progress: Int,
    val target: Int,
    val type: String, // daily, weekly, achievement
    val completed: Boolean
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val siteName: String,
    val province: String,
    val description: String,
    val unescoStatus: String,
    val era: String,
    val facts: String, // Comma-separated or JSON string
    val xpEarned: Int,
    val imageUrl: String?,
    val timestamp: Long
)

@Entity(tableName = "saved_sites")
data class SavedSiteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val siteName: String,
    val province: String,
    val imageUrl: String?,
    val timestamp: Long
)

@Entity(tableName = "mythic_conversations")
data class MythicConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val messageId: String,
    val role: String, // user, assistant
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val userId: String,
    val darkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val language: String = "English",
    val mythicColorHex: String = "#86FC5C",
    val mythicMood: String = "Friendly Guide",
    val hasCompletedOnboarding: Boolean = false
)

data class BadgeDefinition(
    val code: String,
    val name: String,
    val description: String,
    val tier: String, // bronze, silver, gold, platinum
    val icon: String
)

val ALL_BADGES = listOf(
    BadgeDefinition("pioneer", "Pioneer First 100", "Awarded to the first 100 users who joined the platform.", "ultra rare", "💎"),
    BadgeDefinition("legendary_historian", "Legendary Historian", "Reached the absolute top tier of knowledge.", "legendary", "🌟"),
    BadgeDefinition("explorer", "Explorer Badge", "Unlock by exploring your first heritage site", "bronze", "🏆"),
    BadgeDefinition("archaeologist", "Heritage Guardian", "Successfully scan 5 historical sites", "silver", "🏛️"),
    BadgeDefinition("photo_expert", "Expert Photographer", "Scan multiple heritage views with your camera", "gold", "📸"),
    BadgeDefinition("historian", "Knowledge Seeker", "Read all suggestions in your personalized feed", "platinum", "📜"),

    // Site-specific badges
    BadgeDefinition("sigiriya_explorer", "Sigiriya Ascender", "Unlock by scanning or visiting Sigiriya Rock Fortress", "silver", "🦁"),
    BadgeDefinition("kandy_explorer", "Sacred Tooth Guardian", "Unlock by scanning or visiting the Temple of the Tooth", "gold", "🛕"),
    BadgeDefinition("galle_explorer", "Galle Rampart Walker", "Unlock by scanning or visiting Galle Fort", "bronze", "🏰"),
    BadgeDefinition("dambulla_explorer", "Dambulla Cave Seeker", "Unlock by scanning or visiting Dambulla Cave Temple", "silver", "🛖"),
    BadgeDefinition("anuradhapura_explorer", "Anuradhapura Pilgrim", "Unlock by scanning or visiting Anuradhapura Sacred City", "gold", "🌳"),
    BadgeDefinition("polonnaruwa_explorer", "Polonnaruwa Chronicler", "Unlock by scanning or visiting Polonnaruwa Ancient City", "silver", "🗿"),
    BadgeDefinition("pidurangala_explorer", "Pidurangala Sunrise Watcher", "Unlock by scanning or visiting Pidurangala Rock", "bronze", "🌅"),
    BadgeDefinition("ella_explorer", "Ella Arch Crosser", "Unlock by scanning or visiting Nine Arch Bridge in Ella", "bronze", "🚂"),

    // Creative/Interesting badges
    BadgeDefinition("heritage_conqueror", "Heritage Conqueror", "Unlock by visiting/scanning all 8 major historical sites", "platinum", "👑"),
    BadgeDefinition("quiz_master", "Heritage Quiz Master", "Score 100% (perfect score) on any heritage quiz", "gold", "🎓"),
    BadgeDefinition("curious_mind", "Curious Historian", "Ask Mythic AI 10 questions about Sri Lankan history", "bronze", "🧠"),
    BadgeDefinition("night_owl", "Midnight Explorer", "Use the app past midnight to study history", "bronze", "🦉"),
    BadgeDefinition("gemini_vision", "AI Visionary", "Successfully scan a heritage site using Gemini Vision API", "platinum", "👁️"),
    BadgeDefinition("feed_curator", "Social Culturist", "Like or share articles on the feed to spread culture", "bronze", "💖"),
    BadgeDefinition("first_step", "Aayubowan!", "Welcome to the Sri Lanka Heritage explorer community!", "bronze", "🇱🇰"),
    BadgeDefinition("site_reporter", "Heritage Guardian", "Report a broken or endangered heritage site", "silver", "🛡️"),
    BadgeDefinition("pro_member", "Mythic Pro", "Unlock the premium Mythic Pro membership", "gold", "👑"),
    BadgeDefinition("max_member", "Mythic Max", "Unlock the ultimate Mythic Max membership", "platinum", "💎"),
    BadgeDefinition("early_adopter", "Early Adopter", "Joined MYTHIC during its first month of release", "silver", "👶"),
    BadgeDefinition("world_traveler", "World Traveler", "Explore heritage sites from at least 3 different provinces", "gold", "✈️"),
    BadgeDefinition("diligent_student", "Diligent Student", "Read over 50 articles in the heritage feed", "platinum", "📚"),
    BadgeDefinition("loyal_supporter", "Loyal Supporter", "Use the app for 7 consecutive days", "gold", "🔥"),
    BadgeDefinition("master_sculptor", "Master Sculptor", "Scan 10 different stone sculptures or statues", "silver", "🔨"),
    BadgeDefinition("royal_guest", "Royal Guest", "Visit or scan 5 ancient royal palace ruins", "gold", "👑"),
    BadgeDefinition("nature_lover", "Nature Lover", "Scan a heritage site located in a forest or park", "bronze", "🍃"),
    BadgeDefinition("monk_path", "The Monk's Path", "Visit or scan 10 different monasteries or temples", "platinum", "📿")
)
