package vn.edu.usth.connect.repository;

import android.os.Handler;
import android.os.Looper;
import java.util.*;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.storage.SettingsStore;

/** Application owns this object. Never holds an Activity, Fragment, View or lifecycle owner. */
public final class SessionRepository implements IrcClient.Listener {
  private static final long JOIN_TIMEOUT_MS = 15000;
  private static final long NAMES_TIMEOUT_MS = 15000;
  private static final long LIST_TIMEOUT_MS = 30000;

  public interface Listener {
    void changed();
  }

  public interface IncomingListener {
    void incoming(String target, ChatMessage message);
  }

  public final SessionState data = new SessionState();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
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

  private void notifyListeners() {
    for (Listener listener : new ArrayList<>(listeners)) listener.changed();
  }

  public void connect(ConnectionConfig newConfig) {
    if (config == null
        || !config.serverId().equals(newConfig.serverId())
        || !IrcParser.fold(config.nickname).equals(IrcParser.fold(newConfig.nickname))) {
      data.availableChannels.clear();
      data.awayUsers.clear();
      data.onlineUsers.clear();
      data.listing = false;
      data.listLoaded = false;
      data.requestedAway = "";
      data.prefixModes = "qaohv";
      data.prefixSymbols = "~&@%+";
      data.channelModes = "beI,k,l,imnpst";
      data.channels.clear();
      data.conversations.clear();
      channelKeys.clear();
      data.activeTarget = "";
    }
    config = newConfig;
    settings.save(newConfig);
    data.nickname = newConfig.nickname;
    data.errorCode = "";
    client.connect(newConfig);
  }

  @Override
  public void onState(long epoch, ConnectionState state, String reason) {
    mainHandler.post(
        () -> {
          if (epoch != client.epoch()) return;
          data.connection = state;
          this.reason = reason;
          if (state != ConnectionState.CONNECTED) data.connectionLost();
          else {
            config = config.withNick(data.nickname);
            settings.save(config);
            if (!data.requestedAway.isEmpty()) setAway(data.requestedAway);
            for (Channel c : data.channels.values())
              if (c.wanted && !c.joined)
                join(c.target, channelKeys.getOrDefault(IrcParser.fold(c.target), ""));
          }
          notifyListeners();
        });
  }

