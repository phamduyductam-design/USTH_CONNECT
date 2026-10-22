"""Legacy baseline generator, retained for historical reference.

The redesigned UI is maintained directly in app/src/main/res. Do not run this
script on the current app: it recreates the old design and can overwrite it.
"""
from pathlib import Path
import json
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main'
def write(name, text):
    p = ROOT / name
    p.parent.mkdir(parents=True, exist_ok=True)
    if name.endswith('.xml'):
        ET.register_namespace('android','http://schemas.android.com/apk/res/android')
        ET.register_namespace('app','http://schemas.android.com/apk/res-auto')
        ET.register_namespace('tools','http://schemas.android.com/tools')
        tree=ET.fromstring(text)
        if name in ['res/layout/activity_main.xml','res/layout/activity_connect.xml','res/layout/dialog_members.xml']:
            tree.attrib.pop('{http://schemas.android.com/apk/res/android}background',None)
        if name.startswith('res/layout/'):
            sizes={'24dp':'space_24','80dp':'row_height','96dp':'empty_icon','72dp':'profile_avatar'}
            for node in tree.iter():
                for key,value in list(node.attrib.items()):
                    if value in sizes:node.attrib[key]='@dimen/'+sizes[value]
        ET.indent(tree, space='    ')
        text=ET.tostring(tree,encoding='unicode')
    p.write_text(text.strip()+'\n', encoding='utf-8')

