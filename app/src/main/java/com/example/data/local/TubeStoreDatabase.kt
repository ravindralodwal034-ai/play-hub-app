package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalChannelEntity::class,
        RoadmapMilestoneEntity::class,
        LocalAppEntity::class,
        LocalVideoEntity::class,
        LocalBookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TubeStoreDatabase : RoomDatabase() {
    abstract fun tubeStoreDao(): TubeStoreDao

    companion object {
        @Volatile
        private var INSTANCE: TubeStoreDatabase? = null

        fun getInstance(context: Context): TubeStoreDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TubeStoreDatabase::class.java,
                    "tube_store_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
