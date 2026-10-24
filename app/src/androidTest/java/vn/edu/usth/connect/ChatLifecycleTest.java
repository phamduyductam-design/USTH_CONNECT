package vn.edu.usth.connect;

import static org.junit.Assert.*;

import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.repository.IrcSessionController;
import vn.edu.usth.connect.ui.MainActivity;
import vn.edu.usth.connect.ui.dialog.CommonDialogs;

/** UI lifecycle regressions; no external IRC server is needed for these tests. */
@RunWith(AndroidJUnit4.class)
public class ChatLifecycleTest {
  private ActivityScenario<MainActivity> scenario;
  private ConnectApplication app;

  @Before
  public void openOfflineChat() throws Exception {
    app =
        (ConnectApplication)
            InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              app.session.disconnect();
              app.session = new IrcSessionController(app.settings);
              app.session.config =
                  new ConnectionConfig("offline.test", 6667, false, "minh", "usth", "test");
              app.session.data.nickname = "minh";
              app.session.saveDraft("an", "");
              app.session.saveDraft("ann", "");
            });
    scenario = ActivityScenario.launch(MainActivity.class);
    scenario.onActivity(activity -> activity.openChat("an"));
    waitForActive("an");
  }

  private void feed(String line) {
    // A fresh, deliberately unconnected client has epoch zero.
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(() -> app.session.onMessage(0, IrcParser.parse(line)));
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  private void waitForActive(String target) throws Exception {
    long end = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < end) {
      AtomicBoolean active = new AtomicBoolean();
      scenario.onActivity(activity -> active.set(app.session.data.activeTarget.equals(target)));
      if (active.get()) return;
      Thread.sleep(50);
    }
    fail("Expected visible chat: " + target);
  }

  @After
  public void close() {
    if (scenario != null) scenario.close();
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> app.session.disconnect());
  }

  @Test
  public void coveredChatCountsUnreadAndPopRestoresReading() throws Exception {
    scenario.onActivity(MainActivity::showServerChannels);
    waitForActive("");
    feed(":an!u@h PRIVMSG minh :covered message");
    scenario.onActivity(activity -> assertEquals(1, app.session.data.conversation("an").unread));
    scenario.onActivity(MainActivity::back);
    waitForActive("an");
    scenario.onActivity(activity -> assertEquals(0, app.session.data.conversation("an").unread));
  }

  @Test
  public void dialogSuspendsReadingAndPreservesUnpersistedCommand() throws Exception {
    scenario.onActivity(
        activity -> {
          ((EditText) activity.findViewById(R.id.composer)).setText("/join #c in-memory-key");
          CommonDialogs.showCommandHelp(activity);
        });
    waitForActive("");
    feed(":an!u@h PRIVMSG minh :while dialog is open");
    scenario.onActivity(
        activity -> {
          assertEquals(1, app.session.data.conversation("an").unread);
          assertEquals("", app.session.draft("an"));
        });
    Espresso.pressBack();
    waitForActive("an");
    scenario.onActivity(
        activity ->
            assertEquals(
                "/join #c in-memory-key",
                ((EditText) activity.findViewById(R.id.composer)).getText().toString()));
  }

  @Test
  public void nestedChatKeepsNewestDraftAndFollowsMergedPm() throws Exception {
    scenario.onActivity(
        activity -> {
          ((EditText) activity.findViewById(R.id.composer)).setText("source draft");
          activity.openChat("ann");
        });
    waitForActive("ann");
    scenario.onActivity(
        activity -> ((EditText) activity.findViewById(R.id.composer)).setText("destination draft"));
    feed(":an!u@h PRIVMSG minh :source history");
    feed(":an!u@h NICK :ann");
    feed(":ann!u@h PRIVMSG minh :after merge");
    scenario.onActivity(MainActivity::back);
    waitForActive("ann");
    scenario.onActivity(
        activity -> {
          assertEquals(
              "destination draft",
              ((EditText) activity.findViewById(R.id.composer)).getText().toString());
          assertEquals(0, app.session.data.conversation("ann").unread);
        });
  }

  @Test
  public void backgroundAndResumeKeepUnreadUntilForeground() throws Exception {
    scenario.moveToState(Lifecycle.State.STARTED);
    feed(":an!u@h PRIVMSG minh :background message");
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(() -> assertEquals(1, app.session.data.conversation("an").unread));
    scenario.moveToState(Lifecycle.State.RESUMED);
    waitForActive("an");
    scenario.onActivity(activity -> assertEquals(0, app.session.data.conversation("an").unread));
  }
}