strings = {
 'app_name':'USTH Connect', 'usth':'USTH', 'connect_word':'Connect', 'tagline':'Kết nối cùng bạn học',
 'nickname':'Biệt danh', 'server':'Máy chủ', 'server_hint':'irc.example.edu', 'server_note':'Nhập máy chủ IRC của bạn. Địa chỉ mẫu không phải máy chủ thật của USTH.',
 'advanced':'Tùy chọn nâng cao', 'port':'Cổng', 'tls':'Kết nối bảo mật TLS', 'username':'Username', 'real_name':'Real name',
 'connect':'Kết nối', 'cancel':'Hủy', 'retry':'Thử lại', 'reconnect':'Kết nối lại', 'connecting':'Đang kết nối đến máy chủ…', 'registering':'Đang chờ máy chủ chấp nhận biệt danh…',
 'connected':'Đã kết nối', 'disconnected':'Đã ngắt kết nối', 'reconnecting':'Đang kết nối lại…', 'failed':'Không thể kết nối', 'connect_footer':'Biệt danh sẽ hiển thị trong các kênh.',
 'courses':'Môn học', 'messages':'Tin nhắn', 'profile':'Cá nhân', 'search_courses':'Tìm môn hoặc kênh', 'joined':'Đã tham gia', 'discover':'Khám phá',
 'join_channel':'Tham gia kênh', 'channel_name':'Tên kênh, ví dụ #web-general', 'channel_key':'Key (nếu kênh yêu cầu)', 'join':'Tham gia', 'joining':'Đang tham gia…',
 'channel_info':'Kênh môn học', 'joined_channels':'Kênh đã tham gia', 'available_channels':'Kênh có thể tham gia', 'other_channels':'Kênh khác',
 'course_count':'%1$d kênh · %2$d tin mới', 'no_courses':'Bạn chưa tham gia môn nào', 'no_courses_detail':'Chọn môn học hoặc nhập tên kênh để bắt đầu trao đổi.', 'no_results':'Không có kết quả phù hợp',
 'back':'Quay lại', 'leave':'Rời kênh', 'leave_title':'Rời %1$s?', 'leave_detail':'Bạn có thể tham gia lại kênh sau.', 'members':'Thành viên', 'member_count':'%1$d thành viên',
 'search_members':'Tìm thành viên', 'you':'Bạn', 'private_message':'Nhắn riêng', 'close':'Đóng', 'recent':'Gần đây', 'search_nick':'Tìm theo biệt danh', 'new_conversation':'Tạo hội thoại',
 'no_conversations':'Chưa có hội thoại riêng', 'no_conversations_detail':'Chọn thành viên trong kênh hoặc nhập biệt danh để nhắn riêng.', 'conversation_empty':'Hội thoại mới',
 'chat_hint':'Nhắn vào %1$s', 'pm_hint':'Nhắn cho %1$s', 'send':'Gửi', 'chat_empty':'Bắt đầu cuộc trò chuyện', 'chat_empty_detail':'Chưa có tin nhắn trong kênh này. Gửi lời chào đến các bạn cùng nhóm.',
 'pm_empty_detail':'Gửi tin nhắn đầu tiên để bắt đầu trao đổi.', 'new_messages':'Tin nhắn mới', 'today':'Hôm nay', 'yesterday':'Hôm qua', 'latest':'Tin mới · xuống cuối',
 'connection_lost':'Mất kết nối với máy chủ', 'offline_hint':'Kết nối lại để gửi tin.', 'not_joined':'Tham gia kênh để gửi tin.', 'no_topic':'Kênh chưa có chủ đề.',
 'queued':'Đang ghi vào kết nối…', 'written':'Đã ghi vào kết nối · chưa có xác nhận từ người nhận', 'send_failed':'Chưa gửi / máy chủ từ chối', 'send_uncertain':'Chưa rõ đã gửi hay chưa',
 'retry_title':'Gửi lại tin nhắn?', 'retry_detail':'Máy chủ có thể đã nhận tin trước khi kết nối bị mất. Gửi lại có thể tạo tin trùng. Chỉ gửi lại khi bạn xác nhận.',
 'event_join':'%1$s đã vào kênh', 'event_part':'%1$s đã rời kênh', 'event_quit':'%1$s đã ngắt kết nối', 'event_kick':'%1$s bị đưa ra khỏi kênh', 'event_nick':'%1$s đổi biệt danh thành %2$s',
 'connection':'Kết nối', 'options':'Tùy chọn', 'notifications':'Thông báo', 'connection_options':'Tùy chọn kết nối', 'change':'Đổi', 'disconnect':'Ngắt kết nối', 'disconnect_title':'Ngắt kết nối?',
 'disconnect_detail':'Bạn sẽ rời các kênh đang tham gia. Kết nối lại tự động sẽ dừng.', 'change_nickname':'Đổi biệt danh', 'confirm':'Xác nhận',
 'settings_detail':'Máy chủ: %1$s\nCổng: %2$d\nTLS: %3$s\nUsername: %4$s\nReal name: %5$s\n\nMuốn sửa cấu hình, hãy ngắt kết nối rồi nhập lại trên màn hình Kết nối.',
 'yes':'Bật', 'no':'Tắt', 'permission_denied':'Thông báo chưa được cấp quyền trong Android. Bạn có thể bật trong Cài đặt hệ thống.',
 'foreground_limit':'Phiên chat được ưu tiên khi ứng dụng đang sử dụng. Android có thể dừng kết nối khi hủy process.', 'notification_title':'%1$s · %2$s',
 'error_config':'Kiểm tra máy chủ, cổng (1–65535), username và real name.', 'error_nick':'Biệt danh bắt đầu bằng chữ hoặc _; tối đa 30 ký tự ASCII, không có khoảng trắng.',
 'error_channel':'Nhập tên kênh bắt đầu bằng # hoặc &; không có khoảng trắng, dấu phẩy hay ký tự điều khiển.', 'error_message':'Tin nhắn trống, có ký tự điều khiển hoặc quá dài (tối đa 512 byte gồm lệnh IRC và CRLF).',
 'error_network':'Không kết nối được hoặc kết nối đã bị đóng. Kiểm tra mạng, địa chỉ và cổng.', 'error_tls':'Không xác minh được kết nối TLS. Kiểm tra hostname và chứng chỉ máy chủ.',
 'error_timeout':'Máy chủ chưa phản hồi trong thời gian chờ.', 'error_server':'Máy chủ đã đóng phiên IRC.', 'error_join_timeout':'Máy chủ chưa xác nhận JOIN. Bạn có thể thử tham gia lại.',
 'error_433':'Biệt danh đã được sử dụng. Hãy chọn biệt danh khác.', 'error_432':'Máy chủ không chấp nhận định dạng biệt danh này.', 'error_401':'Không tìm thấy người nhận hoặc kênh.',
 'error_404':'Máy chủ không cho phép gửi tin vào kênh này.', 'error_471':'Kênh đã đủ thành viên.', 'error_473':'Kênh chỉ dành cho người được mời.', 'error_474':'Bạn bị cấm tham gia kênh.',
 'error_475':'Key của kênh không đúng.', 'error_403':'Kênh không tồn tại hoặc tên kênh không hợp lệ.', 'error_442':'Bạn chưa tham gia kênh.', 'error_482':'Bạn không có quyền thực hiện thao tác này.',
 'error_464':'Máy chủ yêu cầu mật khẩu. Phiên bản này chưa hỗ trợ PASS/SASL.', 'error_465':'Máy chủ từ chối kết nối từ thiết bị này.', 'error_generic':'Máy chủ từ chối thao tác (mã %1$s).',
 'error_offline':'Cần kết nối với máy chủ để thực hiện thao tác.', 'error_caption':'%1$s · %2$s', 'course_subtitle':'Cùng học, cùng trao đổi.', 'empty_members':'Chưa nhận danh sách thành viên.',
 'nickname_pending':'Đang chờ máy chủ xác nhận biệt danh.', 'network_retry':'Thử kết nối lại sau %1$s giây…', 'tap_to_open':'Mở hội thoại', 'draft_saved':'Nháp được giữ theo máy chủ và hội thoại.'
}
for key in ['failed','channel_info','connection_lost','change_nickname','confirm','course_subtitle','tap_to_open','draft_saved']:
 strings.pop(key)
