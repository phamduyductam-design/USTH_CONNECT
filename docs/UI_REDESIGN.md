# Nâng cấp giao diện USTH Connect

Ngày 22/10/2026. Nhánh riêng `codex/android-ui-redesign`, dựa trên `461bde9` (bản đổi tên file). Không thay đổi hoặc push nhánh chính.

## Khảo sát code trước khi sửa

App có một module Android, Java 17, XML Views, AndroidX Fragment/RecyclerView và Material Components. AGP 8.7.3, Gradle 8.9, minSdk 24, compile/target SDK 35; giữ nguyên cấu hình và dependency. Không có Compose hoặc ViewModel trong kiến trúc hiện tại.

`ConnectApplication` sở hữu `SettingsStore`, `CourseRepository`, `SessionRepository`. `ConnectActivity` đọc cấu hình đã lưu và yêu cầu kết nối; chỉ chuyển sang MainActivity khi server xác nhận đăng ký. MainActivity điều hướng Fragment bằng FragmentManager/back stack. SessionAwareFragment đăng ký/hủy listener theo onStart/onStop.

SessionRepository dùng Handler main thread nhận callback từ IrcClient. Socket/TLS, parser, command builder, epoch/reconnect, trạng thái JOIN/PART/NAMES/LIST/WHO/MODE/TOPIC, trạng thái gửi và xử lý notification đã có sẵn. UI tiếp tục gọi các API này; không viết lại mạng. SettingsStore giữ nickname, host/port/TLS, nháp và lựa chọn theme bằng đúng các khóa cũ.

Danh mục bốn môn và tên kênh là metadata cấu hình trong `courses.json`. Số kênh cấu hình có thể khác ảnh demo và được giữ nguyên. Các tin nhắn, thành viên, topic, unread, quyền và danh sách phòng lấy từ IRC thật. Các công cụ demo/bot trong tools là tiện ích phát triển riêng, không phải nguồn dữ liệu của production.

## Thiết kế và các màn hình

Tham chiếu trực tiếp tám ảnh `01_Ket_noi` đến `08_Ca_nhan` trong bộ `USTH_Connect_Redesign_390x844`. Giữ giao diện mobile từng màn hình, không thêm bố cục desktop nhiều cột.

- Màu chính #2E3B8F, đỏ USTH #EF2028, nền #F5F7FB, surface trắng, chữ #19233C/#667085, đường viền #E5E9F2. Chấm kết nối dùng #12B76A; chữ trạng thái xanh đậm hơn để đọc rõ trên nền sáng. Dark mode có palette riêng và giữ lựa chọn đã lưu.
- Font sans hệ thống hỗ trợ tiếng Việt; tiêu đề 26sp, section 18sp, body 16sp, supporting 14sp, timestamp/nav 12sp. Bo góc 12–16dp; các nút và vùng thao tác chính tối thiểu 48dp. Hạn chế shadow.
- Logo JPG được sao chép nguyên bản từ asset được cung cấp vào drawable-nodpi, giữ tỷ lệ bằng fitCenter; SHA-256 trùng file gốc. Không vẽ lại logo, không đổi màu. Chỉ dùng logo lớn ở màn Kết nối.

| Màn hình | Triển khai |
|---|---|
| Kết nối | Logo, giới thiệu, card biệt danh/máy chủ; nâng cao cho port/TLS/tên IRC; tóm tắt cấu hình thực; loading, lỗi, thử lại, hủy. Không thay host mặc định hoặc ép 10.0.2.2 |
| Môn học | Avatar thực từ nickname; tìm kiếm; banner kênh đã tham gia; ưu tiên môn đã JOIN; card môn khác; nhóm Kênh khác; empty state có hành động; đường vào phòng công khai |
| Kênh môn | Giữ nguyên #channel thật; mô tả môn; tách đã tham gia/có thể tham gia; JOIN qua dialog thật; mở chat cho kênh đã tham gia; PART và xác nhận vẫn ở menu chat |
| Chat kênh | Header/topic/thành viên; avatar, nickname, thời gian, bubble; nhóm tin liên tiếp cùng người trong 5 phút, ngắt nhóm ở ngày mới/unread/sự kiện; mốc ngày và tin chưa đọc |
| Thành viên | Bottom sheet tìm kiếm, NAMES/WHO và vai trò thật; nút nhắn riêng ngay từng hàng; chọn người để hiện quản trị; không tạo trạng thái online ngẫu nhiên |
| Danh sách DM | Hội thoại thật sắp theo tin mới nhất, nickname, preview, timestamp, unread; tìm kiếm; nút tạo hội thoại và empty state có hướng xử lý |
| Chat DM | Tin nhận bên trái, tin gửi bên phải; bubble và timestamp; cùng composer với chat kênh; không có dấu đã đọc hoặc nút attachment giả |
| Cá nhân | Profile card, trạng thái thật, host:port và số kênh JOIN; đổi nick, thông báo/quyền Android, mô tả reconnect có sẵn, đổi theme, AWAY, chi tiết kết nối, ngắt có xác nhận |

