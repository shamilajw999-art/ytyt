package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MythicDao {
    
    // --- Profile Queries ---
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :userId LIMIT 1")
    fun getProfileFlowById(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :userId LIMIT 1")
    suspend fun getProfileById(userId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY level DESC, xp DESC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY level DESC, xp DESC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :userId")
    suspend fun deleteProfileById(userId: String)

    @Query("DELETE FROM profiles")
    suspend fun clearProfile()

    // --- Badges Queries ---
    @Query("SELECT * FROM badges ORDER BY unlockedAt DESC")
    fun getBadgesFlow(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun getBadgesFlowByUserId(userId: String): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE code = :code LIMIT 1")
    suspend fun getBadgeByCode(code: String): BadgeEntity?

    @Query("SELECT * FROM badges WHERE userId = :userId AND code = :code LIMIT 1")
    suspend fun getBadgeByCodeAndUser(userId: String, code: String): BadgeEntity?

    @Query("SELECT * FROM badges")
    suspend fun getAllBadges(): List<BadgeEntity>

    @Query("SELECT * FROM badges WHERE userId = :userId")
    suspend fun getAllBadgesByUserId(userId: String): List<BadgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeEntity)

    @Query("DELETE FROM badges WHERE userId = :userId")
    suspend fun clearBadgesByUserId(userId: String)

    @Query("DELETE FROM badges")
    suspend fun clearBadges()

    // --- Quests Queries ---
    @Query("SELECT * FROM quests ORDER BY id ASC")
    fun getQuestsFlow(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE userId = :userId ORDER BY id ASC")
    fun getQuestsFlowByUserId(userId: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests ORDER BY id ASC")
    suspend fun getQuests(): List<QuestEntity>

    @Query("SELECT * FROM quests WHERE userId = :userId ORDER BY id ASC")
    suspend fun getQuestsByUserId(userId: String): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>)

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE userId = :userId")
    suspend fun clearQuestsByUserId(userId: String)

    @Query("DELETE FROM quests")
    suspend fun clearQuests()

    // --- Scan History Queries ---
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getScanHistoryFlow(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScanHistoryFlowByUserId(userId: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScans(scans: List<ScanHistoryEntity>)

    @Query("DELETE FROM scan_history WHERE userId = :userId")
    suspend fun clearScanHistoryByUserId(userId: String)

    @Query("DELETE FROM scan_history")
    suspend fun clearScanHistory()

    // --- Saved Sites Queries ---
    @Query("SELECT * FROM saved_sites ORDER BY timestamp DESC")
    fun getSavedSitesFlow(): Flow<List<SavedSiteEntity>>

    @Query("SELECT * FROM saved_sites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSavedSitesFlowByUserId(userId: String): Flow<List<SavedSiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedSite(site: SavedSiteEntity)

    @Query("DELETE FROM saved_sites WHERE siteName = :siteName")
    suspend fun deleteSavedSite(siteName: String)

    @Query("DELETE FROM saved_sites WHERE userId = :userId AND siteName = :siteName")
    suspend fun deleteSavedSiteByUser(userId: String, siteName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_sites WHERE siteName = :siteName)")
    suspend fun isSiteSaved(siteName: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM saved_sites WHERE userId = :userId AND siteName = :siteName)")
    suspend fun isSiteSavedByUser(userId: String, siteName: String): Boolean

    @Query("DELETE FROM saved_sites WHERE userId = :userId")
    suspend fun clearSavedSitesByUserId(userId: String)

    @Query("DELETE FROM saved_sites")
    suspend fun clearSavedSites()

    // --- Mythic Conversations Queries ---
    @Query("SELECT * FROM mythic_conversations ORDER BY timestamp ASC")
    fun getMythicConversationsFlow(): Flow<List<MythicConversationEntity>>

    @Query("SELECT * FROM mythic_conversations WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMythicConversationsFlowByUserId(userId: String): Flow<List<MythicConversationEntity>>

    @Query("SELECT * FROM mythic_conversations ORDER BY timestamp ASC")
    suspend fun getMythicConversations(): List<MythicConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMythicMessage(message: MythicConversationEntity)

    @Query("DELETE FROM mythic_conversations WHERE userId = :userId")
    suspend fun clearMythicConversationsByUserId(userId: String)

    @Query("DELETE FROM mythic_conversations")
    suspend fun clearMythicConversations()

    // --- User Preferences Queries ---
    @Query("SELECT * FROM user_preferences LIMIT 1")
    fun getUserPreferencesFlow(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun getUserPreferencesFlowByUserId(userId: String): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(prefs: UserPreferencesEntity)
}
