# Gom UI theo chức năng — 23/10/2026

Nền `fbc1a8e`, nhánh `codex/organize-android-features`. Mục tiêu: giảm các lớp cùng nằm ở `ui/`, để tìm ngay theo nhóm môn/kênh, chat, thành viên, cá nhân và thành phần chung. Tên lớp đã rõ được giữ nguyên.

Đây là tổ chức package Android Java theo chức năng/trách nhiệm, không phải quy ước package cụ thể do lecture MOBI đưa ra. Bằng chứng decomposition/OOP và ví dụ Activity/Fragment/Adapter trong lecture được ghi trong [FILE_NAMING.md](FILE_NAMING.md).

| Vị trí cũ dưới `ui/` | Vị trí mới dưới `ui/` | Chức năng / lý do |
|---|---|---|
| `CoursesFragment.java` | `courses/CoursesFragment.java` | Danh mục môn, dễ tìm cùng kênh môn |
| `CourseChannelsFragment.java` | `courses/CourseChannelsFragment.java` | Kênh của môn đang chọn |
| `ServerChannelsFragment.java` | `courses/ServerChannelsFragment.java` | Phòng LIST thật từ server, cùng nhóm khám phá kênh |
| `ChatFragment.java` | `chat/ChatFragment.java` | Chat kênh/PM, composer, nháp và lịch sử |
| `ConversationsFragment.java` | `chat/ConversationsFragment.java` | Danh sách hội thoại riêng |
| `MembersBottomSheet.java` | `members/MembersBottomSheet.java` | Danh sách thành viên/vai trò và mở quản trị |
| `ProfileFragment.java` | `profile/ProfileFragment.java` | Hồ sơ, nickname, presence, thiết lập |
| `SessionAwareFragment.java` | `common/SessionAwareFragment.java` | Lifecycle/listener chung, không phải một màn hình |
| `UiHelpers.java` | `common/UiHelpers.java` | Avatar, ngày giờ, trạng thái, theme toggle dùng lại |
| `AppTheme.java` | `common/AppTheme.java` | Áp dụng sáng/tối dùng chung |
| `WindowInsetsHelper.java` | `common/WindowInsetsHelper.java` | Insets hệ thống/IME cho hai Activity |

Các Activity vẫn ở `ui/ConnectActivity.java` và `ui/MainActivity.java` để giữ nguyên component launcher/Intent/thông báo. `ui/dialog/` và `adapter/` tiếp tục chứa thành phần tái sử dụng có tên rõ. Model, storage, repository và IRC giữ nguyên vị trí/trách nhiệm; không gom socket hoặc lưu trữ vào Fragment.

## Tương thích và phạm vi

Di chuyển Fragment đổi tên lớp đầy đủ mà Android lưu trong task/back stack. `FeatureFragmentFactory` ánh xạ 7 tên cũ sang lớp mới, cài trước `super.onCreate()` của MainActivity; các tên khác chuyển tiếp cho factory chuẩn. Cách khôi phục tuân theo [Android FragmentManager/FragmentFactory](https://developer.android.com/guide/fragments/fragmentmanager). Không tạo lớp wrapper trùng cho từng màn hình.

Thân các lớp đã di chuyển được kiểm tra tương đương sau khi bỏ khai báo package/import và chuẩn hóa khoảng trắng. Riêng WindowInsetsHelper cùng hai hàm apply được mở public để Activity ở package khác gọi được. MainActivity thêm bước cài factory; ConnectApplication/adapter/dialog cập nhật tham chiếu/import. Không đổi phương thức xử lý mạng, thuật toán, tên biến/hàm, layout/XML, resource/theme/logo, application ID, quyền manifest, SharedPreferences hay dữ liệu môn/người dùng.

Các tên cũ còn xuất hiện có chủ đích trong factory tương thích, test nâng cấp và tài liệu lịch sử. [CODE_MAP.md](CODE_MAP.md), [QUICK_REFERENCE.md](QUICK_REFERENCE.md) và cây thư mục trong README là hướng dẫn hiện tại. Không di chuyển file Gradle/.vscode vì đây là vị trí chuẩn của build/IDE.

## Kiểm tra lại

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest
python3 tools/format_code.py --check
bash tools/network-smoke.sh 127.0.0.1 17667 false
# Trên thiết bị kiểm thử riêng, với daemon local tại 17667:
adb shell am instrument -w -r -e server 10.0.2.2 -e port 17667 \
  vn.edu.usth.connect.test/androidx.test.runner.AndroidJUnitRunner
```

Kết quả nghiệm thu được ghi trong [UI_ORGANIZATION_VALIDATION.md](UI_ORGANIZATION_VALIDATION.md); log/APK/ảnh nằm ngoài Git.
