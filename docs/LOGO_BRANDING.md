# Đồng bộ logo USTH

Asset dùng chung: `app/src/main/res/drawable-nodpi/usth_university_logo.png`, PNG RGBA có alpha thật. Bản PNG tách nền trắng từ ảnh `channels4_profile.jpg` người dùng cung cấp bằng công cụ imagegen, không còn dùng JPG có nền trắng. PNG là bản xử lý ảnh, không trùng bytes với JPG gốc.

- Giữ nhận diện chữ USTH xanh, biểu tượng đỏ/xanh và dòng Vietnam France University. Không tint logo theo theme, không dùng chữ hệ thống để thay chữ USTH.
- `view_app_brand.xml` ghép ảnh logo trong suốt với nhãn Connect; dùng chung ở Môn học, Tin nhắn, Phòng trên server và Cá nhân. Trang kênh môn tiếp tục hiển thị tên môn ở toolbar.
- Màn Kết nối dùng cùng PNG với `fitCenter`; bỏ `bg_logo.xml` và nền trắng của ImageView. Nhãn bên cạnh chỉ ghi Connect để tránh lặp chữ USTH.
- Manifest dùng `@mipmap/ic_launcher` cho icon thường và roundIcon. API 24–25 dùng bitmap trong khung trong suốt 48×48dp; phần ảnh giữ tỷ lệ 2:1. API 26+ dùng adaptive icon có background trong suốt, foreground giữ tỷ lệ và nằm trong vùng an toàn của mask.
- Không thêm bản monochrome thay màu thương hiệu. Cảnh báo `MonochromeLauncherIcon` được bỏ qua ở đúng resource; launcher vẫn có thể áp dụng hiệu ứng theo cài đặt hệ điều hành. Pixel Launcher trên máy ảo API 35 gộp vùng alpha của adaptive icon thành đĩa đen; app không gán nền trắng. Logo bên trong các màn hình giữ alpha thật và không có khung.
- Generator resource lịch sử không quản lý logo hiện tại. Không chạy lại generator để cập nhật UI.
- Không sửa giao thức IRC, kho dữ liệu, preferences hoặc logic điều hướng.

Kiểm chứng cập nhật: `assembleDebug`, `lintDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest` thành công; 30 unit test và 3 bài AppFlowTest qua. Lint 0 lỗi, 8 cảnh báo có sẵn. Formatter và `git diff --check` qua. Ảnh kiểm tra lấy từ máy ảo API 35 riêng, bao gồm logo trên nền sáng/tối và icon trong launcher. Không cài lại hoặc xóa dữ liệu máy ảo emulator-5554 của người dùng.

Nguồn kỹ thuật: [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).
