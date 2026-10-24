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
  public void ambiguousNumericErrorDoesNotMarkEarlierOutgoingFailed() {
    SessionState s = session();
    Conversation c = s.conversation("an");
    ChatMessage m = new ChatMessage("minh", "test", true, ChatMessage.Status.WRITTEN);
    c.add(m);
    feed(s, ":server 401 minh an :No such nick");
    assertEquals(ChatMessage.Status.WRITTEN, m.status);
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

  @Test
  public void listUsesServerRepliesAndCompletesEmptyResults() {
    SessionState s = session();
    feed(s, ":server 321 minh Channel :Users Name");
    feed(s, ":server 322 minh #live 3 :Chủ đề thật");
    assertTrue(s.listing);
    assertEquals(3, s.availableChannels.get("#live").users);
    feed(s, ":server 323 minh :End of LIST");
    assertFalse(s.listing);
    assertTrue(s.listLoaded);
    feed(s, ":server 321 minh Channel :Users Name");
    feed(s, ":server 323 minh :End");
    assertTrue(s.availableChannels.isEmpty());
    s.listing = true;
    feed(s, ":server 421 minh LIST :Unsupported");
    assertFalse(s.listing);
  }

  @Test
  public void modeParametersDoNotShiftRoleTargetsAndRefreshKeepsChanges() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":s 353 minh = #c :@minh an ha");
    feed(s, ":s 366 minh #c :end");
    feed(s, ":minh!u@h MODE #c +kov secret an ha");
    assertEquals("o", s.role("#c", "an"));
    assertEquals("v", s.role("#c", "ha"));
    s.channel("#c").beginNames();
    feed(s, ":s 353 minh = #c :@minh @an +ha");
    feed(s, ":minh!u@h MODE #c -o+v an an");
    feed(s, ":s 366 minh #c :end");
    assertEquals("v", s.role("#c", "an"));
    feed(s, ":s 324 minh #c +nt");
    assertTrue(s.channel("#c").modes.contains('t'));
    feed(s, ":minh!u@h MODE #c -t");
    assertFalse(s.channel("#c").modes.contains('t'));
    assertTrue(s.canManage("#c"));
  }

  @Test
  public void negotiatedPrefixesAndAwayStayInSyncAcrossNickAndQuit() {
    SessionState s = session();
    feed(s, ":s 005 minh PREFIX=(ov)@+ CHANMODES=b,k,l,imnt :supported");
    feed(s, ":minh!u@h JOIN #c");
    feed(s, ":s 353 minh = #c :@minh @+an");
    feed(s, ":s 366 minh #c :end");
    assertEquals("o", s.role("#c", "an"));
    feed(s, ":s 352 minh #c u h s an G@+ :0 An");
    assertTrue(s.awayUsers.containsKey("an"));
    feed(s, ":an!u@h AWAY :Đang học");
    assertEquals("Đang học", s.awayUsers.get("an"));
    feed(s, ":an!u@h NICK :ann");
    assertEquals("Đang học", s.awayUsers.get("ann"));
    feed(s, ":ann!u@h AWAY");
    assertFalse(s.awayUsers.containsKey("ann"));
    feed(s, ":s 301 minh ann :Bận");
    assertTrue(s.awayUsers.containsKey("ann"));
    feed(s, ":ann!u@h QUIT :bye");
    assertFalse(s.awayUsers.containsKey("ann"));
    s.requestedAway = "Bận";
    feed(s, ":s 306 minh :away");
    assertTrue(s.away);
    feed(s, ":s 305 minh :back");
    assertFalse(s.away);
  }

  @Test
  public void mentionDetectionAndPermissionFailuresNeverGrantRights() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c");
    assertFalse(s.canManage("#c"));
    feed(s, ":s 482 minh #c :Not operator");
    assertFalse(s.canManage("#c"));
    assertEquals("Not operator", s.errorDetail);
    ChatMessage m = s.accept(IrcParser.parse(":an!u@h PRIVMSG #c :Chào @minh!"));
    assertEquals("minh", m.mentionNick);
    m = s.accept(IrcParser.parse(":an!u@h PRIVMSG #c :minh2"));
    assertEquals("", m.mentionNick);
    feed(s, ":s 332 minh #c :old");
    feed(s, ":an!u@h TOPIC #c :new");
    assertEquals("new", s.channel("#c").topic);
  }

  @Test
  public void only404FailsARecentChannelMessage() {
    SessionState s = session();
    ChatMessage sent = new ChatMessage("minh", "hi", true, ChatMessage.Status.WRITTEN);
    s.channel("#c").add(sent);
    for (String code : new String[] {"401", "407", "421", "442", "482"}) {
      feed(s, ":s " + code + " minh #c :Rejected command");
      assertEquals(ChatMessage.Status.WRITTEN, sent.status);
      assertEquals(code, s.errorCode);
    }
    feed(s, ":s 404 minh #c :Cannot send to channel");
    assertEquals(ChatMessage.Status.FAILED, sent.status);
  }

  @Test
  public void privateSelfEchoUsesPeerAndDoesNotCreateSelfConversation() {
    SessionState s = session();
    Conversation pm = s.conversation("an");
    pm.add(new ChatMessage("minh", "hello", true, ChatMessage.Status.WRITTEN));
    feed(s, ":minh!u@h PRIVMSG an :hello");
    assertEquals(1, pm.messages.size());
    assertFalse(s.conversations.containsKey("minh"));
  }

  @Test
  public void mergedPmPreservesEarliestUnreadBoundary() {
    SessionState s = session();
    feed(s, ":ann!u@h PRIVMSG minh :older");
    long boundary = s.conversation("ann").firstUnreadId;
    feed(s, ":an!u@h PRIVMSG minh :newer");
    feed(s, ":an!u@h NICK :ann");
    assertEquals(2, s.conversation("ann").unread);
    assertEquals(boundary, s.conversation("ann").firstUnreadId);
  }

  @Test
  public void extendedJoinUsesChannelParameter() {
    SessionState s = session();
    feed(s, ":minh!u@h JOIN #c account :Real Name");
    assertTrue(s.channel("#c").joined);
    assertFalse(s.channels.containsKey("real name"));
  }
}
