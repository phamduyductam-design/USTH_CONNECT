package vn.edu.usth.connect.repository;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.storage.*;

public class IrcSessionControllerTest {
  private static final class Clock implements IrcSessionController.Scheduler {
    long now;
    final Map<Runnable, Long> timers = new LinkedHashMap<>();

    public void post(Runnable callback) {
      callback.run();
    }

    public void postDelayed(Runnable callback, long delay) {
      timers.put(callback, now + delay);
    }

    public void removeCallbacks(Runnable callback) {
      timers.remove(callback);
    }

    void advance(long ms) {
      long end = now + ms;
      while (true) {
        Map.Entry<Runnable, Long> next =
            timers.entrySet().stream().min(Map.Entry.comparingByValue()).orElse(null);
        if (next == null || next.getValue() > end) break;
        now = next.getValue();
        timers.remove(next.getKey());
        next.getKey().run();
      }
      now = end;
    }
  }

  private static final class Client implements IrcTransport {
    long epoch = 1;
    final List<String> commands = new ArrayList<>();
    IrcClient.SendCallback deferred;
    boolean defer;

    public long epoch() {
      return epoch;
    }

    public void connect(ConnectionConfig config) {
      epoch++;
    }

    public void send(String line, IrcClient.SendCallback callback) {
      commands.add(line);
      if (defer) {
        deferred = callback;
        defer = false;
      } else if (callback != null) callback.complete(true, false);
    }

    public void reconnectNow() {
      epoch++;
    }

    public void disconnect() {
      epoch++;
    }
  }

  private Clock clock;
  private Client client;
  private IrcSessionController session;

  @Before
  public void createSession() {
    clock = new Clock();
    client = new Client();
    session =
        new IrcSessionController(
            new SettingsStore(new MemoryPreferences().prefs), clock, listener -> client);
    session.connect(new ConnectionConfig("irc.example", 6667, false, "minh", "usth", "test"));
    session.onState(client.epoch(), ConnectionState.CONNECTED, "");
  }

  private void feed(String line) {
    session.onMessage(client.epoch(), IrcParser.parse(line));
  }

  private void joined(String target) {
    feed(":minh!u@h JOIN " + target);
    feed(":s 353 minh = " + target + " :@minh an");
    feed(":s 366 minh " + target + " :end");
  }

  @Test
  public void completedListCannotShortenNextDeadline() {
    session.listChannels();
    clock.advance(5000);
    feed(":s 323 minh :end");
    session.listChannels();
    clock.advance(25000);
    assertTrue(session.data.listing);
    clock.advance(5000);
    assertFalse(session.data.listing);
    assertEquals("timeout", session.data.errorCode);
  }

  @Test
  public void listTryAgainPermitsFreshRetry() {
    session.listChannels();
    clock.advance(4000);
    feed(":s 263 minh LIST :try later");
    assertFalse(session.data.listing);
    session.listChannels();
    clock.advance(26000);
    assertTrue(session.data.listing);
    feed(":s 323 minh :end");
    assertTrue(clock.timers.isEmpty());
  }

  @Test
  public void oldJoinTimerAndWriteCallbackCannotCancelRetry() {
    client.defer = true;
    session.join("#c", "");
    IrcClient.SendCallback first = client.deferred;
    clock.advance(3000);
    feed(":s 475 minh #c :bad key");
    session.join("#C", "new-key");
    first.complete(false, false);
    clock.advance(12000);
    assertTrue(session.data.channel("#c").joining);
    clock.advance(3000);
    assertFalse(session.data.channel("#c").joining);
  }

  @Test
  public void completedNamesCannotExpireNextMultipartRequest() {
    joined("#c");
    session.refreshNames("#c");
    clock.advance(3000);
    feed(":s 353 minh = #c :minh an");
    feed(":s 366 minh #c :end");
    session.refreshNames("#C");
    feed(":s 353 minh = #c :minh an ha");
    clock.advance(12000);
    assertTrue(session.data.channel("#c").namesInProgress);
    feed(":s 366 minh #c :end");
    assertEquals(3, session.data.channel("#c").members.size());
    clock.advance(3000);
    assertEquals("", session.data.errorCode);
  }

  @Test
  public void initialJoinNamesHasDeadlineAndLateTailIsIgnored() {
    feed(":minh!u@h JOIN #c");
    feed(":s 353 minh = #c :minh an");
    clock.advance(15000);
    Channel c = session.data.channel("#c");
    assertFalse(c.namesInProgress);
    assertEquals(1, c.members.size());
    feed(":s 353 minh = #c :late");
    feed(":s 366 minh #c :end");
    assertEquals(1, c.members.size());
    assertTrue(clock.timers.isEmpty());
  }

