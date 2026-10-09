package vn.edu.usth.connect.irc;

import java.nio.charset.StandardCharsets;

/**
 * Typed builders prevent user input from injecting another IRC command. 512 bytes includes CRLF.
 */
public final class IrcCommands {
  private IrcCommands() {}

  public static String checked(String line) {
    if (line.indexOf('\r') >= 0 || line.indexOf('\n') >= 0 || line.indexOf('\0') >= 0)
      throw new IllegalArgumentException("control");
    if (line.getBytes(StandardCharsets.UTF_8).length + 2 > 512)
      throw new IllegalArgumentException("length");
    return line;
  }

  private static String token(String s) {
    if (s == null || s.isEmpty() || s.startsWith(":") || s.matches(".*[\\s,\\x00].*"))
      throw new IllegalArgumentException("token");
    return s;
  }

  public static String nick(String nick) {
    if (nick == null
        || !nick.matches("[A-Za-z\\[\\]\\\\`_^{|}][A-Za-z0-9\\[\\]\\\\`_^{|}\\-]{0,29}"))
      throw new IllegalArgumentException("nick");
    return checked("NICK " + nick);
  }

  public static String user(String username, String realName) {
    if (realName == null || realName.trim().isEmpty())
      throw new IllegalArgumentException("realName");
    return checked("USER " + token(username) + " 0 * :" + realName);
  }

  public static String channel(String channel) {
    token(channel);
    if (!IrcParser.isChannel(channel) || channel.length() < 2 || channel.indexOf('\u0007') >= 0)
      throw new IllegalArgumentException("channel");
    return channel;
  }

  public static String join(String channel, String key) {
    return checked(
        "JOIN " + channel(channel) + (key == null || key.isEmpty() ? "" : " " + token(key)));
  }

  public static String part(String channel) {
    return checked("PART " + channel(channel));
  }

  public static String message(String target, String text) {
    if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("empty");
    return checked("PRIVMSG " + token(target) + " :" + text);
  }
}
