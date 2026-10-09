package vn.edu.usth.connect.repository;

import android.os.Handler;
import android.os.Looper;
import java.util.*;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.storage.SettingsStore;

/** Application owns this object. Never holds an Activity, Fragment, View or lifecycle owner. */
public final class SessionRepository implements IrcClient.Listener {
  public interface Listener {
    void changed();
  }

  public interface IncomingListener {
    void incoming(String target, ChatMessage message);
  }

  public final SessionState data = new SessionState();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Set<Listener> listeners = new LinkedHashSet<>();
  private final Map<String, String> channelKeys = new HashMap<>();
  private final SettingsStore settings;
  private final IrcClient client = new IrcClient(this);
  public ConnectionConfig config;
  public String reason = "";
  public IncomingListener incomingListener;

  public SessionRepository(SettingsStore settings) {
    this.settings = settings;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
    listener.changed();
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void publish() {
    for (Listener listener : new ArrayList<>(listeners)) listener.changed();
  }

  public void connect(ConnectionConfig c) {
    if (config == null
        || !config.serverId().equals(c.serverId())
        || !IrcParser.fold(config.nickname).equals(IrcParser.fold(c.nickname))) {
      data.channels.clear();
      data.conversations.clear();
      channelKeys.clear();
      data.activeTarget = "";
    }
    config = c;
    settings.save(c);
    data.nickname = c.nickname;
    data.errorCode = "";
    client.connect(c);
  }

  @Override
  public void onState(long epoch, ConnectionState state, String reason) {
    main.post(
        () -> {
          if (epoch != client.epoch()) return;
          data.connection = state;
          this.reason = reason;
          if (state != ConnectionState.CONNECTED) data.connectionLost();
          else {
            config = config.withNick(data.nickname);
            settings.save(config);
            for (Channel c : data.channels.values())
              if (c.wanted && !c.joined)
                join(c.target, channelKeys.getOrDefault(IrcParser.fold(c.target), ""));
          }
          publish();
        });
  }

  @Override
  public void onMessage(long epoch, IrcMessage message) {
    main.post(
        () -> {
          if (epoch != client.epoch()) return;
          ChatMessage received = data.accept(message);
          if (message.command.equals("NICK") && config != null) {
            config = config.withNick(data.nickname);
            settings.save(config);
          }
          if (received != null && !received.outgoing && incomingListener != null)
            incomingListener.incoming(
                IrcParser.isChannel(message.param(0)) ? message.param(0) : message.nick(),
                received);
          publish();
        });
  }

  public void join(String name, String key) {
    String line = IrcCommands.join(name, key);
    if (data.connection != ConnectionState.CONNECTED) throw new IllegalStateException("offline");
    Channel c = data.channel(name);
    if (c.joined || c.joining) return;
    c.joining = true;
    c.wanted = true;
    data.errorCode = "";
    channelKeys.put(IrcParser.fold(name), key == null ? "" : key);
    long epoch = client.epoch();
    client.send(
        line,
        (written, uncertain) ->
            main.post(
                () -> {
                  if (epoch != client.epoch()) return;
                  if (!written) {
                    c.joining = false;
                    data.errorCode = "network";
                    publish();
                  }
                }));
    // A server that silently drops JOIN must not leave the button pending forever.
    main.postDelayed(
        () -> {
          if (epoch == client.epoch() && c.joining && !c.joined) {
            c.joining = false;
            c.wanted = false;
            data.errorCode = "join_timeout";
            data.errorTarget = name;
            publish();
          }
        },
        15000);
    publish();
  }

  public void part(String target) {
    Channel c = data.channel(target);
    c.wanted = false;
    channelKeys.remove(IrcParser.fold(target));
    if (data.connection == ConnectionState.CONNECTED && c.joined)
      client.send(IrcCommands.part(target), null);
    publish();
  }

  public void changeNick(String nick) {
    if (data.connection != ConnectionState.CONNECTED) throw new IllegalStateException("offline");
    data.errorCode = "";
    client.send(IrcCommands.nick(nick), null);
  }

  public boolean canSend(String target) {
    return data.connection == ConnectionState.CONNECTED
        && (!IrcParser.isChannel(target) || data.channel(target).joined);
  }

  public ChatMessage send(String target, String text) {
    String line = IrcCommands.message(target, text);
    if (!canSend(target)) throw new IllegalStateException("offline");
    ChatMessage message = new ChatMessage(data.nickname, text, true, ChatMessage.Status.QUEUED);
    data.conversation(target).add(message);
    sendExisting(target, message, line);
    return message;
  }

  public void retry(String target, ChatMessage message) {
    if (!canSend(target)
        || (message.status != ChatMessage.Status.FAILED
            && message.status != ChatMessage.Status.UNCERTAIN)) return;
    message.status = ChatMessage.Status.QUEUED;
    sendExisting(target, message, IrcCommands.message(target, message.text));
  }

  private void sendExisting(String target, ChatMessage message, String line) {
    long epoch = client.epoch();
    client.send(
        line,
        (written, uncertain) ->
            main.post(
                () -> {
                  if (epoch != client.epoch()) return;
                  // A rejection can arrive before the write callback is processed.
                  if (message.status == ChatMessage.Status.QUEUED)
                    message.status =
                        written
                            ? ChatMessage.Status.WRITTEN
                            : uncertain ? ChatMessage.Status.UNCERTAIN : ChatMessage.Status.FAILED;
                  publish();
                }));
    publish();
  }

  public void setActive(String target) {
    data.activeTarget = target;
    if (!target.isEmpty()) data.markRead(target);
    publish();
  }

  public void clearActive(String target) {
    if (IrcParser.fold(data.activeTarget).equals(IrcParser.fold(target))) data.activeTarget = "";
  }

  public void reconnect() {
    data.errorCode = "";
    client.reconnectNow();
  }

  public void disconnect() {
    for (Channel c : data.channels.values()) c.wanted = false;
    channelKeys.clear();
    client.disconnect();
  }

  public String draft(String target) {
    return config == null ? "" : settings.draft(config.serverId(), target);
  }

  public void saveDraft(String target, String text) {
    if (config != null) settings.draft(config.serverId(), target, text);
  }
}
