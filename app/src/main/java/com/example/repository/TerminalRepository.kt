package com.example.repository

import com.example.data.local.AppDatabase
import com.example.data.remote.CmdClient
import com.example.model.CommandHistory
import com.example.model.CommandSnippet
import com.example.model.ConnectionProfile
import com.example.model.ConnectionStatus
import com.example.model.MessageType
import com.example.model.ServerInfo
import com.example.model.ShellType
import com.example.model.TerminalMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalRepository(
    private val database: AppDatabase,
    private val client: CmdClient = CmdClient()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val allProfiles: Flow<List<ConnectionProfile>> = database.connectionProfileDao().getAllProfiles()
    val allHistory: Flow<List<CommandHistory>> = database.commandHistoryDao().getAllHistory()
    val allSnippets: Flow<List<CommandSnippet>> = database.commandSnippetDao().getAllSnippets()

    private val _currentProfile = MutableStateFlow<ConnectionProfile?>(null)
    val currentProfile: StateFlow<ConnectionProfile?> = _currentProfile.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _terminalMessages = MutableStateFlow<List<TerminalMessage>>(emptyList())
    val terminalMessages: StateFlow<List<TerminalMessage>> = _terminalMessages.asStateFlow()

    private val _currentCwd = MutableStateFlow("C:\\")
    val currentCwd: StateFlow<String> = _currentCwd.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    init {
        // Initial welcome message
        addMessage(
            TerminalMessage(
                type = MessageType.SYSTEM,
                content = "=== REMOTE CMD ANDROID ===\nSẵn sàng kết nối tới máy tính qua Wi-Fi / LAN.\nXem tab 'Cài đặt PC' để khởi động server trên máy tính."
            )
        )
    }

    fun selectProfile(profile: ConnectionProfile) {
        _currentProfile.value = profile
        connectToProfile(profile)
    }

    fun connectToProfile(profile: ConnectionProfile? = _currentProfile.value) {
        if (profile == null) return
        _currentProfile.value = profile
        _connectionStatus.value = ConnectionStatus.Connecting

        addMessage(
            TerminalMessage(
                type = MessageType.SYSTEM,
                content = "Đang kiểm tra kết nối tới ${profile.host}:${profile.port}..."
            )
        )

        scope.launch {
            val pingResult = client.ping(profile.host, profile.port)
            pingResult.onSuccess { info ->
                _connectionStatus.value = ConnectionStatus.Connected(info)
                if (info.currentDir.isNotBlank()) {
                    _currentCwd.value = info.currentDir
                }
                database.connectionProfileDao().updateLastConnected(profile.id, System.currentTimeMillis())

                addMessage(
                    TerminalMessage(
                        type = MessageType.SYSTEM,
                        content = "🟢 Đã kết nối thành công tới ${info.hostname} (${info.osName})!\nNgười dùng: ${info.username} | Độ trễ: ${info.latencyMs}ms\nThư mục làm việc: ${_currentCwd.value}"
                    )
                )
            }.onFailure { error ->
                _connectionStatus.value = ConnectionStatus.Failed(error.message ?: "Không thể kết nối")
                addMessage(
                    TerminalMessage(
                        type = MessageType.ERROR,
                        content = "🔴 Không thể kết nối tới ${profile.host}:${profile.port}\nLỗi: ${error.localizedMessage}\nKiểm tra lại xem server đã chạy trên PC chưa và firewall đã mở chưa."
                    )
                )
            }
        }
    }

    fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
        addMessage(
            TerminalMessage(
                type = MessageType.SYSTEM,
                content = "Đã ngắt kết nối với máy tính."
            )
        )
    }

    fun executeCommand(commandStr: String, shellOverride: ShellType? = null) {
        val cmd = commandStr.trim()
        if (cmd.isEmpty()) return

        val profile = _currentProfile.value
        if (profile == null) {
            addMessage(
                TerminalMessage(
                    type = MessageType.ERROR,
                    content = "Chưa chọn máy tính nào! Vui lòng cấu hình IP máy tính trước."
                )
            )
            return
        }

        // Support local terminal helper commands
        if (cmd.equals("cls", ignoreCase = true) || cmd.equals("clear", ignoreCase = true)) {
            clearTerminal()
            return
        }

        val activeShell = shellOverride ?: profile.shellType
        val promptPrefix = "${_currentCwd.value}> $cmd"

        addMessage(
            TerminalMessage(
                type = MessageType.INPUT,
                content = promptPrefix,
                command = cmd
            )
        )

        _isExecuting.value = true

        scope.launch {
            val result = client.executeCommand(
                host = profile.host,
                port = profile.port,
                cmd = cmd,
                shell = activeShell,
                pin = profile.authPin,
                cwd = _currentCwd.value
            )

            _isExecuting.value = false

            result.onSuccess { execRes ->
                if (execRes.cwd.isNotBlank()) {
                    _currentCwd.value = execRes.cwd
                }

                if (execRes.stdout.isNotBlank()) {
                    addMessage(
                        TerminalMessage(
                            type = MessageType.OUTPUT,
                            content = execRes.stdout.trimEnd(),
                            exitCode = execRes.exitCode,
                            durationMs = execRes.durationMs
                        )
                    )
                }

                if (execRes.stderr.isNotBlank()) {
                    addMessage(
                        TerminalMessage(
                            type = MessageType.STDERR,
                            content = execRes.stderr.trimEnd(),
                            exitCode = execRes.exitCode
                        )
                    )
                }

                if (execRes.stdout.isBlank() && execRes.stderr.isBlank()) {
                    addMessage(
                        TerminalMessage(
                            type = MessageType.SYSTEM,
                            content = "[Lệnh thực thi xong trong ${execRes.durationMs}ms - Exit code: ${execRes.exitCode}]"
                        )
                    )
                }

                // Save to Room history
                database.commandHistoryDao().insertHistory(
                    CommandHistory(
                        command = cmd,
                        output = (execRes.stdout + "\n" + execRes.stderr).trim(),
                        exitCode = execRes.exitCode,
                        durationMs = execRes.durationMs,
                        profileHost = "${profile.host}:${profile.port}"
                    )
                )
            }.onFailure { error ->
                addMessage(
                    TerminalMessage(
                        type = MessageType.ERROR,
                        content = "Lỗi khi gửi lệnh: ${error.localizedMessage}"
                    )
                )
            }
        }
    }

    fun clearTerminal() {
        _terminalMessages.value = listOf(
            TerminalMessage(
                type = MessageType.SYSTEM,
                content = "Đã xóa màn hình console. Thư mục hiện tại: ${_currentCwd.value}"
            )
        )
    }

    fun addMessage(msg: TerminalMessage) {
        val current = _terminalMessages.value
        // Keep last 400 messages to prevent memory explosion
        val updated = if (current.size > 400) current.drop(current.size - 400) + msg else current + msg
        _terminalMessages.value = updated
    }

    suspend fun saveProfile(profile: ConnectionProfile): Long {
        return if (profile.id == 0L) {
            database.connectionProfileDao().insertProfile(profile)
        } else {
            database.connectionProfileDao().updateProfile(profile)
            profile.id
        }
    }

    suspend fun deleteProfile(profile: ConnectionProfile) {
        database.connectionProfileDao().deleteProfile(profile)
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = null
            _connectionStatus.value = ConnectionStatus.Disconnected
        }
    }

    suspend fun setDefaultProfile(id: Long) {
        database.connectionProfileDao().clearDefaultFlags()
        database.connectionProfileDao().setDefaultProfile(id)
    }

    suspend fun saveSnippet(snippet: CommandSnippet) {
        if (snippet.id == 0L) {
            database.commandSnippetDao().insertSnippet(snippet)
        } else {
            database.commandSnippetDao().updateSnippet(snippet)
        }
    }

    suspend fun deleteSnippet(snippet: CommandSnippet) {
        database.commandSnippetDao().deleteSnippet(snippet)
    }

    suspend fun clearHistory() {
        database.commandHistoryDao().clearAllHistory()
    }

    suspend fun deleteHistory(item: CommandHistory) {
        database.commandHistoryDao().deleteHistory(item)
    }

    suspend fun toggleFavoriteHistory(item: CommandHistory) {
        database.commandHistoryDao().updateHistory(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun testConnection(host: String, port: Int): Pair<Boolean, String> {
        return client.testSocketConnection(host, port)
    }
}
