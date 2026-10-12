package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionProfileDao {
    @Query("SELECT * FROM connection_profiles ORDER BY isDefault DESC, lastConnected DESC, id ASC")
    fun getAllProfiles(): Flow<List<ConnectionProfile>>

    @Query("SELECT * FROM connection_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ConnectionProfile?

    @Query("SELECT * FROM connection_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ConnectionProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ConnectionProfile): Long

    @Update
    suspend fun updateProfile(profile: ConnectionProfile)

    @Delete
    suspend fun deleteProfile(profile: ConnectionProfile)

    @Query("UPDATE connection_profiles SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE connection_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: Long)

    @Query("UPDATE connection_profiles SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
