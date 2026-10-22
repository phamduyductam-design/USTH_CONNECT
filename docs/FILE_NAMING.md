# Rà soát tên file USTH_CONNECT

Ngày 22/10/2026. Nền: `449e06e` (`origin/main` tại lúc bắt đầu). Nhánh riêng: `codex/readable-file-names`. Phiên bản app vẫn 1.2 / versionCode 3.

## Căn cứ lecture MOBI

Đợt này đã đọc 5 PDF có sẵn trong Downloads: `1. intro.md.pdf` (19 trang PDF), `3. intro_android.md.pdf` (37), `4. fundamentals.md.pdf` (65), `5. resources.md.pdf` (101), `6. tasks.md.pdf` (42). Các file có slide lặp theo animation nên **trang PDF** có thể khác **số slide in ở chân trang**. Bảng dưới ghi cả hai. Không tìm thấy bản gốc hai lecture networking mà tài liệu cũ `COURSE_MAPPING.md` nhắc tới; không dùng lời mô tả cũ làm bằng chứng về quy tắc đặt tên.

| Lecture / vị trí | Ví dụ hoặc nội dung đã đọc | Cách áp dụng và giới hạn |
|---|---|---|
| `1. intro`, PDF 13–15, slide 13–15/19 | Phần trình bày cần giải thích Architecture, Activities và Networking | Bản đồ code chỉ ra thành phần và chức năng; đây không phải quy định tên file |
| `3. intro_android`, PDF 21, slide 14/24; PDF 31, slide 18/24 | Decomposition, tách UI và chức năng; kiến thức OOP/package | Giữ các package hiện có; lecture không bắt buộc cây thư mục `ui/adapter/repository/irc/model/storage` |
| `4. fundamentals`, PDF 21–22, slide 21–22/65 | `MyApplication extends Application` | Ví dụ tên lớp có hậu tố vai trò; giữ `ConnectApplication` |
| `4. fundamentals`, PDF 25 và 36, slide 25 và 36/65 | Package `vn.edu.usth.weather`; `WeatherActivity`; bài tập nhắc cập nhật manifest khi bỏ `MainActivity` | Dùng tên có nghĩa và hậu tố `Activity`; kiểm tra manifest. Không đổi package ứng dụng hay Activity đã rõ |
| `4. fundamentals`, PDF 52, slide 52/65 | `ForecastFragment`, ví dụ `DetailFragment` | Giữ hậu tố `Fragment`; `CourseChannelsFragment` nói rõ kênh thuộc môn học |
| `5. resources`, PDF 14, slide 13/92 | `R.layout.activity_layout`, `R.layout.fragment_layout` | Ví dụ cụ thể về tên XML chữ thường, gạch dưới, tiền tố theo loại màn hình |
| `5. resources`, PDF 41, slide 40/92 | `HomeFragmentPagerAdapter` và constructor cùng tên | Giữ hậu tố `Adapter`; cập nhật constructor đồng bộ với tên lớp khi có đổi |
| `5. resources`, PDF 53–59, slide 50–56/92 | `values/strings.xml`, `colors.xml`, `dimens.xml`, `fragment_weather.xml`, truy cập qua `R` | Giữ file values chuẩn và các biến thể cấu hình; đổi tên resource phải cập nhật `R` và XML sử dụng |
| `5. resources`, PDF 62, slide 59/92; PDF 69–72, slide 66–69/92 | `res/drawable`, `res/raw`, `assets` có vai trò khác nhau | Giữ đúng thư mục resource; không đổi `courses.json` hay các giá trị dữ liệu |
| `6. tasks`, PDF 9–12, slide 9–12/30 | Main/worker thread; ví dụ `WeatherActivity` | Chỉ đối chiếu trách nhiệm UI/mạng; không thay cơ chế thực thi hay đưa AsyncTask vào app |

