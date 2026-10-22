# USTH Connect

Ứng dụng Android native Java/XML, có chế độ sáng/tối lưu theo lựa chọn người dùng: kết nối IRC, trao đổi theo môn học và nhắn riêng. Dự án này được tạo từ prompt và các ảnh mẫu đã cung cấp. Danh mục môn là metadata cục bộ; tin nhắn, topic, thành viên và xác nhận JOIN đến từ phiên IRC thật.

## Đọc và tìm chức năng trong source

Bắt đầu từ [bản đồ mã nguồn](docs/CODE_MAP.md): có bảng tính năng → file/hàm/layout, luồng gửi/nhận tin và thứ tự đọc cho người mới. Xem [nhận xét code và các phần đã làm sạch](docs/CODE_REVIEW.md).

## Mở và build

Clone repository, rồi mở thư mục vừa clone bằng Android Studio hoặc VS Code:

```powershell
git clone https://github.com/phamduyductam-design/USTH_CONNECT.git
cd USTH_CONNECT
```

Repository chứa source, test, tài liệu, Gradle wrapper và task VS Code. SDK/JDK, cache, APK, ZIP, log và `local.properties` được giữ ngoài Git; mỗi máy cần cài JDK 17/Android SDK và cấu hình đường dẫn theo hướng dẫn dưới đây.

Mở **thư mục chứa `settings.gradle` này** bằng Android Studio. Chọn Gradle JDK **17**, cài **Android SDK Platform 35** bằng SDK Manager và chờ Gradle Sync. Android Studio Ladybug 2024.2.1 hoặc bản mới có hỗ trợ AGP 8.7 phù hợp. Không chọn JDK 25 cho Gradle 8.9.

| Thành phần | Phiên bản |
|---|---|
| Ngôn ngữ / giao diện | Java 17 / XML / AndroidX |
| Android Gradle Plugin | 8.7.3 |
| Gradle wrapper | 8.9, có SHA-256 của distribution |
| JDK chạy Gradle | 17 |
| min / compile / target SDK | 24 / 35 / 35 |
| Build Tools AGP sử dụng mặc định | 34.0.0 |
| AppCompat / RecyclerView / Material | 1.7.0 / 1.3.2 / 1.12.0 |

