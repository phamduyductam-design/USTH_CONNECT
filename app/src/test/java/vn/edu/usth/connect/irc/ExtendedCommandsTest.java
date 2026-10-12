package vn.edu.usth.connect.irc;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class ExtendedCommandsTest {
  @Test public void slashCommandsProduceTypedOperationsAndKeepBodies() {
    SlashCommand c = SlashCommand.parse("/msg an Chào bạn: đến đây", "#c");
    assertEquals("msg", c.name); assertEquals("an", c.target); assertEquals("Chào bạn: đến đây", c.text);
    assertEquals("key", SlashCommand.parse("/JOIN #c key", "an").extra);
    assertEquals("#c", SlashCommand.parse("/part", "#c").target);
    assertEquals("", SlashCommand.parse("/away", "#c").text);
    assertNull(SlashCommand.parse("/topic #c", "#d").text);
    assertEquals("", SlashCommand.parse("/topic #c :", "#d").text);
    c = SlashCommand.parse("/mode #c +ov an ha", "#c");
    assertEquals("+ov", c.text); assertEquals("an ha", c.extra);
    c = SlashCommand.parse("/kick #c an spam test", "#d");
    assertEquals("an", c.text); assertEquals("spam test", c.extra);
    for (String input : new String[] {"/unknown", "/msg an", "/nick bad nick", "/join web", "/part #c extra", "/list extra", "/mode an +o b"}) {
      try { SlashCommand.parse(input, "#c"); fail(input); } catch (IllegalArgumentException expected) { }
    }
  }
  @Test public void buildersRejectInjectionAndKeepByteLimit() {
    assertEquals("NAMES #c", IrcCommands.names("#c"));
    assertEquals("LIST", IrcCommands.list());
    assertEquals("TOPIC #c", IrcCommands.topic("#c", null));
    assertEquals("TOPIC #c :", IrcCommands.topic("#c", ""));
    assertEquals("MODE #c +ov an ha", IrcCommands.mode("#c", "+ov", "an ha"));
    assertEquals("AWAY", IrcCommands.away(""));
    assertEquals("KICK #c an :Lý do", IrcCommands.kick("#c", "an", "Lý do"));
    Runnable[] attempts = {() -> IrcCommands.topic("#c", "ok\r\nQUIT"), () -> IrcCommands.away("x\nQUIT"),
        () -> IrcCommands.kick("#c", "an", "\0"), () -> IrcCommands.mode("#c", "+o", "an\r\nQUIT"),
        () -> IrcCommands.topic("#c", "ế".repeat(200))};
    for (Runnable attempt : attempts) { try { attempt.run(); fail(); } catch (IllegalArgumentException expected) { } }
    int bytes = 510 - "TOPIC #c :".getBytes(StandardCharsets.UTF_8).length;
    assertEquals(512, (IrcCommands.topic("#c", "a".repeat(bytes)) + "\r\n").getBytes(StandardCharsets.UTF_8).length);
  }
  @Test public void mentionsRespectBoundariesRfcFoldingAndOriginalIndices() {
    assertTrue(Mentions.ranges("minh_usth2 aminh_usth minh_usth_x", "minh_usth").isEmpty());
    assertEquals(2, Mentions.ranges("@MINH_usth, chào minh_usth!", "minh_usth").size());
    assertEquals(1, Mentions.ranges("Chào n{ck~!", "N[CK^").size());
    String text = "İ 😊 @minh, xin chào";
    int[] range = Mentions.ranges(text, "minh").get(0);
    assertEquals("minh", text.substring(range[0], range[1]));
  }
}
