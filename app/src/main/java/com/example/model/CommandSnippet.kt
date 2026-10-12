package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_snippets")
data class CommandSnippet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val command: String,
    val category: String, // "Mạng (Network)", "Hệ thống (System)", "Nguồn (Power)", "Tệp tin (Files)", "Tùy chọn (Custom)"
    val shellType: ShellType = ShellType.CMD,
    val description: String = "",
    val isSystemPreset: Boolean = false
)
