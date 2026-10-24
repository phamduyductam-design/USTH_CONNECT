# Kiến thức slide và cách áp dụng

Đã đọc đủ 7 PDF người dùng cung cấp. Đây là tài liệu học, không phải tập yêu cầu triển khai mọi bài thực hành. Các ví dụ Weather/Forecast, tải logo, MP3, OpenWeather và Volley không được đưa thành tính năng của USTH Connect. Các số phiên bản, số liệu thị phần/hardware trong slide là bối cảnh lịch sử.

| Tài liệu / kiến thức trong slide | Lớp hoặc file trong dự án | Cách áp dụng |
|---|---|---|
| `1. intro`: learning outcomes, IRC client, presentation Architecture/Activities/Networking/Optimization/Demo | README; docs/DEFENSE.md; cả app | Chọn đề tài IRC client; mô tả thành phần, kiểm thử, phần đã làm và giới hạn để bảo vệ |
| `3. intro_android`: OOP, package, decomposition, XML/JSON, asynchronous callbacks, Android Studio | package ui/model/irc/repository/storage; Gradle; assets | Model độc lập, listener interface, cấu trúc rõ trách nhiệm, native Java/XML |
| `4. fundamentals`: Context và Application | ConnectApplication; CourseRepository; SettingsStore | Application Context đọc assets/resources/preferences; session sống theo process |
| `4. fundamentals`: manifest, permissions, compilation/DEX | AndroidManifest.xml; app/build.gradle | Khai báo Activity, Application, INTERNET; Gradle biên dịch Java thành APK/DEX |
| `4. fundamentals`: Activity lifecycle, Intent, Bundle, screen orientation | ConnectActivity; MainActivity; ChatFragment | Intent chuyển Activity; onStart/onStop quản lý listener; Bundle giữ target/vị trí khi tạo lại |
| `4. fundamentals`: FragmentManager, DialogFragment | các Fragment; MembersBottomSheet; MainActivity | Một Activity điều phối từng Fragment, back stack, bottom sheet thành viên |
| `4. fundamentals`: View/ViewGroup, Button/EditText, click handling | res/layout; UiHelpers; ChatFragment | XML tạo View; đọc EditText; nút gọi repository; không networking trong callback UI |
| `5. resources`: LinearLayout, FrameLayout, layout_weight, ScrollView, RecyclerView | activity_main.xml; fragment_chat.xml; fragment_profile.xml | Content co giãn bằng weight; chat/composer dùng không gian còn lại; form/profile cuộn |
| `5. resources`: dp/sp, padding/margin, values, styles, Vietnamese resources | strings.xml; colors.xml; dimens.xml; themes.xml | Chữ Việt, bảng màu của ảnh, spacing/lề, font hệ thống; không khóa theo pixel ảnh |
| `5. resources`: drawables, raw/assets, menus, adapter | ic_*.xml; courses.json; navigation.xml; CatalogAdapter; ChatAdapter | Vector icon nét, danh mục bundled, menu bottom nav, tái sử dụng row |
| `6. tasks`: main/worker thread, Thread, Handler, Looper | IrcClient; IrcSessionController | Reader/writer chạy ngoài UI; Handler(Looper.getMainLooper()) chuyển kết quả về UI |
| `6. tasks`: AsyncTask bao gói Thread/Handler | IrcClient/IrcSessionController; giải thích dưới đây | Giữ nguyên ý tưởng tách background và main callback, dùng ExecutorService cho code mới |
| `7. networking.p1`: INTERNET, java.net, streams, client/server | Manifest; IrcClient | Permission INTERNET normal; Socket/SSLSocket, InputStream/OutputStream TCP |
| `7. networking.p2`: JSONObject/JSONArray, structured data | CourseRepository; courses.json | Đọc nhóm môn/kênh và metadata cục bộ bằng parser JSON built-in |
| `7. networking.p2`: Volley request queue và callback | Không dùng Volley; đối chiếu IrcClient | Volley giải quyết HTTP; IRC là dòng TCP hai chiều nên dùng reader/writer của Socket |

## AsyncTask và công cụ thay thế

Slide minh họa `onPreExecute`/`doInBackground`/`onProgressUpdate`/`onPostExecute`. Bản mới dùng thao tác bắt đầu từ main thread, ExecutorService xử lý I/O, rồi Handler.post trả callback về main thread. Tương ứng tư tưởng vẫn là “không làm việc chậm trên UI và không chạm View từ worker”.

AsyncTask đã deprecated từ API 30; xem [Android AsyncTask reference](https://developer.android.com/reference/android/os/AsyncTask). ExecutorService phù hợp Java và phiên Socket dài hơn một tác vụ tải ảnh. Một reader blocking không được dùng chung single executor với writer, vì nó có thể chặn gửi và PONG.

## Kiến thức bổ sung ngoài slide

| Phần bổ sung | Lý do cần | Nơi thể hiện |
|---|---|---|
| IRC wire protocol / numeric replies / RFC1459 case folding | HTTP examples không mô tả IRC | IrcParser, IrcCommands, SessionState; [RFC 2812](https://www.rfc-editor.org/rfc/rfc2812) |
| TLS hostname verification | Certificate chain hợp lệ chưa đủ nếu sai hostname | IrcClient, SSLParameters; [Java 17 reference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/net/ssl/SSLParameters.html) |
| Epoch để bỏ callback phiên cũ | Cancel/reconnect xảy ra đồng thời với reader/writer | IrcClient và IrcSessionController |
| Backoff, timer timeout, fixed delay heartbeat | Kết nối dài cần phục hồi và hủy đúng | IrcClient |
| AndroidX, Material BottomSheet, insets/edge-to-edge | Thay Support Library cũ, hỗ trợ Android hiện tại | Gradle, MembersBottomSheet, WindowInsetsHelper |
| POST_NOTIFICATIONS runtime từ Android 13 | Thông báo phải tuân theo quyền và thiết lập hệ thống | Manifest, ProfileFragment, ConnectApplication |
| SharedPreferences cho nháp/cấu hình | Các tài liệu đính kèm không có chương Storage riêng | SettingsStore |
| JUnit, Espresso, local IRC daemon smoke test | Kiểm chứng parser, lifecycle và dữ liệu thực | src/test, src/androidTest, tools/NetworkSmoke.java |

Slide từng dùng cách hạ target SDK để tránh xin quyền trong bài tập cũ. Dự án này giữ target 35, xử lý quyền Android phù hợp; không áp dụng mẹo đó cho app mới.
