import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/** Live IRC feature checks, isolated to two temporary nicknames and a unique channel. */
public final class AdvancedNetworkSmoke {
  static void pass(String text) {
    System.out.println("PASS " + text);
  }

  public static void main(String[] args) throws Exception {
    String host = args.length > 0 ? args[0] : "127.0.0.1";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 6667;
    boolean tls = args.length > 2 && Boolean.parseBoolean(args[2]);
    String suffix = Long.toString(System.nanoTime(), 36);
    suffix = suffix.substring(Math.max(0, suffix.length() - 6));
    String owner = "oa" + suffix, user = "ob" + suffix, room = "#usth-feature-" + suffix;
    try (NetworkSmoke.Peer a = new NetworkSmoke.Peer(host, port, tls, owner);
        NetworkSmoke.Peer b = new NetworkSmoke.Peer(host, port, tls, user)) {
      a.awaitConnection();
      b.awaitConnection();
      a.send(IrcCommands.join(room, ""));
      a.await(m -> m.command.equals("366") && m.param(1).equals(room));
      b.send(IrcCommands.join(room, ""));
      b.await(m -> m.command.equals("366") && m.param(1).equals(room));
      a.await(m -> m.command.equals("JOIN") && m.nick().equals(user));
      a.send(IrcCommands.list());
      IrcMessage listed = a.await(m -> m.command.equals("322") && m.param(1).equals(room));
      if (!listed.param(2).equals("2"))
        throw new IllegalStateException("Incorrect LIST member count");
      a.await(m -> m.command.equals("323"));
      pass("LIST returns the real temporary channel and its two users");
      a.send(IrcCommands.names(room));
      a.await(m -> m.command.equals("353") && m.last().contains(owner) && m.last().contains(user));
      a.await(m -> m.command.equals("366"));
      pass("explicit NAMES refresh");
      a.send(IrcCommands.topic(room, "Chủ đề kiểm thử IRC"));
      b.await(m -> m.command.equals("TOPIC") && m.last().equals("Chủ đề kiểm thử IRC"));
      pass("operator TOPIC broadcast");
      a.send(IrcCommands.mode(room, "+tv", user));
      b.await(m -> m.command.equals("MODE") && m.param(1).contains("v") && m.param(2).equals(user));
      pass("MODE grants Voice and updates topic restriction");
      b.send(IrcCommands.mode(room, "+o", owner));
      b.await(m -> m.command.equals("482"));
      b.send(IrcCommands.topic(room, "Không được phép"));
      b.await(m -> m.command.equals("482"));
      pass("server rejects non-operator MODE and TOPIC with 482");
      b.send(IrcCommands.away("Đang học"));
      b.await(m -> m.command.equals("306"));
      a.await(
          m -> m.command.equals("AWAY") && m.nick().equals(user) && m.last().equals("Đang học"));
      a.send(IrcCommands.who(room));
      a.await(
          m -> m.command.equals("352") && m.param(5).equals(user) && m.param(6).startsWith("G"));
      a.await(m -> m.command.equals("315"));
      pass("AWAY acknowledgement, negotiated away-notify and WHO presence");
      b.send(IrcCommands.away(""));
      b.await(m -> m.command.equals("305"));
      a.await(m -> m.command.equals("AWAY") && m.nick().equals(user) && m.parameters.isEmpty());
      pass("clearing AWAY updates the other client");
      a.send(IrcCommands.mode(room, "+o", user));
      b.await(m -> m.command.equals("MODE") && m.param(1).equals("+o"));
      b.send(IrcCommands.topic(room, "Chủ đề từ Operator mới"));
      a.await(m -> m.command.equals("TOPIC") && m.last().equals("Chủ đề từ Operator mới"));
      a.send(IrcCommands.mode(room, "-o", user));
      b.await(m -> m.command.equals("MODE") && m.param(1).equals("-o"));
      a.send(IrcCommands.mode(room, "-v", user));
      b.await(m -> m.command.equals("MODE") && m.param(1).equals("-v"));
      pass("grant and revoke Operator / Voice");
      a.send(IrcCommands.mode(room, "+m", ""));
      b.await(m -> m.command.equals("MODE") && m.param(1).equals("+m"));
      b.send(IrcCommands.message(room, "Bị chặn bởi moderated mode"));
      b.await(m -> m.command.equals("404"));
      a.send(IrcCommands.mode(room, "-m", ""));
      b.await(m -> m.command.equals("MODE") && m.param(1).equals("-m"));
      pass("moderated channel enforces permissions on the server");
      b.send(IrcCommands.message(room, "Chào @" + owner + "!"));
      IrcMessage mention =
          a.await(m -> m.command.equals("PRIVMSG") && m.last().contains("@" + owner));
      if (Mentions.ranges(mention.last(), owner).isEmpty())
        throw new IllegalStateException("Missing mention");
      pass("real incoming nickname mention");
      a.send(IrcCommands.kick(room, user, "Kiểm thử mời ra khỏi phòng"));
      b.await(m -> m.command.equals("KICK") && m.param(1).equals(user));
      a.await(m -> m.command.equals("KICK") && m.param(1).equals(user));
      pass("KICK broadcast to both clients");
      a.send(IrcCommands.topic(room, ""));
      a.await(m -> m.command.equals("TOPIC") && m.last().isEmpty());
      pass("explicit topic clearing");
    }
  }
}
