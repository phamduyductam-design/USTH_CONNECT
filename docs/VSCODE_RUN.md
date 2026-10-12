# Chạy bằng VS Code

Mở thư mục chứa settings.gradle và .vscode/tasks.json, không mở thư mục con app.

## Task

- Terminal → Run Task → **USTH: Build APK**.
- Terminal → Run Task → **USTH: Chay may ao va ung dung**.

Task có cấu hình riêng cho Windows PowerShell và macOS bash. Máy chạy cần JDK 17, Android SDK Platform 35, build-tools, platform-tools và một máy ảo phù hợp kiến trúc.

## macOS

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
bash tools/run-android.sh --build-only
```

Trên máy đã chuẩn bị demo, bỏ --build-only để chạy. tools/start-demo.py dùng Python 3, AVD `usth_demo_api35` (đổi bằng USTH_AVD_NAME), SDK theo ANDROID_HOME và server Ergo trong .tools/ergo. SDK/AVD/server không đưa vào Git. Bot demo và helper khởi động nằm trong tools, là source có version control.

Nếu cài Android Studio/SDK ở vị trí khác, đặt ANDROID_HOME hoặc local.properties. Bản demo local dùng host 10.0.2.2, port 6667, TLS tắt; cần daemon trên host lắng nghe localhost:6667. Script giữ nháp và biệt danh khi khởi động lại demo.

## Windows

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\run-android.ps1 -BuildOnly
```

Bỏ -BuildOnly để chạy task emulator theo cấu hình script. Bật tăng tốc emulator phù hợp (Windows Hypervisor Platform) và chọn system image phù hợp kiến trúc. Không chạy AVD cùng tên trong hai tiến trình.

## Chạy và dùng tính năng

Sau kết nối, bấm Tham gia kênh ở màn trống hoặc mở tab Phòng để tải LIST. Chọn tên kênh, xác nhận JOIN, rồi nhắn. Các chức năng mới được mô tả tại [IRC_FEATURES.md](IRC_FEATURES.md).

Dùng [interactive-peer.sh](../tools/interactive-peer.sh) để người thứ hai tự nhập tin; demo không bắt buộc phải dùng bot echo.
