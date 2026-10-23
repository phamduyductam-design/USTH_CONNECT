# Kiểm chứng gom package UI — 23/10/2026

Nền `fbc1a8e`, nhánh `codex/organize-android-features`; JDK 17, compile/target SDK 35, Android emulator API 35 riêng ở 390×844 dp. IRC daemon Ergo riêng trên 127.0.0.1:17667. Không dùng máy ảo/dữ liệu của phiên demo cá nhân.

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug, assembleDebugAndroidTest | Thành công |
| testDebugUnitTest | 30/30: parser/commands 11, transport 5, SessionState 14 |
| lintDebug | Thành công, 0 lỗi / 9 cảnh báo về target SDK, bản thư viện và notifyDataSetChanged; các dependency/phương thức này không đổi |
| format-code.py --check | 47 Java, 66 XML; không có khác biệt XML |
| Kiểm tra diff resource, manifest, assets, IRC/model/repository/storage | Không thay đổi so với nền |
| So sánh 11 lớp di chuyển | Thân lớp tương đương sau chuẩn hóa package/import/khoảng trắng; WindowInsetsHelper chỉ thêm public cho class/hai hàm apply |
| AndroidJUnitRunner | 4/4 test trên emulator, 38.936 giây |
| Smoke hai Java client TCP thật | Đăng ký 001, JOIN/NAMES, UTF-8/PM, NICK/PART/QUIT, LIST, TOPIC/MODE/quyền, AWAY/WHO, mention và KICK đều qua |
| git diff --check | Qua |

Ba AppFlowTest giữ nguyên nội dung kiểm thử từ nền: môn/kênh, trao đổi hai client, thành viên/PM, xoay và theme giữ nháp/session, phòng server và quản trị, ba tab/empty state/bàn phím. Test mới FragmentCompatibilityTest kiểm tra cả 7 tên lớp cũ và việc chuyển tiếp tên mới/Fragment mặc định cho factory chuẩn. Đây là kiểm tra tương thích tên lưu trong state; không tuyên bố đã nâng cấp trên mọi thiết bị Android.

Đã chụp 11 ảnh tại các bước kiểm thử thật để xem lại kết nối, môn/kênh, chat/PM, thành viên, cá nhân, bàn phím và sáng/tối. Layout, màu, logo và dữ liệu môn không đổi byte nào. Chưa đo benchmark FPS/pin hoặc chạy lại trên điện thoại thật; đợt này không thay đổi cách render/transport để suy ra mức cải thiện hiệu năng.

SDK/AVD/Ergo/formatter, log, APK và ảnh nằm ngoài Git. Cấu hình trước đó trong các checkout của người dùng được giữ nguyên. Hướng dẫn tìm code: [QUICK_REFERENCE.md](QUICK_REFERENCE.md), [CODE_MAP.md](CODE_MAP.md); bảng chuyển vị trí và cơ chế restore: [UI_ORGANIZATION.md](UI_ORGANIZATION.md).
