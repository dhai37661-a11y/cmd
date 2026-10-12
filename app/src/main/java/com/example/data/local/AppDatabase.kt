package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CommandHistory
import com.example.model.CommandSnippet
import com.example.model.ConnectionProfile
import com.example.model.ShellType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ConnectionProfile::class, CommandHistory::class, CommandSnippet::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionProfileDao(): ConnectionProfileDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun commandSnippetDao(): CommandSnippetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remote_cmd_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default snippets and a starter profile
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateInitialData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val profileDao = database.connectionProfileDao()
            profileDao.insertProfile(
                ConnectionProfile(
                    name = "Máy tính Windows (Mặc định)",
                    host = "192.168.1.100",
                    port = 8080,
                    authPin = "",
                    shellType = ShellType.CMD,
                    isDefault = true
                )
            )

            val snippetDao = database.commandSnippetDao()
            val presets = listOf(
                // Trình duyệt & Ứng dụng
                CommandSnippet(
                    title = "Mở Trình duyệt Web (Google)",
                    command = "start https://google.com",
                    category = "Trình duyệt & Web",
                    shellType = ShellType.CMD,
                    description = "Mở trình duyệt mặc định trên máy tính và vào Google",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Mở YouTube trên máy tính",
                    command = "start https://youtube.com",
                    category = "Trình duyệt & Web",
                    shellType = ShellType.CMD,
                    description = "Mở trang YouTube trên máy tính từ điện thoại",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Mở Google Chrome",
                    command = "start chrome",
                    category = "Trình duyệt & Web",
                    shellType = ShellType.CMD,
                    description = "Khởi chạy ứng dụng Google Chrome trên PC",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Mở Microsoft Edge",
                    command = "start msedge",
                    category = "Trình duyệt & Web",
                    shellType = ShellType.CMD,
                    description = "Khởi chạy trình duyệt Microsoft Edge",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Mở File Explorer (Thư mục)",
                    command = "explorer .",
                    category = "Trình duyệt & Web",
                    shellType = ShellType.CMD,
                    description = "Mở cửa sổ quản lý tệp tin File Explorer",
                    isSystemPreset = true
                ),
                // Mạng
                CommandSnippet(
                    title = "Xem thông tin IP & Card mạng",
                    command = "ipconfig /all",
                    category = "Mạng (Network)",
                    shellType = ShellType.CMD,
                    description = "Hiển thị địa chỉ IPv4, IPv6, Gateway và DNS",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Kiểm tra kết nối Internet (Ping)",
                    command = "ping 8.8.8.8 -n 4",
                    category = "Mạng (Network)",
                    shellType = ShellType.CMD,
                    description = "Gửi 4 gói tin ping tới DNS Google",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Xem các cổng Port & Kết nối mở",
                    command = "netstat -ano",
                    category = "Mạng (Network)",
                    shellType = ShellType.CMD,
                    description = "Hiển thị danh sách TCP/UDP và PID tiến trình",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Tra cứu tên miền DNS",
                    command = "nslookup google.com",
                    category = "Mạng (Network)",
                    shellType = ShellType.CMD,
                    description = "Kiểm tra phân giải tên miền",
                    isSystemPreset = true
                ),
                // Hệ thống
                CommandSnippet(
                    title = "Xem toàn bộ cấu hình máy tính",
                    command = "systeminfo",
                    category = "Hệ thống (System)",
                    shellType = ShellType.CMD,
                    description = "Hệ điều hành, RAM, BIOS, Hotfix, Card mạng",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Danh sách phần mềm đang chạy",
                    command = "tasklist",
                    category = "Hệ thống (System)",
                    shellType = ShellType.CMD,
                    description = "Xem mọi tiến trình exe và mức tiêu thụ RAM",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Kiểm tra tài khoản người dùng",
                    command = "whoami",
                    category = "Hệ thống (System)",
                    shellType = ShellType.CMD,
                    description = "Xem domain/username hiện tại",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Tên máy tính hiện tại",
                    command = "hostname",
                    category = "Hệ thống (System)",
                    shellType = ShellType.CMD,
                    description = "Xem Hostname của máy tính",
                    isSystemPreset = true
                ),
                // Nguồn
                CommandSnippet(
                    title = "Hẹn giờ tắt máy (sau 60s)",
                    command = "shutdown /s /t 60",
                    category = "Nguồn & Tắt máy",
                    shellType = ShellType.CMD,
                    description = "Hẹn tắt máy tính Windows sau 60 giây",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "HỦY lệnh hẹn tắt máy",
                    command = "shutdown /a",
                    category = "Nguồn & Tắt máy",
                    shellType = ShellType.CMD,
                    description = "Hủy bỏ yêu cầu tắt máy đang đếm ngược",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Khởi động lại máy tính",
                    command = "shutdown /r /t 10",
                    category = "Nguồn & Tắt máy",
                    shellType = ShellType.CMD,
                    description = "Khởi động lại PC sau 10 giây",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Xuất báo cáo tình trạng Pin",
                    command = "powercfg /batteryreport",
                    category = "Nguồn & Tắt máy",
                    shellType = ShellType.CMD,
                    description = "Tạo báo cáo chi tiết độ chai pin laptop",
                    isSystemPreset = true
                ),
                // File & Ổ đĩa
                CommandSnippet(
                    title = "Danh sách file thư mục hiện tại",
                    command = "dir /o:d",
                    category = "Tệp tin & Ổ đĩa",
                    shellType = ShellType.CMD,
                    description = "Liệt kê tệp tin sắp xếp theo ngày sửa đổi",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Sơ đồ cây thư mục",
                    command = "tree /f",
                    category = "Tệp tin & Ổ đĩa",
                    shellType = ShellType.CMD,
                    description = "Xem trực quan cấu trúc thư mục dạng cây",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Kiểm tra dung lượng các ổ đĩa",
                    command = "wmic logicaldisk get caption, freespace, size",
                    category = "Tệp tin & Ổ đĩa",
                    shellType = ShellType.CMD,
                    description = "Xem ổ C:, D:... và dung lượng còn trống",
                    isSystemPreset = true
                ),
                // PowerShell
                CommandSnippet(
                    title = "Kiểm tra RAM trống (PowerShell)",
                    command = "Get-CimInstance Win32_OperatingSystem | Select-Object @{N='FreeRAM_GB';E={[math]::Round(\$_.FreePhysicalMemory/1MB,2)}}, @{N='TotalRAM_GB';E={[math]::Round(\$_.TotalVisibleMemorySize/1MB,2)}}",
                    category = "PowerShell Chuyên sâu",
                    shellType = ShellType.POWERSHELL,
                    description = "Xem dung lượng RAM khả dụng bằng GB",
                    isSystemPreset = true
                ),
                CommandSnippet(
                    title = "Top 5 phần mềm chiếm CPU cao nhất",
                    command = "Get-Process | Sort-Object CPU -Descending | Select-Object -First 5 ProcessName, CPU, WorkingSet64",
                    category = "PowerShell Chuyên sâu",
                    shellType = ShellType.POWERSHELL,
                    description = "Xem ứng dụng gây nóng máy / ăn CPU",
                    isSystemPreset = true
                )
            )
            snippetDao.insertSnippets(presets)
        }
    }
}
