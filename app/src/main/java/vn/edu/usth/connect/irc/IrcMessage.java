package vn.edu.usth.connect.irc;

import java.util.List;
import java.util.Map;

public final class IrcMessage {
  public final Map<String, String> tags;
  public final String source, command, trailing;
  public final List<String> parameters;

  IrcMessage(
      Map<String, String> tags,
      String source,
      String command,
      List<String> params,
      String trailing) {
    this.tags = tags;
    this.source = source;
    this.command = command;
    this.parameters = params;
    this.trailing = trailing;
  }

  public String param(int i) {
    return i >= 0 && i < parameters.size() ? parameters.get(i) : "";
  }

  public String nick() {
    int at = source.indexOf('!');
    return at < 0 ? source : source.substring(0, at);
  }

  public String last() {
    return trailing != null ? trailing : param(parameters.size() - 1);
  }
}
