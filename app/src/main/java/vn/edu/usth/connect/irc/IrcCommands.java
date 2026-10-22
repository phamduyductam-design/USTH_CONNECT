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

  public static String names(String channel) {
    return checked("NAMES " + channel(channel));
  }

  public static String list() {
    return "LIST";
  }

  public static String who(String target) {
    return checked("WHO " + (IrcParser.isChannel(target) ? channel(target) : token(target)));
  }

  public static String topic(String channel, String text) {
    return checked("TOPIC " + channel(channel) + (text == null ? "" : " :" + text));
  }

  public static String kick(String channel, String nick, String reason) {
    nick(nick);
    return checked(
        "KICK "
            + channel(channel)
            + " "
            + token(nick)
            + (reason == null || reason.isEmpty() ? "" : " :" + reason));
  }

  public static String mode(String channel, String flags, String arguments) {
    String line = "MODE " + channel(channel);
    if (flags == null || flags.isEmpty()) return line;
    if (!flags.matches("[+-][A-Za-z+-]+")) throw new IllegalArgumentException("mode");
    line += " " + flags;
    if (arguments != null && !arguments.isEmpty())
      for (String arg : arguments.split(" +")) line += " " + token(arg);
    return checked(line);
  }

  public static String away(String reason) {
    return checked(reason == null || reason.isEmpty() ? "AWAY" : "AWAY :" + reason);
  }

  public static String message(String target, String text) {
    if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("empty");
    return checked("PRIVMSG " + token(target) + " :" + text);
  }
}
