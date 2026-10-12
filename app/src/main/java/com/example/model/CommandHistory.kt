package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val output: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val profileHost: String = "",
    val isFavorite: Boolean = false
)
