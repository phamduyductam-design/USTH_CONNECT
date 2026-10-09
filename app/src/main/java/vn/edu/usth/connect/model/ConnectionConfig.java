package vn.edu.usth.connect.model;

import vn.edu.usth.connect.irc.IrcCommands;

public final class ConnectionConfig {
  public final String host, nickname, username, realName;
  public final int port;
  public final boolean tls;

  public ConnectionConfig(
      String host, int port, boolean tls, String nickname, String username, String realName) {
    if (host == null
        || !host.matches("[A-Za-z0-9:.\\-]+")
        || host.length() > 253
        || port < 1
        || port > 65535) throw new IllegalArgumentException("config");
    IrcCommands.nick(nickname);
    IrcCommands.user(username, realName);
    this.host = host;
    this.port = port;
    this.tls = tls;
    this.nickname = nickname;
    this.username = username;
    this.realName = realName;
  }

  public String serverId() {
    return host.toLowerCase(java.util.Locale.ROOT) + ":" + port + ":" + tls;
  }

  public ConnectionConfig withNick(String nick) {
    return new ConnectionConfig(host, port, tls, nick, username, realName);
  }
}
