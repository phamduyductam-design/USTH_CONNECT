package vn.edu.usth.connect.irc;

import java.util.*;

/** Nickname boundaries and RFC1459 folding; indices remain valid in the original UTF-16 text. */
public final class Mentions {
  private Mentions() {}
  private static String fold(String text) {
    StringBuilder out = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (c >= 'A' && c <= 'Z') c += 32;
      else if (c == '[') c = '{';
      else if (c == ']') c = '}';
      else if (c == '\\') c = '|';
      else if (c == '^') c = '~';
      out.append(c);
    }
    return out.toString();
  }
  private static boolean nickChar(char c) {
    return Character.isLetterOrDigit(c) || "_-[]\\`^{}|~".indexOf(c) >= 0;
  }
  public static List<int[]> ranges(String text, String nick) {
    List<int[]> ranges = new ArrayList<>();
    if (nick == null || nick.isEmpty()) return ranges;
    String haystack = fold(text), needle = fold(nick);
    for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
      int end = at + needle.length();
      if ((at == 0 || !nickChar(text.charAt(at - 1)))
          && (end == text.length() || !nickChar(text.charAt(end)))) ranges.add(new int[] {at, end});
    }
    return ranges;
  }
}
