package vn.edu.usth.connect.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vn.edu.usth.connect.model.ChatMessage;

/** Literal, case-insensitive search over message content already held in memory. */
public final class MessageSearch {
  private MessageSearch() {}

  public static List<ChatMessage> filter(List<ChatMessage> messages, String query) {
    Pattern pattern = pattern(query);
    if (pattern == null) return new ArrayList<>(messages);
    List<ChatMessage> result = new ArrayList<>();
    for (ChatMessage message : messages)
      if (message.event.isEmpty() && pattern.matcher(message.text).find()) result.add(message);
    return result;
  }

  /** Returns non-overlapping [start, end) offsets into the original UTF-16 text. */
  public static List<int[]> ranges(String text, String query) {
    List<int[]> result = new ArrayList<>();
    Pattern pattern = pattern(query);
    if (pattern == null) return result;
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) result.add(new int[] {matcher.start(), matcher.end()});
    return result;
  }

  private static Pattern pattern(String query) {
    String trimmed = query.trim();
    return trimmed.isEmpty()
        ? null
        : Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }
}
