# Chạy USTH Connect bằng VS Code trên Windows

VS Code sửa source và chạy Gradle/ADB. Giao diện app hiển thị trong cửa sổ Android Emulator hoặc trên điện thoại Android.

## Chuẩn bị hypervisor

Máy này dùng Intel Core i9-14900HX, ảo hóa trong BIOS đã bật. SDK, image Android 35 và AVD `usth_api35` đã chuẩn bị trong `.tools/`. Sau khi người dùng bật Windows Hypervisor Platform ngày 09/10/2026, WHPX đã sẵn sàng và task đã mở app thành công. Nếu `-accel-check` đã báo mã 0, có thể chuyển thẳng đến phần chạy từ VS Code.

1. Lưu công việc đang mở.
2. Nhấn **Win + R**, nhập `optionalfeatures`, nhấn Enter.
3. Đánh dấu **Windows Hypervisor Platform**, bấm **OK**, chờ Windows cài xong.
4. **Restart máy tính**. Việc cài Windows feature có thể yêu cầu quyền quản trị.

Đây là cách [Android Developers khuyến nghị](https://developer.android.com/studio/run/emulator-acceleration#vm-windows). Không cần sửa BIOS khi mục ảo hóa đã bật. Script của dự án không bật/tắt Windows feature và không tự restart máy tính.

## Chạy từ VS Code

Nếu thấy **No matching tasks**, mở đúng `D:\prj_mobile`; task nằm tại `D:\prj_mobile\.vscode\tasks.json`. Thư mục bản giải nén cũ `D:\prj_mobile\output\USTH-Connect-source` không có task này ở workspace root.

1. **File → Open Folder → `D:\prj_mobile`** (thư mục có `settings.gradle` và `.vscode/tasks.json`).
2. Nhấn **Ctrl + Shift + P**, chọn **Tasks: Run Task**.
3. Chọn **USTH: Chay may ao va ung dung**.
4. Chờ cửa sổ Android khởi động; task tự build, cài APK và mở USTH Connect. Lần đầu có thể cần vài phút.

Task **USTH: Build APK** chỉ build; có thể gọi bằng **Ctrl + Shift + B**.

Tương đương trong terminal PowerShell của VS Code:

```powershell
cd D:\prj_mobile
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\run-android.ps1
```

`ExecutionPolicy Bypass` chỉ áp dụng cho tiến trình chạy script này, không đổi execution policy toàn hệ thống.

## Kiểm tra khi còn lỗi

```powershell
& 'D:\prj_mobile\.tools\android-sdk\emulator\emulator.exe' -accel-check
```

Kết quả sẵn sàng thường có mã **0** và `WHPX ... is installed and usable`. Nếu vẫn báo thiếu hypervisor sau restart, kiểm tra Windows feature đã được bật; gửi lại toàn bộ kết quả lệnh để xác định tiếp. Không dùng `-accel off` để thay thế trên máy này vì lần thử software mode trước đã crash.

Log của task: `.tools/vscode-emulator-stdout.log` và `.tools/vscode-emulator-stderr.log`. Nếu emulator lỗi đồ họa sau khi WHPX đã sẵn sàng, có thể thử `-gpu software` theo hướng dẫn chính thức. Chỉ mở một instance của AVD này; đóng cửa sổ cũ trước khi chạy lại task.

## Dùng bản ZIP trên máy khác

ZIP không chứa `.tools/` nhiều GB. Cần JDK 17, Android SDK với Platform 35, build tools, platform-tools, emulator và system image `system-images;android-35;google_apis;x86_64`. Đặt JAVA_HOME/ANDROID_HOME theo máy đó và tạo AVD bằng SDK avdmanager:

```powershell
$env:ANDROID_AVD_HOME = "$PWD\.tools\avd"
New-Item -ItemType Directory -Path $env:ANDROID_AVD_HOME -Force
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\avdmanager.bat" create avd -n usth_api35 -k 'system-images;android-35;google_apis;x86_64' -p "$PWD\.tools\avd\usth_api35.avd"
```

Nếu cmdline-tools dùng thư mục phiên bản khác, sửa `latest` cho đúng. Android Studio SDK Manager/Device Manager cũng có thể chuẩn bị SDK/AVD; để task tìm được AVD, đặt AVD home của task cùng đường dẫn AVD đã tạo.
