import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/**
 * Opt-in live smoke test. Its two clients communicate only with each other in a unique temporary
 * room.
 */
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

    public void onState(long e, ConnectionState state, String reason) {
      if (state == ConnectionState.CONNECTED) connected.countDown();
      if (state == ConnectionState.FAILED) {
        failure = reason;
        connected.countDown();
      }
    }

    public void onMessage(long e, IrcMessage message) {
      messages.add(message);
    }

    void awaitConnection() throws Exception {
      if (!connected.await(35, TimeUnit.SECONDS) || !failure.isEmpty())
        throw new IllegalStateException("Connection failed: " + failure);
    }

    IrcMessage await(Predicate<IrcMessage> predicate) throws Exception {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
      while (System.nanoTime() < deadline) {
        IrcMessage m = messages.poll(100, TimeUnit.MILLISECONDS);
        if (m != null && predicate.test(m)) return m;
      }
      throw new IllegalStateException("Expected IRC reply not received");
    }

    void send(String line) throws Exception {
      CountDownLatch sent = new CountDownLatch(1);
      java.util.concurrent.atomic.AtomicBoolean ok =
          new java.util.concurrent.atomic.AtomicBoolean();
      client.send(
          line,
          (written, uncertain) -> {
            ok.set(written);
            sent.countDown();
          });
      if (!sent.await(5, TimeUnit.SECONDS) || !ok.get())
        throw new IllegalStateException("write failed");
    }

    public void close() {
      client.disconnect();
      try {
        Thread.sleep(150);
      } catch (InterruptedException e) {
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
    String aNick = "ua" + suffix, bNick = "ub" + suffix, room = "#usth-test-" + suffix;
    try (Peer a = new Peer(host, port, tls, aNick);
        Peer b = new Peer(host, port, tls, bNick)) {
      a.awaitConnection();
      b.awaitConnection();
      pass("two real TCP sessions accepted with 001 (TLS=" + tls + ")");
      a.send(IrcCommands.join(room, ""));
      a.await(m -> m.command.equals("JOIN") && m.nick().equals(aNick));
      b.send(IrcCommands.join(room, ""));
      b.await(m -> m.command.equals("JOIN") && m.nick().equals(bNick));
      a.await(m -> m.command.equals("JOIN") && m.nick().equals(bNick));
      pass("JOIN acknowledgements and peer membership event");
      b.await(m -> m.command.equals("366") && m.param(1).equals(room));
      pass("NAMES completed");
      a.send(IrcCommands.message(room, "Xin chào từ USTH Connect test"));
      b.await(m -> m.command.equals("PRIVMSG") && m.last().equals("Xin chào từ USTH Connect test"));
      pass("UTF-8 channel PRIVMSG delivered to second client");
      b.send(IrcCommands.message(aNick, "Tin riêng thử nghiệm"));
      a.await(m -> m.command.equals("PRIVMSG") && m.last().equals("Tin riêng thử nghiệm"));
      pass("private PRIVMSG delivered");
      String newNick = "uc" + suffix;
      b.send(IrcCommands.nick(newNick));
      a.await(m -> m.command.equals("NICK") && m.last().equals(newNick));
      pass("NICK broadcast");
      b.send(IrcCommands.message("absent" + suffix, "not delivered"));
      b.await(m -> m.command.equals("401"));
      pass("401 for missing recipient");
      b.send(IrcCommands.part(room));
      a.await(m -> m.command.equals("PART") && m.nick().equals(newNick));
      pass("PART broadcast");
      b.send(IrcCommands.join(room, ""));
      b.await(m -> m.command.equals("JOIN") && m.nick().equals(newNick));
      a.await(m -> m.command.equals("JOIN") && m.nick().equals(newNick));
      b.client.disconnect();
      a.await(m -> m.command.equals("QUIT") && m.nick().equals(newNick));
      pass("QUIT broadcast; no implicit resend");
    }
  }
}
