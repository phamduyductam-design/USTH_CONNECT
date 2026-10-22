package vn.edu.usth.connect.repository;

import java.util.*;
import vn.edu.usth.connect.irc.*;
import vn.edu.usth.connect.model.*;

/**
 * Protocol-driven state. All access in Android is confined to the main thread by SessionRepository.
 */
public final class SessionState {
  private static final int MAX_LIST_CHANNELS = 5000;
  private static final int ECHO_LOOKBACK_MESSAGES = 10;
  private static final long ECHO_WINDOW_MS = 10000;
  private static final long ERROR_MESSAGE_WINDOW_MS = 30000;

  public final Map<String, Channel> channels = new LinkedHashMap<>();
  public final Map<String, Conversation> conversations = new LinkedHashMap<>();
  public final Map<String, ServerChannel> availableChannels = new LinkedHashMap<>();
  public final Set<String> onlineUsers = new HashSet<>();
  public final Map<String, String> awayUsers = new HashMap<>();
  public boolean listing, listLoaded, away;
  public String awayMessage = "", requestedAway = "", errorDetail = "";
  public String prefixModes = "qaohv", prefixSymbols = "~&@%+", channelModes = "beI,k,l,imnpst";
  public String nickname = "", activeTarget = "", errorCode = "", errorTarget = "";
  public ConnectionState connection = ConnectionState.DISCONNECTED;

  public Channel channel(String target) {
    return channels.computeIfAbsent(IrcParser.fold(target), k -> new Channel(target));
  }

  public Conversation conversation(String target) {
    return IrcParser.isChannel(target)
        ? channel(target)
        : conversations.computeIfAbsent(IrcParser.fold(target), k -> new Conversation(target));
  }

  public void connectionLost() {
    listing = false;
    listLoaded = false;
    away = false;
    awayMessage = "";
    awayUsers.clear();
    onlineUsers.clear();
    for (Channel channel : channels.values()) {
      channel.joined = false;
      channel.joining = false;
      channel.members.clear();
      channel.modes.clear();
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
      channel.namesInProgress = false;
    }
    for (Conversation conversation : all())
      for (ChatMessage message : conversation.messages)
        if (message.status == ChatMessage.Status.QUEUED)
          message.status = ChatMessage.Status.UNCERTAIN;
  }

  public List<Conversation> all() {
    List<Conversation> all = new ArrayList<>(channels.values());
    all.addAll(conversations.values());
    return all;
  }

  public void markRead(String target) {
    Conversation conversation = conversation(target);
    conversation.unread = 0;
    conversation.firstUnreadId = 0;
  }

  /**
   * Dispatch one server message. Return the accepted PRIVMSG, or null for state-only/ignored
   * messages.
   */
  public ChatMessage accept(IrcMessage message) {
    String senderNickname = message.nick(), senderKey = IrcParser.fold(senderNickname);
    boolean ownMessage = senderKey.equals(IrcParser.fold(nickname));
    switch (message.command) {
      case "001":
        nickname = message.param(0);
        onlineUsers.add(IrcParser.fold(nickname));
        break;
      case "005":
        handleServerSupport(message);
        break;
      case "321":
        availableChannels.clear();
        listing = true;
        break;
      case "322":
        handleListEntry(message);
        break;
      case "323":
        listing = false;
        listLoaded = true;
        break;
      case "MODE":
        if (IrcParser.isChannel(message.param(0)))
          applyModes(
              channel(message.param(0)),
              message.param(1),
              message.parameters.subList(
                  Math.min(2, message.parameters.size()), message.parameters.size()));
        break;
      case "324":
        handleChannelModesReply(message);
        break;
      case "301":
        onlineUsers.add(IrcParser.fold(message.param(1)));
        awayUsers.put(IrcParser.fold(message.param(1)), message.last());
        break;
      case "305":
        away = false;
        awayMessage = "";
        awayUsers.remove(IrcParser.fold(nickname));
        break;
      case "306":
        away = true;
        awayMessage = requestedAway;
        awayUsers.put(IrcParser.fold(nickname), awayMessage);
        break;
      case "AWAY":
        if (message.parameters.isEmpty()) awayUsers.remove(senderKey);
        else awayUsers.put(senderKey, message.last());
        break;
      case "352":
        handleWhoReply(message);
        break;
      case "JOIN":
        handleJoin(message, senderNickname, senderKey, ownMessage);
        break;
      case "PART":
        handlePart(message, senderNickname, senderKey, ownMessage);
        break;
      case "KICK":
        handleKick(message);
        break;
      case "QUIT":
        handleQuit(senderNickname, senderKey);
        break;
      case "NICK":
        handleNicknameChange(message, senderNickname, senderKey, ownMessage);
        break;
      case "353":
        handleNamesBatch(message);
        break;
      case "366":
        handleNamesComplete(message);
        break;
      case "332":
        channel(message.param(1)).topic = message.last();
        break;
      case "331":
        channel(message.param(1)).topic = "";
        break;
      case "TOPIC":
        channel(message.param(0)).topic = message.last();
        break;
      case "PRIVMSG":
        return handleChatMessage(message, senderNickname, ownMessage);
      default:
        handleErrorReply(message);
    }
    return null;
  }

