# Kiểm chứng đợt đổi tên file

Ngày 22/10/2026. Commit nền `449e06e`; nhánh `codex/readable-file-names`. Chi tiết căn cứ lecture, phạm vi và bảng 12 tên: [FILE_NAMING.md](FILE_NAMING.md).

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| Trước sửa: `assembleDebug lintDebug testDebugUnitTest` | BUILD SUCCESSFUL; 30 unit test qua; 0 lỗi lint, 8 cảnh báo |
| Sau sửa: `assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest` | BUILD SUCCESSFUL; app và APK test biên dịch được |
| Unit test | 30 test, 0 failures, 0 errors, 0 skipped: IrcClientTest 5, ExtendedCommandsTest 3, IrcParserTest 8, SessionStateTest 14 |
| Lint | 0 lỗi; đúng 8 cảnh báo nền: 6 GradleDependency, 2 NotifyDataSetChanged. ID, severity và message trùng bản trước sửa |
| AppFlowTest trên Android API 35 | `OK (2 tests)`; cả hai test hoàn tất, không bỏ qua |
| NetworkSmoke và AdvancedNetworkSmoke | 21 bước PASS qua IRC TCP thật trên daemon riêng |
| Formatter `--check` | 45 file Java, 56 file view XML, không còn khác biệt định dạng |
| Đối chiếu Java với nền | 45/45 file giữ nguyên chuỗi token sau khi chỉ thay tên class/resource trong bảng; nội dung literal không thay đổi; bỏ qua comment/whitespace khi so sánh |
| Đối chiếu XML với nền | 68/68 XML giữ nguyên canonical XML sau khi chỉ quy đổi tên resource trong bảng; bao gồm cả manifest, build/test resources và values |
| File khác ngoài Markdown | 25 file còn lại giữ nguyên nội dung sau các phép thay tên đã duyệt; tính đến chuẩn hóa CRLF của Git. Có kiểm tra binary, Gradle/scripts/assets |
| Tham chiếu cũ | Không còn trong code, test, script hoặc tài liệu hướng dẫn hiện hành; tên cũ chỉ xuất hiện có chủ đích ở bảng lịch sử đổi tên |
| `git diff --check` | Qua |

Formatter chỉ cần xuống dòng lại 2 file Java có dòng dài hơn sau khi tên lớp dài hơn: ChatAdapter và ChatFragment. Không đổi token hoặc format lại toàn bộ source ngoài phạm vi cần thiết.

## Môi trường và bảo toàn dữ liệu

- macOS Apple Silicon, JDK 17, Gradle 8.9, Android SDK/API 35.
- Worktree mới được tạo từ `origin/main`; checkout cũ vẫn giữ nhánh và trạng thái sạch ban đầu. Bản checkout ở `/Users/mathew/Documents/USTH_CONNECT` cũng được kiểm tra và không bị sửa.
- Máy ảo kiểm thử riêng `emulator-5556`, chạy bản sao AVD bằng `-read-only` và `-no-snapshot`. App chỉ được cài/xóa dữ liệu ở bản sao này. Không cài lại hoặc xóa dữ liệu `emulator-5554` đang mở.
- Ergo kiểm thử riêng tại `127.0.0.1:16667`, database mới trong thư mục làm việc. Không gửi thử vào daemon demo cổng 6667 của người dùng.
- Không thay dependency, SDK, manifest component name, applicationId, phiên bản app, SharedPreferences/key, assets hay protocol. Đây là kiểm tra hồi quy local TCP, không phải lần kiểm thử mới trên TLS công cộng hoặc điện thoại vật lý.

## Những luồng đã kiểm tra

AppFlowTest xác nhận: kết nối; chọn môn và JOIN; gửi/nhận tin kênh và PM; mở thành viên; giữ nháp khi Activity tạo lại và đổi sáng/tối; vẫn giữ kết nối và nhận tin sau đổi theme; LIST; sửa TOPIC; cấp/thu Voice; AWAY; notification khi nhắc nickname ở nền; KICK; PART.

Hai script mạng xác nhận: NICK/USER, JOIN, NAMES, tin UTF-8 trong kênh/PM, đổi nick, lỗi 401, PART/QUIT không tự gửi lại, LIST, TOPIC, quyền MODE, lỗi 482 từ server, AWAY/WHO, cấp/thu Operator/Voice, moderated channel, nhận diện nickname mention, KICK và xóa topic.

## Lệnh tái chạy

Từ thư mục gốc repo, với JDK 17 và Android SDK 35 được cấu hình:

```bash
bash ./gradlew assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest --console=plain
python3 tools/format-code.py --check
bash tools/network-smoke.sh 127.0.0.1 16667 false
git diff --check
```

Với máy ảo riêng và Ergo riêng đã sẵn sàng (điều chỉnh đường dẫn adb theo SDK trên máy):

```bash
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r \
  -e server 10.0.2.2 -e port 16667 \
  vn.edu.usth.connect.test/androidx.test.runner.AndroidJUnitRunner
```

Không chạy script sinh toàn bộ resource trên source đang dùng; đợt này chỉ cập nhật các tên resource liên quan trong script. Build, SDK, emulator, daemon, log và dữ liệu kiểm thử nằm ngoài Git. Không push hoặc merge.
