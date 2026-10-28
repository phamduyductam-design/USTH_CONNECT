package vn.edu.usth.connect.irc;

import java.util.*;

public final class IrcParser {
  private IrcParser() {}

  public static IrcMessage parse(String line) {
    if (line == null
        || line.isEmpty()
        || line.indexOf('\r') >= 0
        || line.indexOf('\n') >= 0
        || line.indexOf('\0') >= 0) throw new IllegalArgumentException("line");
    String rest = line;
    Map<String, String> tags = new LinkedHashMap<>();
    if (rest.startsWith("@")) {
      int space = rest.indexOf(' ');
      if (space < 2) throw new IllegalArgumentException("tags");
      for (String tag : rest.substring(1, space).split(";")) {
        int eq = tag.indexOf('=');
        tags.put(
            eq < 0 ? tag : tag.substring(0, eq), eq < 0 ? "" : unescape(tag.substring(eq + 1)));
      }
      rest = leftTrim(rest.substring(space + 1));
    }
    String source = "";
    if (rest.startsWith(":")) {
      int space = rest.indexOf(' ');
      if (space < 2) throw new IllegalArgumentException("source");
      source = rest.substring(1, space);
      rest = leftTrim(rest.substring(space + 1));
    }
    int space = rest.indexOf(' ');
    String command = space < 0 ? rest : rest.substring(0, space);
    if (!command.matches("[A-Za-z]+|[0-9]{3}")) throw new IllegalArgumentException("command");
    rest = space < 0 ? "" : rest.substring(space + 1);
    List<String> params = new ArrayList<>();
    String trailing = null;
    while (!rest.isEmpty()) {
      while (rest.startsWith(" ")) rest = rest.substring(1);
      if (rest.isEmpty()) break;
      if (rest.startsWith(":")) {
        trailing = rest.substring(1);
        params.add(trailing);
        break;
      }
      space = rest.indexOf(' ');
      params.add(space < 0 ? rest : rest.substring(0, space));
      rest = space < 0 ? "" : rest.substring(space + 1);
    }
    if (params.size() > 15) throw new IllegalArgumentException("parameters");
    return new IrcMessage(
        Collections.unmodifiableMap(tags),
        source,
        command.toUpperCase(Locale.ROOT),
        Collections.unmodifiableList(params),
        trailing);
  }

  private static String leftTrim(String s) {
    int i = 0;
    while (i < s.length() && s.charAt(i) == ' ') i++;
    return s.substring(i);
  }

  private static String unescape(String s) {
    StringBuilder out = new StringBuilder();
    boolean escape = false;
    for (char c : s.toCharArray()) {
      if (escape) {
        out.append(c == ':' ? ';' : c == 's' ? ' ' : c == 'r' ? '\r' : c == 'n' ? '\n' : c);
        escape = false;
      } else if (c == '\\') escape = true;
      else out.append(c);
    }
    return out.toString();
  }

  public static String fold(String s) {
    return s.toLowerCase(Locale.ROOT)
        .replace('[', '{')
        .replace(']', '}')
        .replace('\\', '|')
        .replace('^', '~');
  }

  public static boolean isChannel(String target) {
    return !target.isEmpty() && "#&+!".indexOf(target.charAt(0)) >= 0;
  }
}
