package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ShellType(val displayName: String, val binary: String) {
    CMD("CMD (Command Prompt)", "cmd.exe"),
    POWERSHELL("PowerShell", "powershell.exe"),
    BASH("Bash / Linux Shell", "bash"),
    WSL("WSL (Ubuntu/Linux)", "wsl.exe")
}

@Entity(tableName = "connection_profiles")
data class ConnectionProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 8080,
    val authPin: String = "",
    val shellType: ShellType = ShellType.CMD,
    val isDefault: Boolean = false,
    val lastConnected: Long = 0
)
