# Bản đồ mã nguồn USTH Connect

Tài liệu này dành cho người mới đọc dự án. Các đường dẫn trong bảng tính từ thư mục gốc có `settings.gradle`.

Tên file Java dùng PascalCase, XML dùng snake_case với tiền tố theo loại view. Xem [căn cứ lecture và bảng đổi tên](FILE_NAMING.md): lecture có ví dụ Activity/Fragment/Adapter và layout, nhưng không quy định đầy đủ mọi tiền tố hoặc cấu trúc package.

## Nên đọc theo thứ tự nào?

1. `app/src/main/AndroidManifest.xml`: app chạy Activity nào, quyền nào được khai báo.
2. `ConnectApplication`: tạo SettingsStore, CourseRepository và một SessionRepository dùng chung trong process.
3. `ui/ConnectActivity`: đọc nickname/server rồi gọi `session.connect()`.
4. `repository/SessionRepository`: các thao tác app có thể làm, callback từ mạng và cách báo thay đổi cho UI.
5. `irc/IrcClient` → `IrcParser` → `repository/SessionState.accept()`: kết nối, phân tích dòng IRC, cập nhật dữ liệu.
6. Chọn tính năng trong bảng bên dưới để đọc Fragment, dialog, adapter và layout tương ứng.

Trong các mục Java bên dưới, tiền tố chung là `app/src/main/java/vn/edu/usth/connect/`. Các đường dẫn rút gọn `res/...`, `assets/...` tính từ `app/src/main/`.

## Muốn sửa tính năng nào thì mở đâu?

| Tính năng | Điểm bắt đầu trong Java | Giao diện / dữ liệu liên quan |
|---|---|---|
| Đăng nhập/kết nối IRC | `ui/ConnectActivity.connect()` → `SessionRepository.connect()` | `res/layout/activity_connect.xml` |
| Kết nối TCP/TLS, CAP, PING/PONG, reconnect | `irc/IrcClient` | `model/ConnectionConfig`, `ConnectionState` |
| Đọc một dòng IRC | `irc/IrcParser.parse()`; dữ liệu `IrcMessage` | `irc/IrcClient` gọi parser |
| Danh mục môn học | `ui/CoursesFragment`, `repository/CourseRepository` | `assets/courses.json`; `fragment_catalog.xml` |
| Kênh thuộc một môn | `ui/CourseChannelsFragment` | `model/Course`; `fragment_catalog.xml` |
| Phòng công khai từ server | `ui/ServerChannelsFragment` → `SessionRepository.listChannels()` | `SessionState.handleListEntry()`; `model/ServerChannel` |
| Tham gia/rời kênh | `ui/dialog/ChannelDialogs.showJoin()` / `showMenu()` | `dialog_join_channel.xml`; `SessionRepository.join()` / `part()`; `SessionState.handleJoin()` / `handlePart()` |
| Gửi tin nhắn | `ui/ChatFragment.send()` → `SessionRepository.send()` → `IrcCommands.message()` | `res/layout/fragment_chat.xml`; `model/ChatMessage` |
| Nhận tin kênh hoặc PM | `SessionState.handleChatMessage()` | `model/Conversation`, `Channel`; `adapter/ChatAdapter`; `item_chat_message.xml`, `item_chat_separator.xml`; `bg_outgoing_message.xml` cho tin gửi đi |
| Lệnh `/join`, `/msg`, `/topic`… | `irc/SlashCommand.parse()` → `SessionRepository.execute()` | `ChatFragment.sendSlashCommand()`; hướng dẫn trong `CommonDialogs.showCommandHelp()` |
| Hiển thị header, mất kết nối, lịch sử chat | `ChatFragment.renderHeader()` / `renderConnectionState()` / `renderMessages()` | `fragment_chat.xml`; `adapter/ChatAdapter` |
| Nháp và cuộn hội thoại | `ChatFragment.syncTargetAndDraft()` / `onSaveInstanceState()` | `storage/SettingsStore.draft()` |
| Thành viên, vai trò và NAMES | `ui/MembersBottomSheet` → `SessionRepository.refreshNames()` | `SessionState.handleNamesBatch()` / `handleNamesComplete()`; `dialog_members.xml` |
| TOPIC và chế độ phòng | `ui/dialog/ChannelDialogs.showMenu()` / `showRoomModes()` | `SessionRepository.topic()` / `mode()`; `SessionState.applyModes()` |
| Cấp/thu quyền hoặc KICK | `ui/dialog/UserDialogs.showMemberActions()` | `SessionRepository.mode()` / `kick()`; `SessionState.handleKick()` |
| Đổi nickname | `ui/ProfileFragment` → `SessionRepository.changeNick()` | `SessionState.handleNicknameChange()` |
| Vắng mặt / WHO | `UserDialogs.showAway()` / `showPrivateInfo()` | `SessionRepository.setAway()` / `refreshWho()`; `SessionState.handleWhoReply()` |
| Danh sách PM, tạo PM | `ui/ConversationsFragment`; `UserDialogs.showNewConversation()` | `model/Conversation`; `fragment_catalog.xml` |
| Nhắc nickname và thông báo | `irc/NicknameMentions`; `ConnectApplication` | `ChatAdapter`; notification channels `messages`, `mentions` |
| Sáng/tối | `ui/UiHelpers.themeToggle()` → `ThemeMode.apply()` | `storage/SettingsStore.darkMode()`; `res/values-night/`; `view_theme_toggle.xml` |
| Điều hướng, back stack | `ui/MainActivity` | `activity_main.xml`; `res/menu/navigation.xml`; `res/color/navigation_item_tint.xml` |
| Xác nhận / nhập liệu dùng chung | `ui/dialog/CommonDialogs` | `dialog_confirm.xml`, `dialog_input.xml` |
| Listener/lifecycle dùng chung cho Fragment | `ui/SessionAwareFragment` | Lớp nền trừu tượng; đăng ký/hủy listener phiên theo onStart/onStop |
| Khoảng chừa thanh hệ thống/bàn phím | `ui/WindowInsetsHelper.apply()` | Dùng ở `ConnectActivity` và `MainActivity`; phân biệt với kiểu giá trị `androidx.core.graphics.Insets` |
| Danh sách, avatar, icon, màu dùng chung | `adapter/CatalogAdapter`, `ui/UiHelpers` | `item_catalog.xml`; `res/drawable/`; `res/values/` |
| Chạy demo trên Mac | `Chay-demo.command` → `tools/run-android.sh` → `tools/start-demo.py` | SDK, máy ảo và Ergo được chuẩn bị trong `.tools/` |
| Client thứ hai nhập từ terminal | `tools/interactive-peer.sh` → `InteractivePeer.java` | Dùng cùng engine `IrcClient`; không tự trả lời |
| Bot echo minh họa | `tools/demo-bot.py` | Công cụ demo riêng, không phải xử lý nhận tin của app |

