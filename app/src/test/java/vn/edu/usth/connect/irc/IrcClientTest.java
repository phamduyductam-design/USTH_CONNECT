package vn.edu.usth.connect.irc;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.Test;
import vn.edu.usth.connect.model.*;

public class IrcClientTest {
  @Test
  public void reconnectRegistersAgainWithoutReplayingSentMessage() throws Exception {
    CountDownLatch first = new CountDownLatch(1), second = new CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger accepts =
        new java.util.concurrent.atomic.AtomicInteger();
    try (ServerSocket server = new ServerSocket(0)) {
      server.setSoTimeout(5000);
      ExecutorService fixture = Executors.newSingleThreadExecutor();
      Future<Boolean> replayed =
          fixture.submit(
              () -> {
                try (Socket initial = server.accept()) {
                  initial.setSoTimeout(5000);
                  BufferedReader in =
                      new BufferedReader(
                          new InputStreamReader(initial.getInputStream(), StandardCharsets.UTF_8));
                  in.readLine();
                  in.readLine();
                  initial
                      .getOutputStream()
                      .write(":server 001 minh :Welcome\r\n".getBytes(StandardCharsets.UTF_8));
                  initial.getOutputStream().flush();
                  if (!"PRIVMSG #c :once".equals(in.readLine()))
                    throw new IOException("missing initial message");
                }
                try (Socket next = server.accept()) {
                  next.setSoTimeout(5000);
                  BufferedReader in =
                      new BufferedReader(
                          new InputStreamReader(next.getInputStream(), StandardCharsets.UTF_8));
                  if (!"NICK minh".equals(in.readLine())) throw new IOException("missing NICK");
                  if (!in.readLine().startsWith("USER ")) throw new IOException("missing USER");
                  next.getOutputStream()
                      .write(":server 001 minh :Welcome back\r\n".getBytes(StandardCharsets.UTF_8));
                  next.getOutputStream().flush();
                  next.setSoTimeout(400);
                  try {
                    return in.readLine() != null;
                  } catch (SocketTimeoutException expected) {
                    return false;
                  }
                }
              });
      IrcClient client =
          new IrcClient(
              new IrcClient.Listener() {
                public void onState(long e, ConnectionState s, String reason) {
                  if (s == ConnectionState.CONNECTED) {
                    if (accepts.incrementAndGet() == 1) first.countDown();
                    else second.countDown();
                  }
                }

                public void onMessage(long e, IrcMessage m) {}
              });
      try {
        client.connect(
            new ConnectionConfig("127.0.0.1", server.getLocalPort(), false, "minh", "u", "test"));
        assertTrue(first.await(3, TimeUnit.SECONDS));
        client.send(IrcCommands.message("#c", "once"), null);
        assertTrue(second.await(5, TimeUnit.SECONDS));
        assertFalse(replayed.get(3, TimeUnit.SECONDS));
        assertEquals(2, accepts.get());
      } finally {
        client.shutdown();
        fixture.shutdownNow();
      }
    }
  }

