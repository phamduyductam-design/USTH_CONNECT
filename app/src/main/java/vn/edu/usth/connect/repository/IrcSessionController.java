package vn.edu.usth.connect.repository;

import android.os.Handler;
import android.os.Looper;
import java.util.*;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;
import vn.edu.usth.connect.storage.SettingsStore;

/** Application owns this object. Never holds an Activity, Fragment, View or lifecycle owner. */
public final class IrcSessionController implements IrcClient.Listener {
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

  interface Scheduler {
    void post(Runnable callback);

    void postDelayed(Runnable callback, long delay);

    void removeCallbacks(Runnable callback);
  }

  private static final class MainScheduler implements Scheduler {
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void post(Runnable callback) {
      handler.post(callback);
    }

    public void postDelayed(Runnable callback, long delay) {
      handler.postDelayed(callback, delay);
    }

    public void removeCallbacks(Runnable callback) {
      handler.removeCallbacks(callback);
    }
  }

  private final Scheduler mainHandler;
  private final Set<Listener> listeners = new LinkedHashSet<>();
  private final Map<String, String> channelKeys = new HashMap<>();
  private final SettingsStore settings;
  private final IrcTransport client;
  private final Map<String, Runnable> joinTimers = new HashMap<>(), namesTimers = new HashMap<>();
  private Runnable listTimer;
  private boolean foreground = true;
  public ConnectionConfig config;
  public String reason = "";
  public IncomingListener incomingListener;

  public IrcSessionController(SettingsStore settings) {
    this(settings, new MainScheduler(), IrcClient::new);
  }

  IrcSessionController(
      SettingsStore settings,
      Scheduler scheduler,
      java.util.function.Function<IrcClient.Listener, IrcTransport> factory) {
    this.settings = settings;
    mainHandler = scheduler;
    client = factory.apply(this);
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
    cancelRequests();
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
          if (state != ConnectionState.CONNECTED) {
            cancelRequests();
            data.connectionLost();
          } else {
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
          if (message.command.equals("NICK") && config != null) {
            String oldTarget = message.nick(), newTarget = message.last();
            if (!IrcParser.fold(oldTarget).equals(IrcParser.fold(newTarget))) {
              String sourceDraft = draft(oldTarget), destinationDraft = draft(newTarget);
              boolean sourceVisible =
                  IrcParser.fold(data.activeTarget).equals(IrcParser.fold(oldTarget));
              saveDraft(oldTarget, "");
              if (!sourceDraft.isEmpty() && (sourceVisible || destinationDraft.isEmpty()))
                saveDraft(newTarget, sourceDraft);
            }
          }
          ChatMessage received = data.accept(message);
          boolean namesRejected =
              message.command.equals("421")
                  || message.command.equals("461")
                  || (message.command.equals("263")
                      && namesTimers.size() == 1
                      && data.channels.values().stream().filter(c -> c.namesInProgress).count()
                          == 1);
          if (namesRejected && message.param(1).equalsIgnoreCase("NAMES"))
            for (String key : namesTimers.keySet()) data.channels.get(key).cancelNames(true);
          cancelFinishedRequests();
          if (message.command.equals("JOIN")
              && IrcParser.fold(message.nick()).equals(IrcParser.fold(data.nickname))) {
            startNamesTimer(message.param(0));
            refreshWho(message.param(0));
            mode(message.param(0), "", "");
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
    String channelKey = IrcParser.fold(name);
    Runnable timer =
        new Runnable() {
          public void run() {
            if (joinTimers.get(channelKey) != this) return;
            joinTimers.remove(channelKey);
            if (epoch == client.epoch() && c.joining && !c.joined) {
              c.joining = false;
              c.wanted = false;
              data.errorCode = "join_timeout";
              data.errorTarget = name;
              notifyListeners();
            }
          }
        };
    joinTimers.put(channelKey, timer);
    mainHandler.postDelayed(timer, JOIN_TIMEOUT_MS);
    client.send(
        line,
        (written, uncertain) ->
            mainHandler.post(
                () -> {
                  if (epoch != client.epoch() || joinTimers.get(channelKey) != timer) return;
                  if (!written) {
                    cancelTimer(joinTimers.remove(channelKey));
                    c.joining = false;
                    data.errorCode = "network";
                    notifyListeners();
                  }
                }));
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
    cancelTimer(listTimer);
    listTimer =
        new Runnable() {
          public void run() {
            if (listTimer != this) return;
            listTimer = null;
            if (epoch == client.epoch() && data.listing) {
              data.listing = false;
              data.errorCode = "timeout";
              data.errorTarget = "LIST";
              notifyListeners();
            }
          }
        };
    mainHandler.postDelayed(listTimer, LIST_TIMEOUT_MS);
    notifyListeners();
  }

  public void refreshNames(String target) {
    String line = IrcCommands.names(target);
    if (!canSend(target)) throw new IllegalStateException("offline");
    Channel c = data.channel(target);
    if (c.namesInProgress) return;
    c.beginNames();
    startNamesTimer(target);
    sendCommand(line);
    refreshWho(target);
  }

  private void startNamesTimer(String target) {
    Channel c = data.channel(target);
    long epoch = client.epoch();
    String key = IrcParser.fold(target);
    cancelTimer(namesTimers.remove(key));
    Runnable timer =
        new Runnable() {
          public void run() {
            if (namesTimers.get(key) != this) return;
            namesTimers.remove(key);
            if (epoch == client.epoch() && c.namesInProgress) {
              c.cancelNames(false);
              data.errorCode = "timeout";
              data.errorTarget = target;
              notifyListeners();
            }
          }
        };
    namesTimers.put(key, timer);
    mainHandler.postDelayed(timer, NAMES_TIMEOUT_MS);
  }

  private void cancelTimer(Runnable timer) {
    if (timer != null) mainHandler.removeCallbacks(timer);
  }

  private void cancelFinishedRequests() {
    if (!data.listing) {
      cancelTimer(listTimer);
      listTimer = null;
    }
    for (String key : new ArrayList<>(joinTimers.keySet()))
      if (!data.channel(key).joining) cancelTimer(joinTimers.remove(key));
    for (String key : new ArrayList<>(namesTimers.keySet()))
      if (!data.channel(key).namesInProgress) cancelTimer(namesTimers.remove(key));
  }

  private void cancelRequests() {
    cancelTimer(listTimer);
    listTimer = null;
    for (Runnable timer : joinTimers.values()) cancelTimer(timer);
    for (Runnable timer : namesTimers.values()) cancelTimer(timer);
    joinTimers.clear();
    namesTimers.clear();
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
    if (!foreground) return;
    data.activeTarget = target;
    if (!target.isEmpty()) data.markRead(target);
    notifyListeners();
  }

  public void clearActive(String target) {
    if (IrcParser.fold(data.activeTarget).equals(IrcParser.fold(target))) data.activeTarget = "";
  }

  public void setForeground(boolean visible) {
    foreground = visible;
    if (!visible) data.activeTarget = "";
  }

  public void reconnect() {
    data.errorCode = "";
    client.reconnectNow();
  }

  public void disconnect() {
    cancelRequests();
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
