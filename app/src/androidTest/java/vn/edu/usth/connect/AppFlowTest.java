package vn.edu.usth.connect;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
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
    onView(withId(R.id.nickname)).perform(replaceText("minh_ui"), closeSoftKeyboard());
    onView(withId(R.id.host)).perform(replaceText(host), closeSoftKeyboard());
    onView(withId(R.id.advanced_toggle)).perform(scrollTo(), click());
    onView(withId(R.id.port)).perform(scrollTo(), replaceText("6667"), closeSoftKeyboard());
    onView(withId(R.id.tls)).perform(scrollTo());
    try {
      onView(withId(R.id.tls)).check(matches(isChecked()));
      onView(withId(R.id.tls)).perform(click());
    } catch (AssertionError ignored) {
    }
    onView(withId(R.id.connect)).perform(scrollTo(), click());
    waitFor(() -> onMain(() -> app.session.data.connection == ConnectionState.CONNECTED));
    onView(withId(R.id.title)).check(matches(withText(R.string.courses)));
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
    peer.connect(new ConnectionConfig(host, 6667, false, "an_ui", "usth", "Android UI test peer"));
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
    onView(withText("Web Development")).perform(click());
    onView(withId(R.id.search)).perform(replaceText("web-general"), closeSoftKeyboard());
    onView(withText(R.string.join)).perform(click());
    onView(withId(R.id.channel)).check(matches(withText("#web-general")));
    onView(withText(R.string.join)).perform(click());
    waitFor(() -> onMain(() -> app.session.data.channel("#web-general").joined));
    peer.send(IrcCommands.join("#web-general", ""), null);
    waitFor(
        () -> onMain(() -> app.session.data.channel("#web-general").members.containsKey("an_ui")));
    peer.send(IrcCommands.message("#web-general", "Xin chào Minh từ client thứ hai"), null);
    onView(withId(R.id.composer)).perform(replaceText("Chào An"), closeSoftKeyboard());
    onView(withId(R.id.send)).perform(click());
    waitFor(
        () ->
            received.stream()
                .anyMatch(m -> m.command.equals("PRIVMSG") && m.last().equals("Chào An")));
    onView(withText("Xin chào Minh từ client thứ hai")).check(matches(isDisplayed()));
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
    onView(withId(R.id.members)).perform(click());
    onView(withText("an_ui")).perform(click());
    onView(withId(R.id.title)).check(matches(withText("an_ui")));
    onView(withId(R.id.composer))
        .perform(replaceText("Tin riêng trên Android"), closeSoftKeyboard());
    onView(withId(R.id.send)).perform(click());
    waitFor(
        () ->
            received.stream()
                .anyMatch(
                    m -> m.command.equals("PRIVMSG") && m.last().equals("Tin riêng trên Android")));
  }
}
