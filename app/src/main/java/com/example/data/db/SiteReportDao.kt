package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SiteReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteReportDao {
    @Query("SELECT * FROM site_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<SiteReportEntity>>

    @Query("SELECT * FROM site_reports WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
    fun getPendingQueue(): Flow<List<SiteReportEntity>>

    @Query("SELECT COUNT(*) FROM site_reports WHERE syncStatus != 'SYNCED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM site_reports WHERE syncStatus != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingReportsList(): List<SiteReportEntity>

    @Query("SELECT COUNT(*) FROM site_reports")
    suspend fun getTotalReportCount(): Int

    @Query("SELECT * FROM site_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Long): SiteReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SiteReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<SiteReportEntity>)

    @Query("UPDATE site_reports SET syncStatus = :status, syncErrorMessage = :errorMessage, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, errorMessage: String?, syncedAt: Long?)

    @Delete
    suspend fun deleteReport(report: SiteReportEntity)

    @Query("DELETE FROM site_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)
}