  private void handleServerSupport(IrcMessage message) {
    for (String token : message.parameters) {
      if (token.startsWith("PREFIX=(")) {
        int end = token.indexOf(')');
        if (end > 8 && token.length() - end - 1 == end - 8) {
          prefixModes = token.substring(8, end);
          prefixSymbols = token.substring(end + 1);
        }
      } else if (token.startsWith("CHANMODES=")) channelModes = token.substring(10);
    }
  }

  private void handleListEntry(IrcMessage message) {
    try {
      if (availableChannels.size() < MAX_LIST_CHANNELS)
        availableChannels.put(
            IrcParser.fold(message.param(1)),
            new ServerChannel(
                message.param(1), Integer.parseInt(message.param(2)), message.last()));
    } catch (NumberFormatException ignored) {
    }
  }

  private void handleChannelModesReply(IrcMessage message) {
    Channel channel = channel(message.param(1));
    channel.modes.clear();
    applyModes(
        channel,
        message.param(2),
        message.parameters.subList(
            Math.min(3, message.parameters.size()), message.parameters.size()));
  }

  private void handleWhoReply(IrcMessage message) {
    String whoNick = message.param(5), flags = message.param(6);
    onlineUsers.add(IrcParser.fold(whoNick));
    if (flags.startsWith("G")) awayUsers.putIfAbsent(IrcParser.fold(whoNick), "");
    else if (flags.startsWith("H")) awayUsers.remove(IrcParser.fold(whoNick));
    Channel whoChannel = channels.get(IrcParser.fold(message.param(1)));
    if (whoChannel != null
        && whoChannel.joined
        && whoChannel.members.containsKey(IrcParser.fold(whoNick))) {
      String prefixes = "";
      for (char symbol : prefixSymbols.toCharArray())
        if (flags.indexOf(symbol) >= 0) prefixes += symbol;
      memberAdd(whoChannel, new IrcUser(whoNick, prefixes));
    }
  }

  private void handleJoin(
      IrcMessage message, String senderNickname, String senderKey, boolean ownMessage) {
    onlineUsers.add(senderKey);
    Channel channel = channel(message.last());
    if (ownMessage) {
      channel.joined = true;
      channel.wanted = true;
      channel.joining = false;
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
      channel.namesInProgress = true;
    }
    memberAdd(channel, new IrcUser(senderNickname, ""));
    event(channel, senderNickname, "", "JOIN");
  }

  private void handlePart(
      IrcMessage message, String senderNickname, String senderKey, boolean ownMessage) {
    Channel channel = channel(message.param(0));
    memberRemove(channel, senderKey);
    if (ownMessage) {
      channel.joined = false;
      channel.wanted = false;
      channel.joining = false;
      channel.members.clear();
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
      channel.namesInProgress = false;
    }
    event(channel, senderNickname, "", "PART");
  }

  private void handleKick(IrcMessage message) {
    Channel channel = channel(message.param(0));
    memberRemove(channel, IrcParser.fold(message.param(1)));
    if (IrcParser.fold(message.param(1)).equals(IrcParser.fold(nickname))) {
      channel.joined = false;
      channel.wanted = false;
      channel.members.clear();
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
      channel.namesInProgress = false;
    }
    event(channel, message.param(1), "", "KICK");
  }

