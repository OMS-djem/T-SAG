package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.SiteReportEntity
import com.example.data.model.TelecomSiteEntity
import com.example.data.model.UserEntity

@Database(entities = [SiteReportEntity::class, UserEntity::class, TelecomSiteEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteReportDao(): SiteReportDao
    abstract fun userDao(): UserDao
    abstract fun telecomSiteDao(): TelecomSiteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "site_access_reports.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