## Trách nhiệm từng thư mục

| Thư mục | Chứa gì? | Khi nào sửa? |
|---|---|---|
| `ui/` | Activity, Fragment, binding view, thao tác người dùng | Thay màn hình, điều hướng hoặc cách hiển thị |
| `ui/dialog/` | `ChannelDialogs`, `UserDialogs`, `CommonDialogs` | Thay dialog/menu theo nhóm tính năng |
| `adapter/` | Gắn dữ liệu vào RecyclerView | Thay hàng tin nhắn hoặc hàng danh mục |
| `repository/` | SessionRepository điều phối; SessionState nhận sự kiện; CourseRepository đọc metadata | Thay thao tác hoặc cập nhật trạng thái |
| `irc/` | Socket, parser, builder, slash command và nhận diện mention | Thay giao tiếp IRC; không đặt View Android ở đây |
| `model/` | Dữ liệu kênh, người dùng, hội thoại, cấu hình | Thêm/sửa trường dữ liệu |
| `storage/` | SharedPreferences cho cấu hình, theme và nháp | Thay thông tin lưu cục bộ |
| `res/layout/` | Bố cục màn hình, dialog và từng hàng danh sách | Thay vị trí, kích thước, view ID |
| `res/values/`, `values-night/`, `values-v27/` | Chuỗi, màu, style và biến thể theo theme/API | Thay câu chữ hoặc bảng màu |
| `res/drawable/`, `drawable-nodpi/` | Vector/background và logo gốc | Thay icon/logo/background |
| `assets/` | Danh mục môn/kênh cục bộ | Thay metadata; không thay thế kết quả LIST từ server |
| `src/test/` | Unit test chạy bằng JVM | Kiểm tra parser, builder, state và transport fixture |
| `src/androidTest/` | Kiểm thử UI trên Android với IRC server thật | Kiểm tra luồng người dùng và lifecycle |
| `tools/` | Chạy demo, kiểm tra mạng, formatter | Công cụ hỗ trợ phát triển |
| `docs/` | Kiến trúc, hướng dẫn, báo cáo kiểm thử, nhận xét code | Tra cứu hoặc cập nhật giải thích |
| `.tools/`, `.gradle/`, `build/` | Runtime, cache, log và kết quả build | Không phải source; đã ẩn khỏi Explorer/search mặc định |

