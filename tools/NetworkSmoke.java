import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import vn.edu.usth.connect.irc.IrcClient;
import vn.edu.usth.connect.irc.IrcCommands;
import vn.edu.usth.connect.irc.IrcMessage;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.model.ConnectionState;

public final class NetworkSmoke {
  static final class Peer implements IrcClient.Listener, AutoCloseable {
    final BlockingQueue<IrcMessage> messages = new LinkedBlockingQueue<>();
    final CountDownLatch connected = new CountDownLatch(1);
    final IrcClient client = new IrcClient(this);
    final String nick;
    volatile String failure = "";

    Peer(String host, int port, boolean tls, String nick) {
      this.nick = nick;
      client.connect(
          new ConnectionConfig(
              host, port, tls, nick, "usth", "USTH Connect automated integration test"));
    }

    public void onState(long epoch, ConnectionState state, String reason) {
      if (state == ConnectionState.CONNECTED) {
        connected.countDown();
      }
      if (state == ConnectionState.FAILED) {
        failure = reason;
        connected.countDown();
      }
    }

    public void onMessage(long epoch, IrcMessage message) {
      messages.add(message);
    }

    void awaitConnection() throws Exception {
      if (!connected.await(35, TimeUnit.SECONDS) || !failure.isEmpty()) {
        throw new IllegalStateException("Connection failed: " + failure);
      }
    }

    IrcMessage await(Predicate<IrcMessage> predicate) throws Exception {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
      while (System.nanoTime() < deadline) {
        IrcMessage message = messages.poll(100, TimeUnit.MILLISECONDS);
        if (message != null && predicate.test(message)) {
          return message;
        }
      }
      throw new IllegalStateException("Expected IRC reply not received");
    }

    void send(String line) throws Exception {
      CountDownLatch sent = new CountDownLatch(1);
      AtomicBoolean writeSucceeded = new AtomicBoolean();
      client.send(
          line,
          (written, uncertain) -> {
            writeSucceeded.set(written);
            sent.countDown();
          });
      if (!sent.await(5, TimeUnit.SECONDS) || !writeSucceeded.get()) {
        throw new IllegalStateException("write failed");
      }
    }

    public void close() {
      client.disconnect();
      try {
        Thread.sleep(150);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
      }
      client.shutdown();
    }
  }

  static void pass(String text) {
    System.out.println("PASS " + text);
  }

  public static void main(String[] args) throws Exception {
    String host = args.length > 0 ? args[0] : "127.0.0.1";
    int port = args.length > 1 ? Integer.parseInt(args[1]) : 6667;
    boolean tls = args.length > 2 && Boolean.parseBoolean(args[2]);
    String suffix = Long.toString(System.nanoTime(), 36);
    suffix = suffix.substring(Math.max(0, suffix.length() - 6));
    String firstNickname = "ua" + suffix;
    String secondNickname = "ub" + suffix;
    String room = "#usth-test-" + suffix;
    try (Peer firstPeer = new Peer(host, port, tls, firstNickname);
        Peer secondPeer = new Peer(host, port, tls, secondNickname)) {
      firstPeer.awaitConnection();
      secondPeer.awaitConnection();
      pass("two real TCP sessions accepted with 001 (TLS=" + tls + ")");
      firstPeer.send(IrcCommands.join(room, ""));
      firstPeer.await(
          message -> message.command.equals("JOIN") && message.nick().equals(firstNickname));
      secondPeer.send(IrcCommands.join(room, ""));
      secondPeer.await(
          message -> message.command.equals("JOIN") && message.nick().equals(secondNickname));
      firstPeer.await(
          message -> message.command.equals("JOIN") && message.nick().equals(secondNickname));
      pass("JOIN acknowledgements and peer membership event");
      secondPeer.await(message -> message.command.equals("366") && message.param(1).equals(room));
      pass("NAMES completed");
      firstPeer.send(IrcCommands.message(room, "Xin chào từ USTH Connect test"));
      secondPeer.await(
          message ->
              message.command.equals("PRIVMSG")
                  && message.last().equals("Xin chào từ USTH Connect test"));
      pass("UTF-8 channel PRIVMSG delivered to second client");
      secondPeer.send(IrcCommands.message(firstNickname, "Tin riêng thử nghiệm"));
      firstPeer.await(
          message ->
              message.command.equals("PRIVMSG") && message.last().equals("Tin riêng thử nghiệm"));
      pass("private PRIVMSG delivered");
      String newNick = "uc" + suffix;
      secondPeer.send(IrcCommands.nick(newNick));
      firstPeer.await(message -> message.command.equals("NICK") && message.last().equals(newNick));
      pass("NICK broadcast");
      secondPeer.send(IrcCommands.message("absent" + suffix, "not delivered"));
      secondPeer.await(message -> message.command.equals("401"));
      pass("401 for missing recipient");
      secondPeer.send(IrcCommands.part(room));
      firstPeer.await(message -> message.command.equals("PART") && message.nick().equals(newNick));
      pass("PART broadcast");
      secondPeer.send(IrcCommands.join(room, ""));
      secondPeer.await(message -> message.command.equals("JOIN") && message.nick().equals(newNick));
      firstPeer.await(message -> message.command.equals("JOIN") && message.nick().equals(newNick));
      secondPeer.client.disconnect();
      firstPeer.await(message -> message.command.equals("QUIT") && message.nick().equals(newNick));
      pass("QUIT broadcast; no implicit resend");
    }
  }
}