**Kết luận:** các lecture có ví dụ phù hợp PascalCase và hậu tố Android, nhưng không đưa ra một bộ quy ước đầy đủ, thống nhất cho mọi loại file Java/XML, repository/helper hay kiến trúc package. Không gán cho thầy một phong cách chưa được chứng minh. Các tiền tố `dialog_`, `item_`, `view_`, `ic_`, `bg_` và tên helper trong đợt này là quy ước thông dụng được chọn cho dự án, đồng thời thực hiện yêu cầu của người dùng.

Java dùng PascalCase, tên file trùng tên lớp; hậu tố mô tả vai trò khi phù hợp. Đối chiếu [Google Java Style, Class names](https://google.github.io/styleguide/javaguide.html#s5.2.2-class-names). XML dùng snake_case và tiền tố mô tả loại view: `activity_`, `fragment_`, `dialog_`, `item_`, `view_`; drawable giữ `ic_`/`bg_`. Đây là lựa chọn nhất quán cho repo, không phải mọi tiền tố đều là yêu cầu của Android. [Android app resources](https://developer.android.com/guide/topics/resources/providing-resources) giải thích thư mục resource, resource ID và các file values riêng theo loại.

## Cấu trúc đã khảo sát trước khi sửa

Một module Android Java/XML `app`, JDK 17, AGP 8.7.3, compile/target SDK 35. Đã kiểm tra toàn bộ danh sách file được Git theo dõi, manifest, build scripts, Java sản phẩm, XML, unit test, instrumentation test, `tools/`, `.vscode/` và `docs/`. Repo không có `AGENTS.md`.

Package hiện có: `ui/` cho màn hình/lifecycle; `ui/dialog/` cho hộp thoại; `adapter/` gắn dữ liệu vào RecyclerView; `repository/` điều phối và trạng thái phiên; `irc/` transport/parser/lệnh; `model/` dữ liệu; `storage/` SharedPreferences. Không di chuyển package hoặc tách/gộp lớp.

## Bảng đổi tên

Đường dẫn Java tính từ `app/src/main/java/vn/edu/usth/connect/`; XML tính từ `app/src/main/res/`.

| Tên cũ | Tên mới | Chức năng | Lý do đổi |
|---|---|---|---|
| `ui/ChannelsFragment.java` | `ui/CourseChannelsFragment.java` | Danh sách kênh của môn được chọn | Phân biệt rõ với `ServerChannelsFragment` lấy phòng từ server |
| `ui/SessionFragment.java` | `ui/SessionAwareFragment.java` | Lớp Fragment nền đăng ký/hủy listener phiên và render khi trạng thái đổi | Tránh hiểu là một màn hình phiên riêng; tên mới nói rõ lớp phản ứng với session |
| `ui/Ui.java` | `ui/UiHelpers.java` | Hàm hỗ trợ hiển thị, avatar, ngày giờ, lỗi, trạng thái và nút theme | Tên cũ quá ngắn; tên mới chỉ rõ tập helper dùng chung |
| `ui/Insets.java` | `ui/WindowInsetsHelper.java` | Áp dụng khoảng chừa cho thanh hệ thống và bàn phím | Nói rõ thao tác window insets; phân biệt với kiểu giá trị `androidx.core.graphics.Insets` |
| `irc/Mentions.java` | `irc/NicknameMentions.java` | Tìm vị trí nhắc nickname theo ranh giới và case folding IRC | Nói rõ đối tượng được nhận diện, phục vụ tô sáng và thông báo |
| `layout/theme_toggle.xml` | `layout/view_theme_toggle.xml` | Nút chuyển sáng/tối dùng qua include | Bổ sung tiền tố `view_` cho view tái sử dụng, không phải màn hình |
| `layout/sheet_members.xml` | `layout/dialog_members.xml` | Nội dung bottom sheet thành viên | Thống nhất `dialog_` cho nội dung hộp thoại; lớp vẫn là `MembersBottomSheet` |
| `layout/dialog_join.xml` | `layout/dialog_join_channel.xml` | Nhập kênh và key để JOIN | Chỉ rõ đối tượng tham gia là kênh |
| `layout/item_message.xml` | `layout/item_chat_message.xml` | Một hàng tin nhắn chat | Chỉ rõ nhóm chức năng chat |
| `layout/item_separator.xml` | `layout/item_chat_separator.xml` | Hàng phân cách ngày, tin chưa đọc và sự kiện trong chat | Tránh nhầm với separator dùng chung cho danh sách khác |
| `drawable/bg_bubble.xml` | `drawable/bg_outgoing_message.xml` | Nền phần nội dung tin nhắn gửi đi | Tên mới mô tả đúng trường hợp dùng thay vì chỉ hình dạng |
| `color/nav_tint.xml` | `color/navigation_item_tint.xml` | Selector màu icon/chữ mục điều hướng | Bỏ viết tắt `nav`, chỉ rõ đối tượng được tô màu |

Giữ các tên đã rõ: `ConnectActivity`, `MainActivity`, `CoursesFragment`, `ServerChannelsFragment`, `ChatFragment`, `ConversationsFragment`, `ProfileFragment`, `MembersBottomSheet`, `CatalogAdapter`, `ChatAdapter`, `SessionRepository`, `SessionState`, `CourseRepository`, `SettingsStore`, `ThemeMode`, `IrcClient`, `IrcParser`, `IrcCommands`, các model và dialog helper. `Irc` là tên giao thức phổ biến, có giải thích trong CODE_MAP; không đổi sang tên dài hàng loạt. `fragment_catalog.xml` và `item_catalog.xml` được giữ vì thật sự được dùng chung cho môn/kênh/hội thoại/thành viên. File values, menu, icon và background đã có nghĩa rõ ràng được giữ.

## Tìm bảy nhóm chức năng

| Nhóm | Điểm bắt đầu |
|---|---|
| Kết nối | `ui/ConnectActivity` → `repository/SessionRepository` → `irc/IrcClient`; layout `activity_connect.xml` |
| Môn học/kênh | `CoursesFragment` → `CourseChannelsFragment`; `CourseRepository`, `assets/courses.json`; `ServerChannelsFragment` cho LIST từ server |
| Chat | `ChatFragment`, `ChatAdapter`; `fragment_chat.xml`, `item_chat_message.xml`, `item_chat_separator.xml` |
| Thành viên | `MembersBottomSheet`, `dialog_members.xml`; `SessionState.handleNamesBatch()`/`handleNamesComplete()` |
| Quản trị | `ui/dialog/ChannelDialogs` cho TOPIC/MODE; `UserDialogs.showMemberActions()` cho cấp/thu quyền và KICK; `SessionRepository` gửi lệnh |
| Thông báo | `ConnectApplication` tạo/phát notification; `NicknameMentions` nhận diện nickname, `SessionState.handleChatMessage()` gắn mention; thiết lập ở `ProfileFragment`/`SettingsStore` |
| Sáng/tối | `ThemeMode`, `UiHelpers.themeToggle()`, `view_theme_toggle.xml`, `SettingsStore.darkMode()`, `res/values-night/` |

Xem [CODE_MAP.md](CODE_MAP.md) để tra đến từng hàm và resource.

## Phạm vi bảo toàn

Chỉ đổi tên file/class và tham chiếu tương ứng; constructor của helper đổi đồng bộ, import/call site/test/script/tài liệu dùng tên mới. Manifest đã kiểm tra: các Activity/Application được giữ tên nên không cần sửa. Instrumentation không có tham chiếu đến các class/resource đổi tên; `R.id.theme_toggle` cố ý giữ nguyên, chỉ đổi tên file layout.

Không đổi tên biến/hàm, view ID, package/applicationId, action/extra/Bundle key, SharedPreferences/file/key, notification channel `messages`/`mentions`, assets, chuỗi người dùng, màu, kích thước, hình học XML, giao thức hoặc logic IRC. Kiểu `androidx.core.graphics.Insets` giữ nguyên. Script sinh resource chỉ cập nhật tên đầu ra/tham chiếu, không chạy lại trên source vì sẽ ghi đè thiết kế hiện có.

## Kiểm chứng

Kết quả thực tế và lệnh tái chạy được ghi trong [FILE_NAMING_VALIDATION.md](FILE_NAMING_VALIDATION.md).
