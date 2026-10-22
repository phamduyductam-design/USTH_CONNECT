package vn.edu.usth.connect.irc;

import java.util.Locale;

/** Parses user commands into typed operations; never forwards arbitrary raw IRC input. */
public final class SlashCommand {
  public final String name, target, text, extra;

  private SlashCommand(String name, String target, String text, String extra) {
    this.name = name;
    this.target = target;
    this.text = text;
    this.extra = extra;
  }

  private static String[] split(String value) {
    return value.trim().split("\\s+", 2);
  }

  private static void need(boolean ok, String usage) {
    if (!ok) throw new IllegalArgumentException("Cách dùng: " + usage);
  }

  public static SlashCommand parse(String input, String current) {
    String[] head = split(input.substring(1));
    String name = head[0].toLowerCase(Locale.ROOT), args = head.length == 2 ? head[1].trim() : "";
    String[] a = split(args);
    String first = a[0], rest = a.length == 2 ? a[1] : "";
    String target = current, text = "", extra = "";
    switch (name) {
      case "join":
        need(
            !first.isEmpty() && (rest.isEmpty() || !rest.matches(".*\\s+.*")),
            "/join #phòng [key]");
        target = first;
        extra = rest;
        IrcCommands.join(target, extra);
        break;
      case "nick":
        need(!first.isEmpty() && rest.isEmpty(), "/nick biệt_danh");
        target = first;
        IrcCommands.nick(target);
        break;
      case "msg":
        need(!first.isEmpty() && !rest.isEmpty(), "/msg biệt_danh nội dung");
        target = first;
        text = rest;
        IrcCommands.nick(target);
        IrcCommands.message(target, text);
        break;
      case "part":
      case "names":
      case "who":
        need(rest.isEmpty(), "/" + name + " [#phòng]");
        target = first.isEmpty() ? current : first;
        if (name.equals("who")) IrcCommands.who(target);
        else IrcCommands.channel(target);
        break;
      case "list":
      case "help":
        need(args.isEmpty(), "/" + name);
        break;
      case "away":
        text = args;
        IrcCommands.away(text);
        break;
      case "topic":
        if (IrcParser.isChannel(first)) {
          target = first;
          text = rest;
        } else text = args;
        // Null queries; a lone colon explicitly clears the topic.
        text = text.isEmpty() ? null : text.equals(":") ? "" : text;
        IrcCommands.topic(target, text);
        break;
      case "kick":
        need(IrcParser.isChannel(first) && !rest.isEmpty(), "/kick #phòng biệt_danh [lý do]");
        target = first;
        String[] victim = split(rest);
        text = victim[0];
        extra = victim.length == 2 ? victim[1] : "";
        IrcCommands.kick(target, text, extra);
        break;
      case "mode":
        need(IrcParser.isChannel(first), "/mode #phòng [+o|-o|+v|-v…] [biệt_danh]");
        target = first;
        String[] flags = split(rest);
        text = flags[0];
        extra = flags.length == 2 ? flags[1] : "";
        IrcCommands.mode(target, text, extra);
        break;
      default:
        throw new IllegalArgumentException(
            "Lệnh không hỗ trợ: /" + name + ". Dùng /help để xem hướng dẫn.");
    }
    return new SlashCommand(name, target, text, extra);
  }
}