  @Test
  public void timeoutThenRetryMergesUntilBothEndsAndPreservesRoles() {
    joined("#c");
    session.refreshNames("#c");
    clock.advance(15000);
    session.refreshNames("#c");
    feed(":s 353 minh = #c :minh late");
    feed(":s 366 minh #c :old end");
    Channel c = session.data.channel("#c");
    assertTrue(c.namesInProgress);
    assertTrue(c.members.containsKey("an"));
    assertEquals("o", session.data.role("#c", "minh"));
    feed(":s 353 minh = #c :new-user");
    feed(":s 366 minh #c :new end");
    assertTrue(c.members.containsKey("new-user"));
    assertFalse(c.namesInProgress);
    session.refreshNames("#c");
    feed(":s 353 minh = #c :@minh");
    feed(":s 366 minh #c :clean end");
    assertEquals(1, c.members.size());
  }

  @Test
  public void partQuitAndNickDuringTimedOutNamesCannotResurrectUsers() {
    joined("#c");
    session.refreshNames("#c");
    clock.advance(15000);
    feed(":an!u@h PART #c :bye");
    feed(":unknown!u@h QUIT :bye");
    feed(":old!u@h NICK :new");
    session.refreshNames("#c");
    feed(":s 353 minh = #c :an unknown old minh");
    feed(":s 366 minh #c :old");
    feed(":s 366 minh #c :new");
    Channel c = session.data.channel("#c");
    assertFalse(c.members.containsKey("an"));
    assertFalse(c.members.containsKey("unknown"));
    assertFalse(c.members.containsKey("old"));
  }

  @Test
  public void ambiguousTryAgainDoesNotCancelOtherChannelNames() {
    joined("#c");
    joined("#d");
    session.refreshNames("#c");
    session.refreshNames("#d");
    feed(":s 263 minh NAMES :try later");
    assertTrue(session.data.channel("#c").namesInProgress);
    assertTrue(session.data.channel("#d").namesInProgress);
    clock.advance(15000);
    assertFalse(session.data.channel("#c").namesInProgress);
  }

  @Test
  public void explicitNamesRejectionCancelsTimerAndAllowsRetry() {
    joined("#c");
    session.refreshNames("#c");
    feed(":s 421 minh NAMES :unknown");
    assertFalse(session.data.channel("#c").namesInProgress);
    assertTrue(clock.timers.isEmpty());
    session.refreshNames("#c");
    assertTrue(session.data.channel("#c").namesInProgress);
  }

  @Test
  public void leaveKickAndDisconnectCancelRequestTimers() {
    for (String event : new String[] {":minh!u@h PART #c :bye", ":op!u@h KICK #c minh :bye"}) {
      feed(":minh!u@h JOIN #c");
      feed(event);
      assertTrue(clock.timers.isEmpty());
    }
    session.join("#a", "");
    session.listChannels();
    feed(":minh!u@h JOIN #c");
    session.onState(client.epoch(), ConnectionState.RECONNECTING, "network");
    assertTrue(clock.timers.isEmpty());
    clock.advance(30000);
    assertFalse(session.data.channel("#c").namesInProgress);
  }

  @Test
  public void backgroundMessagesRemainUnreadAndAreNotReplayedOnReconnect() {
    joined("#c");
    session.setActive("#c");
    session.setForeground(false);
    feed(":an!u@h PRIVMSG #c :background");
    assertEquals(1, session.data.channel("#c").unread);
    session.setActive("#c");
    assertEquals(1, session.data.channel("#c").unread);
    session.setForeground(true);
    session.setActive("#c");
    assertEquals(0, session.data.channel("#c").unread);
    session.send("#c", "outgoing");
    int count = client.commands.size();
    session.onState(client.epoch(), ConnectionState.RECONNECTING, "network");
    session.onState(client.epoch(), ConnectionState.CONNECTED, "");
    assertFalse(
        client.commands.subList(count, client.commands.size()).stream()
            .anyMatch(s -> s.startsWith("PRIVMSG")));
  }

  @Test
  public void successivePeerRenamesMoveDraftEvenWhenChatIsCovered() {
    session.saveDraft("an", "source draft");
    feed(":an!u@h NICK :ann");
    feed(":ann!u@h NICK :anna");
    assertEquals("", session.draft("an"));
    assertEquals("", session.draft("ann"));
    assertEquals("source draft", session.draft("anna"));
  }

  @Test
  public void mergingPmKeepsDraftOfVisibleDestinationOrSource() {
    session.saveDraft("an", "source");
    session.saveDraft("ann", "destination");
    session.setActive("ann");
    feed(":an!u@h NICK :ann");
    assertEquals("destination", session.draft("ann"));
    session.saveDraft("an", "new source");
    session.setActive("an");
    feed(":an!u@h NICK :ann");
    assertEquals("new source", session.draft("ann"));
  }
}
