package vn.edu.usth.connect.irc;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class IrcParserTest {
  @Test
  public void prefixAndTrailingKeepSpacesAndColons() {
    IrcMessage m = IrcParser.parse(":an!user@host PRIVMSG #web-general :Xin chào: các bạn");
    assertEquals("an", m.nick());
    assertEquals("PRIVMSG", m.command);
    assertEquals("#web-general", m.param(0));
    assertEquals("Xin chào: các bạn", m.trailing);
  }

  @Test
  public void emptyTrailingIsDifferentFromNoTrailing() {
    assertEquals("", IrcParser.parse("TOPIC #c :").trailing);
    assertNull(IrcParser.parse("NICK new_nick").trailing);
    assertEquals("  text  ", IrcParser.parse(":an PRIVMSG #c :  text  ").trailing);
  }

  @Test
  public void serverNumericAndTagEscapes() {
    IrcMessage m = IrcParser.parse("@label=abc;value=a\\sb\\:c\\\\d :server 353 me = #c :@me +an");
    assertEquals("a b;c\\d", m.tags.get("value"));
    assertEquals("#c", m.param(2));
    assertEquals("@me +an", m.last());
  }

  @Test
  public void pingTokenAndRfcCaseFolding() {
    assertEquals("abc", IrcParser.parse("PING :abc").last());
    assertEquals(IrcParser.fold("N[CK^"), IrcParser.fold("n{ck~"));
  }

  @Test
  public void injectionAndUtf8ByteLimit() {
    try {
      IrcCommands.message("#c", "hello\r\nQUIT");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    String prefix = "PRIVMSG #c :";
    int available = 510 - prefix.getBytes(StandardCharsets.UTF_8).length;
    assertEquals(
        512,
        (IrcCommands.message("#c", "a".repeat(available)) + "\r\n")
            .getBytes(StandardCharsets.UTF_8)
            .length);
    try {
      IrcCommands.message("#c", "a".repeat(available + 1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      IrcCommands.message("#c", "ế".repeat(available / 3 + 1));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void framingWorksWithFragmentedInput() throws Exception {
    ByteArrayInputStream input =
        new ByteArrayInputStream(
            "PING :x\r\n:an PRIVMSG #c :chào\r\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("PING :x", IrcClient.readLine(input));
    assertEquals(":an PRIVMSG #c :chào", IrcClient.readLine(input));
    assertNull(IrcClient.readLine(input));
  }

  @Test
  public void malformedFramingIsRejected() {
    for (String line : new String[] {"a\nb", "a\rX", "a\r", "a".repeat(8193) + "\r\n"}) {
      try {
        IrcClient.readLine(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)));
        fail();
      } catch (IOException expected) {
      }
    }
  }

  @Test
  public void rejectsInvalidUserChannelAndNick() {
    for (String nick : new String[] {"1nick", "bad nick", "a\r", ""}) {
      try {
        IrcCommands.nick(nick);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    for (String channel : new String[] {"web", "#", "#a,#b", "#bad channel"}) {
      try {
        IrcCommands.join(channel, "");
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    try {
      IrcCommands.user("user\nQUIT", "test");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
