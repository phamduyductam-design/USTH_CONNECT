# Báo cáo kiểm thử — tính năng IRC bổ sung

Ngày: 12/10/2026. Nền tảng kiểm tra: macOS Apple Silicon, JDK 17, Gradle 8.9, Android API 35. APK bàn giao versionName 1.2 / versionCode 3.

## Kết quả

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug | Thành công |
| lintDebug | 0 lỗi, 8 cảnh báo có sẵn (dependency và notifyDataSetChanged) |
| Unit test | 30 kiểm thử, 0 lỗi |
| Android instrumentation | 2 kiểm thử, 0 lỗi |
| Hai client qua Ergo localhost | 21 bước PASS ở bản 1.1; engine IRC không thay đổi trong bản 1.2 |

## Android với server thật

- Kết nối NICK/USER; JOIN theo môn; gửi/nhận tin kênh và nhắn riêng.
- Giữ nháp khi Activity tái tạo; gửi nội dung có xuống dòng trong cả kênh và PM.
- Tải phòng bằng /list và mở phòng từ kết quả LIST.
- Đổi topic bằng menu/dialog và nhận topic từ server.
- Làm mới thành viên; cấp Voice bằng GUI; vai trò và away cập nhật trên màn thành viên.
- Đưa app về nền, nhận tin nhắc nickname; kiểm tra Android NotificationManager có notification ở channel mentions.
- Đặt/xóa AWAY qua Cá nhân và chờ phản hồi server xác nhận.
- Thu Voice bằng lệnh gõ; KICK bằng GUI và xác nhận người bị loại không còn trong danh sách.
- /part và xác nhận trạng thái joined bị xóa.

## Các kiểm tra đơn vị đáng chú ý

- Parser/framing, CRLF, UTF-8 byte limit, chặn chèn lệnh, prefix/tag.
- Epoch/reconnect không phát lại tin; ngắt chủ động hủy retry.
- CAP LS nhiều dòng, yêu cầu capability được công bố, CAP END; server cũ trả 421 CAP vẫn đăng ký được.
- LIST bắt đầu/kết thúc/rỗng/lỗi; MODE có tham số key/role không làm lệch nickname; đổi quyền trong lúc NAMES chưa kết thúc.
- PREFIX/CHANMODES từ ISUPPORT; WHO, AWAY, 301/305/306; đổi nickname và QUIT cập nhật presence.
- Nhắc nickname theo nguyên từ/RFC1459; giữ đúng chỉ số khi có Unicode/emoji trước nickname.
- Slash command sai báo lỗi; topic query/clear phân biệt; builder mới vẫn chặn CRLF/NUL và tin quá dài.

## Giao thức trực tiếp với hai phiên client

Script network-smoke.sh dùng đúng Java IrcClient của app. Hai nickname kiểm thử chỉ trao đổi trong các phòng tạm riêng trên daemon cục bộ. Bot demo không tham gia.

```text
PASS two real TCP sessions accepted with 001 (TLS=false)
PASS JOIN acknowledgements and peer membership event
PASS NAMES completed
PASS UTF-8 channel PRIVMSG delivered to second client
PASS private PRIVMSG delivered
PASS NICK broadcast
PASS 401 for missing recipient
PASS PART broadcast
PASS QUIT broadcast; no implicit resend
PASS LIST returns the real temporary channel and its two users
PASS explicit NAMES refresh
PASS operator TOPIC broadcast
PASS MODE grants Voice and updates topic restriction
PASS server rejects non-operator MODE and TOPIC with 482
PASS AWAY acknowledgement, negotiated away-notify and WHO presence
PASS clearing AWAY updates the other client
PASS grant and revoke Operator / Voice
PASS moderated channel enforces permissions on the server
PASS real incoming nickname mention
PASS KICK broadcast to both clients
PASS explicit topic clearing
```

## Kiểm tra logo đăng nhập

Logo USTH do người dùng cung cấp được giữ nguyên ảnh, đặt ở góc trên bên trái màn hình kết nối, fitCenter trong 92 × 40 dp. Kiểm tra bằng ảnh chụp native Android sau khi cài lại APK; build và lint đã chạy lại sau thay đổi resource.

## Kiểm tra sáng/tối ở bản 1.2

- Chạy lại assembleDebug, lintDebug (0 lỗi, 8 cảnh báo nền), 30 unit test và 2 bài Android.
- Bài chat/PM bổ sung đổi theme hai lần khi đang có nháp. Nháp vẫn giữ, channel/session không bị thay thế, vẫn CONNECTED và còn thành viên; tiếp tục nhận tin thật từ client thứ hai sau khi đổi.
- Kiểm tra native đăng nhập light → dark: nickname không mất; đóng process rồi mở lại vẫn dark. Kiểm tra giao diện danh mục, chat, bottom sheet, dialog JOIN, cá nhân và LIST; chuyển về light trong chat.
- Giữ/restoring cấu hình và nháp người dùng trước/sau kiểm thử. `theme-android-checks.log` và `theme-preview.log` nằm ngoài Git trong .tools.

## Phạm vi

Bài kiểm tra mạng mới dùng TCP localhost, không kiểm thử mới TLS trên mạng công cộng hay hai điện thoại vật lý. Không dùng test này để tuyên bố hỗ trợ mọi lệnh/extensions IRC. Away tự cập nhật khi server có away-notify; server cũ dùng WHO/làm mới. Thông báo phụ thuộc quyền hệ điều hành, app ở nền và process còn sống.

Log và XML có thể tái tạo bằng lệnh README; .tools/, build/ và dữ liệu người dùng không được commit. Xem [IRC_FEATURES.md](IRC_FEATURES.md) để thao tác và [ARCHITECTURE.md](ARCHITECTURE.md) để đối chiếu luồng dữ liệu.
