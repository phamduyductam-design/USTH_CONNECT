import vn.edu.usth.connect.irc.IrcCommands;
import vn.edu.usth.connect.irc.IrcMessage;
import vn.edu.usth.connect.irc.NicknameMentions;

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
    String ownerNickname = "oa" + suffix;
    String memberNickname = "ob" + suffix;
    String room = "#usth-feature-" + suffix;
    try (NetworkSmoke.Peer ownerPeer = new NetworkSmoke.Peer(host, port, tls, ownerNickname);
        NetworkSmoke.Peer memberPeer = new NetworkSmoke.Peer(host, port, tls, memberNickname)) {
      ownerPeer.awaitConnection();
      memberPeer.awaitConnection();
      ownerPeer.send(IrcCommands.join(room, ""));
      ownerPeer.await(message -> message.command.equals("366") && message.param(1).equals(room));
      memberPeer.send(IrcCommands.join(room, ""));
      memberPeer.await(message -> message.command.equals("366") && message.param(1).equals(room));
      ownerPeer.await(
          message -> message.command.equals("JOIN") && message.nick().equals(memberNickname));
      ownerPeer.send(IrcCommands.list());
      IrcMessage listed =
          ownerPeer.await(
              message -> message.command.equals("322") && message.param(1).equals(room));
      if (!listed.param(2).equals("2")) {
        throw new IllegalStateException("Incorrect LIST member count");
      }
      ownerPeer.await(message -> message.command.equals("323"));
      pass("LIST returns the real temporary channel and its two users");
      ownerPeer.send(IrcCommands.names(room));
      ownerPeer.await(
          message ->
              message.command.equals("353")
                  && message.last().contains(ownerNickname)
                  && message.last().contains(memberNickname));
      ownerPeer.await(message -> message.command.equals("366"));
      pass("explicit NAMES refresh");
      ownerPeer.send(IrcCommands.topic(room, "Chủ đề kiểm thử IRC"));
      memberPeer.await(
          message ->
              message.command.equals("TOPIC") && message.last().equals("Chủ đề kiểm thử IRC"));
      pass("operator TOPIC broadcast");
      ownerPeer.send(IrcCommands.mode(room, "+tv", memberNickname));
      memberPeer.await(
          message ->
              message.command.equals("MODE")
                  && message.param(1).contains("v")
                  && message.param(2).equals(memberNickname));
      pass("MODE grants Voice and updates topic restriction");
      memberPeer.send(IrcCommands.mode(room, "+o", ownerNickname));
      memberPeer.await(message -> message.command.equals("482"));
      memberPeer.send(IrcCommands.topic(room, "Không được phép"));
      memberPeer.await(message -> message.command.equals("482"));
      pass("server rejects non-operator MODE and TOPIC with 482");
      memberPeer.send(IrcCommands.away("Đang học"));
      memberPeer.await(message -> message.command.equals("306"));
      ownerPeer.await(
          message ->
              message.command.equals("AWAY")
                  && message.nick().equals(memberNickname)
                  && message.last().equals("Đang học"));
      ownerPeer.send(IrcCommands.who(room));
      ownerPeer.await(
          message ->
              message.command.equals("352")
                  && message.param(5).equals(memberNickname)
                  && message.param(6).startsWith("G"));
      ownerPeer.await(message -> message.command.equals("315"));
      pass("AWAY acknowledgement, negotiated away-notify and WHO presence");
      memberPeer.send(IrcCommands.away(""));
      memberPeer.await(message -> message.command.equals("305"));
      ownerPeer.await(
          message ->
              message.command.equals("AWAY")
                  && message.nick().equals(memberNickname)
                  && message.parameters.isEmpty());
      pass("clearing AWAY updates the other client");
      ownerPeer.send(IrcCommands.mode(room, "+o", memberNickname));
      memberPeer.await(message -> message.command.equals("MODE") && message.param(1).equals("+o"));
      memberPeer.send(IrcCommands.topic(room, "Chủ đề từ Operator mới"));
      ownerPeer.await(
          message ->
              message.command.equals("TOPIC") && message.last().equals("Chủ đề từ Operator mới"));
      ownerPeer.send(IrcCommands.mode(room, "-o", memberNickname));
      memberPeer.await(message -> message.command.equals("MODE") && message.param(1).equals("-o"));
      ownerPeer.send(IrcCommands.mode(room, "-v", memberNickname));
      memberPeer.await(message -> message.command.equals("MODE") && message.param(1).equals("-v"));
      pass("grant and revoke Operator / Voice");
      ownerPeer.send(IrcCommands.mode(room, "+m", ""));
      memberPeer.await(message -> message.command.equals("MODE") && message.param(1).equals("+m"));
      memberPeer.send(IrcCommands.message(room, "Bị chặn bởi moderated mode"));
      memberPeer.await(message -> message.command.equals("404"));
      ownerPeer.send(IrcCommands.mode(room, "-m", ""));
      memberPeer.await(message -> message.command.equals("MODE") && message.param(1).equals("-m"));
      pass("moderated channel enforces permissions on the server");
      memberPeer.send(IrcCommands.message(room, "Chào @" + ownerNickname + "!"));
      IrcMessage mention =
          ownerPeer.await(
              message ->
                  message.command.equals("PRIVMSG")
                      && message.last().contains("@" + ownerNickname));
      if (NicknameMentions.ranges(mention.last(), ownerNickname).isEmpty()) {
        throw new IllegalStateException("Missing mention");
      }
      pass("real incoming nickname mention");
      ownerPeer.send(IrcCommands.kick(room, memberNickname, "Kiểm thử mời ra khỏi phòng"));
      memberPeer.await(
          message -> message.command.equals("KICK") && message.param(1).equals(memberNickname));
      ownerPeer.await(
          message -> message.command.equals("KICK") && message.param(1).equals(memberNickname));
      pass("KICK broadcast to both clients");
      ownerPeer.send(IrcCommands.topic(room, ""));
      ownerPeer.await(message -> message.command.equals("TOPIC") && message.last().isEmpty());
      pass("explicit topic clearing");
    }
  }
}