write('res/values/strings.xml', '<resources xmlns:tools="http://schemas.android.com/tools">\n'+''.join(f'    <string name="{k}"'+(' tools:ignore="PluralsCandidate"' if k=='course_count' else '')+f'>{escape(v)}</string>\n' for k,v in strings.items())+'</resources>')
colors={'primary':'#2E3B8F','brand_red':'#EF2028','surface':'#FFFFFF','background':'#F5F7FB','text_primary':'#172033','text_secondary':'#526079','error':'#C51D2B','outline':'#DCE2EF','selected':'#EBF1FF','error_bg':'#FFF0F2','badge':'#1261FF','purple':'#7650CE','teal':'#008E87','disabled':'#B7BECE'}
colors.pop('disabled')
write('res/values/colors.xml','<resources>\n'+''.join(f'<color name="{k}">{v}</color>\n' for k,v in colors.items())+'</resources>')
dimensions={'space_4':'4dp','space_8':'8dp','space_12':'12dp','space_16':'16dp','space_20':'20dp','space_24':'24dp','space_32':'32dp','touch':'48dp','input_height':'56dp','input_radius':'12dp','card_radius':'16dp','avatar':'48dp','badge_size':'28dp','title_size':'28sp','heading_size':'20sp','body_size':'16sp','caption_size':'12sp','logo_size':'48sp','logo_connect_size':'34sp','row_icon':'52dp','line':'1dp','underline_height':'4dp','underline_width':'32dp','sheet_height':'440dp','row_height':'80dp','empty_icon':'96dp','profile_avatar':'72dp'}
write('res/values/dimens.xml','<resources>\n'+''.join(f'<dimen name="{k}">{v}</dimen>\n' for k,v in dimensions.items())+'</resources>')
write('res/values-sw600dp/dimens.xml','<resources><dimen name="space_20">40dp</dimen></resources>')
write('res/values/themes.xml','''<resources>
 <style name="Base.Theme.UsthConnect" parent="Theme.MaterialComponents.Light.NoActionBar">
  <item name="colorPrimary">@color/primary</item><item name="colorPrimaryVariant">@color/primary</item><item name="colorSecondary">@color/badge</item>
  <item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">true</item><item name="android:statusBarColor">@color/surface</item>
  <item name="android:navigationBarColor">@color/primary</item><item name="android:windowBackground">@color/surface</item>
  <item name="android:colorAccent">@color/primary</item><item name="android:windowActionModeOverlay">true</item>
 </style>
 <style name="Theme.UsthConnect" parent="Base.Theme.UsthConnect"/>
 <style name="Body"><item name="android:textColor">@color/text_primary</item><item name="android:textSize">@dimen/body_size</item></style>
 <style name="Caption" parent="Body"><item name="android:textColor">@color/text_secondary</item><item name="android:textSize">@dimen/caption_size</item></style>
 <style name="Heading" parent="Body"><item name="android:textSize">@dimen/heading_size</item><item name="android:textStyle">bold</item></style>
 <style name="Title" parent="Heading"><item name="android:textSize">@dimen/title_size</item></style>
 <style name="Input" parent="Body"><item name="android:background">@drawable/bg_input</item><item name="android:minHeight">@dimen/input_height</item>
  <item name="android:padding">@dimen/space_12</item><item name="android:textColorHint">@color/text_secondary</item><item name="android:singleLine">true</item></style>
 <style name="PrimaryButton" parent="Widget.MaterialComponents.Button"><item name="backgroundTint">@color/primary</item><item name="cornerRadius">@dimen/input_radius</item>
  <item name="android:minHeight">@dimen/input_height</item><item name="android:textAllCaps">false</item><item name="android:textSize">@dimen/body_size</item><item name="android:insetTop">0dp</item><item name="android:insetBottom">0dp</item></style>
 <style name="OutlineButton" parent="Widget.MaterialComponents.Button.OutlinedButton"><item name="cornerRadius">@dimen/input_radius</item>
  <item name="android:minHeight">@dimen/touch</item><item name="android:textAllCaps">false</item><item name="android:textColor">@color/primary</item><item name="strokeColor">@color/primary</item></style>
 <style name="IconButton"><item name="android:layout_width">@dimen/touch</item><item name="android:layout_height">@dimen/touch</item>
  <item name="android:background">?attr/selectableItemBackgroundBorderless</item><item name="android:padding">@dimen/space_12</item></style>
</resources>''')
write('res/values-v27/themes.xml','<resources><style name="Theme.UsthConnect" parent="Base.Theme.UsthConnect"><item name="android:navigationBarColor">@color/surface</item><item name="android:windowLightNavigationBar">true</item></style></resources>')

