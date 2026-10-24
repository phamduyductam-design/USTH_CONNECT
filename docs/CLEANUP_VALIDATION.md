# Kiểm tra đợt làm sạch code

Ngày 22/10/2026. Nền: commit `86c25f4`, app versionName 1.2 / versionCode 3. Nhánh làm việc: `refactor-readable-structure`.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug + assembleDebugAndroidTest | Thành công |
| lintDebug | 0 lỗi; 8 cảnh báo có sẵn |
| testDebugUnitTest | 30 test, 0 lỗi |
| Android instrumentation | 2 test, `OK (2 tests)` |
| NetworkSmoke + AdvancedNetworkSmoke | 21 bước PASS qua IRC TCP thật |
| tools/format_code.py --check | 45 file Java và 56 file XML; không còn khác biệt định dạng |
| So sánh canonical XML với commit nền | 56/56 tài liệu view giữ nguyên nội dung |
| git diff --check | Không có lỗi whitespace |

## Môi trường và cách chạy

- macOS Apple Silicon, JDK 17, Gradle 8.9, SDK/API 35.
- Một emulator headless riêng `emulator-5556`, AVD `usth_refactor_test`; không cài lại hay xóa dữ liệu emulator-5554 đang mở của người dùng.
- Một Ergo riêng trên 127.0.0.1:6669, database riêng. Không dùng cổng 6667 hay bot demo của phiên người dùng để gửi tin kiểm thử.
- AppFlowTest nhận tham số `server` và `port`; mặc định vẫn 10.0.2.2:6667 như trước. Bổ sung tham số port để chọn server kiểm thử riêng, không thay đổi app sản phẩm.

```bash
bash ./gradlew assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest --console=plain
python3 tools/format_code.py --check
bash tools/network-smoke.sh 127.0.0.1 6669 false

# Khi emulator-5556 đã sẵn sàng và có test server riêng:
.tools/android-sdk/platform-tools/adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
.tools/android-sdk/platform-tools/adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
.tools/android-sdk/platform-tools/adb -s emulator-5556 shell am instrument -w -r \
  -e server 10.0.2.2 -e port 6669 \
  vn.edu.usth.connect.test/androidx.test.runner.AndroidJUnitRunner
```

Các lệnh build/formatter cần JAVA_HOME trỏ tới JDK 17 hoặc java 17 trong PATH. Test server và AVD phải được chuẩn bị trước; các lệnh trên không tự tạo server/máy ảo.

## Những luồng được xác nhận sau khi tách code

- JOIN theo môn, gửi tin kênh và PM với nội dung có xuống dòng.
- Giữ nháp sau tái tạo Activity; đổi sáng/tối vẫn giữ kết nối và nhận tin thật.
- Danh sách thành viên/NAMES; LIST, TOPIC, cấp/thu quyền, KICK.
- AWAY/WHO và thông báo nhắc tên khi ở nền.
- Test đơn vị cho parser, byte limit, chặn chèn lệnh, CAP, state, MODE trong lúc NAMES và xử lý lỗi numeric.

Log của lần chạy đạt: `.tools/refactor/checks.log`, `android-tests.log`, `network-tests.log`. Log, SDK, AVD, database và formatter JAR nằm ngoài Git. Đây là kiểm tra hồi quy local, không phải tuyên bố kiểm thử mới trên hai điện thoại thật hay TLS công cộng.
