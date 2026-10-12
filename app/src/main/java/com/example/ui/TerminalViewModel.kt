package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.model.CommandHistory
import com.example.model.CommandSnippet
import com.example.model.ConnectionProfile
import com.example.model.ConnectionStatus
import com.example.model.ShellType
import com.example.model.TerminalMessage
import com.example.repository.TerminalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TerminalTab(val title: String) {
    TERMINAL("Terminal"),
    SNIPPETS("Lệnh mẫu"),
    HISTORY("Lịch sử"),
    PC_SETUP("Cài đặt PC"),
    PROFILES("Máy tính")
}

enum class TerminalColorScheme(val displayName: String) {
    MATRIX_GREEN("Matrix Xanh lá"),
    CYBER_CYAN("Cyber Cyan"),
    AMBER_GOLD("Amber Cổ điển"),
    DRACULA_PURPLE("Dracula Tím")
}

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = TerminalRepository(database)

    val currentTab = MutableStateFlow(TerminalTab.TERMINAL)
    val terminalMessages: StateFlow<List<TerminalMessage>> = repository.terminalMessages
    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val currentProfile: StateFlow<ConnectionProfile?> = repository.currentProfile
    val currentCwd: StateFlow<String> = repository.currentCwd
    val isExecuting: StateFlow<Boolean> = repository.isExecuting

    val profiles: StateFlow<List<ConnectionProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippets: StateFlow<List<CommandSnippet>> = repository.allSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<CommandHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inputCommand = MutableStateFlow("")
    val terminalFontSize = MutableStateFlow(13)
    val colorScheme = MutableStateFlow(TerminalColorScheme.CYBER_CYAN)

    // History browsing with Up/Down buttons
    private var historyIndex = -1
    private var historyCache = listOf<String>()

    init {
        // Auto-select default profile when available
        viewModelScope.launch {
            profiles.collect { list ->
                if (currentProfile.value == null && list.isNotEmpty()) {
                    val defaultP = list.firstOrNull { it.isDefault } ?: list.first()
                    repository.selectProfile(defaultP)
                }
            }
        }
    }

    fun setTab(tab: TerminalTab) {
        currentTab.value = tab
    }

    fun onCommandChange(newCmd: String) {
        inputCommand.value = newCmd
    }

    fun sendCurrentCommand(shellOverride: ShellType? = null) {
        val cmd = inputCommand.value.trim()
        if (cmd.isNotEmpty()) {
            repository.executeCommand(cmd, shellOverride)
            inputCommand.value = ""
            historyIndex = -1
        }
    }

    fun runSnippet(snippet: CommandSnippet) {
        inputCommand.value = snippet.command
        sendCurrentCommand(snippet.shellType)
        currentTab.value = TerminalTab.TERMINAL
    }

    fun insertIntoInput(text: String) {
        inputCommand.value = if (inputCommand.value.isEmpty()) text else "${inputCommand.value} $text"
        currentTab.value = TerminalTab.TERMINAL
    }

    fun historyUp() {
        val hist = history.value.map { it.command }.distinct()
        if (hist.isEmpty()) return
        historyCache = hist
        if (historyIndex + 1 < historyCache.size) {
            historyIndex++
            inputCommand.value = historyCache[historyIndex]
        }
    }

    fun historyDown() {
        if (historyCache.isEmpty() || historyIndex <= 0) {
            historyIndex = -1
            inputCommand.value = ""
            return
        }
        historyIndex--
        inputCommand.value = historyCache[historyIndex]
    }

    fun appendModifier(key: String) {
        when (key) {
            "Ctrl+C" -> {
                repository.executeCommand("^C")
            }
            "Tab" -> {
                inputCommand.value = inputCommand.value + "    "
            }
            "Clear" -> {
                repository.clearTerminal()
            }
            else -> {
                inputCommand.value = inputCommand.value + key
            }
        }
    }

    fun selectProfile(profile: ConnectionProfile) {
        repository.selectProfile(profile)
    }

    fun reconnect() {
        repository.connectToProfile()
    }

    fun saveProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            val id = repository.saveProfile(profile)
            if (profile.isDefault) {
                repository.setDefaultProfile(id)
            }
        }
    }

    fun deleteProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun setDefaultProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            repository.setDefaultProfile(profile.id)
        }
    }

    fun saveSnippet(snippet: CommandSnippet) {
        viewModelScope.launch {
            repository.saveSnippet(snippet)
        }
    }

    fun deleteSnippet(snippet: CommandSnippet) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistory(item: CommandHistory) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun toggleFavorite(item: CommandHistory) {
        viewModelScope.launch {
            repository.toggleFavoriteHistory(item)
        }
    }
}
