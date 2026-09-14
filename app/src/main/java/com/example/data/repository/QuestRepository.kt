package com.example.data.repository

import com.example.data.local.QuestEntity
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    val quests: Flow<List<QuestEntity>>
    suspend fun incrementQuestProgress(type: String)
    suspend fun clearQuests()
}
