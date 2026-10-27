# Tìm tin nhắn trong hội thoại

Tính năng bổ sung cho phép tìm nhanh nội dung trong chat kênh và tin nhắn riêng, phù hợp để minh họa xử lý dữ liệu và cập nhật giao diện khi bảo vệ đồ án.

## Cách dùng

1. Mở một chat kênh hoặc tin nhắn riêng đã có nội dung.
2. Nhấn nút **Tùy chọn** (dấu ba chấm) → **Tìm tin nhắn**.
3. Nhập từ khóa. Danh sách chỉ hiển thị các tin có nội dung khớp, tô sáng từ khóa và hiển thị số tin tìm được.
4. Đóng tìm kiếm để trở lại toàn bộ nội dung hiện có của hội thoại.

Tìm kiếm không phân biệt chữ hoa/chữ thường nhưng có phân biệt dấu: `API` khớp `api`, còn `hoc` không khớp `học`. Từ khóa được đối chiếu với nội dung tin nhắn; tên người gửi và sự kiện hệ thống không thuộc phạm vi tìm kiếm.

## Phạm vi

- Chỉ lọc các tin đang được giữ trong phiên hiện tại của hội thoại, gồm các tin mới đến trong lúc tìm kiếm.
- Không gửi yêu cầu tìm kiếm hoặc tải lịch sử lên máy chủ IRC, không tìm xuyên các kênh hay hội thoại khác.
- Lịch sử vẫn tuân theo giới hạn hiện có: tối đa 500 hàng mỗi hội thoại trong RAM; các tin đã bị loại hoặc mất sau khi process bị hủy không thể tìm lại bằng tính năng này.
- Không có kết quả nghĩa là không tìm thấy trong phần nội dung hiện có của phiên, không khẳng định máy chủ chưa từng có tin đó.

## Demo 30 giây

Chuẩn bị sẵn vài tin có từ `API` và vài tin có nội dung khác trong cùng một hội thoại.

1. **0–10 giây:** mở **Tùy chọn → Tìm tin nhắn**, nhập `api`.
2. **10–20 giây:** chỉ ra danh sách được lọc, số tin khớp và chữ `API` được tô sáng; nhập một từ không có trong hội thoại để minh họa trạng thái không có kết quả.
3. **20–30 giây:** đóng tìm kiếm, chỉ ra hội thoại đầy đủ xuất hiện trở lại và giải thích: “Ứng dụng lọc nội dung đang có trong phiên, không yêu cầu lịch sử từ server.”

Có thể lặp lại các bước trên trong tin nhắn riêng để minh họa cùng một tính năng dùng cho cả hai loại hội thoại.

## Kiểm chứng bản tích hợp

Từ thư mục gốc dự án, với JDK 17 và Android SDK được cấu hình:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest
```

Các test tìm kiếm kiểm tra tiếng Việt, ký tự đặc biệt, emoji, truy vấn rỗng và loại sự kiện hệ thống. Kiểm tra giao diện bổ sung bao phủ lọc kết quả, không có kết quả, đổi theme, khôi phục truy vấn và giữ nháp.

Bản tích hợp trên `main` mới nhất đã build thành công: 56 unit test qua; lint 0 lỗi, 3 cảnh báo. Ba test Python, ba tình huống TLS localhost và kiểm tra định dạng Java/XML đều qua.

Chưa chạy instrumentation trên thiết bị trong đợt tích hợp này. Biên dịch APK test không thay thế việc thực thi test giao diện.
