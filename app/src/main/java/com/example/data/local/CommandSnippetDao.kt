package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CommandSnippet
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandSnippetDao {
    @Query("SELECT * FROM command_snippets ORDER BY category ASC, id ASC")
    fun getAllSnippets(): Flow<List<CommandSnippet>>

    @Query("SELECT * FROM command_snippets WHERE category = :category ORDER BY id ASC")
    fun getSnippetsByCategory(category: String): Flow<List<CommandSnippet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CommandSnippet): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnippets(snippets: List<CommandSnippet>)

    @Update
    suspend fun updateSnippet(snippet: CommandSnippet)

    @Delete
    suspend fun deleteSnippet(snippet: CommandSnippet)

    @Query("SELECT COUNT(*) FROM command_snippets")
    suspend fun getCount(): Int
}
