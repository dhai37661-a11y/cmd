package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ConnectionStatus
import com.example.ui.TerminalTab
import com.example.ui.TerminalViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.pcsetup.PcSetupScreen
import com.example.ui.profiles.ProfilesScreen
import com.example.ui.snippets.SnippetsScreen
import com.example.ui.terminal.TerminalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppRoot(viewModel: TerminalViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()

    // Handle Android system back button: return to Terminal if on another tab
    BackHandler(enabled = currentTab != TerminalTab.TERMINAL) {
        viewModel.setTab(TerminalTab.TERMINAL)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = TerminalCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Remote CMD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Connection status pill
                        val (statusText, dotColor) = when (val s = connectionStatus) {
                            is ConnectionStatus.Connected -> Pair(s.info.hostname.ifEmpty { "Đã kết nối" }, Color(0xFF22C55E))
                            is ConnectionStatus.Connecting -> Pair("Đang kết nối...", Color(0xFFFBBF24))
                            is ConnectionStatus.Failed -> Pair("Mất kết nối", Color(0xFFEF4444))
                            ConnectionStatus.Disconnected -> Pair("Chưa kết nối", Color(0xFF94A3B8))
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == TerminalTab.TERMINAL,
                    onClick = { viewModel.setTab(TerminalTab.TERMINAL) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                    label = { Text("Terminal", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_terminal"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == TerminalTab.SNIPPETS,
                    onClick = { viewModel.setTab(TerminalTab.SNIPPETS) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Lệnh mẫu") },
                    label = { Text("Lệnh mẫu", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_snippets"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == TerminalTab.HISTORY,
                    onClick = { viewModel.setTab(TerminalTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Lịch sử") },
                    label = { Text("Lịch sử", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_history"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == TerminalTab.PC_SETUP,
                    onClick = { viewModel.setTab(TerminalTab.PC_SETUP) },
                    icon = { Icon(Icons.Default.SettingsEthernet, contentDescription = "Cài đặt PC") },
                    label = { Text("Cài đặt PC", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_pc_setup"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == TerminalTab.PROFILES,
                    onClick = { viewModel.setTab(TerminalTab.PROFILES) },
                    icon = { Icon(Icons.Default.Devices, contentDescription = "Máy tính") },
                    label = { Text("Máy tính", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_profiles"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                TerminalTab.TERMINAL -> {
                    TerminalScreen(
                        viewModel = viewModel,
                        onNavigateToSetup = { viewModel.setTab(TerminalTab.PC_SETUP) }
                    )
                }
                TerminalTab.SNIPPETS -> {
                    SnippetsScreen(viewModel = viewModel)
                }
                TerminalTab.HISTORY -> {
                    HistoryScreen(viewModel = viewModel)
                }
                TerminalTab.PC_SETUP -> {
                    PcSetupScreen(viewModel = viewModel)
                }
                TerminalTab.PROFILES -> {
                    ProfilesScreen(viewModel = viewModel)
                }
            }
        }
    }
}