Đây là bộ phiên bản cố định đã build được trong môi trường thực tế; không tự đổi sang dependency mới nhất. Xem [ma trận tương thích AGP](https://developer.android.com/build/releases/agp-8-7-0-release-notes).

```powershell
# Sau khi cài JDK 17 và SDK; sửa đường dẫn theo máy của bạn.
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\gradlew.bat assembleDebug lintDebug testDebugUnitTest
```

macOS/Linux: `chmod +x gradlew`, rồi `./gradlew assembleDebug lintDebug testDebugUnitTest`. Đặt `sdk.dir=...` trong `local.properties` hoặc dùng `ANDROID_HOME`. Không đưa `local.properties`, `.tools/`, `.gradle/` hay `build/` lên Git. APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

SDK, AVD và server demo cục bộ có thể đặt trong `.tools/` (được Git bỏ qua). Các script chạy demo nằm trong `tools/`; không cần đưa SDK/JDK nhiều GB vào repository.

## Giả định và cách dùng

Nếu dùng VS Code, mở thư mục chứa `settings.gradle` và chạy task **USTH: Chay may ao va ung dung**. Có cấu hình Windows và macOS riêng; xem [hướng dẫn chạy](docs/VSCODE_RUN.md).

- `irc.example.edu` chỉ là hint theo ảnh mẫu, không được điền thành máy chủ thật. Chưa có thông tin về máy chủ IRC của USTH.
- Mặc định TLS bật, cổng 6697. Nhập máy chủ do bạn được phép sử dụng. Với server cục bộ không có TLS, tắt TLS và chọn 6667.
- Nickname ASCII tối đa 30 ký tự; máy chủ có thể đặt giới hạn thấp hơn. Không cần tài khoản trong server demo; bản này chưa có PASS, SASL hay NickServ UI.
- Không có dữ liệu chat offline giả. Lần đầu danh sách **Đã tham gia** trống; bấm **Tham gia kênh**, mở môn rồi chọn kênh và JOIN. Tab **Phòng** lấy danh sách thật từ `LIST` trên server.
- Tìm kiếm ở Môn học lọc metadata cục bộ; tìm kiếm ở Phòng lọc kết quả LIST của server. JOIN được công nhận sau phản hồi JOIN của chính mình.
- Chủ đề kênh được hiển thị từ 331/332/TOPIC. Mô tả danh mục cục bộ không được trình bày như topic của máy chủ.
- Kênh ngoài danh mục nằm ở **Kênh khác**. Môn thuộc Đã tham gia nếu ít nhất một kênh đã được xác nhận JOIN; badge cộng các tin chưa đọc của kênh đó.

Có thể thử `irc.libera.chat`, TLS 6697 theo [hướng dẫn chính thức Libera Chat](https://libera.chat/guides/connect). Đây là mạng độc lập, không phải máy chủ của USTH. Chính sách mạng, biệt danh, tài khoản và kênh có thể giới hạn thao tác. Dùng kênh thử riêng có tên khó trùng, không gửi thử vào kênh của người khác.

## Demo hai client trên server bạn kiểm soát

Dùng [Ergo](https://github.com/ergochat/ergo/releases), một IRC daemon thật, hoặc daemon IRC khác. Không cần HTTP, Volley hay Firebase.

1. Giải nén release Ergo; sao chép `default.yaml` thành `ircd.yaml`.
2. Với demo chỉ trên máy tính, chỉ giữ listener `127.0.0.1:6667`, bỏ listener TLS mẫu khi chưa có certificate. Đặt `server.casemapping: "rfc1459"`. Trong thư mục Ergo, chạy `ergo.exe run --conf ircd.yaml`.
3. Emulator Android truy cập máy host bằng `10.0.2.2`, cổng 6667, TLS tắt. Hai emulator dùng `minh_usth` và `an_usth`. Nếu dùng điện thoại thật, cấu hình listener LAN, firewall và server TLS phù hợp; không dùng `10.0.2.2` trên điện thoại.
4. Hai client cùng JOIN `#web-general`, gửi tin tiếng Việt qua lại. Mở Thành viên, chọn nickname kia và nhắn riêng.
5. Đổi nickname trong Cá nhân, xem hàng thành viên thay đổi; rời kênh trong menu chat; ngắt kết nối trong Cá nhân và xác nhận.
6. Thử nickname trùng, key sai, gửi tới nickname không tồn tại, dừng/khởi động server và xoay máy khi có nháp.

Kiểm thử giao thức bằng đúng Java `IrcClient` của app, không cần emulator:

```powershell
.\tools\network-smoke.ps1 -Server 127.0.0.1 -Port 6667
# Opt-in: mở hai phiên thật, chỉ gửi giữa hai nickname thử trong một kênh tạm ngẫu nhiên.
.\tools\network-smoke.ps1 -Server irc.libera.chat -Port 6697 -Tls
```

Kiểm thử UI cần daemon thật đang chạy:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.server=10.0.2.2
```

## Chức năng IRC đầy đủ theo checklist

- Phòng server: LIST (321/322/323), tìm theo tên/topic, JOIN và tạo phòng bằng JOIN khi server cho phép.
- Thành viên: NAMES chủ động, WHO, vai trò Owner/Admin/Operator/Half-operator/Voice/Thành viên; cập nhật NICK/JOIN/PART/QUIT/KICK/MODE.
- Quản trị: đổi/xóa topic, cấp/thu Operator/Voice, KICK với lý do, bật/tắt +i/+m/+t. Server quyết định quyền và trả lỗi 482/404 khi bị từ chối.
- Presence: đặt/xóa AWAY, đọc 301/305/306/352; away-notify cập nhật trực tiếp nếu server hỗ trợ, WHO phục vụ ảnh chụp trạng thái và làm mới.
- Nhắc tên: nhận diện nickname nguyên từ theo RFC1459, tô đậm trong chat và kênh thông báo Android ưu tiên riêng. Thông báo cần quyền, app ở nền và process còn sống.
- Lệnh gõ: `/join`, `/nick`, `/msg`, `/part`, `/list`, `/names`, `/who`, `/topic`, `/mode`, `/kick`, `/away`, `/help`. Lệnh lạ báo lỗi; `//` gửi nội dung bắt đầu bằng `/`.
- Xuống dòng ở ô soạn tin được gộp thành khoảng trắng trước gửi; giữ nháp nếu nội dung không hợp lệ.

Xem [hướng dẫn tính năng và cú pháp](docs/IRC_FEATURES.md).

Trên macOS/Linux, chạy hai nhóm kiểm tra mạng với daemon bạn kiểm soát:

```bash
bash tools/network-smoke.sh 127.0.0.1 6667 false
```

Để người thứ hai tự nhập tin trong terminal (client này không tự nhắc lại):

```bash
bash tools/interactive-peer.sh 127.0.0.1 6667 false an_demo '#web-general'
```

## Tổ chức source

```text
app/src/main/
  java/vn/edu/usth/connect/
    ConnectApplication.java        # Khởi tạo các repository và thông báo
    ui/                            # Activity, Fragment, BottomSheet, dialog, insets
    adapter/                       # RecyclerView course/channel/member/PM/message rows
    model/                         # Các đối tượng dữ liệu và trạng thái
    irc/                           # Parser, command builder, Socket/SSLSocket client
    repository/                    # SessionState, SessionRepository, CourseRepository
    storage/                       # SharedPreferences
  assets/courses.json              # Danh mục môn/kênh cục bộ
  res/                             # XML layout, strings, colors, dimens, icons, styles
app/src/test/                      # Unit test không cần thiết bị
app/src/androidTest/               # Test UI với IRC daemon thật
docs/                              # Kiến trúc, liên hệ slide, kịch bản bảo vệ, báo cáo test
```

Đọc tiếp [kiến trúc và luồng dữ liệu](docs/ARCHITECTURE.md), [bảng liên hệ tài liệu môn học](docs/COURSE_MAPPING.md), [kịch bản bảo vệ](docs/DEFENSE.md) và [báo cáo kiểm thử](docs/TEST_REPORT.md).

## Các giới hạn được chủ động giữ rõ

- Một phiên sống trong Application/process; không dùng foreground service. Phiên có thể nhận thông báo khi process vẫn chạy và đã có quyền, nhưng không bảo đảm tồn tại sau khi Android hủy process. Mở lại sau process death cần kết nối và JOIN lại.
- Lịch sử tối đa 500 hàng mỗi hội thoại, chỉ trong RAM, không tải lịch sử server. Nháp và cấu hình không nhạy cảm dùng SharedPreferences. Không có SQLite ở bản này; không giả vờ có dữ liệu cũ sau process death.
- Không lưu channel key xuống đĩa. Không lưu mật khẩu tài khoản. Lịch sử được xóa khi đổi server hoặc danh tính kết nối; nháp được phân vùng theo host/cổng/TLS/kênh.
- Dùng CASEMAPPING RFC1459. PREFIX và CHANMODES được đọc từ ISUPPORT 005; CAP thương lượng `multi-prefix` và `away-notify` khi server hỗ trợ. CASEMAPPING khác RFC1459 chưa được thương lượng.
- Không có read receipt, typing indicator, SASL/NickServ, IRCv3 CHATHISTORY hoặc thời gian server. Có TOPIC/KICK/MODE, vai trò và away; thao tác quản trị vẫn do server kiểm tra quyền.
- Một tin gửi thành công chỉ có trạng thái **đã ghi vào kết nối**, không khẳng định người nhận đã nhận. Lỗi ghi một phần có trạng thái **chưa rõ đã gửi**. Gửi lại cần xác nhận và không tự gửi lại khi reconnect.
- Reconnect: 1/2/4/8/16/32 giây, tối đa 6 lần thất bại liên tiếp sau một phiên từng được chấp nhận; ngắt chủ động dừng retry. Những kênh còn muốn tham gia được JOIN lại sau 001, không phát lại tin nhắn.
- Không chia tự động tin vượt 512 byte. App báo lỗi và giữ nội dung để người dùng rút ngắn; giới hạn gồm cả tên kênh/nickname, lệnh và CRLF.

Các icon vector là tài nguyên tự tạo của dự án; không dùng ảnh mẫu như ảnh chụp màn hình chạy app. UI sử dụng thanh trạng thái Android thật, dp/sp, touch target 48dp, ScrollView/RecyclerView và xử lý insets của bàn phím.
