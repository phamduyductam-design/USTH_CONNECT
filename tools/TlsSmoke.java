import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/** Connect through the production IrcClient; Python supplies a temporary local TLS peer. */
public final class TlsSmoke {
  public static void main(String[] args) throws Exception {
    CountDownLatch result = new CountDownLatch(1);
    AtomicReference<ConnectionState> state = new AtomicReference<>();
    AtomicReference<String> reason = new AtomicReference<>("");
    IrcClient client =
        new IrcClient(
            new IrcClient.Listener() {
              public void onState(long epoch, ConnectionState next, String detail) {
                if (next == ConnectionState.CONNECTED || next == ConnectionState.FAILED) {
                  state.set(next);
                  reason.set(detail);
                  result.countDown();
                }
              }

              public void onMessage(long epoch, IrcMessage message) {}
            });
    try {
      client.connect(
          new ConnectionConfig(
              args[0], Integer.parseInt(args[1]), true, "tls_smoke", "usth", "TLS smoke"));
      if (!result.await(15, TimeUnit.SECONDS)) throw new AssertionError("TLS result timed out");
      boolean expected = Boolean.parseBoolean(args[2]);
      if (expected
          ? state.get() != ConnectionState.CONNECTED
          : state.get() != ConnectionState.FAILED || !reason.get().equals("tls"))
        throw new AssertionError("Unexpected TLS result: " + state.get() + " " + reason.get());
    } finally {
      client.shutdown();
    }
  }
}
