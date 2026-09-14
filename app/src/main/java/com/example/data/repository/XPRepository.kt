package com.example.data.repository

interface XPRepository {
    suspend fun addXp(amount: Int)
}
