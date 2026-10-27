package vn.edu.usth.connect.ui.chat;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import vn.edu.usth.connect.model.ChatMessage;

public class MessageSearchTest {
  @Test
  public void treatsRegexCharactersAsLiteralContent() {
    ChatMessage literal = message("an", "Giữ nguyên [a+b]. ở đây");
    ChatMessage regexOnly = message("an", "Giữ nguyên aaabX ở đây");
    assertEquals(
        Arrays.asList(literal), MessageSearch.filter(Arrays.asList(literal, regexOnly), "[a+b]."));
    assertArrayEquals(new int[] {2, 8}, MessageSearch.ranges("x [a+b]. y", "[a+b].").get(0));
  }

  @Test
  public void ignoresVietnameseCaseAndTrimsQueryWithoutRemovingAccents() {
    ChatMessage matching = message("an", "Bài ĐIỆN tử");
    ChatMessage unaccented = message("an", "Bài dien tu");
    assertEquals(
        Arrays.asList(matching),
        MessageSearch.filter(Arrays.asList(matching, unaccented), "  điện\t"));
    assertArrayEquals(new int[] {4, 8}, MessageSearch.ranges(matching.text, " ĐIỆN ").get(0));
  }

  @Test
  public void searchesContentOnlyAndExcludesSystemEvents() {
    ChatMessage senderOnly = message("điện", "Xin chào");
    ChatMessage content = message("an", "điện tử");
    ChatMessage system =
        new ChatMessage("an", "điện tử", false, ChatMessage.Status.RECEIVED, "JOIN", 1);
    assertEquals(
        Arrays.asList(content),
        MessageSearch.filter(Arrays.asList(senderOnly, system, content), "điện"));
  }

  @Test
  public void emptyQueryReturnsAnIndependentCopyIncludingSystemRows() {
    ChatMessage chat = message("an", "Xin chào");
    ChatMessage system = new ChatMessage("an", "", false, ChatMessage.Status.RECEIVED, "JOIN", 1);
    List<ChatMessage> original = Arrays.asList(system, chat);
    List<ChatMessage> result = MessageSearch.filter(original, " \t\n");
    assertEquals(original, result);
    assertNotSame(original, result);
    result.clear();
    assertEquals(2, original.size());
    assertTrue(MessageSearch.ranges("Xin chào", " \t\n").isEmpty());
  }

  @Test
  public void keepsOriginalUtf16OffsetsAroundEmoji() {
    String text = "😊 ĐIỆN 😊 điện";
    List<int[]> ranges = MessageSearch.ranges(text, "điện");
    assertEquals(2, ranges.size());
    assertArrayEquals(new int[] {3, 7}, ranges.get(0));
    assertArrayEquals(new int[] {11, 15}, ranges.get(1));
    assertEquals("ĐIỆN", text.substring(ranges.get(0)[0], ranges.get(0)[1]));
    assertArrayEquals(new int[] {0, 2}, MessageSearch.ranges(text, "😊").get(0));
  }

  @Test
  public void returnsAllNonOverlappingMatchesAndHandlesMissingContent() {
    List<int[]> ranges = MessageSearch.ranges("aaaaa", "aa");
    assertEquals(2, ranges.size());
    assertArrayEquals(new int[] {0, 2}, ranges.get(0));
    assertArrayEquals(new int[] {2, 4}, ranges.get(1));
    assertTrue(MessageSearch.ranges("Xin chào", "không có").isEmpty());
    assertTrue(
        MessageSearch.filter(Arrays.asList(message("an", "Xin chào")), "tạm biệt").isEmpty());
  }

  private static ChatMessage message(String sender, String text) {
    return new ChatMessage(sender, text, false, ChatMessage.Status.RECEIVED);
  }
}
