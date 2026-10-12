package com.example.model

enum class MessageType {
    INPUT,       // User command executed
    OUTPUT,      // Standard output from shell
    STDERR,      // Error output
    SYSTEM,      // Connection / internal app info
    ERROR        // Connection error or timeout
}

data class TerminalMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: MessageType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String? = null,
    val exitCode: Int? = null,
    val durationMs: Long? = null
)

data class ServerInfo(
    val hostname: String = "",
    val osName: String = "",
    val username: String = "",
    val currentDir: String = "",
    val pythonVersion: String = "",
    val activeShell: String = "cmd.exe",
    val latencyMs: Long = 0
)

sealed interface ConnectionStatus {
    object Disconnected : ConnectionStatus
    object Connecting : ConnectionStatus
    data class Connected(val info: ServerInfo) : ConnectionStatus
    data class Failed(val error: String) : ConnectionStatus
}
