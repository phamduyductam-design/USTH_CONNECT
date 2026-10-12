# Kịch bản bảo vệ (15 phút + 3 phút hỏi đáp)

## Introduction — 1 phút

“USTH Connect là IRC client native cho Android, giúp sinh viên trao đổi theo môn học. Môn học là nhóm kênh mà app định nghĩa; server IRC quản lý chat và thành viên. App dùng Java, XML và AndroidX. Không khẳng định có máy chủ chính thức USTH khi chưa được cung cấp địa chỉ.”

Mở màn hình Kết nối, chỉ ra nickname, host và advanced settings. Nêu mục tiêu hai client gửi tin thật; ảnh thiết kế chỉ là tài liệu tham khảo.

## Architecture — 2 phút

Mở sơ đồ trong ARCHITECTURE.md. Chỉ 3 trách nhiệm: UI hiển thị, IrcClient xử lý transport/protocol, repository giữ dữ liệu. Application giữ session theo process; Activity/Fragment không giữ socket riêng. Phân biệt courses.json metadata với JOIN/NAMES/topic nhận thật.

Giải thích OOP qua model Conversation/Channel, listener interface, các package. Lịch sử RAM giới hạn 500 hàng/hội thoại, nháp/preferences tách server.

## Activities — 2 phút

ConnectActivity chỉ chuyển sang MainActivity sau 001. MainActivity điều phối Môn học/Phòng/Tin nhắn/Cá nhân và back stack Kênh/Chat; MembersBottomSheet mở từ chat. Intent/Bundle dùng để điều hướng và lưu state. onStart/onStop đăng ký/hủy listener.

Xoay khi đang chat có nháp: Activity tạo lại nhưng cùng Application/session; nháp và vị trí cuộn được giữ. Giải thích process death khác với xoay: không bảo đảm giữ Socket khi hệ điều hành hủy process.

## Networking — 3 phút

Mở IrcCommands.message, IrcParser.parse, IrcClient.readSession/write và SessionRepository.onMessage/send. Vẽ luồng UI → executor ghi → Socket và Socket → parser → Handler → model → adapter.

Nêu:

- TCP/TLS trực tiếp; `NICK`, `USER`, `JOIN`, `PART`, `PRIVMSG`, `QUIT`, `PING/PONG`; thêm `LIST`, `NAMES`, `WHO`, `TOPIC`, `MODE`, `KICK`, `AWAY`.
- CONNECTED cần 001; JOIN cần sự kiện JOIN của chính mình.
- UTF-8, CRLF, giới hạn 512 byte cho cả lệnh; phòng chèn lệnh bằng CR/LF.
- TLS kiểm tra trust chain và hostname. Chưa có SASL/NickServ.
- Reader blocking và writer độc lập; giữ UI không treo.
- NAMES gồm nhiều gói: map tránh trùng; xử lý người rời giữa gói.
- Ghi socket thành công không chứng minh người nhận đã nhận; không có “đã đọc” hoặc “đang gõ”. Retry do người dùng chọn.

Đối chiếu slide: Thread/Handler giống nguyên lý AsyncTask, dùng ExecutorService thay lớp deprecated. Volley là HTTP, không dùng để thay IRC.

## Optimization — 2 phút

RecyclerView tái dùng hàng; vector icon không tải bitmap; một socket cho nhiều kênh; bỏ callback của epoch cũ; lịch sử hữu hạn; lifecycle tránh listener leak. Các adapter hiện đơn giản, cập nhật danh sách nhỏ; DiffUtil và SQLite phân trang là mở rộng.

Trên emulator/điện thoại bật Developer options → Debug GPU overdraw để quan sát. Nêu kết quả thực sự quan sát được, không bịa con số. Bản này chưa có đo benchmark pin/FPS. Các khu vực card vẫn có nền riêng để đáp ứng thiết kế.

## Demo — 4 phút

Chuẩn bị server Ergo riêng và hai client trước buổi bảo vệ; để sẵn log test. Không phụ thuộc chính sách của mạng công cộng.

1. Đăng nhập `minh_usth`, `an_usth`. Cùng JOIN `#web-general`.
2. Minh gửi “Mọi người làm đến phần API chưa?”; An trả lời ở client thứ hai. Chỉ ra dữ liệu xuất hiện từ mạng thật.
3. Mở thành viên; nhắn riêng An; An trả lời.
4. Đổi nickname An; quan sát cập nhật thành viên/conversation. Thử nickname Minh để thấy máy chủ từ chối.
5. Nhập nháp, xoay màn hình. Cuộn lên đọc cũ rồi gửi thêm từ client kia; app giữ vị trí và hiện nút tin mới.
6. Dừng server: banner và send bị vô hiệu; khởi động lại: backoff, 001, JOIN lại. App không tự phát lại tin.
7. Mở tab Phòng để lấy LIST thật; dùng menu đổi topic và cấp Voice nếu là Operator. Đặt AWAY, nhắc nickname từ client thứ hai và xem thông báo khi app ở nền.
8. Rời kênh bằng menu; ngắt kết nối có xác nhận. Nếu hết giờ, ưu tiên các bước 1–4 và ngắt chủ động.

## Conclusion — 1 phút

“Đã có app Android build được, giao diện native, parser/transport IRC, các luồng môn/kênh/chat/thành viên/chat riêng/cá nhân và test được ghi rõ. Giới hạn còn lại là lưu lịch sử lâu dài, xác thực tài khoản, IRCv3 history, hỗ trợ casemapping đầy đủ và foreground service.”

Chỉ sử dụng kết quả trong TEST_REPORT.md để nói phần kiểm thử đã hoàn thành. Demo trong báo cáo bằng hai Java client dùng đúng engine cũng không được gọi là demo hai điện thoại nếu chưa chạy hai điện thoại.

## Câu hỏi thường gặp

- **Vì sao không dùng Firebase/HTTP?** Đề tài là IRC client; IRC là giao thức TCP hai chiều có PING và sự kiện membership.
- **Vì sao không tạo socket ở Fragment?** Fragment thay đổi/rotate; Application giữ một phiên tránh nhân đôi kết nối.
- **Một message có ACK không?** IRC cổ điển không xác nhận nhận từng tin; WRITTEN là kết quả ghi cục bộ. Lỗi numeric tương quan theo target là best-effort.
- **JOIN button bấm là đã vào chưa?** Chưa. Cần self JOIN từ server; rejected/timeout vẫn là chưa tham gia.
- **Ẩn app có chat được mãi không?** Không bảo đảm. Process còn sống có thể nhận; duy trì lâu dài cần foreground service và đánh giá hạn chế Android.
- **Xoay và process death khác nhau?** Xoay giữ Application trong cùng process; process death mất socket và lịch sử RAM, preferences còn.
- **Danh mục khóa cứng server không?** Không. JSON là tên nhóm/channels cục bộ; server quyết định kênh và quyền. Có thể nhập kênh ngoài danh mục.
