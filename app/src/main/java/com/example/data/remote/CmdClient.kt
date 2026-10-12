package com.example.data.remote

import com.example.model.ServerInfo
import com.example.model.ShellType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class CommandExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val cwd: String
)

class CmdClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun ping(host: String, port: Int): Result<ServerInfo> = withContext(Dispatchers.IO) {
        val cleanHost = sanitizeHost(host)
        val url = "http://$cleanHost:$port/ping"
        val startTime = System.currentTimeMillis()

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Máy tính phản hồi mã lỗi HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)

            val serverInfo = ServerInfo(
                hostname = json.optString("hostname", cleanHost),
                osName = json.optString("os", "Windows"),
                username = json.optString("user", "User"),
                currentDir = json.optString("cwd", "C:\\"),
                pythonVersion = json.optString("python", ""),
                activeShell = json.optString("shell", "cmd.exe"),
                latencyMs = latency
            )
            Result.success(serverInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeCommand(
        host: String,
        port: Int,
        cmd: String,
        shell: ShellType,
        pin: String = "",
        cwd: String? = null
    ): Result<CommandExecutionResult> = withContext(Dispatchers.IO) {
        val cleanHost = sanitizeHost(host)
        val url = "http://$cleanHost:$port/exec"

        try {
            val payload = JSONObject().apply {
                put("cmd", cmd)
                put("shell", if (shell == ShellType.POWERSHELL) "powershell" else "cmd")
                put("pin", pin)
                if (!cwd.isNullOrBlank()) {
                    put("cwd", cwd)
                }
            }

            val requestBody = payload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.code == 401) {
                return@withContext Result.failure(
                    Exception("Sai mã PIN bảo mật! Vui lòng kiểm tra mã PIN trong cài đặt.")
                )
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Lỗi kết nối máy tính (${response.code}): $responseBody")
                )
            }

            val json = JSONObject(responseBody)
            val result = CommandExecutionResult(
                stdout = json.optString("stdout", ""),
                stderr = json.optString("stderr", ""),
                exitCode = json.optInt("exit_code", 0),
                durationMs = json.optLong("duration_ms", 0),
                cwd = json.optString("cwd", "")
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testSocketConnection(host: String, port: Int, timeoutMs: Int = 2000): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val cleanHost = sanitizeHost(host)
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
                Pair(true, "Kết nối thành công tới cổng $port")
            } catch (e: java.net.ConnectException) {
                Pair(false, "Từ chối kết nối (Connection Refused): Hãy đảm bảo server trên PC đang chạy!")
            } catch (e: java.net.SocketTimeoutException) {
                Pair(false, "Hết thời gian chờ (Timeout): Kiểm tra Windows Firewall hoặc địa chỉ IP.")
            } catch (e: Exception) {
                Pair(false, "Lỗi mạng: ${e.localizedMessage}")
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }

    private fun sanitizeHost(raw: String): String {
        return raw.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("ws://")
            .removePrefix("wss://")
            .substringBefore(":")
            .trim()
    }
}
