package com.example.data.remote

object CompanionScriptProvider {

    fun getPowerShellOneLiner(port: Int = 8080): String {
        return """${'$'}p=$port;${'$'}l=New-Object System.Net.HttpListener;${'$'}l.Prefixes.Add("http://*:${'$'}p/");${'$'}l.Start();${'$'}ip=(Get-NetIPAddress -AddressFamily IPv4|?{${'$'}_.IPAddress -notlike "127.*"-and ${'$'}_.IPAddress -notlike "169.254.*"}|select -first 1).IPAddress;Write-Host ">>> SERVER DANG CHAY! IP: ${'$'}ip:${'$'}p <<<" -f Green;while(${'$'}l.IsListening){${'$'}c=${'$'}l.GetContext();${'$'}q=${'$'}c.Request;${'$'}r=${'$'}c.Response;${'$'}r.Headers.Add("Access-Control-Allow-Origin","*");${'$'}r.Headers.Add("Access-Control-Allow-Headers","*");if(${'$'}q.HttpMethod -eq "OPTIONS"){${'$'}r.Close();continue};if(${'$'}q.RawUrl -like "*ping*"){${'$'}b=[Text.Encoding]::UTF8.GetBytes((@{status="ok";hostname=${'$'}env:COMPUTERNAME;user=${'$'}env:USERNAME;os="Windows";cwd=(gl).Path}|ConvertTo-Json));${'$'}r.ContentType="application/json";${'$'}r.OutputStream.Write(${'$'}b,0,${'$'}b.Length);${'$'}r.Close();continue};${'$'}b=(New-Object IO.StreamReader(${'$'}q.InputStream)).ReadToEnd()|ConvertFrom-Json;${'$'}cmd=${'$'}b.cmd;${'$'}sw=[Diagnostics.Stopwatch]::StartNew();${'$'}o=try{if(${'$'}b.shell -eq "powershell"){iex ${'$'}cmd 2>&1|Out-String}else{cmd /c ${'$'}cmd 2>&1|Out-String}}catch{${'$'}_.Exception.Message};${'$'}sw.Stop();${'$'}res=(@{stdout=${'$'}o;stderr="";exit_code=0;duration_ms=${'$'}sw.ElapsedMilliseconds;cwd=(gl).Path}|ConvertTo-Json);${'$'}buf=[Text.Encoding]::UTF8.GetBytes(${'$'}res);${'$'}r.ContentType="application/json";${'$'}r.OutputStream.Write(${'$'}buf,0,${'$'}buf.Length);${'$'}r.Close()}"""
    }

