package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CommandHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CommandHistory>>

    @Query("SELECT * FROM command_history WHERE command LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<CommandHistory>>

    @Query("SELECT * FROM command_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<CommandHistory>>

    @Query("SELECT DISTINCT command FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCommands(limit: Int = 30): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CommandHistory): Long

    @Update
    suspend fun updateHistory(item: CommandHistory)

    @Delete
    suspend fun deleteHistory(item: CommandHistory)

    @Query("DELETE FROM command_history")
    suspend fun clearAllHistory()
}
