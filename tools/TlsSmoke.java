import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import vn.edu.usth.connect.irc.IrcClient;
import vn.edu.usth.connect.irc.IrcMessage;
import vn.edu.usth.connect.model.ConnectionConfig;
import vn.edu.usth.connect.model.ConnectionState;

public final class TlsSmoke {
  public static void main(String[] args) throws Exception {
    CountDownLatch connectionResult = new CountDownLatch(1);
    AtomicReference<ConnectionState> connectionState = new AtomicReference<>();
    AtomicReference<String> failureReason = new AtomicReference<>("");
    IrcClient client =
        new IrcClient(
            new IrcClient.Listener() {
              public void onState(long epoch, ConnectionState nextState, String detail) {
                if (nextState == ConnectionState.CONNECTED || nextState == ConnectionState.FAILED) {
                  connectionState.set(nextState);
                  failureReason.set(detail);
                  connectionResult.countDown();
                }
              }

              public void onMessage(long epoch, IrcMessage message) {}
            });
    try {
      client.connect(
          new ConnectionConfig(
              args[0], Integer.parseInt(args[1]), true, "tls_smoke", "usth", "TLS smoke"));
      if (!connectionResult.await(15, TimeUnit.SECONDS)) {
        throw new AssertionError("TLS result timed out");
      }
      boolean expectConnectionSuccess = Boolean.parseBoolean(args[2]);
      if (expectConnectionSuccess
          ? connectionState.get() != ConnectionState.CONNECTED
          : connectionState.get() != ConnectionState.FAILED || !failureReason.get().equals("tls")) {
        throw new AssertionError(
            "Unexpected TLS result: " + connectionState.get() + " " + failureReason.get());
      }
    } finally {
      client.shutdown();
    }
  }
}