  private void handleQuit(String senderNickname, String senderKey) {
    awayUsers.remove(senderKey);
    onlineUsers.remove(senderKey);
    for (Channel ch : channels.values())
      if (ch.members.containsKey(senderKey) || ch.namesBatch.containsKey(senderKey)) {
        memberRemove(ch, senderKey);
        event(ch, senderNickname, "", "QUIT");
      }
  }

  private void handleNicknameChange(
      IrcMessage message, String senderNickname, String senderKey, boolean ownMessage) {
    String newNick = message.last(), key = IrcParser.fold(newNick);
    if (onlineUsers.remove(senderKey)) onlineUsers.add(key);
    if (awayUsers.containsKey(senderKey)) awayUsers.put(key, awayUsers.remove(senderKey));
    for (Channel ch : channels.values()) {
      IrcUser user = ch.members.get(senderKey);
      if (user != null || ch.namesBatch.containsKey(senderKey)) {
        memberRemove(ch, senderKey);
        memberAdd(ch, new IrcUser(newNick, user == null ? "" : user.prefixes));
        event(ch, senderNickname, newNick, "NICK");
      }
    }
    if (ownMessage) nickname = newNick;
    Conversation pm = conversations.remove(senderKey);
    if (pm != null) {
      pm.target = newNick;
      Conversation existing = conversations.remove(key);
      if (existing != null && existing != pm) {
        pm.messages.addAll(existing.messages);
        pm.messages.sort(Comparator.comparingLong(a -> a.timestamp));
        while (pm.messages.size() > Conversation.MAX_HISTORY_MESSAGES) pm.messages.remove(0);
        pm.unread += existing.unread;
      }
      conversations.put(key, pm);
      if (IrcParser.fold(activeTarget).equals(senderKey)) activeTarget = newNick;
    }
  }

  private void handleNamesBatch(IrcMessage message) {
    Channel channel = channel(message.param(2));
    if (!channel.joined) return;
    if (!channel.namesInProgress) {
      channel.namesInProgress = true;
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
    }
    for (String value : message.last().split(" +")) {
      int i = 0;
      while (i < value.length() && prefixSymbols.indexOf(value.charAt(i)) >= 0) i++;
      String nick = value.substring(i).split("!", 2)[0];
      if (!nick.isEmpty() && !channel.namesRemoved.contains(IrcParser.fold(nick))) {
        channel.namesBatch.put(IrcParser.fold(nick), new IrcUser(nick, value.substring(0, i)));
        onlineUsers.add(IrcParser.fold(nick));
      }
    }
  }

  private void handleNamesComplete(IrcMessage message) {
    Channel channel = channel(message.param(1));
    if (!channel.joined) return;
    if (channel.namesInProgress) {
      channel.members.clear();
      channel.members.putAll(channel.namesBatch);
      channel.namesBatch.clear();
      channel.namesRemoved.clear();
      channel.namesInProgress = false;
    }
  }

  private ChatMessage handleChatMessage(
      IrcMessage message, String senderNickname, boolean ownMessage) {
    String target = IrcParser.isChannel(message.param(0)) ? message.param(0) : senderNickname;
    Conversation chat = conversation(target);
    String text = message.last();
    if (text.startsWith("\u0001")) {
      if (text.startsWith("\u0001ACTION ") && text.endsWith("\u0001"))
        text = "• " + text.substring(8, text.length() - 1);
      else return null; // CTCP queries are not displayed as chat or automatically answered.
    }
    if (ownMessage) { // Defensive echo deduplication; we never request echo-message.
      for (int i = chat.messages.size() - 1;
          i >= Math.max(0, chat.messages.size() - ECHO_LOOKBACK_MESSAGES);
          i--) {
        ChatMessage prior = chat.messages.get(i);
        if (prior.outgoing
            && prior.text.equals(text)
            && System.currentTimeMillis() - prior.timestamp < ECHO_WINDOW_MS) return null;
      }
    }
    ChatMessage receivedMessage =
        new ChatMessage(senderNickname, text, ownMessage, ChatMessage.Status.RECEIVED);
    if (!ownMessage && !NicknameMentions.ranges(text, nickname).isEmpty())
      receivedMessage.mentionNick = nickname;
    chat.add(receivedMessage);
    if (!ownMessage && !IrcParser.fold(target).equals(IrcParser.fold(activeTarget))) {
      chat.unread++;
      if (chat.firstUnreadId == 0) chat.firstUnreadId = receivedMessage.id;
    }
    return receivedMessage;
  }