Nút dấu cộng ở composer mở hướng dẫn các lệnh IRC đã hỗ trợ. Không giả lập upload, reaction, gọi thoại/video. Tự reconnect vốn có trong transport được trình bày dưới dạng thông tin; không tạo một switch cài đặt không được engine hỗ trợ.

## Điều hướng và bàn phím

Chỉ có ba tab **Môn học | Tin nhắn | Cá nhân**. Phòng công khai nằm trong **Môn học → Phòng trên server**; `/list` cũng mở cùng màn hình. Back stack giữ đường quay lại. Chat kênh đánh dấu tab Môn học, DM đánh dấu tab Tin nhắn. Đổi trạng thái checked của menu không thay Fragment đang chat.

Thanh tab hiện khi duyệt và ẩn lúc bàn phím mở để chừa chỗ nhập. WindowInsetsHelper dùng system bars/IME để đặt padding root. Composer multiline tối đa 5 dòng, giữ nháp và không bị IME che. Tin mới chỉ kéo xuống cuối nếu người đọc đang ở cuối; nếu đang đọc lịch sử thì giữ anchor và có nút xuống cuối.

## Trạng thái và bảo toàn chức năng

| Trạng thái | Cách xử lý |
|---|---|
| Chưa kết nối | Form cấu hình đã lưu; không gán địa chỉ trường đại học từ mockup |
| Đang kết nối/đăng ký/reconnect | Loading và thông tin thực từ repository; khóa input; có hủy |
| Kết nối thành công | Vào MainActivity khi CONNECTED; số liệu lấy từ phiên thật |
| Cấu hình lỗi/kết nối thất bại | Panel lỗi, thông báo phù hợp và thử lại |
| Mất kết nối/chưa JOIN | Banner, kết nối lại hoặc JOIN; chặn gửi tin thường khi chưa sẵn sàng |
| Chưa tham gia môn | Lời nhắc gọn, danh mục và CTA khám phá/nhập kênh |
| Kênh chưa có tin | Empty state cùng composer; không chèn tin mẫu |
| Chưa có PM | Empty state và nút Bắt đầu trò chuyện |
| Đang gửi | Hiển thị QUEUED theo callback thực |
| Gửi thất bại/không rõ kết quả | FAILED/UNCERTAIN và gửi lại có xác nhận; không khẳng định người nhận đã đọc |

Giữ nguyên bytes source của `irc/`, `repository/`, `model/`, `storage/`, `ConnectApplication`, manifest, assets/courses.json và Gradle. Không đổi applicationId, versionCode/versionName, server mặc định, khóa preferences, dữ liệu người dùng hoặc giao thức.

## File thêm/sửa

| Nhóm | File |
|---|---|
| Routing/insets/helper | `ui/MainActivity.java`, `WindowInsetsHelper.java`, `UiHelpers.java`; `layout/activity_main.xml`; `menu/navigation.xml`; icon `ic_courses.xml` |
| Kết nối/cá nhân | `ConnectActivity.java`, `ProfileFragment.java`; `activity_connect.xml`, `fragment_profile.xml`; logo JPG nguyên bản; `bg_connection_card`, `bg_profile_highlight`, `bg_profile_online` |
| Danh sách/thành viên | `CatalogAdapter.java`, `CoursesFragment.java`, `CourseChannelsFragment.java`, `ServerChannelsFragment.java`, `ConversationsFragment.java`, `MembersBottomSheet.java`; `fragment_catalog`, `dialog_members`, `item_catalog`, `item_header`, `item_empty`; `bg_catalog_*` |
| Chat | `ChatFragment.java`, `ChatAdapter.java`; `fragment_chat`, `item_chat_message`, `item_chat_separator`; `bg_chat_*` |
| Design system | `values/colors.xml`, `values-night/colors.xml`, `dimens.xml`, `themes.xml`, `strings.xml`; ba file strings riêng cho catalog/chat/kết nối-cá nhân |
| Kiểm chứng/tài liệu | `AppFlowTest.java`; `README.md`, `ARCHITECTURE.md`, `CODE_MAP.md`, tài liệu này. Generator resource cũ được ghi rõ là mẫu lịch sử, không chạy lại để ghi đè UI mới |

