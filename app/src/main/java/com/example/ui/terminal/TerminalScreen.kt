package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionProfile
import com.example.model.ConnectionStatus
import com.example.model.MessageType
import com.example.model.ShellType
import com.example.model.TerminalMessage
import com.example.ui.TerminalColorScheme
import com.example.ui.TerminalViewModel
import com.example.ui.theme.DarkTerminalScreen
import com.example.ui.theme.PromptColor
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalError
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalPurple
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.terminalMessages.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val currentCwd by viewModel.currentCwd.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val inputCmd by viewModel.inputCommand.collectAsState()
    val fontSizeSp by viewModel.terminalFontSize.collectAsState()
    val activeColorScheme by viewModel.colorScheme.collectAsState()

    var showProfileDropdown by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showQuickBrowserDialog by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isScrolledUp by remember {
        derivedStateOf {
            listState.canScrollForward
        }
    }

    val primaryTextColor = when (activeColorScheme) {
        TerminalColorScheme.MATRIX_GREEN -> TerminalGreen
        TerminalColorScheme.CYBER_CYAN -> TerminalCyan
        TerminalColorScheme.AMBER_GOLD -> TerminalAmber
        TerminalColorScheme.DRACULA_PURPLE -> TerminalPurple
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP CONTROL BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Selector & Status Pill
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showProfileDropdown = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("profile_selector_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Indicator Dot
                        val dotColor = when (connectionStatus) {
                            is ConnectionStatus.Connected -> Color(0xFF22C55E) // Bright Green
                            is ConnectionStatus.Connecting -> Color(0xFFFBBF24) // Yellow
                            is ConnectionStatus.Failed -> Color(0xFFEF4444) // Red
                            ConnectionStatus.Disconnected -> Color(0xFF94A3B8) // Grey
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val profileLabel = currentProfile?.name ?: "Chưa chọn PC"
                        Text(
                            text = profileLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Chọn máy tính",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile Dropdown
                    DropdownMenu(
                        expanded = showProfileDropdown,
                        onDismissRequest = { showProfileDropdown = false }
                    ) {
                        Text(
                            text = "Danh sách Máy tính",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(profile.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${profile.host}:${profile.port} (${profile.shellType.name})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectProfile(profile)
                                    showProfileDropdown = false
                                },
                                trailingIcon = {
                                    if (currentProfile?.id == profile.id) {
                                        Icon(Icons.Default.Check, contentDescription = "Đang chọn", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("⚙️ Cài đặt & Hướng dẫn PC") },
                            onClick = {
                                showProfileDropdown = false
                                onNavigateToSetup()
                            }
                        )
                    }
                }

                // Action icons: Open Browser, Reconnect, Clear, Theme, Font
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showQuickBrowserDialog = true },
                        modifier = Modifier.size(36.dp).testTag("quick_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Mở Trình duyệt PC",
                            tint = TerminalCyan
                        )
                    }

                    IconButton(
                        onClick = { viewModel.reconnect() },
                        modifier = Modifier.size(36.dp).testTag("reconnect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Kết nối lại",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            val allText = messages.joinToString("\n") { it.content }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", allText))
                            Toast.makeText(context, "Đã sao chép toàn bộ log terminal!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp).testTag("copy_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép log",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showColorMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Đổi màu hiển thị",
                                tint = primaryTextColor
                            )
                        }
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false }
                        ) {
                            TerminalColorScheme.values().forEach { scheme ->
                                DropdownMenuItem(
                                    text = { Text(scheme.displayName) },
                                    onClick = {
                                        viewModel.colorScheme.value = scheme
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showFontSizeMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Cỡ chữ",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showFontSizeMenu,
                            onDismissRequest = { showFontSizeMenu = false }
                        ) {
                            listOf(11 to "Nhỏ (11sp)", 13 to "Vừa (13sp)", 16 to "Lớn (16sp)").forEach { (size, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.terminalFontSize.value = size
                                        showFontSizeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // TERMINAL OUTPUT DISPLAY AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkTerminalScreen)
                .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("terminal_output_list"),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        TerminalMessageRow(
                            message = msg,
                            fontSizeSp = fontSizeSp,
                            textColor = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (isExecuting) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = primaryTextColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Đang thực thi lệnh trên PC...",
                                    color = primaryTextColor.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp
                                )
                            }
                        }
                    }
                }
            }

            // Scroll to bottom floating button
            if (isScrolledUp) {
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Cuộn xuống",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // QUICK DEVELOPER SHORTCUT MODIFIER BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    // History Up button
                    ModifierKeyButton(
                        text = "↑",
                        icon = Icons.Default.KeyboardArrowUp,
                        onClick = { viewModel.historyUp() }
                    )
                }
                item {
                    // History Down button
                    ModifierKeyButton(
                        text = "↓",
                        icon = Icons.Default.KeyboardArrowDown,
                        onClick = { viewModel.historyDown() }
                    )
                }
                item {
                    ModifierKeyButton(
                        text = "🌐 Mở Web",
                        onClick = { showQuickBrowserDialog = true }
                    )
                }
                item {
                    ModifierKeyButton(
                        text = "Ctrl+C",
                        onClick = { viewModel.appendModifier("Ctrl+C") },
                        isSpecial = true
                    )
                }
                item {
                    ModifierKeyButton(
                        text = "Tab",
                        onClick = { viewModel.appendModifier("Tab") }
                    )
                }
                item {
                    ModifierKeyButton(
                        text = "cls",
                        onClick = { viewModel.appendModifier("Clear") }
                    )
                }
                val symbols = listOf("|", "&&", ">", ">>", "\\", "/", "-", "~", "\"", "'")
                items(symbols) { sym ->
                    ModifierKeyButton(
                        text = sym,
                        onClick = { viewModel.appendModifier(sym) }
                    )
                }
            }
        }

        // INPUT PROMPT & SEND BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shell indicator badge
                val shellName = currentProfile?.shellType?.name ?: "CMD"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = shellName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Command input field
                OutlinedTextField(
                    value = inputCmd,
                    onValueChange = { viewModel.onCommandChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    placeholder = {
                        Text(
                            text = "Nhập lệnh cmd/powershell...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            viewModel.sendCurrentCommand()
                        }
                    ),
                    trailingIcon = {
                        if (inputCmd.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onCommandChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Xóa nội dung",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Run Command Button
                FilledIconButton(
                    onClick = { viewModel.sendCurrentCommand() },
                    enabled = inputCmd.isNotBlank() && !isExecuting,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("run_command_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Chạy lệnh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (showQuickBrowserDialog) {
            QuickBrowserDialog(
                onDismiss = { showQuickBrowserDialog = false },
                onExecute = { cmd ->
                    viewModel.repository.executeCommand(cmd)
                }
            )
        }
    }
}

@Composable
fun ModifierKeyButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSpecial) Color(0xFFDC2626).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSpecial) Color(0xFFF87171) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )
        } else {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun TerminalMessageRow(
    message: TerminalMessage,
    fontSizeSp: Int,
    textColor: Color
) {
    val style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp + 4).sp
    )

    when (message.type) {
        MessageType.INPUT -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message.content,
                    style = style,
                    fontWeight = FontWeight.Bold,
                    color = PromptColor
                )
            }
        }
        MessageType.OUTPUT -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message.content,
                    style = style,
                    color = textColor
                )
                if (message.durationMs != null && message.durationMs > 0) {
                    Text(
                        text = "[Thời gian: ${message.durationMs}ms | Exit: ${message.exitCode ?: 0}]",
                        style = style.copy(fontSize = (fontSizeSp - 2).sp),
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
        MessageType.STDERR -> {
            Text(
                text = message.content,
                style = style,
                color = TerminalError
            )
        }
        MessageType.SYSTEM -> {
            Text(
                text = message.content,
                style = style,
                color = TerminalCyan
            )
        }
        MessageType.ERROR -> {
            Text(
                text = message.content,
                style = style,
                fontWeight = FontWeight.SemiBold,
                color = TerminalError
            )
        }
    }
}

@Composable
fun QuickBrowserDialog(
    onDismiss: () -> Unit,
    onExecute: (String) -> Unit
) {
    var url by remember { mutableStateOf("https://google.com") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = TerminalCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mở Trình duyệt & App trên PC", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Gửi lệnh mở trực tiếp trình duyệt trên màn hình máy tính của bạn:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Địa chỉ trang web (URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick URL chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Google" to "https://google.com",
                        "YouTube" to "https://youtube.com",
                        "Facebook" to "https://facebook.com"
                    ).forEach { (name, link) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { url = link }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Button(
                    onClick = {
                        val cleanUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                        onExecute("start $cleanUrl")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mở Web trên máy tính ngay")
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hoặc mở nhanh ứng dụng PC:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExecute("start chrome")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Chrome", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onExecute("start msedge")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Edge", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onExecute("explorer .")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Thư mục", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExecute("calc")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Máy tính (Calc)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onExecute("notepad")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Notepad", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onExecute("start taskmgr")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Taskmgr", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