    fun getPythonServerScript(port: Int = 8080, pin: String = ""): String {
        return """
# =========================================================
# REMOTE CMD COMPANION SERVER (Cho Máy Tính Windows/Mac/Linux)
# Không cần cài đặt thêm thư viện, chỉ cần Python 3 chuẩn.
# Chạy lệnh: python cmd_server.py
# =========================================================
import sys
import os
import json
import socket
import subprocess
import time
from http.server import HTTPServer, BaseHTTPRequestHandler

PORT = $port
AUTH_PIN = "$pin"

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

class CmdHttpHandler(BaseHTTPRequestHandler):
    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

    def do_OPTIONS(self):
        self.send_response(200)
        self._cors()
        self.end_headers()

    def do_GET(self):
        if self.path.startswith("/ping") or self.path.startswith("/api/ping"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._cors()
            self.end_headers()
            info = {
                "status": "ok",
                "hostname": socket.gethostname(),
                "os": os.name + " (" + sys.platform + ")",
                "user": os.getenv("USERNAME", os.getenv("USER", "Admin")),
                "cwd": os.getcwd(),
                "python": sys.version.split()[0],
                "shell": "cmd.exe" if os.name == "nt" else "bash"
            }
            self.wfile.write(json.dumps(info).encode("utf-8"))
        else:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self._cors()
            self.end_headers()
            html = f'''
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Remote CMD Server</title></head>
            <body style="font-family:system-ui,sans-serif;background:#090d16;color:#e2e8f0;padding:40px;text-align:center;">
                <h1 style="color:#22d3ee;">🟢 Remote CMD Server Đang Hoạt Động!</h1>
                <p style="font-size:18px;">IP Máy tính: <b style="color:#4ade80;">{get_local_ip()}:{PORT}</b></p>
                <p style="color:#94a3b8;">Nhập địa chỉ trên vào ứng dụng Remote CMD trên điện thoại Android.</p>
            </body>
            </html>
            '''
            self.wfile.write(html.encode("utf-8"))

    def do_POST(self):
        if self.path.startswith("/exec") or self.path.startswith("/api/exec"):
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length).decode("utf-8", errors="replace")
            try:
                data = json.loads(body)
            except Exception:
                data = {}

            req_pin = data.get("pin", "")
            if AUTH_PIN and req_pin != AUTH_PIN:
                self.send_response(401)
                self.send_header("Content-Type", "application/json")
                self._cors()
                self.end_headers()
                self.wfile.write(json.dumps({"error": "Mã PIN bảo mật không đúng!"}).encode("utf-8"))
                return

            cmd = data.get("cmd", "")
            shell = data.get("shell", "cmd")
            cwd = data.get("cwd", None)

            # Phân giải shell thực thi
            if os.name == "nt":
                if shell == "powershell":
                    exec_args = ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", cmd]
                else:
                    exec_args = ["cmd.exe", "/c", cmd]
            else:
                exec_args = ["/bin/bash", "-c", cmd]

            t0 = time.time()
            try:
                res = subprocess.run(
                    exec_args,
                    capture_output=True,
                    text=True,
                    cwd=cwd,
                    timeout=90,
                    shell=False
                )
                duration = int((time.time() - t0) * 1000)
                out = res.stdout
                err = res.stderr
                code = res.returncode
            except subprocess.TimeoutExpired:
                duration = 90000
                out = ""
                err = "Lệnh đã chạy quá thời gian chờ (Timeout 90s)"
                code = -1
            except Exception as e:
                duration = int((time.time() - t0) * 1000)
                out = ""
                err = f"Lỗi thực thi: {str(e)}"
                code = -1

            resp = {
                "stdout": out,
                "stderr": err,
                "exit_code": code,
                "duration_ms": duration,
                "cwd": os.getcwd()
            }
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self._cors()
            self.end_headers()
            self.wfile.write(json.dumps(resp).encode("utf-8"))

def run():
    ip = get_local_ip()
    server = HTTPServer(("0.0.0.0", PORT), CmdHttpHandler)
    print("=" * 60)
    print("   REMOTE CMD COMPANION SERVER CHO ANDROID")
    print(f"📡 Địa chỉ IP mạng LAN : {ip}")
    print(f"🔌 Cổng Port             : {PORT}")
    print(f"🔐 Mã PIN bảo mật        : {'[Không đặt]' if not AUTH_PIN else AUTH_PIN}")
    print(f"👉 Nhập vào App Android : {ip}:{PORT}")
    print("=" * 60)
    print("Đang lắng nghe kết nối từ điện thoại...")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nĐã dừng server.")

if __name__ == "__main__":
    run()
""".trimIndent()
    }

