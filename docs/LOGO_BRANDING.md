# Đồng bộ logo USTH

Logo ở màn Kết nối và icon ứng dụng dùng chung `drawable-nodpi/usth_university_logo.jpg`, giữ nguyên bytes ảnh người dùng cung cấp, tỷ lệ và màu đỏ/xanh. SHA-256: `73073ce0251c328724e9f3bbec0cff2a9060136d419a313b01160b349067817a`.

- Manifest dùng `@mipmap/ic_launcher` cho icon thường và roundIcon.
- Android 7/API 24–25 có fallback bitmap XML trong `mipmap-anydpi/`.
- Android 8/API 26+ dùng adaptive icon trong `mipmap-anydpi-v26/`, nền trắng cố định. Foreground dùng ảnh gốc với khoảng chừa theo tỷ lệ để logo không bị cắt bởi mask launcher.
- Không tạo lại logo bằng AI hoặc vector. Không tạo bản monochrome thay màu thương hiệu; chỉ bỏ qua cảnh báo lint MonochromeLauncherIcon ở đúng resource này. Hệ điều hành/launcher có thể áp dụng themed icons theo lựa chọn người dùng.
- Xóa vector icon chat cũ; generator resource lịch sử không còn tạo lại vector đó. Bỏ màu brand_red không còn được resource nào sử dụng; màu sắc giao diện và ảnh logo không thay đổi.
- Không sửa logic IRC, preferences, giao diện kết nối hoặc dữ liệu người dùng.

Kiểm chứng: assembleDebug, lintDebug và 30 unit test qua; lint 0 lỗi, 8 cảnh báo nền. Cài APK trên bản sao máy ảo API 35, kiểm tra icon USTH đỏ/xanh thực tế trong app drawer. Không cài/xóa dữ liệu máy ảo emulator-5554 của người dùng.

Nguồn kỹ thuật: [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).
