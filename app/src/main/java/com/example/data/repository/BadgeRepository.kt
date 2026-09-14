package com.example.data.repository

import com.example.data.local.BadgeEntity
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {
    val badges: Flow<List<BadgeEntity>>
    suspend fun unlockBadge(code: String, name: String, description: String, tier: String, icon: String?)
    suspend fun clearBadges()
}