    fun getPowerShellScript(port: Int = 8080): String {
        return """
# =========================================================
# LỆNH POWERSHELL SERVER 1-CLICK (Không cần cài đặt Python)
# Mở PowerShell trên Windows và dán toàn bộ đoạn mã này:
# =========================================================
${'$'}port = $port
${'$'}listener = New-Object System.Net.HttpListener
${'$'}listener.Prefixes.Add("http://*:${'$'}port/")
${'$'}listener.Start()
${'$'}ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { ${'$'}_.IPAddress -notlike "127.*" -and ${'$'}_.IPAddress -notlike "169.254.*" } | Select-Object -First 1).IPAddress
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " REMOTE CMD POWERSHELL SERVER DANG CHAY" -ForegroundColor Green
Write-Host " IP May tinh: ${'$'}ip:${'$'}port" -ForegroundColor Yellow
Write-Host " Nhap vao App Android: ${'$'}ip:${'$'}port" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan

while (${'$'}listener.IsListening) {
    ${'$'}context = ${'$'}listener.GetContext()
    ${'$'}request = ${'$'}context.Request
    ${'$'}response = ${'$'}context.Response
    ${'$'}response.Headers.Add("Access-Control-Allow-Origin", "*")
    ${'$'}response.Headers.Add("Access-Control-Allow-Headers", "Content-Type")

    if (${'$'}request.HttpMethod -eq "OPTIONS") {
        ${'$'}response.StatusCode = 200
        ${'$'}response.Close()
        continue
    }

    if (${'$'}request.RawUrl -like "*ping*") {
        ${'$'}info = @{ status = "ok"; hostname = ${'$'}env:COMPUTERNAME; user = ${'$'}env:USERNAME; os = "Windows PowerShell"; shell = "powershell.exe"; cwd = (Get-Location).Path } | ConvertTo-Json
        ${'$'}buf = [System.Text.Encoding]::UTF8.GetBytes(${'$'}info)
        ${'$'}response.ContentType = "application/json; charset=utf-8"
        ${'$'}response.OutputStream.Write(${'$'}buf, 0, ${'$'}buf.Length)
        ${'$'}response.Close()
        continue
    }

    if (${'$'}request.HttpMethod -eq "POST") {
        ${'$'}reader = New-Object System.IO.StreamReader(${'$'}request.InputStream)
        ${'$'}body = ${'$'}reader.ReadToEnd()
        ${'$'}json = ${'$'}body | ConvertFrom-Json
        ${'$'}cmd = ${'$'}json.cmd
        ${'$'}shell = ${'$'}json.shell

        ${'$'}sw = [System.Diagnostics.Stopwatch]::StartNew()
        ${'$'}out = ""
        ${'$'}err = ""
        ${'$'}code = 0
        try {
            if (${'$'}shell -eq "powershell") {
                ${'$'}out = Invoke-Expression ${'$'}cmd 2>&1 | Out-String
            } else {
                ${'$'}out = cmd.exe /c ${'$'}cmd 2>&1 | Out-String
            }
        } catch {
            ${'$'}err = ${'$'}_.Exception.Message
            ${'$'}code = 1
        }
        ${'$'}sw.Stop()

        ${'$'}res = @{ stdout = ${'$'}out; stderr = ${'$'}err; exit_code = ${'$'}code; duration_ms = ${'$'}sw.ElapsedMilliseconds; cwd = (Get-Location).Path } | ConvertTo-Json
        ${'$'}buf = [System.Text.Encoding]::UTF8.GetBytes(${'$'}res)
        ${'$'}response.ContentType = "application/json; charset=utf-8"
        ${'$'}response.OutputStream.Write(${'$'}buf, 0, ${'$'}buf.Length)
        ${'$'}response.Close()
    }
}
""".trimIndent()
    }

    fun getNodeJsScript(port: Int = 8080): String {
        return """
// =========================================================
// REMOTE CMD COMPANION SERVER (Node.js)
// Chạy lệnh: node cmd_server.js
// =========================================================
const http = require('http');
const { exec } = require('child_process');
const os = require('os');

const PORT = $port;

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  if (req.url.includes('/ping')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      status: 'ok',
      hostname: os.hostname(),
      user: os.userInfo().username,
      os: os.type() + ' ' + os.release(),
      cwd: process.cwd(),
      shell: process.platform === 'win32' ? 'cmd.exe' : 'bash'
    }));
  }

  if (req.method === 'POST' && req.url.includes('/exec')) {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const { cmd, shell } = JSON.parse(body);
        const startTime = Date.now();
        const options = process.platform === 'win32' && shell === 'powershell' 
          ? { shell: 'powershell.exe' } 
          : {};

        exec(cmd, options, (error, stdout, stderr) => {
          const duration = Date.now() - startTime;
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            stdout: stdout || '',
            stderr: (stderr || '') + (error ? '\n' + error.message : ''),
            exit_code: error ? (error.code || 1) : 0,
            duration_ms: duration,
            cwd: process.cwd()
          }));
        });
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('Remote CMD Node.js Server is running!');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('🚀 Remote CMD Node.js Server listening on port ' + PORT);
});
""".trimIndent()
    }
}
