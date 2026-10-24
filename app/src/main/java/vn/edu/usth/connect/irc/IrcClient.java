package vn.edu.usth.connect.irc;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.model.ConnectionState;

/** One process-owned connection. Reader never occupies the writer executor. */
public final class IrcClient implements IrcTransport {
  public interface Listener {
    void onState(long epoch, ConnectionState state, String reason);

    void onMessage(long epoch, IrcMessage message);
  }

  public interface SendCallback {
    void complete(boolean written, boolean uncertain);
  }

  private static final class Attempt {
    final long epoch;
    final AtomicBoolean failed = new AtomicBoolean();
    volatile Socket socket;
    volatile BufferedOutputStream output;
    volatile boolean accepted;
    boolean negotiating = true;
    final Set<String> offered = new LinkedHashSet<>();
    volatile ScheduledFuture<?> deadline, heartbeat;

    Attempt(long epoch) {
      this.epoch = epoch;
    }
  }

  private final Listener listener;
  private final ExecutorService readers =
      Executors.newCachedThreadPool(r -> new Thread(r, "irc-reader"));
  private final ExecutorService writer =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "irc-writer"));
  private final ScheduledExecutorService timer =
      Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "irc-timer"));
  private final AtomicLong generation = new AtomicLong();
  private volatile Attempt active;
  private volatile ConnectionConfig config;
  private volatile boolean stopped = true, hasConnected;
  private int retryCount;
  private ScheduledFuture<?> retry;

  public IrcClient(Listener listener) {
    this.listener = listener;
  }

  public long epoch() {
    return generation.get();
  }

  public synchronized void connect(ConnectionConfig config) {
    stopInternal();
    this.config = config;
    stopped = false;
    hasConnected = false;
    retryCount = 0;
    startAttempt(false);
  }

  private synchronized void startAttempt(boolean reconnect) {
    if (stopped) return;
    Attempt a = new Attempt(generation.incrementAndGet());
    active = a;
    state(a, reconnect ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING, "");
    // This deadline also covers DNS/connect/TLS/registration; closing a socket cancels blocked I/O.
    a.deadline = timer.schedule(() -> fail(a, "timeout", false), 30, TimeUnit.SECONDS);
    readers.execute(() -> readSession(a));
  }

  private void readSession(Attempt a) {
    try {
      ConnectionConfig c = config;
      Socket raw = new Socket();
      a.socket = raw;
      if (!current(a)) {
        close(a);
        return;
      }
      raw.connect(new InetSocketAddress(c.host, c.port), 15000);
      raw.setSoTimeout(180000);
      raw.setTcpNoDelay(true);
      Socket transport = raw;
      if (c.tls) {
        SSLSocket tls =
            (SSLSocket)
                ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(raw, c.host, c.port, true);
        a.socket = tls;
        SSLParameters parameters = tls.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        tls.setSSLParameters(
            parameters); // Default trust store + hostname verification, never trust-all.
        tls.startHandshake();
        transport = tls;
      }
      if (!current(a)) {
        close(a);
        return;
      }
      a.output = new BufferedOutputStream(transport.getOutputStream());
      state(a, ConnectionState.REGISTERING, "");
      write(a, "CAP LS 302", null, false);
      write(a, IrcCommands.nick(c.nickname), null, false);
      write(a, IrcCommands.user(c.username, c.realName), null, false);
      BufferedInputStream input = new BufferedInputStream(transport.getInputStream());
      while (current(a)) {
        String line = readLine(input);
        if (line == null) throw new EOFException();
        if (!current(a)) return; // A blocked read may finish after the user cancelled this epoch.
        IrcMessage message;
        try {
          message = IrcParser.parse(line);
        } catch (IllegalArgumentException invalid) {
          continue;
        }
        if (message.command.equals("CAP") && a.negotiating) {
          negotiate(a, message);
          continue;
        } else if (message.command.equals("421") && message.param(1).equalsIgnoreCase("CAP")) {
          // Legacy servers can register normally without IRCv3 capabilities.
          a.negotiating = false;
          continue;
        } else if (message.command.equals("PING")) {
          write(a, IrcCommands.checked("PONG :" + message.last()), null, false);
        } else if (message.command.equals("001")) {
          acceptRegistration(a, message);
          continue;
        } else if (message.command.equals("NICK")) {
          synchronized (this) {
            if (current(a)
                && IrcParser.fold(message.nick()).equals(IrcParser.fold(config.nickname)))
              config = config.withNick(message.last());
          }
        } else if (!a.accepted
            && (message.command.equals("433")
                || message.command.equals("432")
                || message.command.equals("464")
                || message.command.equals("465"))) {
          listener.onMessage(a.epoch, message);
          fail(a, message.command, true);
          return;
        } else if (message.command.equals("ERROR")) {
          fail(a, "server", !a.accepted);
          return;
        }
        if (current(a)) listener.onMessage(a.epoch, message);
      }
    } catch (SSLException e) {
      fail(a, "tls", true);
    } catch (Exception e) {
      fail(a, e instanceof SocketTimeoutException ? "timeout" : "network", false);
    } finally {
      if (!current(a)) close(a);
    }
  }

  private synchronized void acceptRegistration(Attempt a, IrcMessage message) {
    if (!current(a) || a.accepted) return;
    a.accepted = true;
    a.negotiating = false;
    hasConnected = true;
    retryCount = 0;
    config = config.withNick(message.param(0));
    if (a.deadline != null) a.deadline.cancel(false);
    a.heartbeat =
        timer.scheduleWithFixedDelay(
            () -> write(a, "PING :usth-connect", null, false), 60, 60, TimeUnit.SECONDS);
    // The short lock protects configuration/timer ownership, never a blocking socket read.
    listener.onMessage(a.epoch, message);
    state(a, ConnectionState.CONNECTED, "");
  }

  private void negotiate(Attempt a, IrcMessage message) {
    String subcommand = message.param(1);
    if (subcommand.equals("LS")) {
      for (String cap : message.last().split(" +")) a.offered.add(cap.split("=", 2)[0]);
      if (message.param(2).equals("*")) return;
      List<String> requested = new ArrayList<>();
      for (String cap : new String[] {"multi-prefix", "away-notify"})
        if (a.offered.contains(cap)) requested.add(cap);
      if (!requested.isEmpty()) write(a, "CAP REQ :" + String.join(" ", requested), null, false);
      else finishNegotiation(a);
    } else if (subcommand.equals("ACK") || subcommand.equals("NAK")) finishNegotiation(a);
  }

  private void finishNegotiation(Attempt a) {
    a.negotiating = false;
    write(a, "CAP END", null, false);
  }

  static String readLine(InputStream input) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    boolean cr = false;
    while (true) {
      int c = input.read();
      if (c == -1) {
        if (bytes.size() == 0 && !cr) return null;
        throw new EOFException("partial line");
      }
      if (cr) {
        if (c == '\n') return bytes.toString(StandardCharsets.UTF_8.name());
        throw new IOException("CRLF");
      }
      if (c == '\r') cr = true;
      else if (c == '\n' || c == 0 || bytes.size() >= 8192) throw new IOException("framing");
      else bytes.write(c);
    }
  }

  public void send(String line, SendCallback callback) {
    IrcCommands.checked(line);
    Attempt a = active;
    if (a == null || !a.accepted || !current(a)) {
      if (callback != null) callback.complete(false, false);
      return;
    }
    write(a, line, callback, true);
  }

  private void write(Attempt a, String line, SendCallback callback, boolean requireAccepted) {
    writer.execute(
        () -> {
          if (!current(a) || a.output == null || (requireAccepted && !a.accepted)) {
            if (callback != null) callback.complete(false, false);
            return;
          }
          try {
            a.output.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
            a.output.flush();
            if (callback != null) callback.complete(true, false);
          } catch (IOException e) {
            // Partial writes cannot safely be retried automatically.
            if (callback != null) callback.complete(false, true);
            fail(a, "network", false);
          }
        });
  }

  private synchronized void fail(Attempt a, String reason, boolean terminal) {
    if (!current(a) || !a.failed.compareAndSet(false, true)) return;
    close(a);
    if (!terminal && hasConnected && !stopped && retryCount < 6) {
      int seconds = 1 << Math.min(retryCount++, 5);
      state(a, ConnectionState.RECONNECTING, "retry:" + seconds);
      retry = timer.schedule(() -> startAttempt(true), seconds, TimeUnit.SECONDS);
    } else {
      stopped = true;
      state(a, ConnectionState.FAILED, reason);
    }
  }

  public synchronized void reconnectNow() {
    if (config == null) return;
    stopInternal();
    stopped = false;
    retryCount = 0;
    startAttempt(true);
  }

  public synchronized void disconnect() {
    Attempt old = active;
    stopInternal(false);
    long epoch = generation.get();
    listener.onState(epoch, ConnectionState.DISCONNECTED, "");
    if (old == null) return;
    // Try QUIT in the writer's order; always close within one second even if writer is blocked.
    timer.schedule(() -> close(old), 1, TimeUnit.SECONDS);
    writer.execute(
        () -> {
          try {
            if (old.output != null) {
              old.output.write("QUIT :USTH Connect\r\n".getBytes(StandardCharsets.UTF_8));
              old.output.flush();
            }
          } catch (IOException ignored) {
          } finally {
            close(old);
          }
        });
  }

  private void stopInternal() {
    stopInternal(true);
  }

  private void stopInternal(boolean closeSocket) {
    stopped = true;
    generation.incrementAndGet();
    if (retry != null) {
      retry.cancel(false);
      retry = null;
    }
    if (active != null) {
      cancelTimers(active);
      if (closeSocket) close(active);
    }
    active = null;
  }

  private boolean current(Attempt a) {
    return a != null && !stopped && a == active && a.epoch == generation.get() && !a.failed.get();
  }

  private void state(Attempt a, ConnectionState state, String reason) {
    if (a == active && a.epoch == generation.get()) listener.onState(a.epoch, state, reason);
  }

  private static void cancelTimers(Attempt a) {
    if (a.deadline != null) a.deadline.cancel(false);
    if (a.heartbeat != null) a.heartbeat.cancel(false);
  }

  private static void close(Attempt a) {
    cancelTimers(a);
    try {
      if (a.socket != null) a.socket.close();
    } catch (IOException ignored) {
    }
  }

  public synchronized void shutdown() {
    stopInternal();
    readers.shutdownNow();
    writer.shutdownNow();
    timer.shutdownNow();
  }
}