for name,color,radius,stroke in [('input','background','input_radius',True),('card','surface','card_radius',True),('selected','selected','card_radius',False),('error','error_bg','input_radius',False),('primary','primary','input_radius',False)]:
 write(f'res/drawable/bg_{name}.xml',f'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="@color/{color}"/><corners android:radius="@dimen/{radius}"/>'+('<stroke android:width="@dimen/line" android:color="@color/outline"/>' if stroke else '')+'</shape>')
write('res/drawable/bg_avatar.xml','<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval"><solid android:color="@color/badge"/></shape>')
write('res/color/navigation_item_tint.xml','<selector xmlns:android="http://schemas.android.com/apk/res/android"><item android:state_checked="true" android:color="@color/primary"/><item android:color="@color/text_secondary"/></selector>')
icons={
 'back':'M15,5 L8,12 L15,19', 'close':'M5,5 L19,19 M19,5 L5,19', 'search':'M10,3 A7,7 0,1 0,10,17 A7,7 0,1 0,10,3 M15,15 L21,21',
 'book':'M12,5 C8,2 3,3 2,4 L2,20 C5,18 9,18 12,21 C15,18 19,18 22,20 L22,4 C19,2 15,2 12,5 L12,21',
 'chat':'M5,3 L19,3 Q22,3 22,6 L22,15 Q22,18 19,18 L9,18 L3,22 L3,6 Q3,3 5,3',
 'person':'M12,2 A4,4 0,1 0,12,10 A4,4 0,1 0,12,2 M4,22 L4,19 C4,12 20,12 20,19 L20,22 Z',
 'members':'M9,3 A4,4 0,1 0,9,11 A4,4 0,1 0,9,3 M2,22 L2,20 C2,13 16,13 16,20 L16,22 M17,4 C23,4 23,11 17,11 M19,15 C22,16 23,18 23,22',
 'hash':'M9,2 L6,22 M18,2 L15,22 M3,8 L22,8 M2,16 L21,16',
 'code':'M7,6 L1,12 L7,18 M17,6 L23,12 L17,18 M15,3 L9,21',
 'brain':'M12,3 C7,0 4,4 5,7 C0,9 1,15 5,16 C3,21 8,24 12,20 C16,24 21,21 19,16 C23,15 24,9 19,7 C20,4 17,0 12,3 L12,20 M7,8 L7,15 M17,8 L17,15',
 'phone':'M6,1 L18,1 L18,23 L6,23 Z M10,19 L14,19',
 'plus':'M12,4 L12,20 M4,12 L20,12', 'chevron':'M9,5 L16,12 L9,19',
 'settings':'M4,7 L20,7 M4,17 L20,17 M8,4 L8,10 M16,14 L16,20',
 'server':'M3,3 L21,3 L21,8 L3,8 Z M3,10 L21,10 L21,15 L3,15 Z M3,17 L21,17 L21,22 L3,22 Z M6,5 L7,5 M6,12 L7,12 M6,19 L7,19',
 'edit':'M4,16 L4,21 L9,21 L21,9 L16,4 Z M14,6 L19,11',
 'logout':'M10,3 L3,3 L3,21 L10,21 M9,12 L23,12 M17,6 L23,12 L17,18',
 'notification':'M4,17 L20,17 C17,13 19,4 12,4 C5,4 7,13 4,17 M10,21 L14,21 M12,1 L12,4'
}
for name,path in icons.items():
 write(f'res/drawable/ic_{name}.xml',f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:pathData="{path}" android:fillColor="@android:color/transparent" android:strokeColor="@color/primary" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round"/></vector>')
write('res/drawable/ic_send.xml','<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:pathData="M2,11 L22,2 L15,22 L10,14 L17,7 L8,12 Z" android:fillColor="@color/surface"/></vector>')
# Launcher icons reuse the transparent USTH PNG through mipmap XML; maintained separately.
write('res/menu/navigation.xml','<menu xmlns:android="http://schemas.android.com/apk/res/android">'+''.join(f'<item android:id="@+id/nav_{k}" android:title="@string/{k}" android:icon="@drawable/ic_{ic}"/>' for k,ic in [('courses','book'),('messages','chat'),('profile','person')])+'</menu>')

NS='xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto"'
def tv(id,text='',style='Body',extra=''):
 return f'<TextView android:id="@+id/{id}" style="@style/{style}" android:layout_width="match_parent" android:layout_height="wrap_content" '+(f'android:text="@string/{text}" ' if text else '')+extra+'/>'
def inp(id,hint,extra=''):
 return f'<EditText android:id="@+id/{id}" style="@style/Input" android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="@string/{hint}" '+('android:inputType="text" ' if 'android:inputType=' not in extra else '')+('android:autofillHints="" ' if 'android:autofillHints=' not in extra else '')+extra+'/>'
def btn(id,text,style='PrimaryButton',extra=''):
 return f'<com.google.android.material.button.MaterialButton android:id="@+id/{id}" style="@style/{style}" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/{text}" '+extra+'/>'
def icon(id,name,desc):
 return f'<ImageButton android:id="@+id/{id}" style="@style/IconButton" android:src="@drawable/ic_{name}" android:contentDescription="@string/{desc}"/>'
underline='<View android:layout_width="@dimen/underline_width" android:layout_height="@dimen/underline_height" android:background="@color/brand_red" android:layout_marginTop="@dimen/space_4" android:layout_marginBottom="@dimen/space_16"/>'
margin='android:layout_marginTop="@dimen/space_16" '
write('res/layout/activity_connect.xml',f'''<ScrollView {NS} android:id="@+id/root" android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true" android:background="@color/surface">
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="@dimen/space_20">
  {tv('logo_usth','usth','Title','android:gravity="center" android:textColor="@color/primary" android:textSize="@dimen/logo_size" android:layout_marginTop="@dimen/space_32"')}
  <View android:layout_width="@dimen/underline_width" android:layout_height="@dimen/underline_height" android:layout_gravity="center" android:background="@color/brand_red"/>
  {tv('logo_connect','connect_word','Title','android:gravity="center" android:textSize="@dimen/logo_connect_size"')}
  {tv('tagline','tagline','Body','android:gravity="center" android:layout_marginTop="@dimen/space_8"')}
  <ImageView android:layout_width="80dp" android:layout_height="80dp" android:layout_gravity="center" android:layout_marginTop="@dimen/space_24" android:layout_marginBottom="@dimen/space_24" android:padding="@dimen/space_16" android:background="@drawable/bg_selected" android:src="@drawable/ic_chat" android:importantForAccessibility="no"/>
  {tv('nickname_label','nickname')}{inp('nickname','nickname','android:inputType="textVisiblePassword" android:autofillHints="username"')}
  {tv('server_label','server','Body',margin)}{inp('host','server_hint','android:inputType="textUri"')}
  {tv('server_note','server_note','Caption','android:layout_marginTop="@dimen/space_8"')}
  {btn('advanced_toggle','advanced','OutlineButton',margin)}
  <LinearLayout android:id="@+id/advanced_panel" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:visibility="gone">
   {tv('port_label','port','Body',margin)}{inp('port','port','android:inputType="number"')}
   <com.google.android.material.switchmaterial.SwitchMaterial android:id="@+id/tls" android:layout_width="match_parent" android:layout_height="@dimen/touch" android:text="@string/tls"/>
   {tv('username_label','username')}{inp('username','username','android:inputType="textVisiblePassword"')}
   {tv('realname_label','real_name','Body',margin)}{inp('real_name','real_name','android:inputType="textPersonName"')}
  </LinearLayout>
  {tv('status','','Body',margin+'android:accessibilityLiveRegion="polite"')}
  <ProgressBar android:id="@+id/progress" android:layout_width="@dimen/touch" android:layout_height="@dimen/touch" android:layout_gravity="center" android:visibility="gone"/>
  {btn('connect','connect',extra=margin)}{btn('cancel','cancel','OutlineButton','android:visibility="gone"')}
  {tv('footer','connect_footer','Caption','android:gravity="center" android:layout_marginTop="@dimen/space_8"')}
 </LinearLayout></ScrollView>''')
write('res/layout/activity_main.xml',f'''<LinearLayout {NS} android:id="@+id/root" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:background="@color/surface">
 <androidx.fragment.app.FragmentContainerView android:id="@+id/container" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"/>
 <com.google.android.material.bottomnavigation.BottomNavigationView android:id="@+id/navigation" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@color/surface" app:menu="@menu/navigation" app:labelVisibilityMode="labeled" app:itemIconTint="@color/navigation_item_tint" app:itemTextColor="@color/navigation_item_tint" app:itemActiveIndicatorStyle="@style/NavIndicator"/>
 </LinearLayout>''')
write('res/values/navigation.xml','<resources><style name="NavIndicator" parent="Widget.Material3.BottomNavigationView.ActiveIndicator"><item name="android:color">@color/selected</item></style></resources>')
write('res/layout/fragment_catalog.xml',f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:paddingStart="@dimen/space_20" android:paddingEnd="@dimen/space_20" android:paddingTop="@dimen/space_12">
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
  {icon('back','back','back')}
  <TextView android:id="@+id/brand" style="@style/Heading" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/app_name" android:textColor="@color/primary"/>
  {icon('action','person','profile')}
 </LinearLayout>
 {tv('title','courses','Title')}{underline}
 {tv('description','','Body','android:visibility="gone" android:background="@drawable/bg_selected" android:padding="@dimen/space_16" android:layout_marginBottom="@dimen/space_12"')}
 {inp('search','search_courses','android:drawableStart="@drawable/ic_search" android:drawablePadding="@dimen/space_8" android:imeOptions="actionDone"')}
 <androidx.recyclerview.widget.RecyclerView android:id="@+id/list" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:clipToPadding="false" android:paddingTop="@dimen/space_16" android:paddingBottom="@dimen/space_12"/>
 {btn('join','join_channel','OutlineButton','android:layout_marginBottom="@dimen/space_8" app:icon="@drawable/ic_plus" app:iconGravity="textStart"')}
 </LinearLayout>''')
write('res/layout/item_header.xml',f'<TextView {NS} android:id="@+id/text" style="@style/Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:textStyle="bold" android:textAllCaps="true" android:paddingTop="@dimen/space_16" android:paddingBottom="@dimen/space_8"/>')
write('res/layout/item_empty.xml',f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center" android:paddingTop="@dimen/space_32" android:paddingBottom="@dimen/space_24">
 <ImageView android:layout_width="96dp" android:layout_height="96dp" android:background="@drawable/bg_selected" android:padding="@dimen/space_20" android:src="@drawable/ic_book" android:importantForAccessibility="no"/>
 {tv('empty_title','','Heading','android:gravity="center" android:layout_marginTop="@dimen/space_16"')}
 {tv('empty_detail','','Body','android:gravity="center" android:textColor="@color/text_secondary" android:layout_marginTop="@dimen/space_8"')}
 </LinearLayout>''')
write('res/layout/item_catalog.xml',f'''<LinearLayout {NS} android:id="@+id/row" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="80dp" android:orientation="horizontal" android:gravity="center_vertical" android:padding="@dimen/space_12" android:layout_marginBottom="@dimen/space_8" android:background="@drawable/bg_card">
 <FrameLayout android:layout_width="@dimen/row_icon" android:layout_height="@dimen/row_icon">
  <ImageView android:id="@+id/icon" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="@dimen/space_8" android:background="@drawable/bg_selected" android:importantForAccessibility="no"/>
  <TextView android:id="@+id/avatar" style="@style/Heading" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="center" android:background="@drawable/bg_avatar" android:textColor="@color/surface" android:visibility="gone"/>
 </FrameLayout>
 <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:layout_marginStart="@dimen/space_12">
  {tv('name','','Heading','android:textSize="@dimen/body_size"')}{tv('detail','','Caption','android:layout_marginTop="@dimen/space_4"')}{tv('preview','','Body','android:visibility="gone" android:maxLines="2" android:ellipsize="end" android:layout_marginTop="@dimen/space_4"')}
 </LinearLayout>
 <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center" android:layout_marginStart="@dimen/space_8">
  <TextView android:id="@+id/time" style="@style/Caption" android:layout_width="wrap_content" android:layout_height="wrap_content"/>
  <TextView android:id="@+id/badge" style="@style/Body" android:layout_width="@dimen/badge_size" android:layout_height="@dimen/badge_size" android:gravity="center" android:textColor="@color/surface" android:background="@drawable/bg_avatar" android:visibility="gone"/>
 </LinearLayout>
 <com.google.android.material.button.MaterialButton android:id="@+id/join" style="@style/OutlineButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/join" android:visibility="gone"/>
 <ImageView android:id="@+id/chevron" android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_chevron" android:importantForAccessibility="no"/>
 </LinearLayout>''')
write('res/layout/fragment_chat.xml',f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="@dimen/space_8" android:gravity="center_vertical">
  {icon('back','back','back')}
  <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">{tv('title','','Heading')}{tv('subtitle','','Caption')}</LinearLayout>
  <com.google.android.material.button.MaterialButton android:id="@+id/members" style="@style/OutlineButton" android:layout_width="wrap_content" android:layout_height="wrap_content" app:icon="@drawable/ic_members" android:contentDescription="@string/members"/>
  {icon('more','settings','options')}
 </LinearLayout>
 {tv('topic','','Body','android:background="@drawable/bg_selected" android:padding="@dimen/space_12" android:layout_marginHorizontal="@dimen/space_20" android:maxLines="3" android:ellipsize="end"')}
 <LinearLayout android:id="@+id/banner" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:padding="@dimen/space_8" android:layout_marginHorizontal="@dimen/space_20" android:layout_marginTop="@dimen/space_8" android:background="@drawable/bg_error" android:visibility="gone">
  <TextView android:id="@+id/connection_status" style="@style/Caption" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:textColor="@color/error" android:accessibilityLiveRegion="polite"/>
  <com.google.android.material.button.MaterialButton android:id="@+id/reconnect" style="@style/OutlineButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/reconnect"/>
 </LinearLayout>
 <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
  <androidx.recyclerview.widget.RecyclerView android:id="@+id/list" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="@dimen/space_20" android:clipToPadding="false"/>
  <LinearLayout android:id="@+id/empty" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:gravity="center" android:padding="@dimen/space_20">
   <ImageView android:layout_width="96dp" android:layout_height="96dp" android:padding="@dimen/space_20" android:background="@drawable/bg_selected" android:src="@drawable/ic_chat" android:importantForAccessibility="no"/>
   {tv('empty_title','chat_empty','Heading','android:gravity="center" android:layout_marginTop="@dimen/space_16"')}{tv('empty_detail','chat_empty_detail','Body','android:gravity="center" android:textColor="@color/text_secondary" android:layout_marginTop="@dimen/space_8"')}
  </LinearLayout>
 </FrameLayout>
 {btn('latest','latest','OutlineButton','android:layout_marginHorizontal="@dimen/space_20" android:visibility="gone"')}
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="bottom" android:paddingStart="@dimen/space_20" android:paddingEnd="@dimen/space_20" android:paddingTop="@dimen/space_8" android:paddingBottom="@dimen/space_8">
  <EditText android:id="@+id/composer" style="@style/Input" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:inputType="textMultiLine|textCapSentences" android:singleLine="false" android:maxLines="4" android:imeOptions="flagNoExtractUi" android:gravity="top" android:hint="@string/messages" android:autofillHints=""/>
  <ImageButton android:id="@+id/send" android:layout_width="@dimen/input_height" android:layout_height="@dimen/input_height" android:layout_marginStart="@dimen/space_8" android:padding="@dimen/space_16" android:background="@drawable/bg_primary" android:src="@drawable/ic_send" android:contentDescription="@string/send"/>
 </LinearLayout>
 {tv('send_hint','','Caption','android:paddingStart="@dimen/space_20" android:paddingEnd="@dimen/space_20" android:paddingBottom="@dimen/space_8"')}
 </LinearLayout>''')
write('res/layout/item_chat_message.xml',f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingTop="@dimen/space_12" android:paddingBottom="@dimen/space_12">
 <TextView android:id="@+id/avatar" style="@style/Heading" android:layout_width="@dimen/avatar" android:layout_height="@dimen/avatar" android:gravity="center" android:background="@drawable/bg_avatar" android:textColor="@color/surface"/>
 <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:layout_marginStart="@dimen/space_12">
  <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical">
   <TextView android:id="@+id/name" style="@style/Heading" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="@dimen/body_size"/>
   <TextView android:id="@+id/time" style="@style/Caption" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="@dimen/space_8"/>
  </LinearLayout>
  {tv('text','','Body','android:layout_marginTop="@dimen/space_4" android:textIsSelectable="true"')}
  {tv('status','','Caption','android:layout_marginTop="@dimen/space_4"')}
  <com.google.android.material.button.MaterialButton android:id="@+id/retry" style="@style/OutlineButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/retry" android:visibility="gone"/>
 </LinearLayout></LinearLayout>''')
write('res/layout/item_chat_separator.xml',f'<TextView {NS} android:id="@+id/text" style="@style/Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:padding="@dimen/space_12"/>')
write('res/layout/dialog_members.xml',f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="@dimen/sheet_height" android:orientation="vertical" android:padding="@dimen/space_20" android:background="@color/surface">
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical"><TextView style="@style/Title" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/members"/>{icon('close','close','close')}</LinearLayout>
 {tv('subtitle','','Body','android:layout_marginBottom="@dimen/space_16"')}{inp('search','search_members','android:drawableStart="@drawable/ic_search" android:drawablePadding="@dimen/space_8"')}
 <androidx.recyclerview.widget.RecyclerView android:id="@+id/list" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:paddingTop="@dimen/space_12"/>
 </LinearLayout>''')
write('res/layout/dialog_join_channel.xml',f'<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="@dimen/space_20">{inp("channel","channel_name","android:inputType=\"textVisiblePassword\"")}{inp("key","channel_key",margin+"android:inputType=\"textPassword\" android:autofillHints=\"\"")}</LinearLayout>')
write('res/layout/dialog_input.xml',f'<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="@dimen/space_20">{inp("input","nickname","android:inputType=\"textVisiblePassword\"")}</LinearLayout>')
write('res/layout/fragment_profile.xml',f'''<ScrollView {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true"><LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="@dimen/space_20">
 {tv('title','profile','Title')}{underline}
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:background="@drawable/bg_card" android:padding="@dimen/space_16">
  <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical"><TextView android:id="@+id/avatar" style="@style/Title" android:layout_width="72dp" android:layout_height="72dp" android:gravity="center" android:background="@drawable/bg_avatar" android:textColor="@color/surface"/><TextView android:id="@+id/name" style="@style/Heading" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="@dimen/space_16"/></LinearLayout>
  {tv('nickname_label','nickname','Body',margin)}
  <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical"><EditText android:id="@+id/nickname" style="@style/Input" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:inputType="textVisiblePassword" android:hint="@string/nickname" android:autofillHints="username"/><com.google.android.material.button.MaterialButton android:id="@+id/change" style="@style/OutlineButton" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/change"/></LinearLayout>
 </LinearLayout>
 {tv('connection_label','connection','Caption',margin)}
 {tv('server','','Heading','android:background="@drawable/bg_card" android:padding="@dimen/space_16" android:layout_marginTop="@dimen/space_8" app:drawableStartCompat="@drawable/ic_server" android:drawablePadding="@dimen/space_12"')}
 {tv('status','','Body','android:layout_marginTop="@dimen/space_8" android:accessibilityLiveRegion="polite"')}
 {btn('reconnect','reconnect','OutlineButton',margin)}
 {tv('options_label','options','Caption',margin)}
 <com.google.android.material.switchmaterial.SwitchMaterial android:id="@+id/notifications" android:layout_width="match_parent" android:layout_height="@dimen/input_height" android:text="@string/notifications"/>
 {btn('connection_options','connection_options','OutlineButton')}
 {tv('error','','Body','android:textColor="@color/error" android:layout_marginTop="@dimen/space_12"')}
 {btn('disconnect','disconnect','OutlineButton',margin+'android:textColor="@color/error" app:strokeColor="@color/error" app:icon="@drawable/ic_logout"')}
 {tv('limit','foreground_limit','Caption',margin)}
 </LinearLayout></ScrollView>''')
catalog=[('web','Web Development','Cùng học, cùng xây dựng.','code',['general','questions','resources','project']),('deep','Deep Learning','Cùng khám phá học sâu.','brain',['general','questions','resources']),('mobi','MOBI','Lập trình ứng dụng di động.','phone',['general','questions','project']),('french','Tiếng Pháp','Trao đổi cùng lớp.','book',['general','practice','resources'])]
descs={'general':'Trao đổi chung','questions':'Hỏi đáp bài tập','resources':'Chia sẻ tài liệu','project':'Thảo luận dự án','practice':'Luyện tập tiếng Pháp'}
write('assets/courses.json',json.dumps({'courses':[{'id':id,'name':name,'description':desc,'icon':ic,'channels':[{'name':f'#{id}-{c}','description':descs[c]} for c in channels]} for id,name,desc,ic,channels in catalog]},ensure_ascii=False,indent=2))
print('Resources generated in',ROOT)
