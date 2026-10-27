package vn.edu.usth.connect;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.model.Conversation;
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

  @Test
  public void headerBackFromSearchKeepsReadingAndSavingDrafts() throws Exception {
    onView(withId(R.id.composer)).perform(replaceText("draft before search"), closeSoftKeyboard());
    openSearch();
    onView(withId(R.id.chat_search_query)).perform(replaceText("history"), closeSoftKeyboard());

    onView(withId(R.id.back)).perform(click());
    onView(withId(R.id.chat_search_panel)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    waitForActive("an");
    feed(":an!u@h PRIVMSG minh :message after closing search");
    scenario.onActivity(
        activity -> {
          assertEquals("an", app.session.data.activeTarget);
          assertEquals(0, app.session.data.conversation("an").unread);
        });

    onView(withId(R.id.composer)).perform(replaceText("draft after search"), closeSoftKeyboard());
    onView(withId(R.id.back)).perform(click());
    waitForActive("");
    scenario.onActivity(activity -> activity.openChat("an"));
    waitForActive("an");
    onView(withId(R.id.composer)).check(matches(withText("draft after search")));
  }

  @Test
  public void closingSearchRestoresExistingLatestButton() throws Exception {
    fillAndScrollUp();
    feed(":an!u@h PRIVMSG minh :new message before search");
    onView(withId(R.id.latest)).check(matches(isDisplayed()));

    openSearch();
    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    onView(withId(R.id.chat_search_close)).perform(click());

    assertLatestReturnsToNewestMessage();
  }

  @Test
  public void messageDuringSearchOffersLatestAfterRecreationAndClose() throws Exception {
    fillAndScrollUp();
    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    openSearch();
    onView(withId(R.id.chat_search_query)).perform(replaceText("history"), closeSoftKeyboard());
    feed(":an!u@h PRIVMSG minh :new message outside search results");
    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));

    scenario.recreate();
    waitForActive("an");
    onView(withId(R.id.chat_search_query))
        .check(matches(withText("history")))
        .perform(closeSoftKeyboard());
    Espresso.pressBack();
    onView(withId(R.id.chat_search_panel)).check(matches(withEffectiveVisibility(Visibility.GONE)));

    assertLatestReturnsToNewestMessage();
  }

  @Test
  public void closingSearchFromPinnedChatFollowsMessagesReceivedDuringSearch() throws Exception {
    fillChat(60);
    scenario.onActivity(
        activity -> {
          RecyclerView list = activity.findViewById(R.id.list);
          assertFalse("Chat must start at its newest message", list.canScrollVertically(1));
        });
    openSearch();
    onView(withId(R.id.chat_search_query)).perform(replaceText("history"), closeSoftKeyboard());
    feed(":an!u@h PRIVMSG minh :newest message received during search");
    onView(withId(R.id.chat_search_close)).perform(click());

    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    onView(withText("newest message received during search")).check(matches(isDisplayed()));
    scenario.onActivity(
        activity -> {
          RecyclerView list = activity.findViewById(R.id.list);
          assertFalse("Closing search must follow the newest message", list.canScrollVertically(1));
        });
  }

  @Test
  public void fullHistoryStillOffersLatestForMessagesReceivedDuringSearch() throws Exception {
    fillChat(Conversation.MAX_HISTORY_MESSAGES);
    scrollUp();
    openSearch();
    onView(withId(R.id.chat_search_query)).perform(replaceText("history"), closeSoftKeyboard());
    feed(":an!u@h PRIVMSG minh :new message beyond the history limit");
    scenario.onActivity(
        activity ->
            assertEquals(
                Conversation.MAX_HISTORY_MESSAGES,
                app.session.data.conversation("an").messages.size()));
    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    onView(withId(R.id.chat_search_close)).perform(click());

    assertLatestReturnsToNewestMessage();
    onView(withText("new message beyond the history limit")).check(matches(isDisplayed()));
  }

  private void openSearch() throws Exception {
    onView(withId(R.id.more)).perform(click());
    onView(withText(R.string.search_messages)).perform(click());
    onView(withId(R.id.chat_search_panel)).check(matches(isDisplayed()));
    waitForActive("an");
  }

  private void fillAndScrollUp() {
    fillChat(60);
    scrollUp();
  }

  private void fillChat(int count) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (int i = 0; i < count; i++)
                app.session.onMessage(
                    0, IrcParser.parse(":an!u@h PRIVMSG minh :history message " + i));
            });
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  private void scrollUp() {
    scenario.onActivity(
        activity -> {
          RecyclerView list = activity.findViewById(R.id.list);
          list.scrollBy(0, -3 * list.getHeight());
          assertTrue(
              "History must remain below the scrolled position", list.canScrollVertically(1));
        });
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  private void assertLatestReturnsToNewestMessage() {
    onView(withId(R.id.latest)).check(matches(isDisplayed()));
    scenario.onActivity(
        activity -> {
          RecyclerView list = activity.findViewById(R.id.list);
          assertTrue("Closing search must retain the older position", list.canScrollVertically(1));
        });
    onView(withId(R.id.latest)).perform(click());
    onView(withId(R.id.latest)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    scenario.onActivity(
        activity -> {
          RecyclerView list = activity.findViewById(R.id.list);
          assertFalse("Latest must reveal the newest message", list.canScrollVertically(1));
        });
  }
}
