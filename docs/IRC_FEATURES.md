# Chức năng IRC và cách sử dụng

## Đối chiếu yêu cầu trong ảnh

| Yêu cầu | Trạng thái bản 1.1 |
|---|---|
| Kết nối, đổi nickname, tham gia/rời phòng | NICK/USER, NICK, JOIN, PART qua IRC server |
| Nhắn trong phòng và riêng 1–1 | PRIVMSG qua IRC server |
| Danh sách thành viên và phòng | NAMES/WHO và LIST; có nút làm mới |
| Chủ đề, quản lý thành viên | TOPIC, KICK, MODE; quyền do server kiểm tra |
| Vai trò, vắng mặt và kết nối lại | PREFIX/MODE, WHO/AWAY, CAP away-notify, reconnect |
| Thông báo và lệnh IRC | Tin mới/nhắc tên; lệnh gõ được liệt kê bên dưới |

Các mục trong checklist đã được triển khai. Đây không phải tuyên bố hỗ trợ mọi extension IRC; xem phạm vi kiểm thử trong TEST_REPORT.md.

## Các màn hình

- **Đăng nhập/kết nối**: logo USTH ở góc trên bên trái; form nickname và server IRC.

- **Môn học**: metadata cục bộ, mỗi môn có một hoặc nhiều IRC channel.
- **Phòng**: danh sách phòng công khai thật từ LIST; bấm làm mới, tìm tên/topic và tham gia. Danh sách chỉ gồm những phòng server cho phép nhìn thấy, tối đa 5.000 phòng trong mỗi lượt.
- **Chat → Thành viên**: xem nickname, vai trò và away. Nút làm mới gửi NAMES và WHO.
- **Chat → menu ⋮**: làm mới thành viên, đổi/xóa topic, chế độ phòng (+i/+m/+t), hướng dẫn lệnh và rời phòng.
- **Thành viên → chọn người → Quản lý thành viên**: cấp/thu Operator, cấp/thu Voice hoặc mời ra với lý do. Nút quản trị chỉ xuất hiện khi dữ liệu server cho biết tài khoản có quyền; server vẫn kiểm tra quyền thực tế.
- **Cá nhân**: đổi nickname, đặt/xóa trạng thái vắng mặt, thông báo, ngắt/kết nối lại.
- **Chat riêng**: hiện trạng thái nhận được từ WHO/away-notify/301; chưa có dữ liệu thì ghi chưa xác định.

## Lệnh gõ trong ô chat

| Cú pháp | Hành động |
|---|---|
| `/join #phòng [key]` | JOIN và mở hội thoại; kênh mới được server tạo nếu chính sách cho phép |
| `/nick tên_mới` | Đổi nickname |
| `/msg nickname nội dung` | Nhắn riêng và mở hội thoại đó |
| `/part [#phòng]` | Rời phòng chỉ định hoặc phòng đang mở |
| `/list` | Tải danh sách và mở tab Phòng |
| `/names [#phòng]` | Làm mới thành viên; gửi thêm WHO để lấy presence |
| `/who [#phòng hoặc nickname]` | Yêu cầu thông tin presence |
| `/away lý do` | Đặt trạng thái vắng mặt |
| `/away` | Trở lại trực tuyến |
| `/topic #phòng` | Yêu cầu topic hiện tại |
| `/topic [#phòng] chủ đề mới` | Đổi topic |
| `/topic #phòng :` | Xóa topic |
| `/mode #phòng` | Yêu cầu mode hiện tại |
| `/mode #phòng +o nickname` / `-o` | Cấp/thu Operator |
| `/mode #phòng +v nickname` / `-v` | Cấp/thu Voice |
| `/mode #phòng +m` / `-m` | Bật/tắt chế độ moderated |
| `/kick #phòng nickname [lý do]` | Mời người dùng ra khỏi phòng |
| `/help` | Xem hướng dẫn |
| `//hello` | Gửi tin bắt đầu bằng `/hello` |

Lệnh sai được giữ trong ô nhập để sửa. Không có chuyển tiếp tùy ý raw command, tránh chèn nhiều lệnh bằng CRLF/NUL. Các builder vẫn kiểm tra giới hạn 512 byte gồm CRLF.

## Quyền và phản hồi server

- Operator/Owner/Admin có quyền tùy chính sách server. Voice cho phép nói trong phòng +m, không tự cấp quyền quản trị.
- TOPIC khi +t cần quyền phù hợp; +i hạn chế tham gia; +m hạn chế gửi tin.
- GUI không tự ghi nhận cấp quyền thành công. Role/mode/topic chỉ cập nhật từ phản hồi MODE/TOPIC/NAMES/324.
- Khi không đủ quyền, server thường trả 482; gửi bị cấm có thể trả 404. App hiển thị lỗi và chi tiết server.
- Prefix và cách tiêu thụ tham số MODE được đọc từ ISUPPORT 005. CAP chỉ yêu cầu những capability app hiểu và server công bố.

## Away và nhắc tên

WHO lấy trạng thái hiện tại. Khi server có away-notify, thay đổi away được cập nhật trực tiếp qua AWAY. Khi server không có capability này, dùng làm mới thành viên/WHO để lấy trạng thái mới. Trạng thái tự đặt được xác nhận qua 305/306, không đổi giả ngay khi bấm nút.

Nickname được nhắc đúng nguyên từ (không nhầm `an` với `an_usth`) sẽ được tô nổi bật. Thông báo nhắc tên dùng notification channel riêng có mức ưu tiên cao; quyền hệ điều hành và thiết lập người dùng vẫn được tôn trọng. App không bảo đảm phiên sau khi Android hủy process.

## Demo có hai người nhập tin

Chạy ứng dụng Android, JOIN #web-general. Trong terminal của máy chạy daemon:

```bash
export JAVA_HOME=/path/to/jdk-17
bash tools/interactive-peer.sh 127.0.0.1 6667 false an_demo '#web-general'
```

Người thứ hai nhập tin trực tiếp trong terminal; nhập `/msg mathew_demo nội dung` để nhắn riêng, `/quit` để đóng. Client terminal dùng đúng Java IrcClient và parser/builder của app, không tự phản hồi. Demo_Bot vẫn là công cụ echo riêng và không cần cho bài thử hai người.

Hai điện thoại/hai máy có thể dùng cùng server có địa chỉ truy cập được, nickname khác nhau và cùng channel. Địa chỉ 10.0.2.2 chỉ dành cho Android Emulator tới host của nó; server demo mặc định chỉ lắng nghe localhost. Cấu hình mạng LAN/server dùng chung là việc triển khai riêng, không phải cơ chế chat giả của app.

## Tài liệu giao thức

- [RFC 2812](https://www.rfc-editor.org/rfc/rfc2812)
- [IRCv3 CAP](https://ircv3.net/specs/extensions/capability-negotiation)
- [multi-prefix](https://ircv3.net/specs/extensions/multi-prefix)
- [away-notify](https://ircv3.net/specs/extensions/away-notify)
