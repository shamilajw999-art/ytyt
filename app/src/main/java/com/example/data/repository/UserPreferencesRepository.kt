package com.example.data.repository

import com.example.data.local.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferencesEntity?>
    suspend fun insertUserPreferences(prefs: UserPreferencesEntity)
}
