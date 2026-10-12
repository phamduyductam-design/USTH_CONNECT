import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/** A human-operated second IRC client, using the same protocol implementation as Android. */
public final class InteractivePeer implements IrcClient.Listener {
  final IrcClient client = new IrcClient(this);
  final AtomicReference<String> nick, target;
  final CountDownLatch accepted = new CountDownLatch(1);
  volatile String failure = "";
  InteractivePeer(String nick, String channel) { this.nick = new AtomicReference<>(nick); target = new AtomicReference<>(channel); }
  public void onState(long epoch, ConnectionState state, String reason) {
    System.out.println("[" + state + "] " + reason);
    if (state == ConnectionState.CONNECTED) {
      client.send(IrcCommands.join(target.get(), ""), null); accepted.countDown();
    } else if (state == ConnectionState.FAILED) { failure = reason; accepted.countDown(); }
  }
  public void onMessage(long epoch, IrcMessage m) {
    if (m.command.equals("JOIN") && IrcParser.fold(m.nick()).equals(IrcParser.fold(nick.get()))) target.set(m.last());
    if (m.command.equals("NICK") && IrcParser.fold(m.nick()).equals(IrcParser.fold(nick.get()))) nick.set(m.last());
    if (m.command.equals("PRIVMSG")) System.out.println(m.nick() + " → " + m.param(0) + ": " + m.last());
    else System.out.println("[" + m.command + "] " + m.source + " " + String.join(" ", m.parameters));
  }
  void input(String text) {
    if (text.startsWith("/") && !text.startsWith("//")) {
      SlashCommand c = SlashCommand.parse(text, target.get());
      String line;
      switch (c.name) {
        case "join": line = IrcCommands.join(c.target, c.extra); break;
        case "nick": line = IrcCommands.nick(c.target); break;
        case "msg": line = IrcCommands.message(c.target, c.text); break;
        case "part": line = IrcCommands.part(c.target); break;
        case "names": line = IrcCommands.names(c.target); break;
        case "who": line = IrcCommands.who(c.target); break;
        case "list": line = IrcCommands.list(); break;
        case "topic": line = IrcCommands.topic(c.target, c.text); break;
        case "mode": line = IrcCommands.mode(c.target, c.text, c.extra); break;
        case "kick": line = IrcCommands.kick(c.target, c.text, c.extra); break;
        case "away": line = IrcCommands.away(c.text); break;
        default: System.out.println("/join /nick /msg /part /list /names /who /away /topic /mode /kick; /quit to exit"); return;
      }
      client.send(line, null);
    } else client.send(IrcCommands.message(target.get(), text.startsWith("//") ? text.substring(1) : text), null);
  }
  public static void main(String[] args) throws Exception {
    if (args.length < 4) { System.err.println("Usage: host port tls nickname [#channel]"); return; }
    InteractivePeer peer = new InteractivePeer(args[3], args.length > 4 ? args[4] : "#web-general");
    peer.client.connect(new ConnectionConfig(args[0], Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]), args[3], "usth", "USTH human demo peer"));
    try {
      if (!peer.accepted.await(35, TimeUnit.SECONDS) || !peer.failure.isEmpty()) throw new IllegalStateException("Connection failed: " + peer.failure);
      System.out.println("Type a message or /help. This client does not auto-reply. Use /quit to close.");
      try (Scanner input = new Scanner(System.in)) {
        while (input.hasNextLine()) {
          String line = input.nextLine(); if (line.equals("/quit")) break;
          if (line.trim().isEmpty()) continue;
          try { peer.input(line); } catch (IllegalArgumentException e) { System.err.println(e.getMessage()); }
        }
      }
    } finally { peer.client.disconnect(); Thread.sleep(150); peer.client.shutdown(); }
  }
}
