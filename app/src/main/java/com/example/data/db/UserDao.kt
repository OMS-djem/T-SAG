package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM app_users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM app_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM app_users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM app_users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT * FROM app_users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM app_users WHERE isActive = 0 ORDER BY createdAt DESC")
    fun getPendingUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM app_users WHERE isActive = 0")
    fun getPendingUserCount(): Flow<Int>

    @Query("UPDATE app_users SET isActive = :isActive WHERE id = :id")
    suspend fun setUserActive(id: Long, isActive: Boolean)
}
