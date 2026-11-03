package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TelecomSiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelecomSiteDao {

    @Query("SELECT * FROM telecom_sites ORDER BY siteId ASC")
    fun getAllSitesFlow(): Flow<List<TelecomSiteEntity>>

    @Query("SELECT * FROM telecom_sites ORDER BY siteId ASC")
    suspend fun getAllSitesList(): List<TelecomSiteEntity>

    @Query("SELECT * FROM telecom_sites WHERE siteId = :siteId LIMIT 1")
    suspend fun getSiteById(siteId: String): TelecomSiteEntity?

    @Query("SELECT COUNT(*) FROM telecom_sites")
    suspend fun getSiteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<TelecomSiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: TelecomSiteEntity): Long

    @Update
    suspend fun updateSite(site: TelecomSiteEntity)

    @Query("UPDATE telecom_sites SET accessStatus = :status, refusalReason = :refusalReason, lastAuditTimestamp = :timestamp WHERE siteId = :siteId")
    suspend fun updateSiteAuditStatus(siteId: String, status: String, refusalReason: String?, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM telecom_sites WHERE id = :id")
    suspend fun deleteSite(id: Long)
}