  @Test
  public void connectedOnlyAfter001AndBlockingReaderDoesNotBlockWriter() throws Exception {
    CountDownLatch registered = new CountDownLatch(1),
        accepted = new CountDownLatch(1),
        written = new CountDownLatch(1);
    BlockingQueue<String> sent = new LinkedBlockingQueue<>();
    try (ServerSocket server = new ServerSocket(0)) {
      ExecutorService fixture = Executors.newSingleThreadExecutor();
      Future<?> task =
          fixture.submit(
              () -> {
                try (Socket socket = server.accept()) {
                  socket.setSoTimeout(5000);
                  BufferedReader in =
                      new BufferedReader(
                          new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                  PrintWriter out =
                      new PrintWriter(
                          new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                  sent.add(in.readLine());
                  sent.add(in.readLine());
                  registered.countDown();
                  Thread.sleep(150);
                  out.print(":server 001 minh :Welcome\r\nPING :probe\r\n");
                  out.flush();
                  sent.add(in.readLine());
                  sent.add(in.readLine());
                  while (in.readLine() != null) {}
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
      IrcClient client =
          new IrcClient(
              new IrcClient.Listener() {
                public void onState(long epoch, ConnectionState state, String reason) {
                  if (state == ConnectionState.CONNECTED) accepted.countDown();
                }

                public void onMessage(long epoch, IrcMessage message) {}
              });
      try {
        client.connect(
            new ConnectionConfig(
                "127.0.0.1", server.getLocalPort(), false, "minh", "usth", "test"));
        assertTrue(registered.await(3, TimeUnit.SECONDS));
        assertEquals(1, accepted.getCount());
        assertTrue(accepted.await(3, TimeUnit.SECONDS));
        client.send(
            IrcCommands.message("#c", "xin chào"),
            (ok, uncertain) -> {
              if (ok) written.countDown();
            });
        assertTrue(written.await(3, TimeUnit.SECONDS));
        assertEquals("NICK minh", sent.poll(3, TimeUnit.SECONDS));
        assertEquals("USER usth 0 * :test", sent.poll(3, TimeUnit.SECONDS));
        String a = sent.poll(3, TimeUnit.SECONDS), b = sent.poll(3, TimeUnit.SECONDS);
        assertTrue(a.equals("PONG :probe") || b.equals("PONG :probe"));
        assertTrue(a.equals("PRIVMSG #c :xin chào") || b.equals("PRIVMSG #c :xin chào"));
        client.disconnect();
        task.get(4, TimeUnit.SECONDS);
      } finally {
        client.shutdown();
        fixture.shutdownNow();
      }
    }
  }

  @Test
  public void nicknameRejectionStopsInitialSession() throws Exception {
    CountDownLatch failed = new CountDownLatch(1);
    java.util.concurrent.atomic.AtomicBoolean connected =
        new java.util.concurrent.atomic.AtomicBoolean();
    try (ServerSocket server = new ServerSocket(0)) {
      ExecutorService fixture = Executors.newSingleThreadExecutor();
      Future<?> task =
          fixture.submit(
              () -> {
                try (Socket socket = server.accept()) {
                  BufferedReader input =
                      new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  input.readLine();
                  input.readLine();
                  socket
                      .getOutputStream()
                      .write(":server 433 * minh :in use\r\n".getBytes(StandardCharsets.UTF_8));
                  socket.getOutputStream().flush();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
      IrcClient client =
          new IrcClient(
              new IrcClient.Listener() {
                public void onState(long e, ConnectionState s, String r) {
                  if (s == ConnectionState.CONNECTED) connected.set(true);
                  if (s == ConnectionState.FAILED && r.equals("433")) failed.countDown();
                }

                public void onMessage(long e, IrcMessage m) {}
              });
      try {
        client.connect(
            new ConnectionConfig("127.0.0.1", server.getLocalPort(), false, "minh", "u", "test"));
        assertTrue(failed.await(3, TimeUnit.SECONDS));
        assertFalse(connected.get());
        task.get(3, TimeUnit.SECONDS);
      } finally {
        client.shutdown();
        fixture.shutdownNow();
      }
    }
  }

  @Test
  public void intentionalDisconnectCancelsScheduledReconnect() throws Exception {
    CountDownLatch reconnect = new CountDownLatch(1);
    CountDownLatch receivedQuit = new CountDownLatch(1);
    try (ServerSocket server = new ServerSocket(0)) {
      server.setSoTimeout(1800);
      ExecutorService fixture = Executors.newSingleThreadExecutor();
      Future<Boolean> extra =
          fixture.submit(
              () -> {
                try (Socket socket = server.accept()) {
                  BufferedReader in =
                      new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  in.readLine();
                  in.readLine();
                  socket
                      .getOutputStream()
                      .write(":s 001 minh :ok\r\n".getBytes(StandardCharsets.UTF_8));
                  socket.getOutputStream().flush();
                }
                try (Socket ignored = server.accept()) {
                  return true;
                } catch (SocketTimeoutException expected) {
                  return false;
                }
              });
      IrcClient client =
          new IrcClient(
              new IrcClient.Listener() {
                public void onState(long e, ConnectionState s, String r) {
                  if (s == ConnectionState.RECONNECTING && r.startsWith("retry:"))
                    reconnect.countDown();
                  if (s == ConnectionState.DISCONNECTED) receivedQuit.countDown();
                }

                public void onMessage(long e, IrcMessage m) {}
              });
      try {
        client.connect(
            new ConnectionConfig("127.0.0.1", server.getLocalPort(), false, "minh", "u", "test"));
        assertTrue(reconnect.await(3, TimeUnit.SECONDS));
        client.disconnect();
        assertTrue(receivedQuit.await(1, TimeUnit.SECONDS));
        assertFalse(extra.get(4, TimeUnit.SECONDS));
      } finally {
        client.shutdown();
        fixture.shutdownNow();
      }
    }
  }
}
