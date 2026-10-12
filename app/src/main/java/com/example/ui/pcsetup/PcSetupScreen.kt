package com.example.ui.pcsetup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CompanionScriptProvider
import com.example.model.ConnectionProfile
import com.example.ui.TerminalTab
import com.example.ui.TerminalViewModel
import com.example.ui.theme.DarkTerminalScreen
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import kotlinx.coroutines.launch

@Composable
fun PcSetupScreen(
    viewModel: TerminalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentProfile by viewModel.currentProfile.collectAsState()

    var selectedScriptTab by remember { mutableStateOf(0) }
    val tabs = listOf("PowerShell (1 dòng)", "Python (Khuyên dùng)", "Node.js")

    // Ping / Test Connection state
    var testHost by remember(currentProfile) { mutableStateOf(currentProfile?.host ?: "192.168.1.100") }
    var testPort by remember(currentProfile) { mutableStateOf((currentProfile?.port ?: 8080).toString()) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER TITLE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = TerminalCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cách kết nối điện thoại với CMD thật trên PC",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Để điện thoại gửi lệnh và nhận kết quả từ CMD thật trên máy tính, bạn chỉ cần khởi động một tiến trình lắng nghe (Server) rất nhẹ trên máy tính trong cùng mạng Wi-Fi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                StepInstructionRow(stepNumber = "1", text = "Kết nối máy tính và điện thoại vào cùng mạng Wi-Fi (hoặc phát hotspot).")
                Spacer(modifier = Modifier.height(6.dp))
                StepInstructionRow(stepNumber = "2", text = "Mở PowerShell hoặc Python trên máy tính và chạy đoạn mã bên dưới.")
                Spacer(modifier = Modifier.height(6.dp))
                StepInstructionRow(stepNumber = "3", text = "Nhập địa chỉ IP hiển thị trên màn hình máy tính vào ô kiểm tra.")
            }
        }

        // NETWORK TEST TOOL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = TerminalGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kiểm tra kết nối tới Máy tính (Ping & Test Port)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = testHost,
                        onValueChange = { testHost = it },
                        label = { Text("Địa chỉ IP máy tính") },
                        placeholder = { Text("192.168.1.xxx") },
                        singleLine = true,
                        modifier = Modifier.weight(2f).testTag("test_ip_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = testPort,
                        onValueChange = { testPort = it },
                        label = { Text("Cổng (Port)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("test_port_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isTesting = true
                        testResult = null
                        val portInt = testPort.toIntOrNull() ?: 8080
                        scope.launch {
                            val res = viewModel.repository.testConnection(testHost, portInt)
                            testResult = res
                            isTesting = false

                            if (res.first) {
                                // If successful, also update current profile and connect!
                                val existing = currentProfile
                                val updatedProfile = existing?.copy(host = testHost, port = portInt)
                                    ?: ConnectionProfile(
                                        name = "PC của tôi",
                                        host = testHost,
                                        port = portInt,
                                        isDefault = true
                                    )
                                viewModel.saveProfile(updatedProfile)
                                viewModel.selectProfile(updatedProfile)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("test_connection_button"),
                    enabled = !isTesting && testHost.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang kiểm tra kết nối...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kiểm tra kết nối ngay")
                    }
                }

                testResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (success) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF7F1D1D).copy(alpha = 0.3f))
                            .border(1.dp, if (success) TerminalGreen else Color(0xFFEF4444), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) TerminalGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (success) "🟢 Kết nối thành công!" else "🔴 Thất bại:",
                                    fontWeight = FontWeight.Bold,
                                    color = if (success) TerminalGreen else Color(0xFFEF4444)
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (success) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "👉 Bạn có thể chuyển sang tab Terminal để bắt đầu gõ lệnh!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TerminalCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            viewModel.setTab(TerminalTab.TERMINAL)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SCRIPT PICKER & CODE PREVIEW CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mã chạy Server trên Máy tính",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedScriptTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedScriptTab == index,
                            onClick = { selectedScriptTab = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val scriptCode = when (selectedScriptTab) {
                    0 -> CompanionScriptProvider.getPowerShellScript(testPort.toIntOrNull() ?: 8080)
                    1 -> CompanionScriptProvider.getPythonServerScript(testPort.toIntOrNull() ?: 8080)
                    else -> CompanionScriptProvider.getNodeJsScript(testPort.toIntOrNull() ?: 8080)
                }

                if (selectedScriptTab == 0) {
                    Button(
                        onClick = {
                            val oneLiner = CompanionScriptProvider.getPowerShellOneLiner(testPort.toIntOrNull() ?: 8080)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PowerShell 1-Liner", oneLiner))
                            Toast.makeText(context, "Đã sao chép lệnh 1 dòng! Mở PowerShell trên máy tính và dán là chạy ngay.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_powershell_oneliner_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚡ Sao chép Lệnh 1 Dòng (Chạy Ngay Không Cần Cài Gì)", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Copy button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Server Script", scriptCode))
                        Toast.makeText(context, "Đã sao chép mã! Hãy dán vào máy tính.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("copy_server_script_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sao chép toàn bộ mã (${tabs[selectedScriptTab]})",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Code box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkTerminalScreen)
                        .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = scriptCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalGreen
                    )
                }
            }
        }

        // TROUBLESHOOTING CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TerminalCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xử lý sự cố thường gặp",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Làm sao để lấy IP máy tính? Mở CMD trên máy tính, gõ 'ipconfig' và tìm dòng IPv4 Address (thường có dạng 192.168.1.x hoặc 192.168.0.x).\n\n" +
                            "• Bị lỗi Timeout không kết nối được? Tường lửa Windows Defender Firewall có thể đang chặn. Khi chạy server, nếu Windows hiện hộp thoại cảnh báo Firewall, hãy bấm 'Allow access' (Cho phép truy cập) trên Private network.\n\n" +
                            "• Hai thiết bị có cần chung mạng không? Có, điện thoại và máy tính cần kết nối cùng 1 Wi-Fi gia đình, hoặc bạn bật 'Điểm phát sóng di động' (Hotspot) trên điện thoại rồi cho máy tính kết nối vào.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun StepInstructionRow(stepNumber: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
