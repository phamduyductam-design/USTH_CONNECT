# Hướng dẫn sử dụng USTH Connect

USTH Connect là ứng dụng Android để trao đổi theo môn học và nhắn riêng qua IRC. Danh mục môn học có sẵn trong app; tin nhắn và thành viên được lấy từ máy chủ IRC khi kết nối.

## 1. Chuẩn bị và mở ứng dụng

- Sử dụng điện thoại hoặc máy ảo Android 7.0 (API 24) trở lên, có kết nối mạng.
- Chuẩn bị địa chỉ máy chủ IRC, cổng và lựa chọn TLS do người quản trị cung cấp.
- Nếu chạy từ source: mở thư mục chứa `settings.gradle` bằng Android Studio, dùng Gradle JDK 17 và cài Android SDK Platform 35. Chờ Gradle Sync, chọn cấu hình `app` cùng thiết bị, rồi nhấn **Run**.

Xem chi tiết cài đặt trong [README](../README.md#mở-và-build).

## 2. Kết nối IRC

1. Nhập **Biệt danh** riêng, chẳng hạn `minh_usth_01`. Dùng ký tự ASCII, không dấu và không khoảng trắng; giới hạn của app là 30 ký tự, server có thể giới hạn thấp hơn.
2. Trong **Máy chủ IRC**, nhập tên miền hoặc địa chỉ IP của server. Không nhập số cổng vào ô này. `irc.example.edu` chỉ là ví dụ, không phải server được cung cấp sẵn.
3. Mở **Cổng và tùy chọn nâng cao**, nhập cổng và bật hoặc tắt TLS theo cấu hình server.
4. Nhấn **Kết nối** hoặc **Thử lại**, rồi chờ server chấp nhận phiên.

### Demo với server trên máy tính

Chỉ dùng cấu hình sau khi đã khởi động server IRC trên máy tính với listener không dùng TLS ở cổng 6667:

| Thiết lập | Giá trị trên Android Emulator mặc định |
|---|---|
| Máy chủ | `10.0.2.2` |
| Cổng | `6667` |
| TLS | Tắt |
| Biệt danh | Mỗi client dùng một tên khác nhau |

`10.0.2.2` trỏ về máy tính từ Android Emulator; địa chỉ này không tự tạo server. Điện thoại thật cần dùng địa chỉ LAN của máy chạy server và được phép truy cập listener đó. Xem [hướng dẫn server demo](../README.md#demo-hai-client-trên-server-bạn-kiểm-soát).

## 3. Tham gia phòng theo môn học

1. Mở tab **Môn học**, chọn môn và kênh muốn trao đổi.
2. Chọn **Tham gia** và chờ xác nhận từ server trước khi gửi tin.
3. Để tìm phòng công khai, vào **Môn học → Phòng trên server**.
4. Có thể dùng **Tham gia kênh** để nhập tên kênh, chẳng hạn `#usth-demo-nhom01`. Việc tạo hoặc tham gia phòng phụ thuộc quyền và cấu hình server.

Danh mục môn học là dữ liệu cục bộ, không bảo đảm phòng đã có người tham gia. Kênh ngoài danh mục được hiển thị ở **Kênh khác**.

## 4. Gửi tin và nhắn riêng

- Trong phòng đã tham gia, nhập nội dung rồi gửi. Người nhận cần kết nối cùng server và tham gia cùng kênh để trao đổi trong phòng.
- Mở **Thành viên**, chọn biệt danh người cần liên hệ để nhắn riêng.
- Dùng tab **Tin nhắn** để xem các hội thoại riêng.
- Không coi biệt danh là tài khoản sinh viên đã xác minh. Bản hiện tại chưa có giao diện đăng nhập PASS, SASL hoặc NickServ.

### Thử với hai client

1. Kết nối hai client tới cùng server với hai biệt danh khác nhau.
2. Cho cả hai tham gia cùng kênh thử do nhóm chọn.
3. Gửi tin từ client thứ nhất và kiểm tra ở client thứ hai, sau đó thử chiều ngược lại.
4. Mở danh sách thành viên và thử nhắn riêng giữa hai client.

## 5. Cá nhân và rời phòng

- Vào **Cá nhân** để đổi biệt danh hoặc đặt trạng thái vắng mặt.
- Rời kênh bằng menu trong màn hình chat.
- Khi kết thúc, chọn ngắt kết nối trong **Cá nhân** và xác nhận.
- App có chế độ sáng/tối và lưu lựa chọn của người dùng.

## 6. Xử lý tình huống thường gặp

| Tình huống | Cách kiểm tra |
|---|---|
| Không kết nối được | Kiểm tra mạng, tên server, cổng, TLS và server có đang hoạt động hay không. |
| Dùng `10.0.2.2` vẫn lỗi | Xác nhận đang dùng Android Emulator và đã chạy server IRC trên máy tính. |
| Biệt danh bị từ chối | Thử tên khác, không dấu và không khoảng trắng; đọc phản hồi từ server. |
| Thấy môn nhưng chưa gửi tin được | Chọn kênh, nhấn **Tham gia** và chờ server xác nhận JOIN. |
| Không thấy tin của bạn cùng nhóm | Kiểm tra cả hai dùng cùng server, cùng tên kênh và đều đã tham gia. |
| Không đổi được chủ đề hoặc quản lý thành viên | Server yêu cầu quyền phù hợp; thành viên thông thường có thể bị từ chối. |
| Không thấy thông báo | Kiểm tra quyền thông báo; app cần ở nền và process còn sống để nhận thông báo theo cơ chế hiện tại. |

Không có lịch sử chat offline giả. Không giả định app sẽ nhận tin khi đã ngắt kết nối.

## 7. Checklist trước buổi demo nhóm

- [ ] Server IRC đang hoạt động và cả hai thiết bị đều có mạng.
- [ ] Hai client nhập cùng địa chỉ server, cổng và lựa chọn TLS.
- [ ] Mỗi client sử dụng một biệt danh khác nhau.
- [ ] Hai client đã được server xác nhận tham gia cùng kênh thử.
- [ ] Tin nhắn tiếng Việt được nhận theo cả hai chiều.
- [ ] Danh sách thành viên hiển thị hai biệt danh; nhắn riêng tới đúng người nhận.
- [ ] Thử rời phòng và ngắt kết nối sau khi hoàn tất.

Nếu có bước thất bại, ghi lại thiết bị, phiên bản Android, bước thực hiện và thông báo lỗi để nhóm có thể kiểm tra lại. Đánh dấu kết quả sau khi thử thực tế, không coi checklist này là báo cáo đã kiểm thử.

## Tài liệu liên quan

- [Cài đặt, chạy và giới hạn của dự án](../README.md)
- [Các tính năng IRC và cú pháp lệnh](IRC_FEATURES.md)
- [Chạy dự án bằng VS Code](VSCODE_RUN.md)
