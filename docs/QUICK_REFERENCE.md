# Khi thầy hỏi, mở file nào?

Bắt đầu từ thư mục `app/src/main/java/vn/edu/usth/connect/`. Tên file được giữ theo vai trò Java: Activity, Fragment, Adapter, Repository. `ui/` đã được chia theo chức năng; dữ liệu và IRC nằm ở các package riêng.

| Câu hỏi / chức năng | Mở file trước | Theo tiếp đến đâu? |
|---|---|---|
| App bắt đầu ở đâu? | `ConnectApplication.java`, `ui/ConnectActivity.java` | `app/src/main/AndroidManifest.xml`; `repository/SessionRepository.connect()` |
| Vì sao giao diện không giữ socket? | `repository/SessionRepository.java` | Application giữ một session; `ui/common/SessionAwareFragment.java` đăng ký/hủy listener theo lifecycle |
| Môn học và kênh lấy ở đâu? | `ui/courses/CoursesFragment.java`, `CourseChannelsFragment.java` | `repository/CourseRepository.java` → `app/src/main/assets/courses.json` |
| Danh sách phòng thật từ server? | `ui/courses/ServerChannelsFragment.java` | `SessionRepository.listChannels()` → `SessionState.handleListEntry()` |
| Gửi tin nhắn đi đường nào? | `ui/chat/ChatFragment.send()` | `SessionRepository.send()` → `irc/IrcCommands.message()` → `irc/IrcClient.send()` |
| Nhận và hiển thị tin nhắn? | `irc/IrcClient.java`, `irc/IrcParser.java` | `SessionState.handleChatMessage()` → `adapter/ChatAdapter.java` → `res/layout/item_chat_message.xml` |
| Tin nhắn riêng và danh sách hội thoại? | `ui/chat/ConversationsFragment.java` | `model/Conversation.java`; `ui/dialog/UserDialogs.showNewConversation()` |
| Thành viên và vai trò? | `ui/members/MembersBottomSheet.java` | `SessionRepository.refreshNames()` → `SessionState.handleNamesBatch()` / `handleNamesComplete()` |
| Quản trị phòng, TOPIC, MODE, KICK? | `ui/dialog/ChannelDialogs.java`, `UserDialogs.java` | `SessionRepository.topic()` / `mode()` / `kick()`; quyền và kết quả do server quyết định |
| Nickname, trạng thái, đăng xuất? | `ui/profile/ProfileFragment.java` | `SessionRepository.changeNick()` / `setAway()` / `disconnect()` |
| Thông báo và nhắc nickname? | `ConnectApplication.java` | `irc/NicknameMentions.java`; notification channels `messages`, `mentions` |
| Sáng/tối và nháp lưu ở đâu? | `ui/common/ThemeMode.java`, `UiHelpers.themeToggle()` | `storage/SettingsStore.java`; `res/values-night/`; nháp do `ChatFragment` đọc/ghi |
| Bàn phím, thanh hệ thống và ba tab? | `ui/common/WindowInsetsHelper.java`, `ui/MainActivity.java` | `res/layout/activity_main.xml`; `res/menu/navigation.xml` |
| Logo trong suốt và giao diện XML? | `res/layout/view_app_brand.xml`, `activity_connect.xml` | `res/drawable-nodpi/usth_university_logo.png`; layout/theme không thay trong đợt gom package |
| Cập nhật app có khôi phục màn hình cũ được không? | `ui/common/FeatureFragmentFactory.java` | Ánh xạ 7 tên Fragment cũ; MainActivity cài factory trước `super.onCreate()` |
| Bằng chứng app hoạt động? | `app/src/test/.../irc/`, `repository/SessionStateTest.java` | `app/src/androidTest/.../AppFlowTest.java`, `FragmentCompatibilityTest.java`; `tools/network-smoke.sh` |

`res/...` trong bảng tính từ `app/src/main/`. Với file Java ghi ngắn sau file đầu tiên, giữ cùng package của file đầu. Unit test nằm ở `app/src/test/java/vn/edu/usth/connect/`; Android test nằm ở `app/src/androidTest/java/vn/edu/usth/connect/`.

## Ba đường đi nên nhớ

1. **Mở app:** Manifest → ConnectApplication → ConnectActivity → SessionRepository → IrcClient; nhận numeric `001` rồi vào MainActivity.
2. **Gửi/nhận:** ChatFragment → Repository → IRC; chiều về IRC → SessionState → listener → Fragment/Adapter. Ghi socket thành công chưa xác nhận người nhận đã đọc.
3. **Hiển thị/lưu:** `ui/` và `res/` lo hiển thị; `storage/SettingsStore` lưu cấu hình/nháp; lịch sử chat trong `model/` ở RAM, không phải database.

## Tìm nhanh

- Android Studio: chọn chế độ **Project** để nhìn cây folder thật; mở `ui/courses`, `ui/chat`, `ui/common` theo bảng.
- VS Code: `Cmd+P` tìm tên lớp; `Cmd+Shift+F` tìm tên hàm hoặc view ID. `.tools/`, `.gradle/`, `build/` được ẩn vì chỉ là cache/runtime.
- `.vscode/` là cấu hình VS Code; `gradle/`, `gradlew`, `settings.gradle`, `build.gradle` là điểm vào build. Giữ chúng ở đúng vị trí để IDE/Gradle nhận dự án. Code ứng dụng nằm trong `app/src/main/java`, không phải trong các thư mục cấu hình này.

Bản đồ đầy đủ: [CODE_MAP.md](CODE_MAP.md). Kịch bản bảo vệ: [DEFENSE.md](DEFENSE.md). Căn cứ lecture và giới hạn suy luận: [FILE_NAMING.md](FILE_NAMING.md).
