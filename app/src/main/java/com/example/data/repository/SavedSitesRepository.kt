package com.example.data.repository

import com.example.data.local.SavedSiteEntity
import kotlinx.coroutines.flow.Flow

interface SavedSitesRepository {
    val savedSites: Flow<List<SavedSiteEntity>>
    suspend fun toggleSaveSite(siteName: String, province: String, imageUrl: String?)
    suspend fun isSiteSaved(siteName: String): Boolean
    suspend fun clearSavedSites()
}
