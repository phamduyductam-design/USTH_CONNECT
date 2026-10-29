import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import vn.edu.usth.connect.irc.IrcClient;
import vn.edu.usth.connect.irc.IrcCommands;
import vn.edu.usth.connect.irc.IrcMessage;
import vn.edu.usth.connect.irc.IrcParser;
import vn.edu.usth.connect.irc.SlashCommand;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.model.ConnectionState;

public final class InteractivePeer implements IrcClient.Listener {
  final IrcClient client = new IrcClient(this);
  final AtomicReference<String> nick;
  final AtomicReference<String> target;
  final CountDownLatch accepted = new CountDownLatch(1);
  volatile String failure = "";

  InteractivePeer(String nick, String channel) {
    this.nick = new AtomicReference<>(nick);
    target = new AtomicReference<>(channel);
  }

  public void onState(long epoch, ConnectionState state, String reason) {
    System.out.println("[" + state + "] " + reason);
    if (state == ConnectionState.CONNECTED) {
      client.send(IrcCommands.join(target.get(), ""), null);
      accepted.countDown();
    } else if (state == ConnectionState.FAILED) {
      failure = reason;
      accepted.countDown();
    }
  }

  public void onMessage(long epoch, IrcMessage message) {
    if (message.command.equals("JOIN")
        && IrcParser.fold(message.nick()).equals(IrcParser.fold(nick.get()))) {
      target.set(message.last());
    }
    if (message.command.equals("NICK")
        && IrcParser.fold(message.nick()).equals(IrcParser.fold(nick.get()))) {
      nick.set(message.last());
    }
    if (message.command.equals("PRIVMSG")) {
      System.out.println(message.nick() + " → " + message.param(0) + ": " + message.last());
    } else {
      System.out.println(
          "["
              + message.command
              + "] "
              + message.source
              + " "
              + String.join(" ", message.parameters));
    }
  }

  void input(String text) {
    if (text.startsWith("/") && !text.startsWith("//")) {
      SlashCommand command = SlashCommand.parse(text, target.get());
      String line;
      switch (command.name) {
        case "join":
          line = IrcCommands.join(command.target, command.extra);
          break;
        case "nick":
          line = IrcCommands.nick(command.target);
          break;
        case "msg":
          line = IrcCommands.message(command.target, command.text);
          break;
        case "part":
          line = IrcCommands.part(command.target);
          break;
        case "names":
          line = IrcCommands.names(command.target);
          break;
        case "who":
          line = IrcCommands.who(command.target);
          break;
        case "list":
          line = IrcCommands.list();
          break;
        case "topic":
          line = IrcCommands.topic(command.target, command.text);
          break;
        case "mode":
          line = IrcCommands.mode(command.target, command.text, command.extra);
          break;
        case "kick":
          line = IrcCommands.kick(command.target, command.text, command.extra);
          break;
        case "away":
          line = IrcCommands.away(command.text);
          break;
        default:
          System.out.println(
              "/join /nick /msg /part /list /names /who /away /topic /mode /kick; /quit to exit");
          return;
      }
      client.send(line, null);
    } else {
      client.send(
          IrcCommands.message(target.get(), text.startsWith("//") ? text.substring(1) : text),
          null);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 4) {
      System.err.println("Usage: host port tls nickname [#channel]");
      return;
    }
    InteractivePeer peer = new InteractivePeer(args[3], args.length > 4 ? args[4] : "#web-general");
    peer.client.connect(
        new ConnectionConfig(
            args[0],
            Integer.parseInt(args[1]),
            Boolean.parseBoolean(args[2]),
            args[3],
            "usth",
            "USTH human demo peer"));
    try {
      if (!peer.accepted.await(35, TimeUnit.SECONDS) || !peer.failure.isEmpty()) {
        throw new IllegalStateException("Connection failed: " + peer.failure);
      }
      System.out.println(
          "Type a message or /help. This client does not auto-reply. Use /quit to close.");
      try (Scanner input = new Scanner(System.in)) {
        while (input.hasNextLine()) {
          String line = input.nextLine();
          if (line.equals("/quit")) {
            break;
          }
          if (line.trim().isEmpty()) {
            continue;
          }
          try {
            peer.input(line);
          } catch (IllegalArgumentException error) {
            System.err.println(error.getMessage());
          }
        }
      }
    } finally {
      peer.client.disconnect();
      Thread.sleep(150);
      peer.client.shutdown();
    }
  }
}