Các drawable/string/dimen/style không còn được app sử dụng sau redesign đã được bỏ để tránh resource cũ lẫn với component mới. Không xóa chức năng IRC.

## Kiểm chứng thực tế

- Build: `assembleDebug`, `assembleDebugAndroidTest` thành công.
- Unit test JVM: 30, không lỗi hoặc bỏ qua.
- LintDebug: 0 lỗi; 8 cảnh báo nền (6 GradleDependency, 2 NotifyDataSetChanged), không thêm cảnh báo layout/accessibility/resource thừa.
- Android API 35, kích thước tham chiếu 390×844dp: 3 instrumentation test qua. Hai luồng cũ tiếp tục xác nhận JOIN/chat/PM, khôi phục Activity/nháp, đổi theme, LIST, TOPIC, NAMES/WHO, cấp/thu Voice, AWAY, notification nhắc tên, KICK, PART. Kiểm tra thêm nhóm tin liên tiếp, ba tab, empty PM → tạo PM, multiline/IME, đổi nickname qua GUI và ngắt có xác nhận.
- Chạy thêm bài ba tab/PM/bàn phím/đổi nick/ngắt trên 360×740dp, cỡ chữ hệ thống 115%: `OK (1 test)`. Không thay đổi màn hình hoặc cỡ chữ của emulator người dùng.
- NetworkSmoke và AdvancedNetworkSmoke: 21 bước PASS qua IRC TCP thật trên Ergo riêng. Không dùng bot phản hồi giả để thay kiểm tra giao thức.
- Logo JPG trùng SHA-256 bản người dùng; engine/repository/model/storage/assets/Gradle/manifest không có diff.
- Formatter và `git diff --check` qua. Screenshot lấy trực tiếp từ máy ảo trong các test, không phải render lại mockup.

Máy ảo kiểm thử là bản sao read-only `emulator-5556`; daemon riêng `127.0.0.1:16667` và database riêng. Không cài lại hoặc xóa app/dữ liệu ở emulator-5554 đang mở của người dùng.

## Chạy trên Android Emulator

1. Mở thư mục có `settings.gradle` bằng Android Studio, chọn JDK 17, cài SDK 35, chọn hoặc tạo một AVD rồi Run. Không cần đổi framework hoặc dependency.
2. Hoặc cấu hình JAVA_HOME/ANDROID_HOME và chạy:

```bash
bash ./gradlew assembleDebug lintDebug testDebugUnitTest
adb -s <ma-may-ao> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <ma-may-ao> shell am start -n vn.edu.usth.connect/.ui.ConnectActivity
```

3. Nhập server thật. Khi chạy Ergo trên máy host, Emulator dùng `10.0.2.2`, port của daemon (ví dụ 6667), tắt TLS chỉ nếu daemon đó dùng TCP thường. Không đặt địa chỉ này cho điện thoại vật lý. Server/TLS/port đang lưu được giữ nguyên.
4. Môn học → chọn môn → Tham gia; hoặc Phòng trên server để LIST. Một client IRC thứ hai gửi tin sẽ hiện trực tiếp trong kênh/PM.

Kiểm thử UI cần daemon thật riêng đã chạy:

```bash
bash ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.server=10.0.2.2 \
  -Pandroid.testInstrumentationRunnerArguments.port=16667
```

Có thể thêm `-Pandroid.testInstrumentationRunnerArguments.screenshots=true` để chụp vào thư mục `redesign-screenshots` của app external files trên máy thử.

## Giới hạn giữ nguyên

IRC cơ bản không cung cấp xác nhận đã đọc. Bản này chưa thêm PASS/SASL/NickServ UI, upload, reaction, gọi thoại/video hoặc lịch sử bền vững mới. Lịch sử vẫn giới hạn trong bộ nhớ process như trước, nháp/cấu hình/theme lưu bằng SettingsStore. Trạng thái người dùng phụ thuộc WHO/away-notify server hỗ trợ. Lần kiểm chứng này dùng TCP localhost và máy ảo API 35, không phải lần kiểm thử mới TLS công cộng hoặc điện thoại thật.