  private void handleErrorReply(IrcMessage message) {
    if (message.command.matches("[45][0-9]{2}")) {
      errorCode = message.command;
      errorDetail = message.last();
      if (message.param(1).equalsIgnoreCase("LIST")) listing = false;
      errorTarget = message.param(1);
      Channel failed = channels.get(IrcParser.fold(errorTarget));
      if (failed != null && !failed.joined) {
        failed.joining = false;
        failed.wanted = false;
      }
      if (Arrays.asList("401", "404", "407", "411", "412", "421", "442", "482")
          .contains(message.command)) {
        Conversation failedChat =
            IrcParser.isChannel(errorTarget)
                ? failed
                : conversations.get(IrcParser.fold(errorTarget));
        if (failedChat != null) {
          for (int i = failedChat.messages.size() - 1; i >= 0; i--) {
            ChatMessage sent = failedChat.messages.get(i);
            if (sent.outgoing
                && System.currentTimeMillis() - sent.timestamp < ERROR_MESSAGE_WINDOW_MS) {
              sent.status = ChatMessage.Status.FAILED;
              break;
            }
          }
        }
      }
    }
  }

  public String role(String target, String nick) {
    Channel channel = channels.get(IrcParser.fold(target));
    IrcUser user = channel == null ? null : channel.members.get(IrcParser.fold(nick));
    if (user != null) {
      for (int i = 0; i < prefixSymbols.length(); i++)
        if (user.prefixes.indexOf(prefixSymbols.charAt(i)) >= 0)
          return String.valueOf(prefixModes.charAt(i));
    }
    return "u";
  }

  public boolean canManage(String target) {
    return "qaoh".contains(role(target, nickname));
  }

  private void applyModes(Channel channel, String flags, List<String> args) {
    boolean adding = true;
    int at = 0;
    String[] groups = channelModes.split(",", -1);
    for (char mode : flags.toCharArray()) {
      if (mode == '+') {
        adding = true;
        continue;
      }
      if (mode == '-') {
        adding = false;
        continue;
      }
      int rank = prefixModes.indexOf(mode);
      boolean consumes =
          rank >= 0
              || (groups.length > 0 && groups[0].indexOf(mode) >= 0)
              || (groups.length > 1 && groups[1].indexOf(mode) >= 0)
              || (adding && groups.length > 2 && groups[2].indexOf(mode) >= 0);
      String arg = consumes && at < args.size() ? args.get(at++) : "";
      if (rank >= 0) {
        if (arg.isEmpty()) continue;
        String key = IrcParser.fold(arg);
        updatePrefix(channel.members, key, prefixSymbols.charAt(rank), adding);
        updatePrefix(channel.namesBatch, key, prefixSymbols.charAt(rank), adding);
      } else if (adding) channel.modes.add(mode);
      else channel.modes.remove(mode);
    }
  }

  private void updatePrefix(Map<String, IrcUser> users, String key, char symbol, boolean adding) {
    IrcUser user = users.get(key);
    if (user == null) return;
    String value = user.prefixes.replace(String.valueOf(symbol), "");
    if (adding) value += symbol;
    String ordered = "";
    for (char prefix : prefixSymbols.toCharArray())
      if (value.indexOf(prefix) >= 0) ordered += prefix;
    users.put(key, user.withPrefixes(ordered));
  }

  private static void memberAdd(Channel channel, IrcUser u) {
    channel.members.put(IrcParser.fold(u.nickname), u);
    if (channel.namesInProgress) {
      channel.namesRemoved.remove(IrcParser.fold(u.nickname));
      channel.namesBatch.put(IrcParser.fold(u.nickname), u);
    }
  }

  private static void memberRemove(Channel channel, String key) {
    channel.members.remove(key);
    channel.namesBatch.remove(key);
    if (channel.namesInProgress) channel.namesRemoved.add(key);
  }

  private static void event(Channel channel, String senderNickname, String extra, String code) {
    channel.add(
        new ChatMessage(
            senderNickname,
            extra,
            false,
            ChatMessage.Status.RECEIVED,
            code,
            System.currentTimeMillis()));
  }
}
