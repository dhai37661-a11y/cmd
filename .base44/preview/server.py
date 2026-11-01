"""Local preview server for the native Android app.

The app itself is an Android APK, so it cannot render inside the browser
preview iframe. This serves, from the sandbox's own build output:

  /                 status page (built APK, screenshots captured from the app,
                    companion-server setup instructions)
  /app-debug.apk    the debug APK built from the current checkout
  /shots/<name>.png screenshots rendered from the real Compose UI (if captured)

Nothing here is application code; it only reports what exists on disk.
"""

import html
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from xml.etree import ElementTree

PORT = 3000
APK_PATH = "/workspace/app/build/outputs/apk/debug/app-debug.apk"
SHOTS_DIR = "/workspace/build/base44-screenshots"

PAGE = """<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Remote CMD - Android app</title>
<style>
  :root {{ color-scheme: dark; }}
  body {{
    margin: 0; padding: 32px 20px 56px;
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
    background: #090d16; color: #e2e8f0;
  }}
  .wrap {{ max-width: 880px; margin: 0 auto; }}
  h1 {{ font-size: 22px; margin: 0 0 4px; letter-spacing: .2px; }}
  h1 span {{ color: #22d3ee; }}
  .sub {{ color: #94a3b8; font-size: 14px; margin: 0 0 26px; line-height: 1.55; }}
  .card {{
    background: #0f1626; border: 1px solid #1e293b; border-radius: 14px;
    padding: 20px; margin-bottom: 18px;
  }}
  .card h2 {{ font-size: 14px; text-transform: uppercase; letter-spacing: .09em;
    color: #7dd3fc; margin: 0 0 12px; font-weight: 600; }}
  .row {{ display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }}
  .pill {{ display: inline-flex; align-items: center; gap: 7px; font-size: 13px;
    background: #16233a; border: 1px solid #23344f; border-radius: 999px;
    padding: 4px 11px; color: #cbd5e1; }}
  .dot {{ width: 8px; height: 8px; border-radius: 50%; }}
  .ok {{ background: #22c55e; }} .warn {{ background: #fbbf24; }}
  .btn {{ display: inline-block; margin-top: 14px; padding: 11px 20px;
    background: #22d3ee; color: #052027; font-weight: 600; font-size: 14px;
    border-radius: 10px; text-decoration: none; }}
  .btn.off {{ background: #1e293b; color: #64748b; pointer-events: none; }}
  code {{ background: #0b1220; border: 1px solid #1e293b; border-radius: 6px;
    padding: 2px 6px; font-size: 12.5px; color: #7dd3fc; }}
  ol {{ margin: 0; padding-left: 20px; color: #cbd5e1; font-size: 14px; line-height: 1.75; }}
  .shots {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
    gap: 14px; }}
  figure {{ margin: 0; }}
  figure img {{ width: 100%; border-radius: 12px; border: 1px solid #1e293b; display: block; }}
  figcaption {{ font-size: 12px; color: #94a3b8; margin-top: 7px; }}
  .note {{ font-size: 13px; color: #94a3b8; line-height: 1.6; }}
</style>
</head>
<body>
<div class="wrap">
  <h1>Remote CMD <span>&mdash; native Android app</span></h1>
  <p class="sub">Kết nối và chạy lệnh CMD / PowerShell trên máy tính từ điện thoại Android
  qua Wi-Fi / LAN. Ứng dụng build bằng Jetpack Compose + Room (Kotlin).</p>

  <div class="card">
    <h2>Trạng thái build</h2>
    <div class="row"><span class="pill"><span class="dot {build_dot}"></span>{build_status}</span></div>
    <div class="row"><span class="pill"><span class="dot {test_dot}"></span>{test_status}</span></div>
    {apk_button}
  </div>

  {shots_card}

  <div class="card">
    <h2>Cách chạy trên điện thoại</h2>
    <ol>
      <li>Cài file <code>app-debug.apk</code> lên điện thoại Android (cho phép "Cài từ nguồn không xác định").</li>
      <li>Mở tab <code>Cài đặt PC</code> trong app và chạy script companion server trên máy tính
      (PowerShell / Python / Node.js) &mdash; script lắng nghe cổng <code>8080</code>.</li>
      <li>Nhập địa chỉ IP máy tính (ví dụ <code>192.168.1.10:8080</code>) trong app, rồi gõ lệnh
      ở tab Terminal. Điện thoại và máy tính phải cùng một mạng LAN / Wi-Fi.</li>
    </ol>
  </div>

  <p class="note">Đây là môi trường phát triển: mỗi lần build lại, file APK trên trang này
  được cập nhật. Preview trên trình duyệt không hiển thị trực tiếp UI Android &mdash; giao diện
  thật được chụp lại ở mục phía trên và app chạy trên thiết bị / emulator.</p>
</div>
</body>
</html>
"""


