package vn.edu.usth.connect.repository;

import static org.junit.Assert.*;

import org.junit.Test;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.model.*;

public class SessionStateTest {
  @Test
  public void delayedNamesAfterSelfPartCannotRestoreMembers() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":server 353 minh = #c :minh an");
    feed(s, ":minh!u@h PART #c :bye");
    feed(s, ":server 353 minh = #c :ha");
    feed(s, ":server 366 minh #c :end");
    assertFalse(s.channel("#c").joined);
    assertTrue(s.channel("#c").members.isEmpty());
    assertFalse(s.channel("#c").namesInProgress);
  }

  private SessionState session() {
    SessionState s = new SessionState();
    s.nickname = "minh";
    return s;
  }

  private void feed(SessionState s, String line) {
    s.accept(IrcParser.parse(line));
  }

  @Test
  public void membershipNeedsServerJoinAcknowledgement() {
    SessionState s = session();
    Channel c = s.channel("#web-general");
    c.joining = true;
    assertFalse(c.joined);
    feed(s, ":other!u@h JOIN :#web-general");
    assertFalse(c.joined);
    feed(s, ":minh!u@h JOIN #web-general");
    assertTrue(c.joined);
    assertTrue(c.wanted);
    assertFalse(c.joining);
  }

  @Test
  public void multipartNamesAndInterleavedPartDontDuplicateOrResurrect() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":server 353 minh = #c :@minh an ha");
    feed(s, ":an!u@h PART #c :bye");
    feed(s, ":server 353 minh = #c :an +ha tuan");
    feed(s, ":tuan!u@h NICK :tuan2");
    feed(s, ":server 366 minh #c :end");
    Channel c = s.channel("#c");
    assertEquals(3, c.members.size());
    assertFalse(c.members.containsKey("an"));
    assertFalse(c.members.containsKey("tuan"));
    assertTrue(c.members.containsKey("tuan2"));
  }

  @Test
  public void unreadOnlyCountsMessagesAndActiveConversationIsRead() {
    SessionState s = session();
    feed(s, ":an!u@h JOIN #c");
    assertEquals(0, s.channel("#c").unread);
    feed(s, ":an!u@h PRIVMSG #c :hello");
    assertEquals(1, s.channel("#c").unread);
    assertTrue(s.channel("#c").firstUnreadId > 0);
    s.activeTarget = "#c";
    s.markRead("#c");
    feed(s, ":an!u@h PRIVMSG #c :again");
    assertEquals(0, s.channel("#c").unread);
    feed(s, ":an!u@h PRIVMSG minh :private");
    assertEquals(1, s.conversation("an").unread);
  }

  @Test
  public void nicknameMovesPmAndMemberAndSelfNickname() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":an!u@h JOIN #c");
    feed(s, ":an!u@h PRIVMSG minh :hi");
    Conversation before = s.conversation("an");
    feed(s, ":an!u@h NICK :ann");
    assertSame(before, s.conversation("ann"));
    assertFalse(s.conversations.containsKey("an"));
    assertTrue(s.channel("#c").members.containsKey("ann"));
    feed(s, ":minh!u@h NICK :minh2");
    assertEquals("minh2", s.nickname);
  }

  @Test
  public void reconnectKeepsWantedButExplicitPartAndKickRemoveIt() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    s.connectionLost();
    assertFalse(s.channel("#c").joined);
    assertTrue(s.channel("#c").wanted);
    assertTrue(s.channel("#c").members.isEmpty());
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":op!u@h KICK #c minh :bye");
    assertFalse(s.channel("#c").wanted);
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":minh!u@h PART #c :bye");
    assertFalse(s.channel("#c").wanted);
  }

  @Test
  public void numericRejectionMarksOutgoingFailed() {
    SessionState s = session();
    Conversation c = s.conversation("an");
    ChatMessage m = new ChatMessage("minh", "test", true, ChatMessage.Status.WRITTEN);
    c.add(m);
    feed(s, ":server 401 minh an :No such nick");
    assertEquals(ChatMessage.Status.FAILED, m.status);
    assertEquals("401", s.errorCode);
  }

  @Test
  public void disconnectMarksPendingUncertainAndNeverRetries() {
    SessionState s = session();
    Conversation c = s.conversation("an");
    c.add(new ChatMessage("minh", "test", true, ChatMessage.Status.QUEUED));
    s.connectionLost();
    assertEquals(ChatMessage.Status.UNCERTAIN, c.messages.get(0).status);
    assertEquals(1, c.messages.size());
  }

  @Test
  public void historyIsBoundedAndOwnEchoIsDeduplicated() {
    SessionState s = session();
    Conversation c = s.conversation("#c");
    for (int i = 0; i < 501; i++)
      c.add(new ChatMessage("an", "" + i, false, ChatMessage.Status.RECEIVED));
    assertEquals(500, c.messages.size());
    c.add(new ChatMessage("minh", "hello", true, ChatMessage.Status.WRITTEN));
    feed(s, ":minh!u@h PRIVMSG #c :hello");
    assertEquals(500, c.messages.size());
    assertEquals("hello", c.messages.get(499).text);
  }

  @Test
  public void joinFailureDoesNotPretendJoined() {
    SessionState s = session();
    Channel c = s.channel("#c");
    c.wanted = true;
    c.joining = true;
    feed(s, ":server 475 minh #c :Bad key");
    assertFalse(c.joined);
    assertFalse(c.joining);
    assertFalse(c.wanted);
  }
}
