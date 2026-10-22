package vn.edu.usth.connect;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.ui.*;

/** Requires a real local IRC daemon. Example: Ergo listening at host 127.0.0.1:6667. */
@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
  private ActivityScenario<ConnectActivity> scenario;
  private IrcClient peer;
  private final BlockingQueue<IrcMessage> received = new LinkedBlockingQueue<>();
  private ConnectApplication app;

  private static void waitFor(java.util.concurrent.Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
    while (System.nanoTime() < deadline) {
      if (condition.call()) return;
      Thread.sleep(100);
    }
    fail("State did not arrive from real IRC daemon");
  }

  private boolean onMain(java.util.concurrent.Callable<Boolean> condition) {
    java.util.concurrent.atomic.AtomicBoolean value =
        new java.util.concurrent.atomic.AtomicBoolean();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                value.set(condition.call());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    return value.get();
  }

  @Before
  public void connectTwoClients() throws Exception {
    app =
        (ConnectApplication)
            InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              app.session.disconnect();
              app.session.data.channels.clear();
              app.session.data.conversations.clear();
            });
    scenario = ActivityScenario.launch(ConnectActivity.class);
    String host = InstrumentationRegistry.getArguments().getString("server", "10.0.2.2");
    int port = Integer.parseInt(InstrumentationRegistry.getArguments().getString("port", "6667"));
    onView(withId(R.id.nickname)).perform(replaceText("minh_ui"), closeSoftKeyboard());
    onView(withId(R.id.host)).perform(replaceText(host), closeSoftKeyboard());
    onView(withId(R.id.advanced_toggle)).perform(scrollTo(), click());
    onView(withId(R.id.port))
        .perform(scrollTo(), replaceText(String.valueOf(port)), closeSoftKeyboard());
    onView(withId(R.id.tls)).perform(scrollTo());
    try {
      onView(withId(R.id.tls)).check(matches(isChecked()));
      onView(withId(R.id.tls)).perform(click());
    } catch (AssertionError ignored) {
    }
    onView(withId(R.id.advanced_toggle)).perform(scrollTo(), click());
    capture("01-ket-noi");
    onView(withId(R.id.connect)).perform(scrollTo(), click());
    waitFor(() -> onMain(() -> app.session.data.connection == ConnectionState.CONNECTED));
    onView(withId(R.id.title)).check(matches(withText(R.string.courses)));
    capture("02-mon-hoc");
    CountDownLatch connected = new CountDownLatch(1);
    peer =
        new IrcClient(
            new IrcClient.Listener() {
              public void onState(long e, ConnectionState state, String reason) {
                if (state == ConnectionState.CONNECTED) connected.countDown();
              }

              public void onMessage(long e, IrcMessage m) {
                received.add(m);
              }
            });
    peer.connect(new ConnectionConfig(host, port, false, "an_ui", "usth", "Android UI test peer"));
    assertTrue(connected.await(10, TimeUnit.SECONDS));
  }

  @After
  public void cleanup() {
    if (peer != null) peer.shutdown();
    if (app != null)
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> app.session.disconnect());
    if (scenario != null) scenario.close();
  }

  @Test
  public void courseJoinChatMembersPrivateAndRotation() throws Exception {
    onView(withText(R.string.no_courses)).check(matches(isDisplayed()));
    onView(withId(R.id.empty_join)).perform(click());
    onView(withText("Web Development")).perform(click());
    capture("03-kenh-mon");
    onView(
            allOf(
                withId(R.id.join),
                isDisplayed(),
                withParent(hasDescendant(withText("#web-general")))))
        .perform(click());
    onView(withId(R.id.channel)).check(matches(withText("#web-general")));
    onView(withText(R.string.join)).perform(click());
    waitFor(() -> onMain(() -> app.session.data.channel("#web-general").joined));
    peer.send(IrcCommands.join("#web-general", ""), null);
    waitFor(
        () -> onMain(() -> app.session.data.channel("#web-general").members.containsKey("an_ui")));
    peer.send(IrcCommands.message("#web-general", "Xin chào Minh từ client thứ hai"), null);
    onView(withId(R.id.composer)).perform(replaceText("Chào An\n"), closeSoftKeyboard());
    onView(withId(R.id.send)).perform(click());
    waitFor(
        () ->
            received.stream()
                .anyMatch(m -> m.command.equals("PRIVMSG") && m.last().equals("Chào An")));
    onView(withText("Xin chào Minh từ client thứ hai")).check(matches(isDisplayed()));
    peer.send(IrcCommands.message("#web-general", "Mình đã nhận được lời chào của bạn."), null);
    peer.send(IrcCommands.message("#web-general", "Cùng trao đổi bài tập trong kênh nhé."), null);
    waitFor(
        () ->
            onMain(
                () ->
                    app.session.data.channel("#web-general").messages.stream()
                        .anyMatch(m -> m.text.equals("Cùng trao đổi bài tập trong kênh nhé."))));
    onView(
            allOf(
                withId(R.id.avatar),
                withParent(hasDescendant(withText("Cùng trao đổi bài tập trong kênh nhé.")))))
        .check(matches(withEffectiveVisibility(Visibility.INVISIBLE)));
    capture("04-chat-kenh");
    onView(withId(R.id.composer)).perform(replaceText("Nháp khi xoay"), closeSoftKeyboard());
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              android.app.Activity activity =
                  androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                      .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                      .iterator()
                      .next();
              activity.recreate();
            });
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    onView(withId(R.id.composer)).check(matches(withText("Nháp khi xoay")));
    // A theme change recreates the screen without replacing the live IRC session or draft.
    vn.edu.usth.connect.model.Channel liveChannel = app.session.data.channel("#web-general");
    boolean previousTheme = app.settings.darkMode();
    onView(withId(R.id.theme_toggle)).perform(click());
    onView(withId(R.id.composer)).check(matches(withText("Nháp khi xoay")));
    capture("10-chat-toi");
    assertTrue(
        onMain(
            () ->
                app.session.data.connection == ConnectionState.CONNECTED
                    && app.session.data.channel("#web-general") == liveChannel
                    && liveChannel.joined
                    && liveChannel.members.containsKey("an_ui")));
    assertEquals(!previousTheme, app.settings.darkMode());
    onView(withId(R.id.theme_toggle)).perform(click());
    onView(withId(R.id.composer)).check(matches(withText("Nháp khi xoay")));
    assertEquals(previousTheme, app.settings.darkMode());
    peer.send(IrcCommands.message("#web-general", "Vẫn nhận tin sau khi đổi theme"), null);
    waitFor(
        () ->
            onMain(
                () ->
                    liveChannel.messages.stream()
                        .anyMatch(m -> m.text.equals("Vẫn nhận tin sau khi đổi theme"))));
    onView(withId(R.id.members)).perform(click());
    capture("05-thanh-vien");
    onView(allOf(withId(R.id.detail), withText(startsWith("an_ui ·")), isDisplayed()))
        .perform(click());
    onView(withId(R.id.message_member)).perform(click());
    onView(withId(R.id.title)).check(matches(withText("An")));
    onView(withId(R.id.composer))
        .perform(replaceText("Tin riêng\r\ntrên Android"), closeSoftKeyboard());
    onView(withId(R.id.send)).perform(click());
    waitFor(
        () ->
            received.stream()
                .anyMatch(
                    m -> m.command.equals("PRIVMSG") && m.last().equals("Tin riêng trên Android")));
    peer.send(
        IrcCommands.message("minh_ui", "Mình đã nhận được tin riêng, cùng trao đổi nhé."), null);
    waitFor(
        () ->
            onMain(
                () ->
                    app.session.data.conversation("an_ui").messages.stream()
                        .anyMatch(
                            m ->
                                m.text.equals("Mình đã nhận được tin riêng, cùng trao đổi nhé."))));
    onView(withText("Mình đã nhận được tin riêng, cùng trao đổi nhé."))
        .check(matches(isDisplayed()));
    capture("07-nhan-rieng");
    onView(withId(R.id.nav_messages)).perform(click());
    capture("06-tin-nhan");
    onView(withId(R.id.nav_courses)).perform(click());
    capture("02-mon-hoc-da-tham-gia");
  }

  private void capture(String name) throws Exception {
    if (!"true".equals(InstrumentationRegistry.getArguments().getString("screenshots"))) return;
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    java.io.File folder = new java.io.File(app.getExternalFilesDir(null), "redesign-screenshots");
    if (!folder.isDirectory() && !folder.mkdirs())
      throw new java.io.IOException("Cannot create screenshots directory");
    android.graphics.Bitmap bitmap =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    try (java.io.FileOutputStream output =
        new java.io.FileOutputStream(new java.io.File(folder, name + ".png"))) {
      assertNotNull(bitmap);
      assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
    } finally {
      if (bitmap != null) bitmap.recycle();
    }
  }

  private void sendCommand(String text) {
    onView(withId(R.id.composer)).perform(replaceText(text), closeSoftKeyboard());
    onView(withId(R.id.send)).perform(click());
  }

  private void shell(String command) throws Exception {
    android.os.ParcelFileDescriptor fd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      byte[] buffer = new byte[1024];
      while (input.read(buffer) != -1) {}
    }
  }

  @Test
  public void serverRoomsRolesTopicAwayMentionsAndModeration() throws Exception {
    String room = "#usth-ui-" + Long.toString(System.nanoTime(), 36);
    onView(withId(R.id.empty_join)).perform(click());
    onView(allOf(withId(R.id.join), withText(R.string.join_channel), isDisplayed()))
        .perform(click());
    onView(withId(R.id.channel)).perform(replaceText(room), closeSoftKeyboard());
    onView(withText(R.string.join)).perform(click());
    waitFor(
        () ->
            onMain(
                () -> app.session.data.channel(room).joined && app.session.data.canManage(room)));
    peer.send(IrcCommands.join(room, ""), null);
    waitFor(() -> onMain(() -> app.session.data.channel(room).members.containsKey("an_ui")));
    sendCommand("/list");
    waitFor(
        () ->
            onMain(
                () ->
                    app.session.data.listLoaded
                        && app.session.data.availableChannels.containsKey(room)));
    onView(withText(room)).check(matches(isDisplayed())).perform(click());
    onView(withId(R.id.more)).perform(click());
    onView(withText(R.string.edit_topic)).perform(click());
    onView(withId(R.id.input)).perform(replaceText("Chủ đề từ giao diện"), closeSoftKeyboard());
    onView(withText(R.string.apply)).perform(click());
    waitFor(() -> onMain(() -> app.session.data.channel(room).topic.equals("Chủ đề từ giao diện")));
    onView(withId(R.id.topic)).check(matches(withText("Chủ đề từ giao diện")));
    onView(withId(R.id.members)).perform(click());
    onView(allOf(withId(R.id.detail), withText(startsWith("an_ui ·")), isDisplayed()))
        .perform(click());
    onView(withId(R.id.manage_member)).perform(click());
    onView(withText(R.string.give_voice)).perform(click());
    waitFor(() -> onMain(() -> app.session.data.role(room, "an_ui").equals("v")));
    onView(withText("an_ui · " + app.getString(R.string.voice_role))).check(matches(isDisplayed()));
    peer.send(IrcCommands.away("Đang học"), null);
    waitFor(() -> onMain(() -> app.session.data.awayUsers.containsKey("an_ui")));
    onView(withText("Vắng mặt: Đang học")).check(matches(isDisplayed()));
    onView(withId(R.id.close)).perform(click());

    if (android.os.Build.VERSION.SDK_INT >= 33)
      shell("pm grant vn.edu.usth.connect android.permission.POST_NOTIFICATIONS");
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(() -> app.settings.notifications(true));
    android.app.Activity[] foreground = new android.app.Activity[1];
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () ->
                foreground[0] =
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                        .iterator()
                        .next());
    shell("input keyevent KEYCODE_HOME");
    waitFor(
        () ->
            onMain(
                () ->
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.STOPPED)
                        .contains(foreground[0])));
    String mention = "Chào @minh_ui, đây là tin nhắc tên!";
    peer.send(IrcCommands.message(room, mention), null);
    android.app.NotificationManager notifications =
        app.getSystemService(android.app.NotificationManager.class);
    waitFor(
        () ->
            java.util.Arrays.stream(notifications.getActiveNotifications())
                .anyMatch(
                    n ->
                        n.getId() == room.hashCode()
                            && (android.os.Build.VERSION.SDK_INT < 26
                                || n.getNotification().getChannelId().equals("mentions"))));
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () ->
                app.startActivity(
                    new android.content.Intent(app, MainActivity.class)
                        .addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                | android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
    onView(withText(mention)).check(matches(isDisplayed()));
    assertTrue(
        onMain(
            () ->
                app.session.data.channel(room).messages.stream()
                    .anyMatch(m -> m.text.equals(mention) && m.mentionNick.equals("minh_ui"))));
    notifications.cancelAll();

    onView(withId(R.id.back)).perform(click());
    onView(withId(R.id.nav_profile)).perform(click());
    capture("08-ca-nhan");
    onView(withId(R.id.away_toggle)).perform(scrollTo(), click());
    onView(withId(R.id.input)).perform(replaceText("Bận học"), closeSoftKeyboard());
    onView(withText(R.string.apply)).perform(click());
    waitFor(() -> onMain(() -> app.session.data.away));
    onView(withId(R.id.away_status)).check(matches(withText("Vắng mặt: Bận học")));
    onView(withId(R.id.away_toggle)).perform(scrollTo(), click());
    waitFor(() -> onMain(() -> !app.session.data.away));
    onView(withId(R.id.nav_courses)).perform(click());
    onView(withId(R.id.browse_server)).perform(click());
    onView(withText(room)).perform(click());
    sendCommand("/mode " + room + " -v an_ui");
    waitFor(() -> onMain(() -> app.session.data.role(room, "an_ui").equals("u")));
    onView(withId(R.id.members)).perform(click());
    onView(allOf(withId(R.id.detail), withText(startsWith("an_ui ·")), isDisplayed()))
        .perform(click());
    onView(withId(R.id.manage_member)).perform(click());
    onView(withText(R.string.kick_member)).perform(click());
    onView(withId(R.id.input)).perform(replaceText("Kiểm thử giao diện"), closeSoftKeyboard());
    onView(withText(R.string.apply)).perform(click());
    waitFor(() -> onMain(() -> !app.session.data.channel(room).members.containsKey("an_ui")));
    onView(withId(R.id.close)).perform(click());
    sendCommand("/part");
    waitFor(() -> onMain(() -> !app.session.data.channel(room).joined));
  }

  @Test
  public void threeTabsEmptyPrivateChatAndKeyboardInsets() throws Exception {
    assertTrue(
        onMain(
            () -> {
              android.app.Activity activity =
                  androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                      .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                      .iterator()
                      .next();
              return ((com.google.android.material.bottomnavigation.BottomNavigationView)
                          activity.findViewById(R.id.navigation))
                      .getMenu()
                      .size()
                  == 3;
            }));
    onView(withId(R.id.nav_messages)).perform(click());
    onView(withText(R.string.no_conversations)).check(matches(isDisplayed()));
    onView(withText(R.string.catalog_start_conversation)).perform(click());
    onView(withId(R.id.input)).perform(replaceText("an_ui"), closeSoftKeyboard());
    onView(withText(R.string.private_message)).perform(click());
    onView(withId(R.id.composer)).perform(click(), typeText("First line\nSecond line"));
    waitFor(
        () ->
            onMain(
                () -> {
                  android.app.Activity activity =
                      androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                          .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                          .iterator()
                          .next();
                  android.view.View composer = activity.findViewById(R.id.composer);
                  android.graphics.Rect viewport = new android.graphics.Rect();
                  activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(viewport);
                  int[] position = new int[2];
                  composer.getLocationOnScreen(position);
                  return position[1] + composer.getHeight() <= viewport.bottom
                      && activity.findViewById(R.id.navigation).getVisibility()
                          == android.view.View.GONE;
                }));
    capture("09-ban-phim");
    onView(withId(R.id.composer)).perform(closeSoftKeyboard());
    onView(withId(R.id.navigation)).check(matches(isDisplayed()));
    onView(withId(R.id.send)).perform(click());
    waitFor(
        () ->
            received.stream()
                .anyMatch(
                    m -> m.command.equals("PRIVMSG") && m.last().equals("First line Second line")));
    onView(withId(R.id.nav_profile)).perform(click());
    capture("08-ca-nhan");
    onView(withId(R.id.profile_nickname_toggle)).perform(scrollTo(), click());
    onView(withId(R.id.nickname))
        .perform(scrollTo(), replaceText("minh_redesign"), closeSoftKeyboard());
    onView(withId(R.id.change)).perform(scrollTo(), click());
    waitFor(() -> onMain(() -> app.session.data.nickname.equals("minh_redesign")));
    onView(withId(R.id.profile_nickname_value)).check(matches(withText("minh_redesign")));
    onView(withId(R.id.disconnect)).perform(scrollTo(), click());
    onView(withText(R.string.cancel)).perform(click());
    assertTrue(onMain(() -> app.session.data.connection == ConnectionState.CONNECTED));
    onView(withId(R.id.disconnect)).perform(scrollTo(), click());
    onView(allOf(withText(R.string.disconnect), isDisplayed())).perform(click());
    waitFor(() -> onMain(() -> app.session.data.connection == ConnectionState.DISCONNECTED));
    onView(withId(R.id.host))
        .check(
            matches(
                withText(InstrumentationRegistry.getArguments().getString("server", "10.0.2.2"))));
  }
}