def render():
    apk_ok = os.path.isfile(APK_PATH)
    build_dot, build_status = ("ok", "APK đã build từ mã nguồn hiện tại")
    if not apk_ok:
        build_dot = "warn"
        build_status = "Đang build APK (chưa có output)"
    apk_size = ""
    if apk_ok:
        apk_size = f" &middot; {os.path.getsize(APK_PATH) / (1024 * 1024):.1f} MB"
    apk_button = (
        '<a class="btn" href="/app-debug.apk">Tải app-debug.apk' + apk_size + "</a>"
        if apk_ok
        else '<a class="btn off">app-debug.apk chưa sẵn sàng</a>'
    )

    test_dot, test_status = "warn", "Unit test chưa chạy"
    results = "/workspace/app/build/test-results/testDebugUnitTest"
    if os.path.isdir(results):
        total = failed = 0
        for name in os.listdir(results):
            if not name.endswith(".xml"):
                continue
            try:
                root = ElementTree.parse(os.path.join(results, name)).getroot()
            except (OSError, ElementTree.ParseError):
                continue
            total += int(root.get("tests", 0))
            failed += int(root.get("failures", 0)) + int(root.get("errors", 0))
        if total:
            if failed == 0:
                test_dot = "ok"
                test_status = f"{total} unit test đã pass"
            else:
                test_status = f"{failed} test lỗi / {total} test"

    shots_card = ""
    shots = []
    if os.path.isdir(SHOTS_DIR):
        shots = sorted(n for n in os.listdir(SHOTS_DIR) if n.endswith(".png"))
    if shots:
        figures = "".join(
            '<figure><img src="/shots/%s" alt="%s"><figcaption>%s</figcaption></figure>'
            % (html.escape(n), html.escape(n), html.escape(n[:-4].replace("_", " ")))
            for n in shots
        )
        shots_card = (
            '<div class="card"><h2>Giao diện app (chụp từ Compose UI)</h2>'
            '<div class="shots">' + figures + "</div></div>"
        )

    return PAGE.format(
        build_dot=build_dot,
        build_status=build_status,
        test_dot=test_dot,
        test_status=test_status,
        apk_button=apk_button,
        shots_card=shots_card,
    )


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _send(self, code, body, content_type, extra=None):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        for key, value in (extra or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path in ("/", "/index.html"):
            self._send(200, render().encode("utf-8"), "text/html; charset=utf-8",
                       {"Cache-Control": "no-store"})
        elif path == "/health":
            self._send(200, b"ok", "text/plain")
        elif path == "/app-debug.apk":
            if not os.path.isfile(APK_PATH):
                self._send(404, b"APK not built yet", "text/plain")
                return
            data = open(APK_PATH, "rb").read()
            self._send(200, data, "application/vnd.android.package-archive",
                       {"Content-Disposition": 'attachment; filename="app-debug.apk"'})
        elif path.startswith("/shots/") and path.endswith(".png"):
            name = os.path.basename(path)
            target = os.path.join(SHOTS_DIR, name)
            if not os.path.isfile(target):
                self._send(404, b"not found", "text/plain")
                return
            with open(target, "rb") as fh:
                self._send(200, fh.read(), "image/png", {"Cache-Control": "no-store"})
        else:
            self._send(404, b"not found", "text/plain")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
