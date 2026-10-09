# Báo cáo kiểm thử

Thực hiện ngày 08–09/10/2026, múi giờ Asia/Bangkok (UTC+7), trên Windows. Báo cáo phân biệt test logic/giao thức JVM với chạy ứng dụng Android trên thiết bị.

## Môi trường thực tế

- Temurin JDK 17.0.20.1, Gradle wrapper 8.9, AGP 8.7.3.
- Android SDK 35 revision 2; Build Tools 34.0.0 (mặc định AGP) và 35.0.0 đã cài.
- SDK/JDK/cache nằm trong `.tools/` của workspace; không dựa vào JDK 25 có sẵn.
- IRC daemon Ergo 2.19.1 chính thức, binary Windows x86_64 có đối chiếu SHA-256; chỉ listen `127.0.0.1:6667`, CASEMAPPING RFC1459.
- Mạng công cộng: `irc.libera.chat:6697`, TLS và kiểm tra hostname/trust store mặc định, hai nickname ngẫu nhiên trong kênh tạm ngẫu nhiên chỉ trao đổi với nhau.

## Kết quả build và kiểm tra tĩnh

Lệnh:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest --console=plain --no-daemon
```

Kết quả: **BUILD SUCCESSFUL**. APK debug và APK instrumentation test đã build. `apksigner verify --verbose` xác minh APK debug thành công, APK Signature Scheme v2, một signer. Đây là debug signing; chưa có khóa phát hành hoặc upload lên store.

Lint: **0 lỗi, 8 cảnh báo**:

- 6 cảnh báo có phiên bản dependency mới hơn. Dự án chủ động ghim bộ phiên bản đã build, không dùng dynamic version.
- 2 cảnh báo adapter dùng `notifyDataSetChanged`; danh sách hiện nhỏ, có giới hạn và chat giữ anchor. Có thể chuyển sang DiffUtil khi mở rộng.

Không dùng lint baseline để che lỗi. Các lỗi API 27 trên minSdk 24, tham chiếu style, import test và các cảnh báo resource/accessibility/overdraw phát hiện trong quá trình build đã được sửa.

## Unit test — 22 test, 0 failed, 0 errors

| Bộ test | Số test | Phạm vi |
|---|---:|---|
| IrcParserTest | 8 | Source, command, trailing, khoảng trắng, tags, case folding, CRLF, input phân mảnh, lỗi framing, UTF-8 byte limit, chèn lệnh |
| SessionStateTest | 10 | JOIN xác nhận, NAMES nhiều phần, PART/NICK xen giữa, NAMES đến muộn sau PART, unread, private target, nickname, KICK/PART, mất kết nối, gửi bị từ chối, echo dedup, giới hạn lịch sử |
| IrcClientTest | 4 | Chỉ CONNECTED sau 001, reader blocking không chặn writer/PONG, 433 trước đăng ký, hủy reconnect, reconnect đăng ký lại và không phát lại tin đã gửi |

Unit transport dùng ServerSocket fixture có kiểm soát trên loopback. Các fixture này là công cụ kiểm thử framing/trạng thái, không được gọi là server của USTH và không cấp dữ liệu mẫu cho sản phẩm.

## Smoke test hai client với IRC daemon thật

`tools/NetworkSmoke.java` sử dụng chính IrcClient/IrcCommands/model của app, compile từ source; không có transport thay thế. Đã chạy cả:

```powershell
.\tools\network-smoke.ps1 -Server 127.0.0.1 -Port 6667
.\tools\network-smoke.ps1 -Server irc.libera.chat -Port 6697 -Tls
```

Hai lần đều qua 9 bước:

1. Hai phiên được máy chủ chấp nhận bằng 001.
2. JOIN có xác nhận self và sự kiện peer JOIN.
3. NAMES hoàn tất bằng 366.
4. Tin nhắn tiếng Việt UTF-8 trong kênh được client kia nhận.
5. Tin nhắn riêng được client kia nhận.
6. Đổi nickname được broadcast.
7. Người nhận không tồn tại trả numeric 401.
8. PART được broadcast.
9. QUIT được broadcast.

Đây là hai **Java client dùng engine của app** chạy trên Windows, không phải hai điện thoại. Kiểm thử công cộng xác nhận đường TCP/TLS và việc dùng trust store/hostname verification trong engine trên JVM; không chứng minh đã chạy TLS trên Android thiết bị.

## Lần thử máy ảo ban đầu

Đã tải và thử Android Emulator 37.2.12 với image Android 35 Google APIs x86_64 revision 9. Máy báo **Android Emulator hypervisor driver is not installed**. Thử software mode `-accel off`, SwiftShader và cấu hình không Vulkan/GPU cũng không boot được; lần cuối emulator thoát `-1073741819` (`0xC0000005`). ADB không có thiết bị sẵn sàng.

Vì vậy:

- `AppFlowTest` đã compile và build APK test, **chưa thực thi**.
- Chưa có ảnh chụp app chạy thật. Không dùng ảnh mockup người dùng đưa để thay bằng ảnh chạy app.
- Chưa xác nhận thực tế layout/font/keyboard, xoay màn hình, giữ vị trí cuộn, permission thông báo, foreground/background trên thiết bị.
- Chưa demo đồng thời hai Android client, chưa đo UI frame time/overdraw/pin hoặc test trên API 24.
- Chưa chạy test certificate không tin cậy/sai hostname trên Android và chưa thử IRC server do USTH cung cấp.

Source xử lý các trường hợp trên, nhưng trạng thái **implemented** không được hiểu là **runtime verified**. Các tiến trình emulator thử nghiệm đã được dừng sau kiểm tra; không cài driver hay thay đổi tính năng Windows toàn hệ thống.

Bổ sung ngày 09/10/2026: đã thêm `.vscode/tasks.json`, `tools/run-android.ps1` và hướng dẫn `VSCODE_RUN.md`. Đã kiểm tra cú pháp PowerShell/JSON và nhánh CheckOnly dừng đúng khi thiếu hypervisor. Chưa thực thi nhánh mở máy ảo/cài APK của task vì Windows Hypervisor Platform chưa sẵn sàng; cần bật tính năng Windows và restart trước.

### Cập nhật sau khi người dùng bật WHPX, ngày 09/10/2026

`-accel-check` đã trả mã 0 và `WHPX(10.0.26200) is installed and usable`. Đã chạy `tools/run-android.ps1` thành công: máy ảo `emulator-5554` boot xong (`sys.boot_completed=1`), build APK thành công, `adb install` báo Success, app có process đang chạy và `ConnectActivity` là Activity resumed.

Đã chụp và xem ảnh thật tại `output/screenshots/connect.png`; màn hình Kết nối hiển thị với bàn phím đang mở. Giới hạn “chưa có ảnh chạy thật/chưa chạy được emulator” của lần thử ban đầu đã được giải quyết. Chưa chạy AppFlowTest, chưa xác minh toàn bộ các màn hình hoặc TLS/chat trên Android; các mục đó vẫn cần kiểm tra riêng. Máy ảo đang được giữ mở để người dùng sử dụng.

## Checklist cần chạy khi có điện thoại / emulator hoạt động

- Cài `USTH-Connect-debug.apk`, thử cả màn hình nhỏ và landscape.
- Chạy AppFlowTest với server thật; kiểm tra JOIN, tin kênh/riêng, thành viên, tái tạo Activity và giữ nháp.
- Xoay portrait ↔ landscape với bàn phím mở, kiểm tra không mất nháp và không tạo socket thứ hai bằng log daemon.
- Cuộn lên đọc cũ, cho client kia gửi thêm; kiểm tra giữ vị trí và nút xuống tin mới.
- Ngắt mạng/dừng server rồi khởi động lại; kiểm tra backoff, JOIN lại, nút gửi và không tự phát lại tin.
- Từ chối/bật quyền POST_NOTIFICATIONS, bật/tắt trong hệ thống, nhận tin khi app ở nền và process còn sống.
- Nhập nickname trùng, key sai, channel bị cấm/invite-only/full, target không tồn tại, tin UTF-8 vượt giới hạn.
- Xác nhận ngắt chủ động hủy retry; mở lại sau process death phải kết nối/JOIN lại và không hiển thị lịch sử giả.

## Bằng chứng bàn giao

`output/verification/` chứa log build, kết quả JUnit XML, báo cáo lint HTML/XML, log smoke local/public và log lần thử emulator. `output/SHA256SUMS.txt` kiểm tra checksum source ZIP và APK. Các tài liệu source nằm trong `docs/`; bản APK cài được được copy riêng vào output.
