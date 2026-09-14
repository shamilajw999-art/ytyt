package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        BadgeEntity::class,
        QuestEntity::class,
        ScanHistoryEntity::class,
        SavedSiteEntity::class,
        MythicConversationEntity::class,
        UserPreferencesEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MythicDatabase : RoomDatabase() {
    abstract fun mythicDao(): MythicDao

    companion object {
        @Volatile
        private var INSTANCE: MythicDatabase? = null

        fun getDatabase(context: Context): MythicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MythicDatabase::class.java,
                    "mythic_cache_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
