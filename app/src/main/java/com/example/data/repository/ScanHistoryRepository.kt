package com.example.data.repository

import com.example.data.local.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    val scanHistory: Flow<List<ScanHistoryEntity>>
    suspend fun scanHeritage(siteName: String, imageUrl: String?): ScanHistoryEntity
    suspend fun saveCustomScan(
        siteName: String,
        province: String,
        description: String,
        unescoStatus: String,
        era: String,
        facts: String,
        imageUrl: String?
    ): ScanHistoryEntity
    suspend fun clearScanHistory()
}