## Luồng gửi và nhận một tin nhắn

**Gửi:** `ChatFragment.send()` → `SessionRepository.send()` → `IrcCommands.message()` kiểm tra đầu vào → `IrcClient.send()` ghi socket → callback cập nhật trạng thái WRITTEN/FAILED/UNCERTAIN → `notifyListeners()` → Fragment render.

**Nhận:** socket → `IrcParser.parse()` → `SessionRepository.onMessage()` chuyển về `mainHandler` → `SessionState.accept()` chọn handler → `handleChatMessage()` thêm vào Conversation → Fragment/adapter hiển thị; Application quyết định có thông báo ở nền hay không.

State về JOIN, MODE, TOPIC… đến từ server. Bấm nút hoặc ghi socket thành công không đồng nghĩa server đã chấp nhận thao tác.

## Các tên dễ nhầm

- `target`: tên kênh bắt đầu bằng `#`/`&`, hoặc nickname khi chat riêng.
- `IRC`: Internet Relay Chat; `IrcClient` quản lý kết nối, `IrcParser` phân tích dòng, `IrcCommands` dựng lệnh; `NicknameMentions` tìm nhắc nickname.
- `CourseChannelsFragment`: kênh thuộc một môn từ danh mục cục bộ; `ServerChannelsFragment`: phòng công khai trả về từ lệnh LIST.
- `CatalogAdapter`/`fragment_catalog.xml`: danh sách dùng chung cho nhiều màn hình; không chỉ dành cho môn học. `ChatAdapter` dùng `item_chat_message.xml` và `item_chat_separator.xml` cho lịch sử chat.
- `epoch`: thế hệ kết nối; callback mang epoch cũ bị bỏ để không ghi vào phiên mới.
- `wanted`: người dùng muốn tham gia lại; `joined`: server đã xác nhận đang ở kênh; `joining`: đang chờ phản hồi JOIN.
- `namesBatch`: gom nhiều gói NAMES; `namesRemoved`: tránh đưa lại người đã PART/QUIT trong lúc đang tải.
- `senderKey`: nickname đã fold theo quy tắc IRC để làm khóa map; `senderNickname`: nickname để hiển thị.
- `MAX_HISTORY_MESSAGES`, `JOIN_TIMEOUT_MS`, `NAMES_TIMEOUT_MS`, `LIST_TIMEOUT_MS`: giới hạn có tên, không phải số tùy ý rải trong code.

## Cách tìm bằng VS Code hoặc terminal

VS Code: `Cmd+P` tìm tên file, `Cmd+Shift+F` tìm tên hàm/view ID/chuỗi hiển thị. Các file Java dùng tên tiếng Anh; phần chữ người dùng thấy nằm trong `res/values/strings.xml`.

```bash
rg -n 'handleNamesBatch|refreshNames' app/src/main/java
rg -n 'theme_toggle|darkMode' app/src/main
rg -n 'showMemberActions|kick_member' app/src/main
rg -n 'R.id.composer|@\+id/composer' app/src/main
```

## Giữ code sạch khi sửa tiếp

Chạy từ folder gốc, cần Python 3 và JDK 17:

```bash
python3 tools/format-code.py          # Chuẩn hóa Java và XML của view
python3 tools/format-code.py --check  # Kiểm tra, không sửa source
```

Lần đầu cần mạng để tải google-java-format 1.22.0; file JAR được giữ trong `.tools/` và kiểm tra SHA-256 trước khi chạy. Có cache rồi thì không cần tải lại. Dùng bản cố định để mọi máy tạo kết quả giống nhau; không thêm plugin Gradle hay thay dependency của app.

Khi thêm tính năng: bắt đầu từ bảng chức năng, giữ xử lý mạng/state ngoài Fragment, đặt tên theo thao tác cụ thể, rồi cập nhật test và bảng này. Đọc thêm [kiến trúc](ARCHITECTURE.md), [chức năng IRC](IRC_FEATURES.md) và [nhận xét đợt làm sạch](CODE_REVIEW.md).