  @Override
  public void onMessage(long epoch, IrcMessage message) {
    mainHandler.post(
        () -> {
          if (epoch != client.epoch()) return;
          ChatMessage received = data.accept(message);
          if (message.command.equals("JOIN")
              && IrcParser.fold(message.nick()).equals(IrcParser.fold(data.nickname))) {
            refreshWho(message.last());
            mode(message.last(), "", "");
          }
          if (message.command.equals("NICK") && config != null) {
            config = config.withNick(data.nickname);
            settings.save(config);
          }
          if (received != null && !received.outgoing && incomingListener != null)
            incomingListener.incoming(
                IrcParser.isChannel(message.param(0)) ? message.param(0) : message.nick(),
                received);
          notifyListeners();
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
            mainHandler.post(
                () -> {
                  if (epoch != client.epoch()) return;
                  if (!written) {
                    c.joining = false;
                    data.errorCode = "network";
                    notifyListeners();
                  }
                }));
    // A server that silently drops JOIN must not leave the button pending forever.
    mainHandler.postDelayed(
        () -> {
          if (epoch == client.epoch() && c.joining && !c.joined) {
            c.joining = false;
            c.wanted = false;
            data.errorCode = "join_timeout";
            data.errorTarget = name;
            notifyListeners();
          }
        },
        JOIN_TIMEOUT_MS);
    notifyListeners();
  }

  public void part(String target) {
    Channel c = data.channel(target);
    c.wanted = false;
    channelKeys.remove(IrcParser.fold(target));
    if (data.connection == ConnectionState.CONNECTED && c.joined)
      client.send(IrcCommands.part(target), null);
    notifyListeners();
  }

  public void changeNick(String nick) {
    if (data.connection != ConnectionState.CONNECTED) throw new IllegalStateException("offline");
    data.errorCode = "";
    client.send(IrcCommands.nick(nick), null);
  }

  private void sendCommand(String line) {
    if (data.connection != ConnectionState.CONNECTED) throw new IllegalStateException("offline");
    data.errorCode = "";
    data.errorDetail = "";
    long epoch = client.epoch();
    client.send(
        line,
        (written, uncertain) ->
            mainHandler.post(
                () -> {
                  if (epoch == client.epoch() && !written) {
                    data.errorCode = "network";
                    notifyListeners();
                  }
                }));
    notifyListeners();
  }

  public void listChannels() {
    if (data.listing) return;
    sendCommand(IrcCommands.list());
    data.availableChannels.clear();
    data.listing = true;
    data.listLoaded = false;
    long epoch = client.epoch();
    mainHandler.postDelayed(
        () -> {
          if (epoch == client.epoch() && data.listing) {
            data.listing = false;
            data.errorCode = "timeout";
            data.errorTarget = "LIST";
            notifyListeners();
          }
        },
        LIST_TIMEOUT_MS);
    notifyListeners();
  }

  public void refreshNames(String target) {
    String line = IrcCommands.names(target);
    if (!canSend(target)) throw new IllegalStateException("offline");
    Channel c = data.channel(target);
    if (c.namesInProgress) return;
    c.namesInProgress = true;
    c.namesBatch.clear();
    c.namesRemoved.clear();
    sendCommand(line);
    refreshWho(target);
    long epoch = client.epoch();
    mainHandler.postDelayed(
        () -> {
          if (epoch == client.epoch() && c.namesInProgress) {
            c.namesInProgress = false;
            c.namesBatch.clear();
            c.namesRemoved.clear();
            data.errorCode = "timeout";
            data.errorTarget = target;
            notifyListeners();
          }
        },
        NAMES_TIMEOUT_MS);
  }

  public void refreshWho(String target) {
    String line = IrcCommands.who(target);
    if (!IrcParser.isChannel(target)) {
      data.onlineUsers.remove(IrcParser.fold(target));
      data.awayUsers.remove(IrcParser.fold(target));
    }
    sendCommand(line);
  }

  public void topic(String target, String text) {
    sendCommand(IrcCommands.topic(target, text));
  }

  public void mode(String target, String flags, String arguments) {
    sendCommand(IrcCommands.mode(target, flags, arguments));
  }

  public void kick(String target, String nick, String reason) {
    sendCommand(IrcCommands.kick(target, nick, reason));
  }

  public void setAway(String reason) {
    String line = IrcCommands.away(reason);
    if (data.connection != ConnectionState.CONNECTED) throw new IllegalStateException("offline");
    data.requestedAway = reason == null ? "" : reason;
    sendCommand(line);
  }

  public void execute(SlashCommand command) {
    switch (command.name) {
      case "join":
        join(command.target, command.extra);
        break;
      case "nick":
        changeNick(command.target);
        break;
      case "msg":
        send(command.target, command.text);
        break;
      case "part":
        part(command.target);
        break;
      case "names":
        refreshNames(command.target);
        break;
      case "who":
        refreshWho(command.target);
        break;
      case "list":
        listChannels();
        break;
      case "topic":
        topic(command.target, command.text);
        break;
      case "mode":
        mode(command.target, command.text, command.extra);
        break;
      case "kick":
        kick(command.target, command.text, command.extra);
        break;
      case "away":
        setAway(command.text);
        break;
      default:
        throw new IllegalArgumentException("command");
    }
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
            mainHandler.post(
                () -> {
                  if (epoch != client.epoch()) return;
                  // A rejection can arrive before the write callback is processed.
                  if (message.status == ChatMessage.Status.QUEUED)
                    message.status =
                        written
                            ? ChatMessage.Status.WRITTEN
                            : uncertain ? ChatMessage.Status.UNCERTAIN : ChatMessage.Status.FAILED;
                  notifyListeners();
                }));
    notifyListeners();
  }

  public void setActive(String target) {
    data.activeTarget = target;
    if (!target.isEmpty()) data.markRead(target);
    notifyListeners();
  }

  public void clearActive(String target) {
    if (IrcParser.fold(data.activeTarget).equals(IrcParser.fold(target))) data.activeTarget = "";
  }

  public void reconnect() {
    data.errorCode = "";
    client.reconnectNow();
  }

  public void disconnect() {
    data.requestedAway = "";
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
