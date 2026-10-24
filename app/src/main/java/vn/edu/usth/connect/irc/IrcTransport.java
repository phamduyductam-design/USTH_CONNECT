package vn.edu.usth.connect.irc;

import vn.edu.usth.connect.model.ConnectionConfig;

/** Connection operations used by the session controller and deterministic request tests. */
public interface IrcTransport {
  long epoch();

  void connect(ConnectionConfig config);

  void send(String line, IrcClient.SendCallback callback);

  void reconnectNow();

  void disconnect();
}
