package com.example.data.repository

import com.example.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profile: Flow<ProfileEntity?>
    val allProfiles: Flow<List<ProfileEntity>>
    suspend fun getProfile(): ProfileEntity?
    suspend fun getProfileById(userId: String): ProfileEntity?
    suspend fun insertProfile(profile: ProfileEntity)
    suspend fun updateStreak(userId: String)
    suspend fun updateAvatar(userId: String, newAvatarUrl: String)
    suspend fun clearProfile()
}

